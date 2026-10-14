package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Actual bell overlays and a real ritualist renderer at enabled, disabled and higher quality. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class GolemLinkAudit {
    static int ticks,oldQuality;static volatile boolean ready,restored;static long links,marks,homes,beams;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_GOLEM_LINK FAIL "+text);System.out.println("TC4_GOLEM_LINK PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.golemLinkAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;if(ticks==1)mc.options.renderDistance().set(3);var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();
        if(ticks==20)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var b=GolemBellShowcase.BASE;p.connection.teleport(b.getX()+.5,b.getY()+4,b.getZ()-8,0,28);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();});
        if(ticks==150)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.getInventory().selected=0;GolemBellShowcase.setup(p);var base=GolemBellShowcase.BASE;var cleric=CrimsonContent.CLERIC.get().create(p.serverLevel());cleric.moveTo(base.getX()-2,base.getY()+1,base.getZ()-1,0,0);cleric.restrictTo(base,8);cleric.setRitualist(true);cleric.setNoAi(true);p.serverLevel().addFreshEntity(cleric);p.connection.teleport(base.getX()+.5,base.getY()+4,base.getZ()-8,0,28);ready=true;});
        if(ticks==205){check(ready,"original bell fixture and ritualist loaded");mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));mc.options.hideGui=false;oldQuality=WarpClientConfig.GOLEM_LINK_QUALITY.get();WarpClientConfig.GOLEM_LINK_QUALITY.set(16);}
        if(ticks==450){check(GolemBellClient.selected(mc.player.getMainHandItem(),mc)!=null,"client bell has a tracked live golem: "+mc.player.getMainHandItem());check(GolemBellClient.links>0&&GolemBellClient.lastQuality==16,"actual default bell ribbon quality16");check(CrimsonRenderers.CultistRenderer.lineRenders>0&&CrimsonRenderers.CultistRenderer.lastLineQuality==8,"actual cultist ritual beam uses original half quality8");LiquidDeathAudit.shot(mc,"golem-link-quality16.png");WarpClientConfig.GOLEM_LINK_QUALITY.set(3);links=GolemBellClient.links;marks=GolemBellClient.marks;homes=GolemBellClient.homes;beams=CrimsonRenderers.CultistRenderer.lineRenders;}
        if(ticks==530){check(WarpClientConfig.golemLinkQuality()==0&&GolemBellClient.links==links,"original below-four quality disables actual bell ribbons");check(GolemBellClient.marks>marks&&GolemBellClient.homes>homes,"disabling ribbons preserves actual home and dyed-marker overlays");check(CrimsonRenderers.CultistRenderer.lineRenders==beams,"same setting disables actual ritual beams");LiquidDeathAudit.shot(mc,"golem-link-disabled.png");WarpClientConfig.GOLEM_LINK_QUALITY.set(32);}
        if(ticks==610){check(GolemBellClient.links>links&&GolemBellClient.lastQuality==32&&GolemBellClient.lastSegments%32==0,"quality32 restores actual bell ribbon segments");check(CrimsonRenderers.CultistRenderer.lineRenders>beams&&CrimsonRenderers.CultistRenderer.lastLineQuality==16,"quality32 restores actual cultist beam at half quality16");LiquidDeathAudit.shot(mc,"golem-link-quality32.png");WarpClientConfig.GOLEM_LINK_QUALITY.set(oldQuality);server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==800){check(restored,"original client preference restored");System.out.println("TC4_GOLEM_LINK ALL PASS bell ribbons, preserved marker overlays and shared ritual beam quality");mc.stop();}
    }
}
