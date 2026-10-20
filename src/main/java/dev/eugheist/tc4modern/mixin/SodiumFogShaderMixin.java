package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.SodiumFogShaderBridge;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sodium loads shader includes from its classloader, bypassing resource packs. No compile/runtime dependency. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class SodiumFogShaderMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void tc4$exponentialFog(ResourceLocation resource, CallbackInfoReturnable<String> result) {
        if (resource.getNamespace().equals("sodium") && resource.getPath().equals("include/fog.glsl"))
            result.setReturnValue(SodiumFogShaderBridge.apply(result.getReturnValue()));
    }
}
