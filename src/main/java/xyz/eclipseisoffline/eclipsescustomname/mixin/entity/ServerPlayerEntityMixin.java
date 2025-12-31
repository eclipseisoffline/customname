package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.entity.ServerPlayerEntityOverrides;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player implements FakeTextDisplayHolder, ServerPlayerEntityOverrides {
    @Shadow
    private Input lastClientInput;

    @Shadow
    public abstract ServerLevel level();

    @Unique
    private int[] fakeTextDisplayIds = new int[0];
    @Unique
    private UUID[] fakeTextDisplayUuids = new UUID[0];

    public ServerPlayerEntityMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initFakeArmorStand(MinecraftServer server, ServerLevel world, GameProfile profile, ClientInformation clientOptions,
                                   CallbackInfo callbackInfo) {
        if (CustomName.getConfig().displayAbovePlayer()) {
            fakeTextDisplayIds = new int[]{EntityAccessor.getCurrentId().incrementAndGet(), EntityAccessor.getCurrentId().incrementAndGet()};
            fakeTextDisplayUuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        }
    }

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    public void getCustomPlayerListName(CallbackInfoReturnable<Component> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(getDisplayName());
    }

    @Inject(method = "setLastClientInput", at = @At("HEAD"))
    public void updateTextDisplays(Input newInput, CallbackInfo callbackInfo) {
        if (fakeTextDisplayIds.length > 0 && newInput.shift() != lastClientInput.shift()) {
            byte flags = (byte) (newInput.shift() ? 0 : 1 << 1); // See through blocks when not sneaking

            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[0],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextDisplayFlags(), flags))));
            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[1],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(newInput, true)))));
        }
    }

    @Override
    public void customName$setFlag(int index, boolean value) {
        // Invisible flag
        if (fakeTextDisplayIds.length > 0 && index == 5) {
            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[0],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, false)))));
            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[1],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, true)))));
        }
    }

    @Override
    public void customName$onStartedTrackingBy(ServerPlayer player) {
        if (fakeTextDisplayIds.length > 0) {
            player.connection.send(new ClientboundAddEntityPacket(fakeTextDisplayIds[0], fakeTextDisplayUuids[0], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0));
            player.connection.send(new ClientboundAddEntityPacket(fakeTextDisplayIds[1], fakeTextDisplayUuids[1], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3.ZERO, 0.0));
            player.connection.send(new ClientboundSetPassengersPacket(this)); // Text display passenger is added through mixin in network handler to increase compatibility with other mods

            player.connection.send(new ClientboundSetEntityDataPacket(fakeTextDisplayIds[0],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, false)),
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.getTranslationData(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.getBillboardData(), (byte) 3), // Centre billboard
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextOpacityData(), (byte) 127),
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextDisplayFlags(), lastClientInput.shift() ? (byte) 0 : (byte) (1 << 1))))); // See through blocks when not sneaking

            player.connection.send(new ClientboundSetEntityDataPacket(fakeTextDisplayIds[1],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, true)),
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.getTranslationData(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.getBillboardData(), (byte) 3), // Centre billboard
                            SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getBackgroundData(), 0))));
        }
    }

    @Override
    public void customName$onStoppedTrackingBy(ServerPlayer player) {
        if (fakeTextDisplayIds.length > 0) {
            player.connection.send(new ClientboundRemoveEntitiesPacket(fakeTextDisplayIds));
        }
    }

    @Override
    public void customName$updateName() {
        if (fakeTextDisplayIds.length > 0) {
            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[0],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, false)))));
            level().getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEntityDataPacket(fakeTextDisplayIds[1],
                    List.of(SynchedEntityData.DataValue.create(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(lastClientInput, true)))));
        }
    }

    @Override
    public int[] customName$getFakeTextDisplayIds() {
        return fakeTextDisplayIds;
    }

    @Unique
    private Component displayNameText(Input input, boolean disappearWhenSneaking) {
        return isInvisible() || (input.shift() && disappearWhenSneaking) ? Component.empty() : getDisplayName();
    }
}
