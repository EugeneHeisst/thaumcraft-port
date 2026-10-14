package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Real client HUD and jar label renders, with original preference defaults restored afterwards. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class ClientPreferencesAudit {
    static int ticks,oldDelay,oldMax;static boolean oldBottom,oldCrooked;static volatile boolean ready,restored;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_CLIENT_PREFS FAIL "+text);System.out.println("TC4_CLIENT_PREFS PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.clientPreferencesAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();
        if(ticks==50)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);p.closeContainer();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();
            var base=new BlockPos(32,180,32);for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){p.serverLevel().setBlock(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);for(int y=0;y<4;y++)p.serverLevel().setBlock(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
            var jarPos=base.offset(0,0,2);p.serverLevel().setBlock(jarPos,EssentiaDevices.JAR.get().defaultBlockState(),3);var jar=(WardedJarEntity)p.serverLevel().getBlockEntity(jarPos);jar.state.aspect=Aspect.FIRE;jar.state.amount=32;jar.state.aspectFilter=Aspect.FIRE;jar.state.facing=2;jar.changed();
            var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));for(var aspect:Aspect.getPrimalAspects())WandData.add(wand,aspect,35,true);FociContent.setFocus(wand,new ItemStack(FociContent.FIRE.get()));p.getInventory().setItem(0,wand);p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();p.serverLevel().setDayTime(6000);p.connection.teleport(32.5,180,28.5,0,10);ready=true;
        });
        if(ticks==95){check(ready&&WarpClientConfig.SPEC.isLoaded(),"real client configuration and fixture loaded");mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));mc.options.hideGui=false;oldBottom=WarpClientConfig.dialBottom();oldCrooked=WarpClientConfig.crooked();oldDelay=WarpClientConfig.notificationDelay();oldMax=WarpClientConfig.notificationMax();WarpClientConfig.DIAL_BOTTOM.set(false);WarpClientConfig.CROOKED.set(true);WarpClientConfig.NOTIFICATION_DELAY.set(4000);WarpClientConfig.NOTIFICATION_MAX.set(2);var queue=new NotificationQueue(WarpClientConfig::notificationDelay);queue.add("timing",1000);var entry=queue.update(1000).getFirst();check(entry.expire()==7000&&entry.created()==2000,"configured duration controls queue grace period and quarter-duration arrival");ClientKnowledge.notifications.clear();long now=System.nanoTime()/1000000L;for(int i=1;i<=3;i++)ClientKnowledge.notifications.add("Проверка уведомления "+i,now);}
        if(ticks==125){check(WandHud.lastDialY==0,"actual default wand HUD renders at top-left");check(ResearchNotificationHud.displayed==2,"actual notification HUD respects configured visible maximum two");check(EssentiaRenderers.labelRenderCalls>0,"real labelled jar renderer is active");LiquidDeathAudit.shot(mc,"client-preferences-top.png");WarpClientConfig.DIAL_BOTTOM.set(true);WarpClientConfig.CROOKED.set(false);}
        if(ticks==150){check(WandHud.lastDialY==mc.getWindow().getGuiScaledHeight()-32,"actual original bottom wand dial uses screen height minus32");check(EssentiaRenderers.lastLabelRotation==0,"actual jar labels render straight with crooked disabled");LiquidDeathAudit.shot(mc,"client-preferences-bottom.png");WarpClientConfig.NOTIFICATION_MAX.set(0);}
        if(ticks==170){check(ResearchNotificationHud.displayed==0,"zero notification maximum hides HUD messages");check(ClientKnowledge.notifications.update(System.nanoTime()/1000000L).size()==3,"hiding notifications does not discard the knowledge notification queue");WarpClientConfig.DIAL_BOTTOM.set(oldBottom);WarpClientConfig.CROOKED.set(oldCrooked);WarpClientConfig.NOTIFICATION_DELAY.set(oldDelay);WarpClientConfig.NOTIFICATION_MAX.set(oldMax);ClientKnowledge.notifications.clear();server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(false);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==190){check(restored,"fixture flags and original preferences restored");System.out.println("TC4_CLIENT_PREFS ALL PASS wand placement, original notification timing/limits, zero-visible mode and straight jar labels");mc.stop();}
    }
}
