package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.network.CustomEntityPassengersPacket;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.Optional;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin implements ServerCommonPacketListener {

    @Inject(method = "send", at = @At("HEAD"))
    public void addFakeArmorStandToPassengerPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        //noinspection ConstantValue
        if (packet instanceof EntityPassengersSetS2CPacket passengersPacket && (Object) this instanceof ServerPlayNetworkHandler playHandler
                && playHandler.player.getWorld().getEntityById(passengersPacket.getEntityId()) instanceof ServerPlayerEntity player) {
            int[] fakeTextDisplays = ((FakeTextDisplayHolder) player).customName$getFakeTextDisplayIds();
            if (fakeTextDisplays.length > 0) {
                ((CustomEntityPassengersPacket) passengersPacket).customName$addPassengers(fakeTextDisplays);
            }
        }
    }

    @Inject(method = "onCustomClickAction", at = @At("HEAD"), cancellable = true)
    public void handleClearName(CustomClickActionC2SPacket packet, CallbackInfo callbackInfo) {
        //noinspection ConstantValue
        if (packet.id().equals(CustomName.CLEAR_NAME_EVENT) && (Object) this instanceof ServerPlayNetworkHandler playNetworkHandler) {
            callbackInfo.cancel();
            packet.payload().ifPresent(element -> NameType.CODEC.parse(NbtOps.INSTANCE, element)
                    .ifSuccess(type -> CustomName.clearPlayerName(null, playNetworkHandler.player, type)));
        }
    }
}
