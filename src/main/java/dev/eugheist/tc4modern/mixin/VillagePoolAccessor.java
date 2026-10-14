package dev.eugheist.tc4modern.mixin;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplatePool.class)
public interface VillagePoolAccessor {
    @Accessor("rawTemplates") List<Pair<StructurePoolElement,Integer>> tc4modern$raw();
    @Mutable @Accessor("rawTemplates") void tc4modern$raw(List<Pair<StructurePoolElement,Integer>> list);
    @Accessor("templates") ObjectArrayList<StructurePoolElement> tc4modern$templates();
    @Accessor("maxSize") void tc4modern$maxSize(int size);
}
