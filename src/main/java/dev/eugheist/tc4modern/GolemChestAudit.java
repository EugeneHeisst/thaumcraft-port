package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** A real golem fills its home chest in front of the client; the chest lid must open on the client and close again. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class GolemChestAudit {
    static final BlockPos BASE=new BlockPos(144,200,144);
    static int ticks,openTick=-1;static float maxOpen;static volatile boolean ready,restored;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_GOLEM_CHEST FAIL "+text);System.out.println("TC4_GOLEM_CHEST PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.golemChestAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10){if(mc.player.isDeadOrDying())mc.player.respawn();mc.options.renderDistance().set(3);server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+4.5,180,20);});}
        if(ticks==45)server.execute(()->server.getPlayerList().getPlayer(uuid).serverLevel().getEntitiesOfClass(StrawGolem.class,new net.minecraft.world.phys.AABB(BASE).inflate(32)).forEach(e->e.discard()));
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();l.setDayTime(6000);
            for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlock(BASE.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<5;y++)l.setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            l.getEntitiesOfClass(StrawGolem.class,new net.minecraft.world.phys.AABB(BASE).inflate(16)).forEach(e->e.discard());l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);((ChestBlockEntity)l.getBlockEntity(BASE)).clearContent();
            var g=new StrawGolem(Golemancy.ENTITY.get(),l);g.material(GolemMaterial.WOOD,true);g.home=BASE.above();g.homeFace=net.minecraft.core.Direction.UP;g.getEntityData().set(StrawGolem.CORE,(byte)2);g.moveTo(BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+1.5,0,0);g.carried(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG,4));l.addFreshEntity(g);
            p.connection.teleport(BASE.getX()+.5,BASE.getY()+1,BASE.getZ()+4.5,180,20);ready=true;
        });
        if(ready&&ticks>60&&ticks<200&&mc.level.getBlockEntity(BASE) instanceof ChestBlockEntity chest){float open=chest.getOpenNess(1);if(open>maxOpen){maxOpen=open;if(openTick<0)openTick=ticks;}if(openTick>0&&ticks==openTick+4)LiquidDeathAudit.shot(mc,"golem-chest-open.png");}
        if(ticks==200){var chest=(ChestBlockEntity)mc.level.getBlockEntity(BASE);check(ready&&GolemChests.opened>=1,"real gather golem deposit triggered the original chest interaction");check(maxOpen>=.3f,"client chest lid swings open during the five-tick task like the original 0.1-per-tick lid (max openness "+maxOpen+")");check(chest.getOpenNess(1)==0,"client chest lid closed again after the task reset");
            server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var c=(ChestBlockEntity)p.serverLevel().getBlockEntity(BASE);int logs=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(net.minecraft.world.item.Items.OAK_LOG))logs+=c.getItem(i).getCount();p.serverLevel().getEntitiesOfClass(StrawGolem.class,new net.minecraft.world.phys.AABB(BASE).inflate(32)).forEach(e->e.discard());c.clearContent();check(logs==4,"logs really arrived in the chest ("+logs+")");p.setInvulnerable(false);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==220){check(restored,"fixture removed and flags restored");System.out.println("TC4_GOLEM_CHEST ALL PASS original golem chest open animation and close");mc.stop();}
    }
}
