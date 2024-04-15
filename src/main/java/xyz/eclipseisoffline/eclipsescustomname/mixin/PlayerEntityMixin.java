package xyz.eclipseisoffline.eclipsescustomname.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType,
            World world) {
        super(entityType, world);
    }

    @Redirect(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getName()Lnet/minecraft/text/Text;"))
    public Text setCustomName(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return PlayerNameManager.getPlayerNameManager(serverPlayer.server)
                    .getFullPlayerName(serverPlayer);
        }
        return player.getName();
    }
}
