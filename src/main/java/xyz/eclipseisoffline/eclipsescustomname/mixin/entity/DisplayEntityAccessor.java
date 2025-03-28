package xyz.eclipseisoffline.eclipsescustomname.mixin.entity;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayEntity.class)
public interface DisplayEntityAccessor {

    @Accessor("TRANSLATION")
    static TrackedData<Vector3f> getTranslationData() {
        throw new AssertionError();
    }

    @Accessor("BILLBOARD")
    static TrackedData<Byte> getBillboardData() {
        throw new AssertionError();
    }

    @Mixin(DisplayEntity.TextDisplayEntity.class)
    interface TextDisplayEntityAccessor {

        @Accessor("TEXT")
        static TrackedData<Text> getTextData() {
            throw new AssertionError();
        }

        @Accessor("BACKGROUND")
        static TrackedData<Integer> getBackgroundData() {
            throw new AssertionError();
        }

        @Accessor("TEXT_OPACITY")
        static TrackedData<Byte> getTextOpacityData() {
            throw new AssertionError();
        }

        @Accessor("TEXT_DISPLAY_FLAGS")
        static TrackedData<Byte> getTextDisplayFlags() {
            throw new AssertionError();
        }
    }
}
