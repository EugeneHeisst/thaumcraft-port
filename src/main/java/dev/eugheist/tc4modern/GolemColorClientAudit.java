package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real bell clicks with the order upgrade and real clicks on the golem screen colour strips. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class GolemColorClientAudit {
    static final BlockPos BASE=new BlockPos(192,200,192),MARK=BASE.east(2);
    static int ticks;static StrawGolem golem;static volatile boolean ready,restored;static volatile int serverColor=-9,serverSlot=-9;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_GOLEM_COLOR_CLIENT FAIL "+text);System.out.println("TC4_GOLEM_COLOR_CLIENT PASS "+text);}
    static void click(Minecraft mc){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(MARK).add(0,.5,0),Direction.UP,MARK,false));}
    static void probe(net.minecraft.server.MinecraftServer server){server.execute(()->{var m=golem.markers;serverColor=m.size()==1?m.getCompound(0).getByte("color"):-9-m.size();serverSlot=golem.colors[0];});}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.golemColorClientAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10){if(mc.player.isDeadOrDying())mc.player.respawn();mc.options.renderDistance().set(3);server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-2.5,0,30);});}
        if(ticks==45)server.execute(()->server.getPlayerList().getPlayer(uuid).serverLevel().getEntitiesOfClass(StrawGolem.class,new net.minecraft.world.phys.AABB(BASE).inflate(32)).forEach(e->e.discard()));
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();l.setDayTime(6000);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlock(BASE.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<5;y++)l.setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(BASE)).clearContent();l.setBlock(MARK,Blocks.STONE.defaultBlockState(),3);
            var g=new StrawGolem(Golemancy.ENTITY.get(),l);g.material(GolemMaterial.THAUMIUM,true);g.home=BASE.above();g.homeFace=Direction.UP;g.getEntityData().set(StrawGolem.CORE,(byte)0);g.installUpgrade(4);g.installUpgrade(5);
            g.filters.setItem(0,new ItemStack(Items.COBBLESTONE,8));g.colors[0]=5;g.filters.setItem(1,new ItemStack(Items.DIRT,4));g.moveTo(BASE.getX()-2.5,BASE.getY(),BASE.getZ()+.5,0,0);l.addFreshEntity(g);golem=g;
            var bell=new ItemStack(Golemancy.BELL.get());bell.getItem().interactLivingEntity(bell,p,g,InteractionHand.MAIN_HAND);p.getInventory().clearContent();p.getInventory().setItem(0,bell);p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();
            p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-2.5,0,30);ready=true;
        });
        if(ticks==100){check(ready&&golem.upgrades(4)>0,"golem with the order upgrade and linked bell ready");mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));ClientKnowledge.notifications.clear();click(mc);}
        if(ticks==115){probe(server);}
        if(ticks==120){check(serverColor==-1,"first real bell click marks the face with any colour");click(mc);}
        if(ticks==135){probe(server);}
        if(ticks==140){var shown=ClientKnowledge.notifications.update(System.nanoTime()/1_000_000L);check(serverColor==0,"second real bell click recolours the marker to white");check(shown.stream().anyMatch(n->n.text().contains("White")),"client shows the original marker colour notification ("+shown.stream().map(n->n.text()).toList()+")");}
        if(ticks==150)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.openMenu(golem,b->b.writeVarInt(golem.getId()));});
        if(ticks==175){check(mc.screen instanceof GolemClient.Screen,"real golem screen opened");var menu=((GolemClient.Screen)mc.screen).getMenu();check(menu.colorsEnabled()&&menu.visibleColors[0]==5&&menu.visibleColors[1]==-1,"client menu receives the synchronised slot colours");
            mc.options.hideGui=false;LiquidDeathAudit.shot(mc,"golem-color-strips.png");int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-166)/2;mc.screen.mouseClicked(left+96+18,top+4+5,0);}
        if(ticks==190){probe(server);}
        if(ticks==195){check(serverSlot==6,"real click on the right of the first strip steps the server colour to pink");var menu=((GolemClient.Screen)mc.screen).getMenu();check(menu.visibleColors[0]==6,"strip colour updates on the client");int left=(mc.getWindow().getGuiScaledWidth()-176)/2,top=(mc.getWindow().getGuiScaledHeight()-166)/2;mc.screen.mouseClicked(left+96+2,top+4+5,0);}
        if(ticks==210){probe(server);}
        if(ticks==215){check(serverSlot==5,"real click on the left steps it back to lime");LiquidDeathAudit.shot(mc,"golem-color-strips-after.png");mc.setScreen(null);
            server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);golem.discard();p.getInventory().clearContent();p.setInvulnerable(false);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==235){check(restored,"fixture removed and flags restored");System.out.println("TC4_GOLEM_COLOR_CLIENT ALL PASS real bell recolouring, notification and golem screen colour strips");mc.stop();}
    }
}
