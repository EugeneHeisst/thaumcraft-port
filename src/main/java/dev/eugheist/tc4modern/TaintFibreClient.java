package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;

/**
 * Original BlockTaintFibreRenderer glowing nodules: variant 0 fibres get, on every attached face, the overlay chosen by
 * BlockTaintFibres.getOverlayBlockTexture (new Random(side+y+x*z): 95 in 100 blank, else taint_over_1..3) at brightness 200,
 * untinted and slightly above the fibre. Config.glowyTaint is the client option general.glowing_taint.
 */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class TaintFibreClient {
    static final ModelProperty<BlockPos> POS=new ModelProperty<>();
    static ModelResourceLocation model(int n){return ModelResourceLocation.standalone(AlchemyContent.id("block/taint_fibres_glow_"+n));}
    @SubscribeEvent static void additional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){for(int n=1;n<=3;n++)event.register(model(n));}
    @SubscribeEvent static void bake(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event){
        BakedModel[] over=new BakedModel[3];for(int n=1;n<=3;n++)over[n-1]=event.getModels().get(model(n));
        for(var state:TC4Modern.TAINT_FIBRES.get().getStateDefinition().getPossibleStates())if(state.getValue(TaintFibresBlock.VARIANT)==0){
            var key=BlockModelShaper.stateToModelLocation(state);event.getModels().put(key,new Glowing(event.getModels().get(key),over));
        }
    }
    /** Overlay index 1..3 for the attached side, or 0 for the original blank icon. */
    public static int overlay(int x,int y,int z,Direction side){var r=new Random(side.get3DDataValue()+y+x*z);return r.nextInt(100)<95?0:r.nextInt(3)+1;}
    static final class Glowing extends BakedModelWrapper<BakedModel> {
        final BakedModel[] over;
        Glowing(BakedModel base,BakedModel[] over){super(base);this.over=over;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return data.derive().with(POS,pos.immutable()).build();}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,RenderType layer){
            var base=originalModel.getQuads(state,face,random,data,layer);var pos=data.get(POS);
            if(face!=null||state==null||pos==null||layer!=null&&layer!=RenderType.translucent()||!WarpClientConfig.glowingTaint())return base;
            List<BakedQuad> out=null;
            for(var side:Direction.values()){if(!state.getValue(TaintFibresBlock.FACES.get(side)))continue;int n=overlay(pos.getX(),pos.getY(),pos.getZ(),side);if(n==0)continue;
                if(out==null)out=new ArrayList<>(base);for(var q:over[n-1].getQuads(state,null,random,data,layer))if(q.getDirection()==side.getOpposite())out.add(q);}
            return out==null?base:out;
        }
    }
}
