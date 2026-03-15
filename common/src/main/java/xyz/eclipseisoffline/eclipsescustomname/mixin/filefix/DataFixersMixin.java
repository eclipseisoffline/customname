package xyz.eclipseisoffline.eclipsescustomname.mixin.filefix;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.filefix.FileFixerUpper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.eclipseisoffline.eclipsescustomname.filefix.CustomNameStorageFileFix;

@Mixin(DataFixers.class)
public abstract class DataFixersMixin {

    @Definition(id = "addSchema", method = "Lnet/minecraft/util/filefix/FileFixerUpper$Builder;addSchema(Lcom/mojang/datafixers/DataFixerBuilder;ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;")
    @Expression("?.addSchema(?, 4772, ?)")
    @ModifyExpressionValue(method = "addFixers", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static Schema addCustomNameFileFixer(Schema original, @Local(argsOnly = true) FileFixerUpper.Builder fileFixerUpper) {
        fileFixerUpper.addFixer(new CustomNameStorageFileFix(original));
        return original;
    }
}
