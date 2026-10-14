package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Loaded TOML and actual modern block ticks, rather than a second copy of spread rules. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class TaintConfigAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_TAINT_CONFIG FAIL "+text);System.out.println("TC4_TAINT_CONFIG PASS "+text);}
    static final net.minecraft.util.RandomSource RANDOM=new net.minecraft.world.level.levelgen.LegacyRandomSource(1){
        @Override public int nextInt(int bound){return bound==3?1:bound==25?1:0;}
    };
    static void plain(ServerLevel level,BlockPos pos){TaintBiomes.setColumn(level,pos,level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));}
    static void surround(ServerLevel level,BlockPos pos){
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
        for(var d:Direction.Plane.HORIZONTAL)level.setBlock(pos.relative(d),Blocks.STONE.defaultBlockState(),3);
    }
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.taintConfigAudit"))return;
        var server=event.getServer();try{
            check(WarpConfig.SPEC.isLoaded()&&TaintConfig.spreadRate()==0&&!TaintConfig.fromFlux(),"actual TOML loaded zero spread and disabled flux taint");
            var level=server.overworld();var base=new BlockPos(512,180,512);
            for(boolean fibre:new boolean[]{false,true}){
                var pos=base.offset(fibre?32:0,0,0);plain(level,pos);surround(level,pos);
                level.setBlock(pos.north(),TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,2),3);
                level.setBlock(pos.south(),TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,2),3);
                var state=fibre?TC4Modern.TAINT_FIBRES.get().defaultBlockState():TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,1);
                level.setBlock(pos,state,3);TaintConfig.SPREAD.set(0);
                if(fibre)TC4Modern.TAINT_FIBRES.get().tick(state,level,pos,RANDOM);else TC4Modern.TAINT.get().tick(state,level,pos,RANDOM);
                check(!TaintBiomes.isTainted(level,pos),"actual "+(fibre?"fibre":"soil")+" tick cannot spread biome at zero");
                level.setBlock(pos,state,3);TaintConfig.SPREAD.set(1);
                if(fibre)TC4Modern.TAINT_FIBRES.get().tick(state,level,pos,RANDOM);else TC4Modern.TAINT.get().tick(state,level,pos,RANDOM);
                check(TaintBiomes.isTainted(level,pos),"actual "+(fibre?"fibre":"soil")+" tick spreads biome when enabled");
            }
            TaintConfig.SPREAD.set(0);
            var goo=base.offset(64,0,0);plain(level,goo);surround(level,goo);
            var state=TC4Modern.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.AMOUNT,8);
            level.setBlock(goo,state,3);TC4Modern.FLUX_GOO.get().tick(state,level,goo,RANDOM);
            check(level.getBlockState(goo).is(TC4Modern.FLUX_GOO.get())&&!TaintBiomes.isTainted(level,goo),"actual concentrated goo tick preserves goo and plains with flux taint disabled");
            TaintConfig.FROM_FLUX.set(true);TC4Modern.FLUX_GOO.get().tick(state,level,goo,RANDOM);
            check(level.getBlockState(goo).is(TC4Modern.TAINT_FIBRES.get())&&TaintBiomes.isTainted(level,goo),"actual concentrated goo tick creates fibres and tainted biome when enabled");
            TaintConfig.FROM_FLUX.set(false);
            // Disabling biome spread must not freeze growth inside already tainted land.
            var growth=base.offset(96,0,0);surround(level,growth);TaintBiomes.spread(level,growth);
            level.setBlock(growth,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);
            level.setBlock(growth.below(),Blocks.OAK_LOG.defaultBlockState(),3);
            level.setBlock(growth.below().north(),TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,2),3);
            var growthRandom=new net.minecraft.world.level.levelgen.LegacyRandomSource(1){@Override public int nextInt(int bound){return bound==3?1:bound==5?2:1;}};
            TC4Modern.TAINT_FIBRES.get().tick(level.getBlockState(growth),level,growth,growthRandom);
            check(level.getBlockState(growth.below()).is(TC4Modern.TAINT.get()),"zero biome spread retains actual infected-wood conversion inside existing taint");
            System.out.println("TC4_TAINT_CONFIG ALL PASS loaded configuration, real soil/fibre biome ticks, flux goo conversion and existing-biome growth");
        }finally{server.halt(false);}
    }
}
