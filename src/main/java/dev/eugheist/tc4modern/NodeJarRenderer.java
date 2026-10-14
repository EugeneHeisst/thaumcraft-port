package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.blockentity.*;import thaumcraft.api.aspects.*;import thaumcraft.api.nodes.*;
/** A sealed node remains visible without goggles, as in TileJarNodeRenderer. */
public final class NodeJarRenderer implements BlockEntityRenderer<NodeJarEntity>{
 public NodeJarRenderer(BlockEntityRendererProvider.Context context){}
 static void draw(net.minecraft.nbt.CompoundTag snapshot,float partial,PoseStack poses,MultiBufferSource buffers,net.minecraft.world.phys.Vec3 center){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null)return;var aspects=new AspectList();aspects.readFromNBT(snapshot);int t=snapshot.getByte("type"),m=snapshot.contains("modifier")?snapshot.getByte("modifier"):-1;var type=t>=0&&t<NodeType.values().length?NodeType.values()[t]:NodeType.NORMAL;var modifier=m>=0&&m<NodeModifier.values().length?NodeModifier.values()[m]:null;var layers=thaumcraft.common.lib.vis.NodeVisuals.layers(System.nanoTime(),(int)center.x,mc.player.tickCount,0,32,true,aspects,type,modifier);poses.pushPose();poses.translate(.5,.5,.5);poses.scale(.25f,.25f,.25f);AuraNodeRenderer.renderLayers(layers,partial,poses,buffers,center,true);poses.popPose();}
 @Override public void render(NodeJarEntity jar,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){draw(jar.snapshot,partial,poses,buffers,jar.getBlockPos().getCenter());}
}
