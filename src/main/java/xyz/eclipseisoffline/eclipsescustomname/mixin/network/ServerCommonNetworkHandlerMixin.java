package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.eclipseisoffline.eclipsescustomname.network.CustomEntityPassengersPacket;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

import java.util.Arrays;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin implements ServerCommonPacketListener {

    @Inject(method = "send", at = @At("HEAD"))
    public void addFakeArmorStandToPassengerPacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo callbackInfo) {
        //noinspection ConstantValue
        if (packet instanceof EntityPassengersSetS2CPacket passengersPacket && (Object) this instanceof ServerPlayNetworkHandler playHandler
                && playHandler.player.getServerWorld().getEntityById(passengersPacket.getEntityId()) instanceof ServerPlayerEntity player) {
            int[] fakeTextDisplays = ((FakeTextDisplayHolder) player).customName$getFakeTextDisplayIds();
            if (fakeTextDisplays.length > 0) {
                ((CustomEntityPassengersPacket) passengersPacket).customName$addPassengers(fakeTextDisplays);
            }
        }
    }
}
