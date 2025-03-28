package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Entity.class)
public interface EntityAccessor {

    @Accessor("CURRENT_ID")
    static AtomicInteger getCurrentId() {
        throw new AssertionError();
    }
}
