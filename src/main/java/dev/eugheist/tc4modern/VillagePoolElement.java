package dev.eugheist.tc4modern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Modern road connector around the original procedural wizard tower. The banker uses a modern vanilla house of the
 * village's biome (issue #4) with a Hungry Chest as job site; saves from before 0.17.182 have no template and keep the
 * original procedural banker home.
 */
public final class VillagePoolElement extends StructurePoolElement {
    public static final MapCodec<VillagePoolElement> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.BOOL.fieldOf("wizard").forGetter(e->e.wizard),Codec.STRING.optionalFieldOf("template").forGetter(e->e.template),projectionCodec()).apply(i,VillagePoolElement::new));
    final boolean wizard;final java.util.Optional<String> template;private StructurePoolElement house;
    public boolean wizardType(){return wizard;}
    VillagePoolElement(boolean wizard,StructureTemplatePool.Projection projection){this(wizard,java.util.Optional.empty(),projection);}
    VillagePoolElement(boolean wizard,java.util.Optional<String> template,StructureTemplatePool.Projection projection){super(projection);this.wizard=wizard;this.template=template;}
    /** The vanilla template with its cartography table replaced by the banker's job site. */
    StructurePoolElement house(){
        if(house==null&&template.isPresent()){
            var rule=new net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule(new net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest(Blocks.CARTOGRAPHY_TABLE),net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest.INSTANCE,VillageContent.jobSite(false).defaultBlockState());
            var processors=net.minecraft.core.Holder.direct(new net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList(List.of(new net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor(List.of(rule)))));
            house=StructurePoolElement.single(template.get(),processors).apply(getProjection());
        }
        return house;
    }
    @Override public Vec3i getSize(StructureTemplateManager manager,Rotation rotation){if(house()!=null)return house().getSize(manager,rotation);boolean turn=rotation==Rotation.CLOCKWISE_90||rotation==Rotation.COUNTERCLOCKWISE_90;int x=wizard?8:4,z=wizard?12:6;return new Vec3i(turn?z:x,wizard?12:6,turn?x:z);}
    @Override public BoundingBox getBoundingBox(StructureTemplateManager manager,BlockPos pos,Rotation rotation){if(house()!=null)return house().getBoundingBox(manager,pos,rotation);var first=new BlockPos(0,0,wizard?0:-1).rotate(rotation).offset(pos);var last=new BlockPos(wizard?7:3,wizard?11:5,wizard?11:4).rotate(rotation).offset(pos);return BoundingBox.fromCorners(first,last);}
    @Override public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager manager,BlockPos pos,Rotation rotation,RandomSource random){
        if(house()!=null)return house().getShuffledJigsawBlocks(manager,pos,rotation,random);
        var tag=new CompoundTag();tag.putString("name","minecraft:building_entrance");tag.putString("target","minecraft:building_entrance");tag.putString("pool","minecraft:empty");tag.putString("joint","aligned");tag.putString("final_state","minecraft:air");
        var state=Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION,FrontAndTop.fromFrontAndTop(Direction.NORTH,Direction.UP)).rotate(rotation);
        return List.of(new StructureTemplate.StructureBlockInfo(new BlockPos(wizard?3:1,0,wizard?0:-1).rotate(rotation).offset(pos),state,tag));
    }
    @Override public boolean place(StructureTemplateManager manager,WorldGenLevel world,StructureManager structures,ChunkGenerator generator,BlockPos offset,BlockPos pos,Rotation rotation,BoundingBox box,RandomSource random,LiquidSettings liquids,boolean keepJigsaws){
        if(house()==null)return VillageBuildings.place(world,offset,rotation,box,wizard);
        if(!house().place(manager,world,structures,generator,offset,pos,rotation,box,random,liquids,keepJigsaws))return false;
        // The banker lives beside the job site; only the chunk holding the chest spawns him, so exactly once.
        var b=house().getBoundingBox(manager,offset,rotation);
        if(b.intersects(box))for(var at:BlockPos.betweenClosed(Math.max(b.minX(),box.minX()),Math.max(b.minY(),box.minY()),Math.max(b.minZ(),box.minZ()),Math.min(b.maxX(),box.maxX()),Math.min(b.maxY(),box.maxY()),Math.min(b.maxZ(),box.maxZ())))if(world.getBlockState(at).is(VillageContent.jobSite(false))){
            for(var side:Direction.Plane.HORIZONTAL){var spot=at.relative(side);if(world.isEmptyBlock(spot)&&world.isEmptyBlock(spot.above())){VillageBuildings.spawn(world,false,spot.immutable());break;}}
            break;
        }
        return true;
    }
    @Override public StructurePoolElementType<?> getType(){return VillagePools.TYPE.get();}
    @Override public String toString(){return "TC4Village["+(wizard?"wizard":"banker")+template.map(t->" "+t).orElse("")+"]";}
}
