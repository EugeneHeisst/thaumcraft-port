package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Conditional ModelResearchTable parts and the original six parchment sheets. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableEntity> {
    private final ModelPart inkwell, scroll, ribbon;
    public ResearchTableRenderer(BlockEntityRendererProvider.Context context) {
        inkwell = part("Inkwell", 0,44, 0,0,0,3,2,3, PartPose.offset(-6,-2,3));
        scroll = part("ScrollTube",0,0,-21,-.5f,-8,8,2,2,PartPose.offsetAndRotation(-2,-2,2,0,10,0));
        ribbon = part("ScrollRibbon",0,4,-15.1f,-.275f,-6.75f,1,2,2,PartPose.offsetAndRotation(-2,-2,2,0,10,0));
    }
    private static ModelPart part(String name,int u,int v,float x,float y,float z,int dx,int dy,int dz,PartPose pose) {
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,dx,dy,dz),pose);
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TC4Modern.RESEARCH_TABLE_ENTITY.get(),ResearchTableRenderer::new);
    }
    @Override public void render(ResearchTableEntity table,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();
        pose.translate(.5,1,.5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        float rotation=switch(table.getBlockState().getValue(ResearchTableBlock.FACING)) {
            case SOUTH -> 90; case WEST -> 180; case NORTH -> 270; default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        if(ScribingToolsItem.isScribe(table.inventory.getItem(0))) {
            inkwell.render(pose,buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/restable.png"))),light,overlay);
            pose.pushPose();
            pose.mulPose(Axis.YN.rotationDegrees(90));pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.translate(-.17,.1,-.15);pose.mulPose(Axis.YP.rotationDegrees(15));pose.scale(.5f,.5f,.5f);
            renderQuill(pose,buffers,light,overlay);
            pose.popPose();
        }
        for(int a=0;a<6;a++) {
            pose.pushPose();pose.translate(.1,-.01-a*.015,.35);
            pose.mulPose(Axis.XN.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(15+a%3*2));
            pose.scale(.5f,.6f,.6f);
            var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/misc/parchment.png")));
            var last=pose.last();
            for(float[] v:new float[][]{{0,1,0,1},{1,1,1,1},{1,0,1,0},{0,0,0,0}})
                out.addVertex(last,v[0],v[1],0).setColor(0xFFFFFFFF).setUv(v[2],v[3]).setOverlay(overlay).setLight(light).setNormal(last,0,0,-1);
            pose.popPose();
        }
        if(table.inventory.getItem(1).is(TC4Modern.RESEARCH_NOTES.get())) {
            var out=buffers.getBuffer(RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/restable2.png")));
            scroll.render(pose,out,light,overlay);
            int color=0x999999;
            var note=NoteData.read(table.inventory.getItem(1));
            if(note!=null) color=note.color;
            pose.pushPose();pose.scale(1.2f,1.2f,1.2f);
            ribbon.render(pose,out,light,overlay,0xFF000000|color);pose.popPose();
        }
        pose.popPose();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ResearchTableEntity table) {
        return new net.minecraft.world.phys.AABB(table.getBlockPos()).inflate(1);
    }
    /** Original ItemRenderer.renderItemIn2D: faces plus one edge strip per texture pixel. */
    private static void renderQuill(PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(AlchemyContent.id("textures/blocks/tablequill.png")));
        var last=pose.last();float depth=.025f;
        face(out,last,light,overlay,0,0,1,new float[][]{{0,0,0,1,1},{1,0,0,0,1},{1,1,0,0,0},{0,1,0,1,0}});
        face(out,last,light,overlay,0,0,-1,new float[][]{{0,1,-depth,1,0},{1,1,-depth,0,0},{1,0,-depth,0,1},{0,0,-depth,1,1}});
        for(int i=0;i<16;i++) {
            float a=i/16f,b=(i+1)/16f,u=1-(i+.5f)/16f,v=(i+.5f)/16f;
            face(out,last,light,overlay,-1,0,0,new float[][]{{a,0,-depth,u,1},{a,0,0,u,1},{a,1,0,u,0},{a,1,-depth,u,0}});
            face(out,last,light,overlay,1,0,0,new float[][]{{b,1,-depth,u,0},{b,1,0,u,0},{b,0,0,u,1},{b,0,-depth,u,1}});
            face(out,last,light,overlay,0,1,0,new float[][]{{0,1-a,0,1,v},{1,1-a,0,0,v},{1,1-a,-depth,0,v},{0,1-a,-depth,1,v}});
            face(out,last,light,overlay,0,-1,0,new float[][]{{0,1-b,-depth,1,v},{1,1-b,-depth,0,v},{1,1-b,0,0,v},{0,1-b,0,1,v}});
        }
    }
    private static void face(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack.Pose pose,int light,int overlay,float nx,float ny,float nz,float[][] vertices) {
        for(float[] v:vertices) out.addVertex(pose,v[0],v[1],v[2]).setColor(0xFFFFFFFF).setUv(v[3],v[4]).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
