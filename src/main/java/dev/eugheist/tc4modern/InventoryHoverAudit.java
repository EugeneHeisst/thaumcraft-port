package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class InventoryHoverAudit {
    static int ticks,samples,grounded;static volatile boolean prepared,delayed,serverScan;
    static final BlockPos POS=new BlockPos(35,120,35);
    static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_INV_HOVER FAIL "+s);System.out.println("TC4_INV_HOVER PASS "+s);}
    static void prepare(ServerPlayer p){
        p.removeAllEffects();p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.setGameMode(GameType.SURVIVAL);p.closeContainer();p.getInventory().clearContent();
        p.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMOMETER.get()));p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE));
        p.getPersistentData().remove(ResearchKnowledge.KEY);ResearchKnowledge.initialize(p.getPersistentData());ThaumometerItem.sync(p,"");
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=3;y++)p.serverLevel().setBlock(POS.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        p.setItemSlot(EquipmentSlot.CHEST,HoverHarnessAudit.fueled(p,20));p.getAbilities().mayfly=false;p.connection.teleport(POS.getX()+.5,POS.getY(),POS.getZ()+.5,0,0);HoverHarness.set(p,true);prepared=true;
    }
    static void click(Minecraft mc,int slot){mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,slot,0,ClickType.PICKUP,mc.player);}
    static void hover(Minecraft mc,int slot){
        var screen=(InventoryScreen)mc.screen;var s=mc.player.containerMenu.getSlot(slot);
        double x=(screen.width-176)/2.0+s.x+8,y=(screen.height-166)/2.0+s.y+8;
        if(screen.getRecipeBookComponent().isVisible()){screen.getRecipeBookComponent().toggleVisibility();mc.setScreen(new InventoryScreen(mc.player));screen=(InventoryScreen)mc.screen;x=(screen.width-176)/2.0+s.x+8;y=(screen.height-166)/2.0+s.y+8;}
        try{var callback=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);callback.setAccessible(true);callback.invoke(mc.mouseHandler,mc.getWindow().getWindow(),x*mc.getWindow().getScreenWidth()/screen.width,y*mc.getWindow().getScreenHeight()/screen.height);}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("tc4modern.inventoryHoverAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();
        if(ticks==40)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));
        if(ticks==65)System.out.println("TC4_INV_HOVER state alive="+mc.player.isAlive()+" active="+HoverHarness.active(mc.player)+" flying="+mc.player.getAbilities().flying+" armor="+HoverHarness.chest(mc.player)+" fuel="+HoverHarness.fuel(HoverHarness.jar(HoverHarness.chest(mc.player),mc.player.registryAccess())));
        if(ticks>=65&&ticks<=105){
            checkFrame(mc);samples++;
        }
        if(ticks==110){check(prepared&&samples==41&&grounded>10,"paid hover stays flying on ground across41frames without FOV oscillation: prepared="+prepared+" samples="+samples+" grounded="+grounded);mc.setScreen(new InventoryScreen(mc.player));}
        if(ticks==120){click(mc,36);hover(mc,37);}
        if(ticks==126)hover(mc,37);
        if(ticks==128){
            check(mc.player.containerMenu.getCarried().is(TC4Modern.THAUMOMETER.get()),"real inventory click holds thaumometer on cursor");
            check(((InventoryScreen)mc.screen).getSlotUnderMouse()!=null&&((InventoryScreen)mc.screen).getSlotUnderMouse().index==37,"real cursor hovers inventory target: hovered="+(((InventoryScreen)mc.screen).getSlotUnderMouse()==null?"null":((InventoryScreen)mc.screen).getSlotUnderMouse().index)+" mouse="+mc.mouseHandler.xpos()+","+mc.mouseHandler.ypos());
            // Make the client finish arrive before the server's twenty ticks, reproducing lag/tick skew deterministically.
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);check(p.getPersistentData().contains("tc4_inventory_start"),"real hover sends start packet");p.getPersistentData().putLong("tc4_inventory_start",p.level().getGameTime()+15);delayed=true;});
        }
        if(ticks==180){
            String key=ScanTarget.item(new ItemStack(Items.COBBLESTONE),mc.level).knowledgeKey();
            check(delayed&&ClientKnowledge.knowledge.getCompound("scanned").getBoolean(key),"early finish eventually scans and syncs aspects instead of stalling");
            LiquidDeathAudit.shot(mc,"inventory-scan-fixed.png");
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);check(p.getPersistentData().getCompound(ResearchKnowledge.KEY).getCompound("scanned").getBoolean(key),"server records inventory scan");serverScan=true;});
        }
        if(ticks==190){click(mc,36);mc.player.closeContainer();mc.setScreen(null);}
        if(ticks==200){
            check(serverScan,"server scan check completed");
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.inventoryMenu.setCarried(new ItemStack(TC4Modern.THAUMOMETER.get()));p.inventoryMenu.getSlot(37).set(new ItemStack(Items.DIRT));var data=p.getPersistentData();data.putInt("tc4_inventory_menu",0);data.putInt("tc4_inventory_slot",37);data.putString("tc4_inventory_target",ScanTarget.item(new ItemStack(Items.DIRT),p.level()).identity());data.putLong("tc4_inventory_start",p.level().getGameTime()-25);data.putBoolean("tc4_inventory_finish",true);p.inventoryMenu.setCarried(ItemStack.EMPTY);InventoryScanPayload.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));check(!data.contains("tc4_inventory_start")&&!data.getCompound(ResearchKnowledge.KEY).getCompound("scanned").getBoolean(ScanTarget.item(new ItemStack(Items.DIRT),p.level()).knowledgeKey()),"removing scanner cancels pending scan");HoverHarness.set(p,false);});
        }
        if(ticks==225){check(!HoverHarness.active(mc.player)&&!mc.player.getAbilities().flying,"switching off harness restores grounded vanilla state");LiquidDeathAudit.shot(mc,"hover-landing-fixed.png");}
        if(ticks==230){mc.player.getInventory().setItem(0,new ItemStack(Levitator.BLOCK.get()));mc.player.getInventory().selected=0;var model=mc.getItemRenderer().getModel(mc.player.getMainHandItem(),mc.level,mc.player,0);check(Math.abs(model.getTransforms().firstPersonRightHand.scale.x()-.4f)<.001f&&Math.abs(model.getTransforms().thirdPersonRightHand.scale.x()-.375f)<.001f,"levitator inherits standard first/third person block scale");}
        if(ticks==245)LiquidDeathAudit.shot(mc,"levitator-hand-fixed.png");
        if(ticks==255){System.out.println("TC4_INV_HOVER ALL PASS");mc.stop();}
    }
    @SubscribeEvent static void land(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event){if(Boolean.getBoolean("tc4modern.inventoryHoverAuditOnly")&&event.getEntity() instanceof net.minecraft.client.player.LocalPlayer p&&ticks>=63&&ticks<=105){p.setPos(POS.getX()+.5,POS.getY(),POS.getZ()+.5);p.setDeltaMovement(0,-.01,0);p.setOnGround(true);}}
    static void checkFrame(Minecraft mc){
        if(mc.player.onGround())grounded++;
        if(!mc.player.getAbilities().flying)throw new IllegalStateException("TC4_INV_HOVER FAIL landing disabled paid flight at tick "+ticks);
        float fov=mc.player.getFieldOfViewModifier();if(Math.abs(fov-1.1f)>.001f)throw new IllegalStateException("TC4_INV_HOVER FAIL unstable FOV="+fov);
    }
}
