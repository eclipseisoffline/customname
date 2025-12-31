package xyz.eclipseisoffline.eclipsescustomname.entity;

import net.minecraft.server.level.ServerPlayer;

public interface ServerPlayerEntityOverrides {

    void customName$setFlag(int index, boolean value);

    void customName$onStartedTrackingBy(ServerPlayer player);

    void customName$onStoppedTrackingBy(ServerPlayer player);
}
