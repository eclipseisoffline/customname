package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Accessor("DATA_TRANSLATION_ID")
    static EntityDataAccessor<Vector3f> getDataTranslationId() {
        throw new AssertionError();
    }

    @Accessor("DATA_BILLBOARD_RENDER_CONSTRAINTS_ID")
    static EntityDataAccessor<Byte> getDataBillboardRenderConstraintsId() {
        throw new AssertionError();
    }

    @Mixin(Display.TextDisplay.class)
    interface TextDisplayAccessor {

        @Accessor("DATA_TEXT_ID")
        static EntityDataAccessor<Component> getDataTextId() {
            throw new AssertionError();
        }

        @Accessor("DATA_BACKGROUND_COLOR_ID")
        static EntityDataAccessor<Integer> getDataBackgroundColorId() {
            throw new AssertionError();
        }

        @Accessor("DATA_TEXT_OPACITY_ID")
        static EntityDataAccessor<Byte> getDataTextOpacityId() {
            throw new AssertionError();
        }

        @Accessor("DATA_STYLE_FLAGS_ID")
        static EntityDataAccessor<Byte> getDataStyleFlagsId() {
            throw new AssertionError();
        }
    }
}
