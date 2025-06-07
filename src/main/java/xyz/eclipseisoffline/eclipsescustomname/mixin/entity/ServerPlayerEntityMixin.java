package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements FakeTextDisplayHolder {
    @Shadow
    private PlayerInput playerInput;

    @Shadow
    public abstract ServerWorld getServerWorld();

    @Unique
    private int[] fakeTextDisplayIds = new int[0];
    @Unique
    private UUID[] fakeTextDisplayUuids = new UUID[0];

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw,
            GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void initFakeArmorStand(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions,
                                   CallbackInfo callbackInfo) {
        if (CustomName.getConfig().displayAbovePlayer()) {
            fakeTextDisplayIds = new int[]{EntityAccessor.getCurrentId().incrementAndGet(), EntityAccessor.getCurrentId().incrementAndGet()};
            fakeTextDisplayUuids = new UUID[]{UUID.randomUUID(), UUID.randomUUID()};
        }
    }

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    public void getCustomPlayerListName(CallbackInfoReturnable<Text> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(getDisplayName());
    }

    @Inject(method = "setPlayerInput", at = @At("HEAD"))
    public void updateTextDisplays(PlayerInput newInput, CallbackInfo callbackInfo) {
        if (fakeTextDisplayIds.length > 0 && newInput.sneak() != playerInput.sneak()) {
            byte flags = (byte) (newInput.sneak() ? 0 : 1 << 1); // See through blocks when not sneaking

            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[0],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextDisplayFlags(), flags))));
            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[1],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(newInput, true)))));
        }
    }

    @Override
    protected void setFlag(int index, boolean value) {
        super.setFlag(index, value);
        // Invisible flag
        if (fakeTextDisplayIds.length > 0 && index == 5) {
            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[0],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, false)))));
            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[1],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, true)))));
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        if (fakeTextDisplayIds.length > 0) {
            player.networkHandler.sendPacket(new EntitySpawnS2CPacket(fakeTextDisplayIds[0], fakeTextDisplayUuids[0], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3d.ZERO, 0.0));
            player.networkHandler.sendPacket(new EntitySpawnS2CPacket(fakeTextDisplayIds[1], fakeTextDisplayUuids[1], getX(), getY(), getZ(),
                    0.0F, 0.0F, EntityType.TEXT_DISPLAY, 0, Vec3d.ZERO, 0.0));
            player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(this)); // Text display passenger is added through mixin in network handler to increase compatibility with other mods

            player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[0],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, false)),
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.getTranslationData(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.getBillboardData(), (byte) 3), // Centre billboard
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextOpacityData(), (byte) 127),
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextDisplayFlags(), playerInput.sneak() ? (byte) 0 : (byte) (1 << 1))))); // See through blocks when not sneaking

            player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[1],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, true)),
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.getTranslationData(), new Vector3f(0.0F, 0.2F, 0.0F)),
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.getBillboardData(), (byte) 3), // Centre billboard
                            DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getBackgroundData(), 0))));
        }
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        if (fakeTextDisplayIds.length > 0) {
            player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(fakeTextDisplayIds));
        }
    }

    @Override
    public void customName$updateName() {
        if (fakeTextDisplayIds.length > 0) {
            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[0],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, false)))));
            getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this, new EntityTrackerUpdateS2CPacket(fakeTextDisplayIds[1],
                    List.of(DataTracker.SerializedEntry.of(DisplayEntityAccessor.TextDisplayEntityAccessor.getTextData(), displayNameText(playerInput, true)))));
        }
    }

    @Override
    public int[] customName$getFakeTextDisplayIds() {
        return fakeTextDisplayIds;
    }

    @Unique
    private Text displayNameText(PlayerInput input, boolean disappearWhenSneaking) {
        return isInvisible() || (input.sneak() && disappearWhenSneaking) ? Text.empty() : getDisplayName();
    }
}
