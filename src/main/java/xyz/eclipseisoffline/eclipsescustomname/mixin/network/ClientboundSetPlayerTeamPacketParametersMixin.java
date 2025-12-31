package xyz.eclipseisoffline.eclipsescustomname.mixin.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;

@Mixin(ClientboundSetPlayerTeamPacket.Parameters.class)
public abstract class ClientboundSetPlayerTeamPacketParametersMixin {

    @WrapOperation(method = "<init>(Lnet/minecraft/world/scores/PlayerTeam;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerTeam;getNameTagVisibility()Lnet/minecraft/world/scores/Team$Visibility;"))
    public Team.Visibility returnNeverIfUsingCustomNames(PlayerTeam instance, Operation<Team.Visibility> original) {
        if (CustomName.getConfig().displayAbovePlayer()) {
            return Team.Visibility.NEVER;
        }
        return original.call(instance);
    }
}
