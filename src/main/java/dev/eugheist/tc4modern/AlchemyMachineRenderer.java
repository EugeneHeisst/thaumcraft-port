package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;

/** Original ModelCentrifuge boxes and TileEssentiaCrystalizerRenderer transforms. */
public final class AlchemyMachineRenderer implements BlockEntityRenderer<AlchemyMachineEntity> {
    static final ModelPart FIXED=part(false),SPIN=part(true);
    static final ObjParts CRYSTALIZER=new ObjParts(AlchemyContent.id("models/crystalizer.obj")),RELAY=new ObjParts(AlchemyContent.id("models/vis_relay.obj"));
    public AlchemyMachineRenderer(BlockEntityRendererProvider.Context c){}
    static ModelPart part(boolean spin){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        if(spin){
            root.addOrReplaceChild("Crossbar",CubeListBuilder.create().texOffs(16,0).mirror().addBox(-4,-1,-1,8,2,2),PartPose.ZERO);
            root.addOrReplaceChild("Dingus1",CubeListBuilder.create().texOffs(0,16).mirror().addBox(4,-3,-2,4,6,4),PartPose.ZERO);
            root.addOrReplaceChild("Dingus2",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-8,-3,-2,4,6,4),PartPose.ZERO);
            root.addOrReplaceChild("Core",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-1.5f,-4,-1.5f,3,8,3),PartPose.ZERO);
        }else{
            root.addOrReplaceChild("Top",CubeListBuilder.create().texOffs(20,16).mirror().addBox(-4,-8,-4,8,4,8),PartPose.ZERO);
            root.addOrReplaceChild("Bottom",CubeListBuilder.create().texOffs(20,16).mirror().addBox(-4,4,-4,8,4,8),PartPose.ZERO);
        }
        return LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(AlchemyMachineEntity t,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
        p.pushPose();p.translate(.5,.5,.5);
        if(!t.state.crystalizer){
            var out=b.getBuffer(RenderType.entityCutout(AlchemyContent.id("textures/models/centrifuge.png")));
            FIXED.render(p,out,light,overlay);p.mulPose(Axis.YP.rotationDegrees(t.state.rotation));SPIN.render(p,out,light,overlay);
        }else{
            switch(t.state.facing){case DOWN->p.mulPose(Axis.XN.rotationDegrees(90));case UP->p.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->p.mulPose(Axis.YP.rotationDegrees(180));case WEST->p.mulPose(Axis.YP.rotationDegrees(90));case EAST->p.mulPose(Axis.YN.rotationDegrees(90));default->{}}
            p.translate(0,0,-.5);
            CRYSTALIZER.renderAll(RenderType.entityCutout(AlchemyContent.id("textures/models/crystalizer.png")),p,b,light,overlay,0xFFFFFFFF);
            int color=0xFF000000|(Math.min(255,(int)(t.state.cr*255))<<16)|(Math.min(255,(int)(t.state.cg*255))<<8)|Math.min(255,(int)(t.state.cb*255));
            for(int q=0;q<4;q++){
                p.pushPose();p.scale(.75f,.75f,.75f);p.mulPose(Axis.ZP.rotationDegrees(90*q));p.translate(.34,0,1.2125);p.mulPose(Axis.ZP.rotationDegrees(t.state.rotation+t.state.speed*partial));
                int ticks=net.minecraft.client.Minecraft.getInstance().player==null?0:net.minecraft.client.Minecraft.getInstance().player.tickCount;
                int bright=50+(int)(150*((float)Math.sin((ticks+partial+q*10)/2f)*.05f+.95f));
                RELAY.render("Crystal",AlchemyContent.id("textures/models/vis_relay.png"),p,b,bright,overlay,color);p.popPose();
            }
        }
        p.popPose();
    }
}
