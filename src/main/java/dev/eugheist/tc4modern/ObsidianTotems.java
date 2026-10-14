package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** BlockCosmeticSolid metadata zero and eight; the charged variant contains a real aura node. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
final class ObsidianTotems {
    static final DeferredHolder<Block,Block> PLAIN=ref("obsidian_totem"),CHARGED=ref("charged_obsidian_totem");
    static DeferredHolder<Block,Block> ref(String name){return DeferredHolder.create(Registries.BLOCK,AlchemyContent.id(name));}
    static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK).strength(30,999).sound(SoundType.STONE).requiresCorrectToolForDrops();}
    static boolean isTotem(BlockState state){return state.is(PLAIN.get())||state.is(CHARGED.get());}
    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.BLOCK,h->{h.register(PLAIN.getId(),new Block(properties()));h.register(CHARGED.getId(),new Charged(properties()));});
        event.register(Registries.ITEM,h->{for(var block:List.of(PLAIN,CHARGED))h.register(block.getId(),new BlockItem(block.get(),new Item.Properties()));});
        event.register(Registries.FEATURE,h->{h.register(AlchemyContent.id("obsidian_totem"),new TotemFeature());h.register(AlchemyContent.id("hilltop_stones"),new HilltopStonesFeature());h.register(AlchemyContent.id("mound"),new MoundFeature());});
    }
    static final class Charged extends AuraNodeBlock {
        Charged(Properties properties){super(properties);}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(Charged::new);}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
        @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player) {
            NodeBlockEffects.destroyed(level,pos);
            if(!player.getAbilities().instabuild&&level.getBlockEntity(pos) instanceof AuraNodeEntity node)NodeBlockEffects.dropWispEssence(level,pos,node.reservoir.aspects);
            return super.playerWillDestroy(level,pos,state,player);
        }
    }
    static final class TotemFeature extends net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> {
        TotemFeature(){super(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC);}
        static boolean free(WorldGenLevel level,BlockPos pos){var state=level.getBlockState(pos);return state.isAir()||state.is(Blocks.SNOW)||state.is(Blocks.SHORT_GRASS);}
        static boolean build(WorldGenLevel level,BlockPos ground,net.minecraft.util.RandomSource random) {
            var state=level.getBlockState(ground);
            if(!(state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.SAND)||state.is(Blocks.DIRT)||state.is(Blocks.STONE)||state.is(Blocks.NETHERRACK)))return false;
            if(!free(level,ground.above()))return false;
            level.setBlock(ground,CrimsonContent.OBSIDIAN_TILE.get().defaultBlockState(),3);
            for(int count=1;count<5&&free(level,ground.above(count));count++) {
                var pos=ground.above(count);level.setBlock(pos,PLAIN.get().defaultBlockState(),3);
                if(count>1&&random.nextInt(4)==0){level.setBlock(pos,CHARGED.get().defaultBlockState(),3);AuraNodeFeature.createRandomNodeAt(level,pos,random,false,true,false);break;}
                if(count==4){var top=ground.above(5);level.setBlock(top,CHARGED.get().defaultBlockState(),3);AuraNodeFeature.createRandomNodeAt(level,top,random,false,true,false);}
            }
            return true;
        }
        @Override public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> context) {
            if(!WorldgenConfig.structures())return false;
            var level=context.level();
            if(level.getLevel().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)return false;
            if(!level.getLevel().dimension().equals(Level.OVERWORLD)||context.random().nextInt(WorldgenConfig.nodeRarity()*33)!=0)return false;
            return placeOnSurface(level,context.origin(),context.random());
        }
        static boolean placeOnSurface(WorldGenLevel level,BlockPos origin,net.minecraft.util.RandomSource random) {
            // func_72940_L is getActualHeight, not sea level.
            var ground=origin.below();if(ground.getY()>=level.getMaxBuildHeight())return false;
            // Original auraGen prevents another structure node in this chunk.
            for(var pos:level.getChunk(ground).getBlockEntitiesPos())if(level.getBlockEntity(pos) instanceof AuraNodeEntity)return false;
            if(level.getBlockState(ground).is(net.minecraft.tags.BlockTags.LEAVES))while(ground.getY()>40&&!level.getBlockState(ground).is(Blocks.GRASS_BLOCK))ground=ground.below();
            if(level.getBlockState(ground).is(Blocks.SNOW)||level.getBlockState(ground).is(Blocks.SHORT_GRASS))ground=ground.below();
            return build(level,ground,random);
        }
    }
}
