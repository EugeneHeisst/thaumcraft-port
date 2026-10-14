package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import thaumcraft.common.lib.vis.NodeVisuals;
/** Billboard adapter for original node animation layers and atlas UVs. */
public final class AuraNodeRenderer implements BlockEntityRenderer<AuraNodeEntity> {
    public AuraNodeRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(AuraNodeEntity node,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var center=net.minecraft.world.phys.Vec3.atCenterOf(node.getBlockPos());
        var relative=center.subtract(mc.player.getEyePosition());double distance=relative.length();
        // Old FOV setting was normalized: 70 degrees + setting * 40.
        double oldFov=(mc.options.fov().get()-70)/40.0;
        boolean within=mc.player.position().distanceTo(net.minecraft.world.phys.Vec3.atLowerCornerOf(node.getBlockPos()))<2 || (distance<400 && (!mc.options.getCameraType().isFirstPerson() || Math.acos(mc.player.getLookAngle().dot(relative.normalize()))<.44+oldFov/2));
        var helmet=mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        boolean reveals=helmet.getItem() instanceof thaumcraft.api.nodes.IRevealer revealer && revealer.showNodes(helmet,mc.player);
        boolean visible=reveals || (mc.player.getMainHandItem().is(TC4Modern.THAUMOMETER.get()) && within);
        var layers=NodeVisuals.layers(System.nanoTime(),node.getBlockPos().getX(),mc.player.tickCount,distance,reveals?64:visible?48:64,visible,node.reservoir.aspects,node.nodeType,node.reservoir.modifier);
        poses.pushPose();poses.translate(.5,.5,.5);
        renderLayers(layers,partial,poses,buffers,center,visible);
        renderBeam(node,partial,poses,buffers,center);
        poses.popPose();
    }
    static void renderLayers(java.util.List<NodeVisuals.Layer> layers,float partial,PoseStack poses,MultiBufferSource buffers,net.minecraft.world.phys.Vec3 center,boolean visible){
        var mc=Minecraft.getInstance();
        var facing=mc.getEntityRenderDispatcher().cameraOrientation();
        // Legacy player posY included the eye offset. Feet-based rotation tilts each
        // aspect into a different plane, splitting the layers when viewed close up.
        var axis=mc.player.getEyePosition(partial).subtract(center).normalize();

        for(var layer:layers){
            var builder=buffers.getBuffer(NodeRenderTypes.get(layer.additive(),visible));
            var rotation=new Quaternionf().fromAxisAngleRad((float)axis.x,(float)axis.y,(float)axis.z,layer.angle());
            float u0=layer.frame()/32f,u1=(layer.frame()+1)/32f,v0=layer.strip()/32f,v1=(layer.strip()+1)/32f;
            float[][] corners={{-1,-1,u1,v1},{-1,1,u1,v0},{1,1,u0,v0},{1,-1,u0,v1}};
            int c=layer.color(),a=Math.max(0,Math.min(255,(int)(layer.alpha()*255)));
            for(float[] corner:corners){
                var v=new Vector3f(corner[0]*layer.scale(),corner[1]*layer.scale(),0).rotate(facing).rotate(rotation);
                builder.addVertex(poses.last(),v.x,v.y,v.z).setColor((c>>16)&255,(c>>8)&255,c&255,a).setUv(corner[2],corner[3]);
            }
        }
    }
    private void renderBeam(AuraNodeEntity node,float partial,PoseStack poses,MultiBufferSource buffers,net.minecraft.world.phys.Vec3 center){
        var mc=Minecraft.getInstance();var entity=node.getLevel().getEntity(node.drainEntityId);
        if(!(entity instanceof net.minecraft.world.entity.player.Player player) || !player.isUsingItem() || !WandData.isWand(player.getUseItem()))return;
        int ticks=player.getTicksUsingItem();float wobble=net.minecraft.util.Mth.sin(ticks/10f)*10f;
        var offset=new net.minecraft.world.phys.Vec3(-.1,-.1,.5).xRot(-net.minecraft.util.Mth.rotLerp(partial,player.xRotO,player.getXRot())*(float)Math.PI/180).yRot(-net.minecraft.util.Mth.rotLerp(partial,player.yRotO,player.getYRot())*(float)Math.PI/180).yRot(-wobble*.01f).xRot(-wobble*.015f);
        // Legacy local-player posY included its eye offset; modern position is feet for all players.
        var source=player.getPosition(partial).add(offset).add(0,player.getEyeHeight(),0);
        int c=node.beamColor;var builder=buffers.getBuffer(NodeRenderTypes.beam());
        for(var strip:thaumcraft.common.lib.vis.NodeBeam.strips(source,center,System.nanoTime(),ticks))
            for(int i=0;i+3<strip.size();i+=2){
                int[] order={i,i+1,i+3,i+2};
                for(int index:order){var v=strip.get(index);builder.addVertex(poses.last(),v.x(),v.y(),v.z()).setColor((c>>16)&255,(c>>8)&255,c&255,(int)(v.alpha()*255)).setUv(v.u(),v.v());}
            }
    }
    @Override public boolean shouldRenderOffScreen(AuraNodeEntity node){return true;}
}
