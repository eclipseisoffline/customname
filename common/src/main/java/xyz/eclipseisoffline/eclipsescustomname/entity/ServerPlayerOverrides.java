package xyz.eclipseisoffline.eclipsescustomname.entity;

import net.minecraft.server.level.ServerPlayer;

public interface ServerPlayerOverrides {

    void customName$setSharedFlag(int index, boolean value);

    void customName$startSeenByPlayer(ServerPlayer player);

    void customName$stopSeenByPlayer(ServerPlayer player);
}
