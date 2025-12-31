package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.eclipseisoffline.eclipsescustomname.entity.ServerPlayerEntityOverrides;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setSharedFlag", at = @At("TAIL"))
    public void runOverrides(int index, boolean value, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$setFlag(index, value);
        }
    }

    @Inject(method = "startSeenByPlayer", at = @At("HEAD"))
    public void runStartTrackOverrides(ServerPlayer player, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$onStartedTrackingBy(player);
        }
    }

    @Inject(method = "stopSeenByPlayer", at = @At("HEAD"))
    public void runStopTrackOverrides(ServerPlayer player, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$onStoppedTrackingBy(player);
        }
    }
}
