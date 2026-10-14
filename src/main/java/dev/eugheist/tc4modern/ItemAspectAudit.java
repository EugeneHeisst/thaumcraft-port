package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real hidden-window Shift events after an actual inventory scan; no user-desktop input. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class ItemAspectAudit {
    static int ticks;static volatile boolean ready,masked;
    static void check(boolean ok,String text){if(!ok)throw new IllegalStateException("TC4_ITEM_ASPECTS FAIL "+text);System.out.println("TC4_ITEM_ASPECTS PASS "+text);}
    static void shift(boolean down){
        if(System.getenv("DISPLAY")==null||System.getenv("WAYLAND_DISPLAY")!=null&&!System.getenv("DISPLAY").startsWith(":"))throw new IllegalStateException("Shift audit requires an isolated Xvfb display");
        try{check(new ProcessBuilder("xdotool",down?"keydown":"keyup","Shift_L").inheritIO().start().waitFor()==0,"hidden X11 Shift "+(down?"pressed":"released"));}catch(Exception ex){throw new IllegalStateException(ex);}
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.itemAspectAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==50)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.closeContainer();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.teleport(35.5,180,35.5,0,0);p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.DIAMOND));p.getInventory().setItem(10,new ItemStack(Items.EMERALD));p.getPersistentData().remove(ResearchKnowledge.KEY);ResearchKnowledge.discoverAll(p.getPersistentData());p.inventoryMenu.setCarried(new ItemStack(TC4Modern.THAUMOMETER.get()));p.inventoryMenu.broadcastChanges();ThaumometerItem.sync(p,"");ready=true;});
        if(ticks==90){check(ready,"server fixture ready");WarpClientConfig.DISPLAY_ASPECTS.set(false);mc.setScreen(new InventoryScreen(mc.player));InventoryHoverAudit.hover(mc,9);}
        if(ticks==140){var target=ScanTarget.item(new ItemStack(Items.DIAMOND),mc.level);check(ClientKnowledge.knowledge.getCompound("scanned").getBoolean(target.knowledgeKey()),"real carried-thaumometer inventory scan completed");check(ItemAspectOverlay.last.isEmpty(),"ordinary overlay does not duplicate the carried scanner");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.inventoryMenu.setCarried(ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});}
        if(ticks==180){check(mc.player.containerMenu.getCarried().isEmpty(),"scanner returned by server fixture");check(!Screen.hasShiftDown()&&ItemAspectOverlay.last.isEmpty(),"scanned aspects hidden by default without Shift");shift(true);}
        if(ticks==205){check(Screen.hasShiftDown(),"GLFW sees actual Shift state");check(!ItemAspectOverlay.last.isEmpty()&&ItemAspectOverlay.last.stream().allMatch(ItemAspectOverlay.Tag::known),"Shift renders real scanned aspect amounts without a cursor scanner");LiquidDeathAudit.shot(mc,"item-aspects-shift.png");shift(false);}
        if(ticks==235){check(!Screen.hasShiftDown()&&ItemAspectOverlay.last.isEmpty(),"releasing Shift hides ordinary aspects");WarpClientConfig.DISPLAY_ASPECTS.set(true);}
        if(ticks==260){check(!ItemAspectOverlay.last.isEmpty(),"display_aspects reverses default visibility");LiquidDeathAudit.shot(mc,"item-aspects-default-visible.png");shift(true);}
        if(ticks==290){check(Screen.hasShiftDown()&&ItemAspectOverlay.last.isEmpty(),"Shift hides aspects when display_aspects is enabled");shift(false);InventoryHoverAudit.hover(mc,10);}
        if(ticks==325){check(ItemAspectOverlay.last.isEmpty()&&ItemAspectOverlay.tags(new ItemStack(Items.EMERALD),mc.level,ClientKnowledge.knowledge).isEmpty(),"unscanned item never reveals aspect tags");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var knowledge=ResearchKnowledge.load(p.getPersistentData());knowledge.getAspectsDiscovered("current").aspects.remove(thaumcraft.api.aspects.Aspect.GREED);ResearchKnowledge.save(p.getPersistentData(),knowledge);ThaumometerItem.sync(p,"");masked=true;});}
        if(ticks==360){check(masked,"unknown-aspect fixture synchronized");InventoryHoverAudit.hover(mc,9);}
        if(ticks==390){check(ItemAspectOverlay.last.stream().anyMatch(tag->tag.aspect()==thaumcraft.api.aspects.Aspect.GREED&&!tag.known())&&ItemAspectOverlay.last.stream().anyMatch(ItemAspectOverlay.Tag::known),"unknown compound aspect is masked beside discovered aspects");LiquidDeathAudit.shot(mc,"item-aspects-unknown.png");WarpClientConfig.DISPLAY_ASPECTS.set(false);shift(false);server.execute(()->server.getPlayerList().getPlayer(uuid).setInvulnerable(false));}
        if(ticks==420){System.out.println("TC4_ITEM_ASPECTS ALL PASS actual inventory scan, native Shift input, inverted preference, unscanned refusal and unknown-aspect masking");mc.stop();}
    }
}
