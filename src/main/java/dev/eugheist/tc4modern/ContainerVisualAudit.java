package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

/** Actual reservoir packets, goggles highlight and close-up golem core rendering. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class ContainerVisualAudit {
    static final BlockPos BASE=new BlockPos(480,121,-90),RES=BASE.offset(3,0,0);
    static final Aspect[] CONTENTS={Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY,Aspect.MAGIC};
    static int ticks;static volatile boolean ready;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_CONTAINER_VISUAL FAIL "+text);System.out.println("TC4_CONTAINER_VISUAL PASS "+text);}
    static void aim(net.minecraft.server.level.ServerPlayer p,BlockPos target){double distance=target.equals(RES)?4.5:2.8;float pitch=(float)Math.toDegrees(Math.atan2(p.getEyeHeight()-.5,distance));p.connection.teleport(target.getX()+.5,target.getY(),target.getZ()+.5-distance,0,pitch);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.containerVisualAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==1){mc.options.renderDistance().set(3);mc.options.fov().set(30);}
        if(ticks==10)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);aim(p,BASE);});
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();p.removeAllEffects();p.getInventory().clearContent();l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,server);
            l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(BASE).inflate(12)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
            for(int x=-5;x<=8;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=3;y++)l.setBlock(BASE.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            var g=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE,0);g.setNoAi(true);g.setYRot(180);g.yBodyRot=180;g.setYHeadRot(180);g.installUpgrade(0);g.installUpgrade(1);
            for(int core=0;core<=11;core++)p.getInventory().setItem(core,new ItemStack(Golemancy.core(core)));
            l.setBlock(RES,EssentiaReservoir.BLOCK.get().defaultBlockState(),3);var reservoir=(EssentiaReservoir.ReservoirEntity)l.getBlockEntity(RES);
            var known=new java.util.HashMap<String,Integer>();for(int i=0;i<CONTENTS.length;i++){reservoir.addToContainer(CONTENTS[i],20-i);known.put(CONTENTS[i].getTag(),1);}
            ResearchKnowledge.discover(p.getPersistentData(),"container_visual_audit",known);ThaumometerItem.sync(p,"");p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));p.containerMenu.broadcastChanges();aim(p,BASE);ready=true;
        });
        if(ticks==160){check(ready,"golem cores and mixed reservoir loaded");LiquidDeathAudit.shot(mc,"golem-core-chest.png");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
        if(ticks==190){LiquidDeathAudit.shot(mc,"golem-core-items.png");mc.options.fov().set(55);mc.setScreen(null);server.execute(()->aim(server.getPlayerList().getPlayer(uuid),RES));GogglesAspectTags.tagscale=0;}
        if(ticks==250){
            check(mc.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(RES),"actual camera ray hits reservoir");
            var reservoir=(EssentiaReservoir.ReservoirEntity)mc.level.getBlockEntity(RES);check(reservoir!=null&&reservoir.essentia.size()==7,"all seven aspect amounts synced to client");
            var shown=GogglesAspectTags.aspects(reservoir);check(shown!=null&&shown.size()==7,"reservoir included in goggles container lookup");
            for(int i=0;i<CONTENTS.length;i++)check(shown.getAmount(CONTENTS[i])==20-i,"exact quantity for "+CONTENTS[i].getTag());
            check(GogglesAspectTags.tagscale>.05,"actual goggles highlight renders mixed aspect rows");LiquidDeathAudit.shot(mc,"reservoir-goggles-on.png");
            server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();p.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(0,p.inventoryMenu.incrementStateId(),5,p.getItemBySlot(EquipmentSlot.HEAD)));});GogglesAspectTags.tagscale=0;
        }
        // Exercise the local highlight equipment gate after the inventory-screen fixture closes.
        if(ticks==270){mc.player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);GogglesAspectTags.tagscale=0;}
        if(ticks==290){check(GogglesAspectTags.tagscale==0,"no aspect popup without goggles");LiquidDeathAudit.shot(mc,"reservoir-goggles-off.png");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));var r=(EssentiaReservoir.ReservoirEntity)p.serverLevel().getBlockEntity(RES);r.essentia=new AspectList();r.sync();p.inventoryMenu.broadcastChanges();p.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(0,p.inventoryMenu.incrementStateId(),5,p.getItemBySlot(EquipmentSlot.HEAD)));});GogglesAspectTags.tagscale=0;}
        if(ticks==310){mc.player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));check(((EssentiaReservoir.ReservoirEntity)mc.level.getBlockEntity(RES)).essentia.size()==0,"empty contents packet received");GogglesAspectTags.tagscale=0;}
        if(ticks==330){check(((EssentiaReservoir.ReservoirEntity)mc.level.getBlockEntity(RES)).essentia.size()==0&&GogglesAspectTags.tagscale==0,"empty reservoir syncs and has no stale aspect popup");System.out.println("TC4_CONTAINER_VISUAL ALL PASS");mc.stop();}
    }
}
