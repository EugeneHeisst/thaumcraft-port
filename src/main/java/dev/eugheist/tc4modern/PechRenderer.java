package dev.eugheist.tc4modern;
import net.minecraft.client.renderer.entity.*;import net.minecraft.resources.ResourceLocation;
public final class PechRenderer extends MobRenderer<PechEntity,PechModel>{
 public PechRenderer(EntityRendererProvider.Context context){super(context,new PechModel(),.4f);addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this,context.getItemInHandRenderer()));}
 @Override public ResourceLocation getTextureLocation(PechEntity entity){return AlchemyContent.id("textures/models/pech_"+(entity.variant()==1?"thaum":entity.variant()==2?"stalker":"forage")+".png");}
}
