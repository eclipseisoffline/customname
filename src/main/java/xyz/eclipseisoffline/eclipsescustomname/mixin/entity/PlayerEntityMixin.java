package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

import java.util.Objects;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType,
            World world) {
        super(entityType, world);
    }

    @WrapOperation(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getName()Lnet/minecraft/text/Text;"))
    public Text setCustomName(PlayerEntity player, Operation<Text> original) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return PlayerNameManager.getPlayerNameManager(Objects.requireNonNull(serverPlayer.getServer()), CustomName.getConfig()).getFullPlayerName(serverPlayer);
        }
        return original.call(player);
    }
}
