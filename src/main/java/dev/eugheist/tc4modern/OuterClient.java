package dev.eugheist.tc4modern;

import java.util.List;
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

/** Coordinate-dependent original stone textures; each face keeps the 1.7.10 selection rule. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class OuterClient {
    private static final ModelProperty<BlockPos> POSITION=new ModelProperty<>();
    private static ModelResourceLocation model(String group,int variant){return ModelResourceLocation.standalone(AlchemyContent.id("block/"+group+"_"+variant));}
    @SubscribeEvent static void additional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){for(String group:List.of("ancient_stone_face","ancient_rock_face","runed_stone_face"))for(int i=0;i<4;i++)event.register(model(group,i));}
    @SubscribeEvent static void bake(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event){
        for(var block:List.of(OuterContent.STONE,OuterContent.NO_SPAWN,OuterContent.ROCK,OuterDevices.TRAP)){
            String group=block==OuterContent.ROCK?"ancient_rock_face":block==OuterDevices.TRAP?"runed_stone_face":"ancient_stone_face";
            BakedModel[] faces=new BakedModel[4];for(int i=0;i<4;i++)faces[i]=event.getModels().get(model(group,i));
            for(var state:block.get().getStateDefinition().getPossibleStates()){var key=BlockModelShaper.stateToModelLocation(state);event.getModels().put(key,new Stone(event.getModels().get(key),faces,block==OuterContent.ROCK));}
        }
    }
    static final class Stone extends BakedModelWrapper<BakedModel>{
        final BakedModel[] faces;final boolean rock;
        Stone(BakedModel base,BakedModel[] faces,boolean rock){super(base);this.faces=faces;this.rock=rock;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return data.derive().with(POSITION,pos.immutable()).build();}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,net.minecraft.client.renderer.RenderType layer){
            var pos=data.get(POSITION);if(pos==null||face==null)return originalModel.getQuads(state,face,random,data,layer);
            int variant;if(rock){variant=switch(face.getAxis()){case Y->Math.abs(pos.getX()%2)+Math.abs(pos.getZ()%2)*2;case Z->Math.abs(pos.getX()%2)+Math.abs(pos.getY()%2)*2;case X->Math.abs(pos.getZ()%2)+Math.abs(pos.getY()%2)*2;};}
            else{String coordinates=""+pos.getX()+pos.getY()+pos.getZ();variant=new java.util.Random(Math.abs(coordinates.hashCode()*100)+1).nextInt(12345+face.ordinal())%4;}
            return faces[variant].getQuads(state,face,random,data,layer);
        }
    }
}
