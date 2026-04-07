package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.CustomNameConfig;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;
import xyz.eclipseisoffline.eclipsescustomname.entity.ServerPlayerOverrides;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements FakeTextDisplayHolder, ServerPlayerOverrides {
    @Unique
    private static final int BACKGROUND_TEXT = 0;
    @Unique
    private static final int FOREGROUND_TEXT = 1;

    @Shadow
    private Input lastClientInput;

    @Shadow
    public abstract ServerLevel level();

    @Shadow
    @Final
    private MinecraftServer server;
    @Unique
    private int[] customName$fakeTextDisplayIds = new int[0];
    @Unique
    private UUID[] customName$fakeTextDisplayUuids = new UUID[0];
    @Unique
    private @Nullable Component customName$cachedDisplayPlayerName = null;

    public ServerPlayerMixin(Level level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initFakeTextDisplay(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation clientInformation,
                                    CallbackInfo callbackInfo) {
        if (CustomName.getConfig().displaySettings().enabled()) {
            customName$fakeTextDisplayIds = new int[]{EntityAccessor.getEntityCounter().incrementAndGet(), EntityAccessor.getEntityCounter().incrementAndGet()};
            customName$fakeTextDisplayUuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        }
    }

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    public void getCustomTabListName(CallbackInfoReturnable<Component> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(getDisplayName());
    }

    @Inject(method = "setLastClientInput", at = @At("HEAD"))
    public void updateTextDisplays(Input newInput, CallbackInfo callbackInfo) {
        if (customName$fakeTextDisplayIds.length > 0 && newInput.shift() != lastClientInput.shift()) {
            byte flags = (byte) (newInput.shift() ? 0 : 1 << 1); // See through blocks when not sneaking

            customName$broadcastTextDisplayData(BACKGROUND_TEXT, List.of(SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataStyleFlagsId(), flags)));
            customName$broadcastTextDisplayData(FOREGROUND_TEXT, List.of(SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextId(),
                    customName$displayNameText(newInput, true, true))));
        }
    }

    @Override
    public void customName$setSharedFlag(int index, boolean value) {
        // Invisible flag
        if (customName$fakeTextDisplayIds.length > 0 && index == 5) {
            customName$updateName(true);
        }
    }

    @Override
    public void customName$startSeenByPlayer(ServerPlayer player) {
        if (customName$fakeTextDisplayIds.length > 0) {
            player.connection.send(new ClientboundAddEntityPacket(customName$fakeTextDisplayIds[0], customName$fakeTextDisplayUuids[0], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0));
            player.connection.send(new ClientboundAddEntityPacket(customName$fakeTextDisplayIds[1], customName$fakeTextDisplayUuids[1], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0));
            player.connection.send(new ClientboundSetPassengersPacket(this)); // Text display passenger is added through mixin in network handler to increase compatibility with other mods

            byte defaultTextOpacity = (byte) CustomName.getConfig().displaySettings().textOpacity();
            byte coveredTextOpacity = (byte) (CustomName.getConfig().displaySettings().textOpacity() / 2);

            player.connection.send(new ClientboundSetEntityDataPacket(customName$fakeTextDisplayIds[BACKGROUND_TEXT],
                    List.of(SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextId(), customName$displayNameText(lastClientInput, false, true)),
                            SynchedEntityData.DataValue.create(DisplayAccessor.getDataTranslationId(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            SynchedEntityData.DataValue.create(DisplayAccessor.getDataBillboardRenderConstraintsId(), (byte) 3), // Centre billboard
                            SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextOpacityId(), coveredTextOpacity),
                            SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataStyleFlagsId(), lastClientInput.shift() ? (byte) 0 : (byte) (1 << 1)), // See through blocks when not sneaking
                            SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataBackgroundColorId(), CustomName.getConfig().displaySettings().backgroundColor()))));

            player.connection.send(new ClientboundSetEntityDataPacket(customName$fakeTextDisplayIds[FOREGROUND_TEXT],
                    List.of(SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextId(), customName$displayNameText(lastClientInput, true, true)),
                            SynchedEntityData.DataValue.create(DisplayAccessor.getDataTranslationId(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            SynchedEntityData.DataValue.create(DisplayAccessor.getDataBillboardRenderConstraintsId(), (byte) 3), // Centre billboard
                            SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextOpacityId(), defaultTextOpacity),
                            SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataBackgroundColorId(), 0))));
        }
    }

    @Override
    public void customName$stopSeenByPlayer(ServerPlayer player) {
        if (customName$fakeTextDisplayIds.length > 0) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(customName$fakeTextDisplayIds));
        }
    }

    @Override
    public void customName$updateName(boolean useCache) {
        if (customName$fakeTextDisplayIds.length > 0) {
            List<SynchedEntityData.DataValue<?>> newData = List.of(SynchedEntityData.DataValue.create(DisplayAccessor.TextDisplayAccessor.getDataTextId(),
                    customName$displayNameText(lastClientInput, false, useCache)));
            customName$broadcastTextDisplayData(BACKGROUND_TEXT, newData);
            customName$broadcastTextDisplayData(FOREGROUND_TEXT, newData);
        }
    }

    @Override
    public int[] customName$getFakeTextDisplayIds() {
        return customName$fakeTextDisplayIds;
    }

    @Unique
    private Component customName$displayNameText(Input input, boolean disappearWhenSneaking, boolean useCache) {
        if (isInvisible() || (input.shift() && disappearWhenSneaking)) {
            return Component.empty();
        } else if (customName$cachedDisplayPlayerName == null || !useCache) {
            customName$cachedDisplayPlayerName = customName$computePlayerNameForDisplay();
        }

        return customName$cachedDisplayPlayerName;
    }

    @Unique
    private Component customName$computePlayerNameForDisplay() {
        PlayerNameManager nameManager = PlayerNameManager.getPlayerNameManager(server);
        List<CustomNameConfig.CustomNameDisplayLine> lines = CustomName.getConfig().displaySettings().lines();
        List<Component> componentLines = new ArrayList<>();
        for (CustomNameConfig.CustomNameDisplayLine line : lines) {
            List<Component> names = new ArrayList<>();
            for (NameType type : line.names()) {
                Component name = nameManager.getPlayerName((ServerPlayer) (Object) this, type);
                if (name != null) {
                    names.add(name);
                }
            }
            Component joinedNames = ComponentUtils.formatList(names, Component.literal(" "));
            if (!joinedNames.getSiblings().isEmpty() || joinedNames.getContents() != PlainTextContents.EMPTY) {
                componentLines.add(joinedNames);
            }
        }
        return CommonComponents.joinLines(componentLines);
    }

    @Unique
    private void customName$broadcastTextDisplayData(int index, List<SynchedEntityData.DataValue<?>> data) {
        customName$broadcast(new ClientboundSetEntityDataPacket(customName$fakeTextDisplayIds[index], data));
    }

    @Unique
    private void customName$broadcast(Packet<ClientGamePacketListener> packet) {
        level().getChunkSource().sendToTrackingPlayers(this, packet);
    }
}
