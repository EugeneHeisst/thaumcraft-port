package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.HoverHarness;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep paid hover enabled across landing; vanilla would disable it and send a conflicting ability packet. */
@Mixin(LocalPlayer.class)
public abstract class HoverLandingMixin {
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;onGround()Z",ordinal=3))
    private boolean tc4modern$landing(LocalPlayer player){return player.onGround()&&!HoverHarness.isPaidHover(player);}
}
