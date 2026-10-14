package dev.eugheist.tc4modern.mixin;

import java.util.List;
import dev.eugheist.tc4modern.VillagePoolElement;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Legacy default village size: independently allow zero or one building of each profession. */
@Mixin(targets="net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement$Placer")
public abstract class VillagePieceLimitMixin {
    @Shadow @Final private List<? super PoolElementStructurePiece> pieces;
    @Unique private int tc4modern$wizard=-1;
    @Unique private int tc4modern$banker=-1;
    @Redirect(method="tryPlacingChildren",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getShuffledTemplates(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"),require=2)
    private List<StructurePoolElement> tc4modern$limit(StructureTemplatePool pool,RandomSource random){
        var choices=pool.getShuffledTemplates(random);if(choices.stream().noneMatch(e->e instanceof VillagePoolElement))return choices;
        if(tc4modern$wizard<0){tc4modern$wizard=random.nextInt(2);tc4modern$banker=random.nextInt(2);}
        boolean wizard=pieces.stream().anyMatch(p->p instanceof PoolElementStructurePiece piece&&piece.getElement() instanceof VillagePoolElement element&&element.wizardType());
        boolean banker=pieces.stream().anyMatch(p->p instanceof PoolElementStructurePiece piece&&piece.getElement() instanceof VillagePoolElement element&&!element.wizardType());
        return choices.stream().filter(p->!(p instanceof VillagePoolElement element)||(element.wizardType()?tc4modern$wizard>0&&!wizard:tc4modern$banker>0&&!banker)).toList();
    }
}
