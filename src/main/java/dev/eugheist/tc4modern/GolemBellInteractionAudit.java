package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real client interaction packets, including Player.interactOn's creative-copy behavior. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class GolemBellInteractionAudit {
    static final BlockPos BASE=new BlockPos(420,121,-90),TARGET=BASE.offset(2,0,0);
    static int ticks,golemId;static volatile boolean ready,creativeMarker,survivalMarker,retrieved;
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_BELL_INTERACT FAIL "+text);System.out.println("TC4_BELL_INTERACT PASS "+text);}
    static StrawGolem golem(Minecraft mc){return (StrawGolem)mc.level.getEntity(golemId);}
    static void select(Minecraft mc){check(golem(mc)!=null,"tracked live golem before right-click");mc.gameMode.interact(mc.player,golem(mc),InteractionHand.MAIN_HAND);}
    static void mark(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(TARGET.getCenter().add(0,0,-.5),Direction.NORTH,TARGET,false));}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.bellInteractionAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-2,0,20);});
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
            l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(BASE).inflate(12)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=3;y++)l.setBlock(BASE.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);l.setBlock(TARGET,Blocks.BARREL.defaultBlockState(),3);
            var g=GolemHarvestAudit.golem(p,GolemMaterial.STRAW,BASE.above(),2);g.setNoAi(true);g.moveTo(BASE.getX()+.5,BASE.getY(),BASE.getZ()+1.5,0,0);golemId=g.getId();
            p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Golemancy.BELL.get()));p.containerMenu.broadcastChanges();ready=true;
        });
        if(ticks==100){mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));}
        if(ticks==120){check(ready&&mc.player.getMainHandItem().is(Golemancy.BELL.get()),"creative fixture ready with unbound bell");select(mc);}
        if(ticks==160)check(GolemBellClient.selected(mc.player.getMainHandItem(),mc)==golem(mc),"creative right-click preserves selection in actual held bell");
        if(ticks==180)mark(mc);
        if(ticks==220)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var g=(StrawGolem)p.serverLevel().getEntity(golemId);creativeMarker=g.markers.size()==1&&NoteData.tag(p.getMainHandItem()).getList("markers",10).size()==1;});
        if(ticks==240){check(creativeMarker&&NoteData.tag(mc.player.getMainHandItem()).getList("markers",10).size()==1,"creative block right-click adds authoritative marker and syncs bell");check(GolemBellClient.homes>0&&GolemBellClient.marks>0&&GolemBellClient.links>0,"home, marker and flowing link rendered");LiquidDeathAudit.shot(mc,"bell-interact-creative.png");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-1.5,0,20);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Golemancy.BELL.get()));((StrawGolem)p.serverLevel().getEntity(golemId)).markers.clear();p.containerMenu.broadcastChanges();});}
        if(ticks==280)select(mc);
        if(ticks==320){check(GolemBellClient.selected(mc.player.getMainHandItem(),mc)==golem(mc),"survival right-click selects live golem");mark(mc);}
        if(ticks==360)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var g=(StrawGolem)p.serverLevel().getEntity(golemId);survivalMarker=g.markers.size()==1&&NoteData.tag(p.getMainHandItem()).getList("markers",10).size()==1;});
        if(ticks==380){check(survivalMarker,"survival block right-click adds marker");LiquidDeathAudit.shot(mc,"bell-interact-survival.png");}
        if(ticks==400)mc.gameMode.attack(mc.player,golem(mc));
        if(ticks==440)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);retrieved=p.serverLevel().getEntity(golemId)==null&&p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(BASE).inflate(6)).stream().anyMatch(e->e.getItem().is(Golemancy.placer(GolemMaterial.STRAW).get()));});
        if(ticks==470){check(retrieved,"left-click retrieval still drops golem item");System.out.println("TC4_BELL_INTERACT ALL PASS");mc.stop();}
    }
}
