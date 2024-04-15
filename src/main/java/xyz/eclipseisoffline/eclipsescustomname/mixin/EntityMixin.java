package xyz.eclipseisoffline.eclipsescustomname.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Redirect(method = "getCommandSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getDisplayName()Lnet/minecraft/text/Text;"))
    public Text checkCalculated(Entity entity) {
        if (entity instanceof ServerPlayerEntity player) {
            // Prevent recursion - player name manager uses a method that uses getCommandSource
            // when applying display name modifications
            if (!PlayerNameManager.getPlayerNameManager(player.server).calculatedFullPlayerName(player)) {
                return entity.getName();
            }
        }
        return entity.getDisplayName();
    }
}
