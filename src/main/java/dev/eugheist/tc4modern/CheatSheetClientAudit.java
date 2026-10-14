package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** The cheat sheet uses the original thaumonomiconcheat icon and the book name with the Cheat Sheet tooltip on a real client. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class CheatSheetClientAudit {
    static int ticks;static volatile boolean ready,restored;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_CHEAT_CLIENT FAIL "+text);System.out.println("TC4_CHEAT_CLIENT PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.cheatSheetClientAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==20)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMONOMICON.get()));p.getInventory().setItem(1,new ItemStack(TC4Modern.THAUMONOMICON_CHEAT.get()));p.getInventory().selected=1;p.inventoryMenu.broadcastChanges();ready=true;});
        if(ticks==60){check(ready,"fixture ready");mc.player.getInventory().selected=1;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(1));mc.options.hideGui=false;
            var stack=new ItemStack(TC4Modern.THAUMONOMICON_CHEAT.get());var model=mc.getItemRenderer().getModel(stack,null,null,0);var sprite=model.getParticleIcon().contents().name().toString();
            check(model!=mc.getModelManager().getMissingModel()&&sprite.equals("tc4modern:items/thaumonomiconcheat"),"cheat sheet renders the original thaumonomiconcheat icon ("+sprite+")");
            var lines=stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level),mc.player,net.minecraft.world.item.TooltipFlag.NORMAL);check(lines.size()>=2&&lines.get(0).getString().equals(new ItemStack(TC4Modern.THAUMONOMICON.get()).getHoverName().getString())&&lines.get(1).getString().equals("Cheat Sheet"),"tooltip shows the book name and Cheat Sheet ("+lines.stream().map(c->c.getString()).toList()+")");}
        if(ticks==80){LiquidDeathAudit.shot(mc,"cheat-sheet-hotbar.png");server.execute(()->{server.getPlayerList().getPlayer(uuid).getInventory().clearContent();restored=true;});}
        if(ticks==100){check(restored,"inventory restored");System.out.println("TC4_CHEAT_CLIENT ALL PASS original cheat sheet icon and tooltip");mc.stop();}
    }
}
