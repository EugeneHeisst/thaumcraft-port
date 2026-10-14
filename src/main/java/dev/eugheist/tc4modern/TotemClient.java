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

/** Original BlockCosmeticSolid.getIcon(world, pos, side): shaded supports and coordinate-selected cap runes. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class TotemClient {
    record Sample(BlockPos pos,boolean above,boolean below){}
    static final ModelProperty<Sample> SAMPLE=new ModelProperty<>();
    static ModelResourceLocation model(int variant){return ModelResourceLocation.standalone(AlchemyContent.id("block/obsidian_totem_face_"+variant));}
    @SubscribeEvent static void additional(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){for(int i=0;i<6;i++)event.register(model(i));}
    @SubscribeEvent static void bake(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event) {
        BakedModel[] faces=new BakedModel[6];for(int i=0;i<6;i++)faces[i]=event.getModels().get(model(i));
        for(var block:List.of(ObsidianTotems.PLAIN,ObsidianTotems.CHARGED))for(var state:block.get().getStateDefinition().getPossibleStates()) {
            var key=BlockModelShaper.stateToModelLocation(state);event.getModels().put(key,new Totem(event.getModels().get(key),faces));
        }
    }
    static final class Totem extends BakedModelWrapper<BakedModel> {
        final BakedModel[] faces;
        Totem(BakedModel base,BakedModel[] faces){super(base);this.faces=faces;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return data.derive().with(SAMPLE,new Sample(pos.immutable(),ObsidianTotems.isTotem(level.getBlockState(pos.above())),ObsidianTotems.isTotem(level.getBlockState(pos.below())))).build();}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,net.minecraft.client.renderer.RenderType layer) {
            var sample=data.get(SAMPLE);if(sample==null||face==null||face.getAxis()==Direction.Axis.Y)return originalModel.getQuads(state,face,random,data,layer);
            var pos=sample.pos();int variant=sample.above()?1:!sample.below()?0:2+Math.abs((face.get3DDataValue()+pos.getX()%4+pos.getZ()%4+pos.getY()%4)%4);
            return faces[variant].getQuads(state,face,random,data,layer);
        }
    }
}
