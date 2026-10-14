package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real chunk-rendered taint fibre nodules in a dark room, with the original glowing_taint preference toggled and restored. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class TaintGlowAudit {
    static final BlockPos BASE=new BlockPos(48,200,48);
    static int ticks,glowing,oldTickSpeed;static long litOn,litOff;static boolean oldGlow;static BlockPos lit,plain;static volatile boolean ready,restored;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_TAINT_GLOW FAIL "+text);System.out.println("TC4_TAINT_GLOW PASS "+text);}
    static long bright(Minecraft mc){try(var im=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){long n=0;for(int x=0;x<im.getWidth();x++)for(int y=0;y<im.getHeight();y++){int c=im.getPixelRGBA(x,y);if((c&255)+(c>>8&255)+(c>>16&255)>150)n++;}return n;}}
    static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads(Minecraft mc,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,RenderType layer){
        var model=mc.getBlockRenderer().getBlockModel(state);var data=model.getModelData(mc.level,pos,state,net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
        return model.getQuads(state,null,net.minecraft.util.RandomSource.create(0),data,layer);
    }
    /** Quads added by the nodule overlay: count with glowing_taint on minus count with it off, restoring the current value. */
    static int extra(Minecraft mc,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,RenderType layer){
        boolean was=WarpClientConfig.glowingTaint();WarpClientConfig.GLOWING_TAINT.set(true);int on=quads(mc,pos,state,layer).size();WarpClientConfig.GLOWING_TAINT.set(false);int off=quads(mc,pos,state,layer).size();WarpClientConfig.GLOWING_TAINT.set(was);return on-off;
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.taintGlowAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10){if(mc.player.isDeadOrDying())mc.player.respawn();mc.options.renderDistance().set(3);server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY()+7,BASE.getZ()+.5,0,90);});}
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();var rules=l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING);oldTickSpeed=rules.get();rules.set(0,server);l.setDayTime(18000);
            for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-1;y<=10;y++){var at=BASE.offset(x,y,z);boolean shell=y==-1||y==10||Math.abs(x)==7||Math.abs(z)==7;l.setBlock(at,shell?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);}
            glowing=0;lit=null;plain=null;
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var at=BASE.offset(x,0,z);l.setBlock(at,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);
                boolean inner=Math.abs(x)<=5&&Math.abs(z)<=5;if(TaintFibreClient.overlay(at.getX(),at.getY(),at.getZ(),Direction.DOWN)!=0){glowing++;if(inner&&lit==null)lit=at;}else if(inner&&plain==null)plain=at;}
            p.connection.teleport(BASE.getX()+.5,BASE.getY()+7,BASE.getZ()+.5,0,90);ready=true;
        });
        if(ticks==140){
            check(ready&&WarpClientConfig.SPEC.isLoaded(),"real client configuration and dark fibre room loaded");check(glowing>=2&&lit!=null&&plain!=null,"fixture floor holds "+glowing+" positions with an original non-blank overlay");
            var state=mc.level.getBlockState(lit);check(state.is(TC4Modern.TAINT_FIBRES.get())&&state.getValue(TaintFibresBlock.FACES.get(Direction.DOWN)),"client fibre state is attached to the floor");
                                    oldGlow=WarpClientConfig.glowingTaint();WarpClientConfig.GLOWING_TAINT.set(true);
            check(extra(mc,lit,state,RenderType.translucent())==1&&extra(mc,lit,state,null)==1,"selected position adds exactly one overlay quad on the attached face");check(extra(mc,plain,mc.level.getBlockState(plain),RenderType.translucent())==0,"blank original overlay roll adds nothing");check(extra(mc,lit,state,RenderType.cutout())==0,"other render layers stay untouched");check(extra(mc,lit,state.setValue(TaintFibresBlock.VARIANT,1),RenderType.translucent())==0,"tall fibre variants get no nodules like the original metadata check");
            var quads=quads(mc,lit,state,RenderType.translucent());var quad=quads.getLast();int n=TaintFibreClient.overlay(lit.getX(),lit.getY(),lit.getZ(),Direction.DOWN);
            check(quad.getSprite().contents().name().getPath().endsWith("taint_over_"+n)&&quad.getDirection()==Direction.UP&&quad.getTintIndex()==-1,"overlay quad uses original taint_over_"+n+" icon, faces up and is untinted");
            int light=quad.getVertices()[6];check((light&0xFFFF)==12<<4&&(light>>>16)==0,"overlay quad bakes original brightness two hundred as block light twelve, sky zero");
            int hits=0;for(int i=0;i<20000;i++)if(TaintFibreClient.overlay(i*7-7000,64+i%50,i*13-9000,Direction.values()[i%6])!=0)hits++;check(hits>600&&hits<1400,"overlay roll keeps original five percent frequency ("+hits+"/20000)");
            mc.options.hideGui=true;mc.levelRenderer.allChanged();
        }
        if(ticks==190){litOn=bright(mc);LiquidDeathAudit.shot(mc,"taint-glow-on.png");WarpClientConfig.GLOWING_TAINT.set(false);check(quads(mc,lit,mc.level.getBlockState(lit),RenderType.translucent()).size()==quads(mc,plain,mc.level.getBlockState(plain),RenderType.translucent()).size(),"glowing_taint false removes nodules from the model");mc.levelRenderer.allChanged();}
        if(ticks==240){litOff=bright(mc);LiquidDeathAudit.shot(mc,"taint-glow-off.png");check(litOn>litOff+40,"rendered dark room shows glowing nodules only when enabled (bright pixels "+litOn+" vs "+litOff+")");
            WarpClientConfig.GLOWING_TAINT.set(oldGlow);mc.options.hideGui=false;mc.levelRenderer.allChanged();
            server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING).set(oldTickSpeed,server);p.setInvulnerable(false);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==260){check(restored&&WarpClientConfig.glowingTaint()==oldGlow,"fixture rules and original preference restored");System.out.println("TC4_TAINT_GLOW ALL PASS original nodule selection, brightness, variant gate and glowing_taint toggle");mc.stop();}
    }
}
