package dev.eugheist.tc4modern.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Issue #16: some mods call EntityRenderers.register directly from the parallel FMLClientSetupEvent. PROVIDERS is a plain
 * fastutil map, so two such calls racing through a rehash corrupt it (AIOOBE in Object2ObjectOpenHashMap.rehash). Our
 * many entity types make that rehash land in the parallel phase, so serialize every registration.
 */
@Mixin(EntityRenderers.class)
abstract class EntityRenderersLockMixin {
    @WrapMethod(method = "register")
    private static <T extends net.minecraft.world.entity.Entity> void tc4$serialize(EntityType<? extends T> type, EntityRendererProvider<T> provider, Operation<Void> original) {
        synchronized (EntityRenderers.class) {
            original.call(type, provider);
        }
    }
}
