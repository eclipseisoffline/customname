package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.network.CustomEntityPassengersPacket;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.Optional;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonNetworkHandlerMixin implements ServerCommonPacketListener {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
    public void addFakeArmorStandToPassengerPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        //noinspection ConstantValue
        if (packet instanceof ClientboundSetPassengersPacket passengersPacket && (Object) this instanceof ServerGamePacketListenerImpl playHandler
                && playHandler.player.level().getEntity(passengersPacket.getVehicle()) instanceof ServerPlayer player) {
            int[] fakeTextDisplays = ((FakeTextDisplayHolder) player).customName$getFakeTextDisplayIds();
            if (fakeTextDisplays.length > 0) {
                ((CustomEntityPassengersPacket) passengersPacket).customName$addPassengers(fakeTextDisplays);
            }
        }
    }

    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    public void handleClearName(ServerboundCustomClickActionPacket packet, CallbackInfo callbackInfo) {
        //noinspection ConstantValue
        if (packet.id().equals(CustomName.CLEAR_NAME_EVENT) && (Object) this instanceof ServerGamePacketListenerImpl playNetworkHandler) {
            callbackInfo.cancel();
            packet.payload().ifPresent(element -> NameType.CODEC.parse(NbtOps.INSTANCE, element)
                    .ifSuccess(type -> CustomName.clearPlayerName(null, playNetworkHandler.player, type)));
        }
    }
}
