package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("ENTITY_COUNTER")
    static AtomicInteger getCurrentId() {
        throw new AssertionError();
    }
}
