package dev.eugheist.tc4modern;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.lib.vis.BiomeAura;
import thaumcraft.common.lib.vis.NodeGenerator;
/** Modern generation adapter for original wild-node placement and environment samples. */
public final class AuraNodeFeature extends Feature<NoneFeatureConfiguration> {
    public AuraNodeFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        if(!WorldgenConfig.aura()||context.random().nextInt(WorldgenConfig.nodeRarity())!=0)return false;
        var world=context.level();var source=context.random();var pos=context.origin();
        { // Utils.getFirstUncoveredY starts at five in both surface and Nether generation.
            int y=5;while(y+1<world.getMaxBuildHeight()&&!world.isEmptyBlock(new net.minecraft.core.BlockPos(pos.getX(),y+1,pos.getZ())))y++;
            pos=pos.atY(y);if(world.isEmptyBlock(pos.above()))pos=pos.above();
            var raised=pos.above(source.nextInt(4));if(world.getBlockState(raised).isAir()||world.getBlockState(raised).canBeReplaced())pos=raised;
        }
        if(pos.getY()>=world.getMaxBuildHeight() || !world.getBlockState(pos).canBeReplaced())return false;
        return createRandomNodeAt(world,pos,source,false,false,false);
    }
    /** ThaumcraftWorldGenerator.createRandomNodeAt: environment sample of the 11x11x11 cube, then the original node roll. */
    public static boolean createRandomNodeAt(net.minecraft.world.level.WorldGenLevel world,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource source,boolean silverwood,boolean eerie,boolean small){
        var random=new java.util.Random(){@Override public int nextInt(int bound){return source.nextInt(bound);}@Override public boolean nextBoolean(){return source.nextBoolean();}};
        var types=BiomeAspectBridge.types(world.getBiome(pos));int water=0,lava=0,stone=0,foliage=0;
        for(int x=-5;x<=5;x++)for(int y=-5;y<=5;y++)for(int z=-5;z<=5;z++){
            var state=world.getBlockState(pos.offset(x,y,z));
            if(state.getFluidState().is(FluidTags.WATER))water++;
            else if(state.getFluidState().is(FluidTags.LAVA))lava++;
            else if(state.is(Blocks.STONE))stone++;
            if(state.is(BlockTags.LEAVES))foliage++;
        }
        var generated=NodeGenerator.generate(random,BiomeAura.aura(types),()->BiomeAura.tag(types,random),water,lava,stone,foliage,silverwood,eerie,small,WorldgenConfig.specialRarity());
        // Original createNodeAt only places BlockAiry into air; existing TileNode blocks retain their body.
        if(world.isEmptyBlock(pos)&&!world.setBlock(pos,TC4Modern.AURA_NODE.get().defaultBlockState(),3))return false;
        if(!(world.getBlockEntity(pos) instanceof AuraNodeEntity node))return false;
        node.nodeType=generated.type();node.reservoir.modifier=generated.modifier();node.reservoir.setAspects(generated.aspects());node.setChanged();
        return true;
    }
}
