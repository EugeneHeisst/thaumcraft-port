package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class GolemClient {
    static int rendered;
    @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{for(var m:GolemMaterial.values())net.minecraft.client.renderer.item.ItemProperties.register(Golemancy.placer(m).get(),AlchemyContent.id("core"),(stack,level,entity,seed)->NoteData.tag(stack).contains("core")&&NoteData.tag(stack).getByte("core")>=0?1:0);for(var m:GolemMaterial.values())net.minecraft.client.renderer.item.ItemProperties.register(Golemancy.placer(m).get(),AlchemyContent.id("advanced"),(stack,level,entity,seed)->NoteData.tag(stack).contains("advanced")?1:0);});}
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Golemancy.ENTITY.get(),Renderer::new);e.registerEntityRenderer(Golemancy.DART.get(),DartRenderer::new);e.registerEntityRenderer(Golemancy.BOBBER.get(),BobberRenderer::new);}
    @SubscribeEvent static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(Golemancy.MENU.get(),Screen::new);}
    static final class Screen extends AbstractContainerScreen<GolemMenu> {
        Screen(GolemMenu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=166;}
        @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
            g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos,topPos,0,0,imageWidth,imageHeight,256,256);
            if(menu.golem!=null&&menu.golem.core()==4){for(int i=0;i<4;i++){boolean available=i==0||menu.golem.upgrades(4)>0;boolean enabled=(menu.golem.getEntityData().get(StrawGolem.ATTACK_FLAGS)&(1<<i))==0;g.drawString(font,Component.translatable("golem.guard."+i),leftPos+96,topPos+12+i*15,available?(enabled?0xff55ff55:0xffff5555):0xff888888,false);}}else for(int a=0;a<Math.min(6,menu.filterCount-menu.page*6);a++)g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+96+a/2*28,topPos+12+a%2*31,184,menu.golem==null?0:menu.golem.material().ordinal()*24,24,24,256,256);
            if(menu.colorsEnabled())for(int a=0;a<Math.min(6,menu.filterCount-menu.page*6);a++){/* GuiGolem: colour strip with a tinted swatch when the slot has a colour */
                g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+96+a/2*28,topPos+4+a%2*31,72,168,24,12,256,256);int c=menu.visibleColors[a];
                if(c>-1){int rgb=GolemColors.RGB[c];g.setColor((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f,1);g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+105+a/2*28,topPos+7+a%2*31,0,176,6,6,256,256);g.setColor(1,1,1,1);}}
            if(menu.golem!=null)InventoryScreen.renderEntityInInventoryFollowsMouse(g,leftPos+8,topPos+8,leftPos+82,topPos+75,40,.15f,mx,my,menu.golem);
            if(menu.golem!=null&&menu.golem.core()==8){/* GuiGolem: three 8x8 toggles at (42,40/50/60), labels at half scale */g.pose().pushPose();g.pose().translate(0,0,300);for(int i=0;i<3;i++){boolean on=(menu.golem.useFlags()&(1<<i))!=0;g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+42,topPos+40+i*10,8,on?168:176,8,8,256,256);g.pose().pushPose();g.pose().translate(leftPos+53,topPos+42+i*10,0);g.pose().scale(.5f,.5f,1);g.drawString(font,Component.translatable("golem.use."+i+(on?".on":".off")),0,0,0xfdfdfd,false);g.pose().popPose();}g.pose().popPose();}
            if(menu.pages()>1&&(menu.golem==null||menu.golem.core()!=4)){g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+111,topPos+68,0,menu.page>0?200:208,24,8,256,256);g.blit(AlchemyContent.id("textures/gui/guigolem.png"),leftPos+135,topPos+68,24,menu.page+1<menu.pages()?200:208,24,8,256,256);}
            if(menu.golem!=null&&menu.golem.core()==0){g.fill(leftPos+62,topPos+54,leftPos+72,topPos+64,0xff999999);g.drawString(font,menu.golem.fillAny?"+":"=",leftPos+64,topPos+55,0xff000000,false);if(mx>=leftPos+62&&mx<leftPos+72&&my>=topPos+54&&my<topPos+64)g.renderTooltip(font,Component.translatable(menu.golem.fillAny?"golem.fill.any":"golem.fill.precise"),mx,my);}

        }
        @Override public boolean mouseClicked(double x,double y,int button){if(menu.colorsEnabled())for(int a=0;a<Math.min(6,menu.filterCount-menu.page*6);a++){double cx=x-(leftPos+96+a/2*28),cy=y-(topPos+4+a%2*31);if(cy>=0&&cy<12&&cx>=0&&cx<8){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100+a);return true;}if(cy>=0&&cy<12&&cx>=16&&cx<24){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,110+a);return true;}}if(menu.golem!=null&&menu.golem.core()==4&&x>=leftPos+96&&x<leftPos+176&&y>=topPos+12&&y<topPos+72){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,80+(int)(y-topPos-12)/15);return true;}if(menu.pages()>1&&y>=topPos+68&&y<topPos+76&&x>=leftPos+111&&x<leftPos+159){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,x<leftPos+135?66:67);return true;}if(menu.golem!=null&&menu.golem.core()==0&&x>=leftPos+62&&x<leftPos+72&&y>=topPos+54&&y<topPos+64){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);return true;}if(menu.golem!=null&&menu.golem.core()==8&&x>=leftPos+42&&x<leftPos+50&&y>=topPos+40&&y<topPos+68&&(int)(y-topPos-40)%10<8){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,50+(int)(y-topPos-40)/10);return true;}return super.mouseClicked(x,y,button);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override public void render(GuiGraphics g,int mx,int my,float p){super.render(g,mx,my,p);
            /* GuiGolem.drawScreen shows the hovered colour name above the screen; the original gates it on upgrade 5, not the order upgrade 4. */
            if(menu.golem!=null&&GolemMenu.hasInventory(menu.golem.core())&&menu.golem.upgrades(5)>0)for(int a=0;a<Math.min(6,menu.filterCount-menu.page*6);a++){int cx=mx-(leftPos+96+a/2*28),cy=my-(topPos+4+a%2*31);if(cx<0||cy<0||cx>=24||cy>=12)continue;int c=menu.visibleColors[a];String text=c>=0?GolemColors.NAMES[c]:"Any color";g.drawString(font,text,leftPos+133-font.width(text)/2,topPos-6,0xFDFDFD,false);}
            renderTooltip(g,mx,my);}
    }
    /** RenderDart: the vanilla arrow at 0.025625 scale (x squeezed to 75%), tinted 0.5/0.5/0.6. */
    static final class DartRenderer extends EntityRenderer<GolemDart> {
        static final ResourceLocation ARROW=ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");static int rendered;
        DartRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(GolemDart dart){return ARROW;}
        @Override public void render(GolemDart dart,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){rendered++;pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(net.minecraft.util.Mth.lerp(partial,dart.yRotO,dart.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(net.minecraft.util.Mth.lerp(partial,dart.xRotO,dart.getXRot())));float shake=dart.shakeTime-partial;if(shake>0)pose.mulPose(Axis.ZP.rotationDegrees(-net.minecraft.util.Mth.sin(shake*3)*shake));pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.025625f*.75f,.025625f,.025625f);pose.translate(-4,0,0);var out=buffers.getBuffer(RenderType.entityCutout(ARROW));int tint=0xff7f7f99;
            PrimalArrowsClient.Renderer.quad(pose,out,new float[][]{{-7,-2,-2,0,.15625f},{-7,-2,2,.15625f,.15625f},{-7,2,2,.15625f,.3125f},{-7,2,-2,0,.3125f}},tint,light);PrimalArrowsClient.Renderer.quad(pose,out,new float[][]{{-7,2,-2,0,.15625f},{-7,2,2,.15625f,.15625f},{-7,-2,2,.15625f,.3125f},{-7,-2,-2,0,.3125f}},tint,light);
            for(int i=0;i<4;i++){pose.mulPose(Axis.XP.rotationDegrees(90));PrimalArrowsClient.Renderer.quad(pose,out,new float[][]{{-8,-2,0,0,0},{8,-2,0,.5f,0},{8,2,0,.5f,.15625f},{-8,2,0,0,.15625f}},tint,light);}pose.popPose();super.render(dart,yaw,partial,pose,buffers,light);}
    }
    /** RenderGolemBobber: a half-size camera-facing hook sprite and a black sagging line to the golem's right hand. */
    static final class BobberRenderer extends EntityRenderer<GolemBobber> {
        static final ResourceLocation HOOK=ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");static int rendered;
        BobberRenderer(EntityRendererProvider.Context c){super(c);}
        @Override public ResourceLocation getTextureLocation(GolemBobber b){return HOOK;}
        @Override public void render(GolemBobber b,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){rendered++;
            pose.pushPose();pose.scale(.5f,.5f,.5f);pose.mulPose(entityRenderDispatcher.cameraOrientation());var out=buffers.getBuffer(RenderType.entityCutout(HOOK));
            PrimalArrowsClient.Renderer.quad(pose,out,new float[][]{{-.5f,-.5f,0,0,1},{.5f,-.5f,0,1,1},{.5f,.5f,0,1,0},{-.5f,.5f,0,0,0}},0xffffffff,light);pose.popPose();
            var f=b.fisher();if(f!=null){float body=net.minecraft.util.Mth.lerp(partial,f.yBodyRotO,f.yBodyRot)*(float)Math.PI/180;double s=Math.sin(body),c=Math.cos(body);
                double hx=net.minecraft.util.Mth.lerp(partial,f.xo,f.getX())-c*.25-s*.7,hy=net.minecraft.util.Mth.lerp(partial,f.yo,f.getY())+f.getEyeHeight()-.4,hz=net.minecraft.util.Mth.lerp(partial,f.zo,f.getZ())-s*.25+c*.7;
                double bx=net.minecraft.util.Mth.lerp(partial,b.xo,b.getX()),by=net.minecraft.util.Mth.lerp(partial,b.yo,b.getY())+.25,bz=net.minecraft.util.Mth.lerp(partial,b.zo,b.getZ());float dx=(float)(hx-bx),dy=(float)(hy-by),dz=(float)(hz-bz);
                var line=buffers.getBuffer(RenderType.lineStrip());var last=pose.last();for(int i=0;i<=16;i++){float t=i/16f,t2=(i+1)/16f;float x=dx*t,y=dy*(t*t+t)*.5f+.25f,z=dz*t,nx=dx*t2-x,ny=dy*(t2*t2+t2)*.5f+.25f-y,nz=dz*t2-z;float len=net.minecraft.util.Mth.sqrt(nx*nx+ny*ny+nz*nz);if(len<1e-6f)len=1;line.addVertex(last,x,y,z).setColor(0xff000000).setNormal(last,nx/len,ny/len,nz/len);}}
            super.render(b,yaw,partial,pose,buffers,light);}
    }
    static final class Renderer extends MobRenderer<StrawGolem,Model> {
        Renderer(EntityRendererProvider.Context c){super(c,new Model(),.25f);}
        @Override public ResourceLocation getTextureLocation(StrawGolem g){return AlchemyContent.id("textures/models/golem_"+g.material().id()+".png");}
        @Override public void render(StrawGolem g,float yaw,float partial,PoseStack pose,MultiBufferSource b,int light){rendered++;super.render(g,yaw,partial,pose,b,light);}
    }
    /** Original ModelGolem(false) cuboids, UVs, 0.4 scale and triangle-wave walk. */
    static final class Model extends EntityModel<StrawGolem> {
        final ModelPart root,accessories,head,body,rightArm,leftArm,rightLeg,leftLeg;StrawGolem golem;
        Model(){super(RenderType::entityCutoutNoCull);var mesh=new MeshDefinition();var r=mesh.getRoot();
            r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-11,-5.5f,8,9,8),PartPose.offset(0,30,-2));
            r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,40).addBox(-8,-2,-6,16,12,11).texOffs(0,70).addBox(-4.5f,10,-3,9,5,6,new CubeDeformation(.5f)),PartPose.offset(0,30,0));
            r.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(60,21).addBox(-12,-2.5f,-3,4,25,6),PartPose.offset(0,30,0));
            r.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(60,21).mirror().addBox(8,-2.5f,-3,4,25,6),PartPose.offset(0,30,0));
            r.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(37,0).addBox(-3.5f,-3,-3,6,16,5),PartPose.offset(-4,48,0));
            r.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(37,0).mirror().addBox(-3.5f,-3,-3,6,16,5),PartPose.offset(5,48,0));
            root=LayerDefinition.create(mesh,128,128).bakeRoot();head=root.getChild("head");body=root.getChild("body");rightArm=root.getChild("right_arm");leftArm=root.getChild("left_arm");rightLeg=root.getChild("right_leg");leftLeg=root.getChild("left_leg");
            /* ModelGolemAccessories(0,30): head pieces pivot with the head at (0,30,-2), the rest at (0,30,0). */
            var am=new MeshDefinition();var a=am.getRoot();PartPose hp=PartPose.offset(0,30,-2),bp=PartPose.offset(0,30,0);
            a.addOrReplaceChild("fez",CubeListBuilder.create().texOffs(0,94).addBox(-4.5f,-15,-6,9,7,9),hp);
            a.addOrReplaceChild("plate",CubeListBuilder.create().texOffs(32,40).addBox(-6.5f,-1,-7,13,12,13).texOffs(0,44).addBox(-8.5f,-4,-6.5f,3,6,12).texOffs(0,44).mirror().addBox(5.5f,-4,-6.5f,3,6,12),bp);
            a.addOrReplaceChild("hat",CubeListBuilder.create().texOffs(0,110).addBox(-4.5f,-17,-6,9,9,9).texOffs(36,114).addBox(-6.5f,-9,-8,13,1,13),hp);
            a.addOrReplaceChild("glasses",CubeListBuilder.create().texOffs(0,80).addBox(-4.5f,-8,-6,9,4,9),hp);
            a.addOrReplaceChild("visor",CubeListBuilder.create().texOffs(0,70).addBox(-5,-8,-6,10,5,5),hp);
            a.addOrReplaceChild("dartgun",CubeListBuilder.create().texOffs(80,80).addBox(7.9f,7.5f,-3.5f,6,16,7),bp);
            a.addOrReplaceChild("mace",CubeListBuilder.create().texOffs(80,26).addBox(-13,15,-5,6,8,10),bp);
            a.addOrReplaceChild("bowtie",CubeListBuilder.create().texOffs(0,0).addBox(-8.5f,-2,-6.5f,17,4,12),bp);
            a.addOrReplaceChild("jar",CubeListBuilder.create().texOffs(96,56).addBox(-4,-15,-5.5f,8,4,8),hp);
            a.addOrReplaceChild("brain",CubeListBuilder.create().texOffs(96,70).addBox(-3.5f,-14,-5,7,3,7),hp);
            a.addOrReplaceChild("evil",CubeListBuilder.create().texOffs(64,65).addBox(-4,-9,-5.5f,8,7,8),hp);
            accessories=LayerDefinition.create(am,128,128).bakeRoot();
        }
        static final ResourceLocation DECORATION=AlchemyContent.id("textures/models/golem_decoration.png");static int accessoryParts;
        /** ModelGolemAccessories.render: head pieces follow the head, dart gun/mace swing with the left/right arm; bowtie and brain jar are blended. */
        void renderAccessories(PoseStack p,MultiBufferSource buffers,int light,int overlay){String deco=golem.decoration();boolean adv=golem.advanced();if(deco.isEmpty()&&!adv)return;
            for(var name:java.util.List.of("fez","hat","glasses","visor","jar","brain","evil")){var part=accessories.getChild(name);part.xRot=head.xRot;part.yRot=head.yRot;}accessories.getChild("dartgun").xRot=leftArm.xRot;accessories.getChild("mace").xRot=rightArm.xRot;
            var solid=buffers.getBuffer(RenderType.entityCutoutNoCull(DECORATION));p.pushPose();p.scale(.4f,.4f,.4f);
            for(var e:java.util.List.of("R:dartgun","F:fez","H:hat","P:plate","G:glasses","V:visor","M:mace"))if(deco.indexOf(e.charAt(0))>=0){p.pushPose();if(adv&&(e.charAt(0)=='F'||e.charAt(0)=='H'))p.translate(0,-.01f,0);accessories.getChild(e.substring(2)).render(p,solid,light,overlay);accessoryParts++;p.popPose();}
            if(adv){accessories.getChild("brain").render(p,solid,light,overlay);accessoryParts++;if(golem.core()>=0){p.pushPose();p.scale(1.01f,1,1.01f);accessories.getChild("evil").render(p,solid,light,overlay);accessoryParts++;p.popPose();}}
            var blend=buffers.getBuffer(RenderType.entityTranslucent(DECORATION));if(deco.indexOf('B')>=0){accessories.getChild("bowtie").render(p,blend,light,overlay);accessoryParts++;}if(adv){accessories.getChild("jar").render(p,blend,light,overlay);accessoryParts++;}
            p.popPose();}
        static float wave(float a,float period){return (Math.abs(a%period-period*.5f)-period*.25f)/(period*.25f);}
        @Override public void setupAnim(StrawGolem g,float limb,float amount,float age,float yaw,float pitch){golem=g;root.getAllParts().forEach(ModelPart::resetPose);if(g.core()<0){head.xRot=.57595867f;return;}head.yRot=yaw*(float)Math.PI/180;head.xRot=pitch*(float)Math.PI/180;rightLeg.xRot=-1.5f*wave(limb,13)*amount;leftLeg.xRot=1.5f*wave(limb,13)*amount;
            if(g.action>0){rightArm.xRot=leftArm.xRot=-2+1.5f*wave(g.action-(age-g.tickCount),5);}else if(!g.displayCarried().isEmpty())rightArm.xRot=leftArm.xRot=-1;else{rightArm.xRot=(-.2f+1.5f*wave(limb,13))*amount;leftArm.xRot=(-.2f-1.5f*wave(limb,13))*amount;}}
        @Override public void renderToBuffer(PoseStack p,VertexConsumer v,int light,int overlay,int color){
            p.pushPose();p.scale(.4f,.4f,.4f);root.render(p,v,light,overlay,color);p.popPose();
            var buffers=Minecraft.getInstance().renderBuffers().bufferSource();if(golem==null)return;
            if(golem.getHealth()<golem.getMaxHealth()){p.pushPose();p.scale(.4f,.4f,.4f);int alpha=(int)(255*(1-golem.getHealth()/golem.getMaxHealth()));root.render(p,buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/golem_damage.png"))),light,overlay,alpha<<24|0xffffff);p.popPose();}
            renderAccessories(p,buffers,light,overlay);if(golem.core()>=0){p.pushPose();p.translate(0,.78,golem.deco('P')?-.185:-.155);p.mulPose(Axis.XP.rotationDegrees(180));p.scale(.17f,.17f,.17f);Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Golemancy.core(golem.core())),ItemDisplayContext.FIXED,light,overlay,p,buffers,golem.level(),0);p.popPose();}
            int slot=0,total=golem.upgradeCount();for(int i=0;i<6;i++)for(int n=0;n<golem.upgrades(i);n++){p.pushPose();p.translate((slot++-(total-1)*.5)*.1,.94,-.16);p.mulPose(Axis.XP.rotationDegrees(180));p.scale(.09f,.09f,.09f);Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Golemancy.upgrade(i)),ItemDisplayContext.FIXED,light,overlay,p,buffers,golem.level(),0);p.popPose();}
            if(!golem.displayCarried().isEmpty()){p.pushPose();p.translate(0,1,-.5);p.mulPose(Axis.XP.rotationDegrees(180));p.scale(.4f,.4f,.4f);if(golem.core()==6){/* RenderGolemBase: whole jar model at .4*(.5+min(64,carry)/128), held before the chest. */float jar=.5f+Math.min(64,golem.carryLimit())/128f;p.scale(jar,jar,jar);p.translate(0,.45,0);Minecraft.getInstance().getItemRenderer().renderStatic(golem.displayCarried(),ItemDisplayContext.NONE,light,overlay,p,buffers,golem.level(),0);}else Minecraft.getInstance().getItemRenderer().renderStatic(golem.displayCarried(),ItemDisplayContext.FIXED,light,overlay,p,buffers,golem.level(),0);p.popPose();}
        }
    }
}
