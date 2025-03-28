package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import xyz.eclipseisoffline.eclipsescustomname.network.CustomEntityPassengersPacket;

@Mixin(EntityPassengersSetS2CPacket.class)
public abstract class EntityPassengersSetS2CPacketMixin implements Packet<ClientPlayPacketListener>, CustomEntityPassengersPacket {

    @Shadow
    @Final
    @Mutable
    private int[] passengerIds;

    @Override
    public void customName$addPassengers(int[] newIds) {
        int[] newPassengers = new int[passengerIds.length + newIds.length];
        System.arraycopy(passengerIds, 0, newPassengers, 0, passengerIds.length);
        System.arraycopy(newIds, 0, newPassengers, passengerIds.length, newIds.length);
        passengerIds = newPassengers;
    }
}
