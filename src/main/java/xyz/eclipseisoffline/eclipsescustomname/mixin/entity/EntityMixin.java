package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.eclipseisoffline.eclipsescustomname.entity.ServerPlayerEntityOverrides;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setSneaking", at = @At("HEAD"))
    public void runOverrides(boolean sneaking, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$setSneaking(sneaking);
        }
    }

    @Inject(method = "setFlag", at = @At("TAIL"))
    public void runOverrides(int index, boolean value, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$setFlag(index, value);
        }
    }

    @Inject(method = "onStartedTrackingBy", at = @At("HEAD"))
    public void runStartTrackOverrides(ServerPlayerEntity player, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$onStartedTrackingBy(player);
        }
    }

    @Inject(method = "onStoppedTrackingBy", at = @At("HEAD"))
    public void runStopTrackOverrides(ServerPlayerEntity player, CallbackInfo callbackInfo) {
        if (this instanceof ServerPlayerEntityOverrides overrides) {
            overrides.customName$onStoppedTrackingBy(player);
        }
    }
}
