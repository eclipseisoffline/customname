package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import xyz.eclipseisoffline.eclipsescustomname.network.CustomEntityPassengersPacket;

@Mixin(ClientboundSetPassengersPacket.class)
public abstract class ClientboundSetPassengersPacketMixin implements Packet<ClientGamePacketListener>, CustomEntityPassengersPacket {

    @Shadow
    @Final
    @Mutable
    private int[] passengers;

    @Override
    public void customName$addPassengers(int[] newIds) {
        int[] newPassengers = new int[passengers.length + newIds.length];
        System.arraycopy(passengers, 0, newPassengers, 0, passengers.length);
        System.arraycopy(newIds, 0, newPassengers, passengers.length, newIds.length);
        passengers = newPassengers;
    }
}
