package xyz.eclipseisoffline.eclipsescustomname.entity;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ServerPlayerEntityOverrides {

    void customName$setSneaking(boolean sneaking);

    void customName$setFlag(int index, boolean value);

    void customName$onStartedTrackingBy(ServerPlayerEntity player);

    void customName$onStoppedTrackingBy(ServerPlayerEntity player);
}
