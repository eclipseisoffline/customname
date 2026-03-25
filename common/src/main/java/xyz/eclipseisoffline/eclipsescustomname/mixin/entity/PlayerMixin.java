package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> type,
                          Level level) {
        super(type, level);
    }

    @ModifyArg(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"))
    public Component applyCustomName(Component original) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayer player) {
            return PlayerNameManager.getPlayerNameManager(player.level().getServer()).getFullPlayerName(player);
        }
        return original;
    }
}
