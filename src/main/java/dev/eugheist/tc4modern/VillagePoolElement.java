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

/** Modern road connector around original procedural village components. */
public final class VillagePoolElement extends StructurePoolElement {
    public static final MapCodec<VillagePoolElement> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.BOOL.fieldOf("wizard").forGetter(e->e.wizard),projectionCodec()).apply(i,VillagePoolElement::new));
    final boolean wizard;
    public boolean wizardType(){return wizard;}
    VillagePoolElement(boolean wizard,StructureTemplatePool.Projection projection){super(projection);this.wizard=wizard;}
    @Override public Vec3i getSize(StructureTemplateManager manager,Rotation rotation){boolean turn=rotation==Rotation.CLOCKWISE_90||rotation==Rotation.COUNTERCLOCKWISE_90;int x=wizard?8:4,z=wizard?12:6;return new Vec3i(turn?z:x,wizard?12:6,turn?x:z);}
    @Override public BoundingBox getBoundingBox(StructureTemplateManager manager,BlockPos pos,Rotation rotation){var first=new BlockPos(0,0,wizard?0:-1).rotate(rotation).offset(pos);var last=new BlockPos(wizard?7:3,wizard?11:5,wizard?11:4).rotate(rotation).offset(pos);return BoundingBox.fromCorners(first,last);}
    @Override public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager manager,BlockPos pos,Rotation rotation,RandomSource random){
        var tag=new CompoundTag();tag.putString("name","minecraft:building_entrance");tag.putString("target","minecraft:building_entrance");tag.putString("pool","minecraft:empty");tag.putString("joint","aligned");tag.putString("final_state","minecraft:air");
        var state=Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION,FrontAndTop.fromFrontAndTop(Direction.NORTH,Direction.UP)).rotate(rotation);
        return List.of(new StructureTemplate.StructureBlockInfo(new BlockPos(wizard?3:1,0,wizard?0:-1).rotate(rotation).offset(pos),state,tag));
    }
    @Override public boolean place(StructureTemplateManager manager,WorldGenLevel world,StructureManager structures,ChunkGenerator generator,BlockPos offset,BlockPos pos,Rotation rotation,BoundingBox box,RandomSource random,LiquidSettings liquids,boolean keepJigsaws){return VillageBuildings.place(world,offset,rotation,box,wizard);}
    @Override public StructurePoolElementType<?> getType(){return VillagePools.TYPE.get();}
    @Override public String toString(){return "TC4Village["+(wizard?"wizard":"banker")+"]";}
}
