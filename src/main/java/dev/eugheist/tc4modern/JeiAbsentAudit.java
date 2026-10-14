package dev.eugheist.tc4modern;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** No JEI API references: verifies optional loading in a real client without JEI. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class JeiAbsentAudit {
    private static int ticks;
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.jeiAuditOnly")||net.neoforged.fml.ModList.get().isLoaded("jei"))return;
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(++ticks==30){
            if(ArcaneRecipes.entries().isEmpty()||CrucibleRecipes.entries().isEmpty()||InfusionItemRecipes.all().isEmpty())throw new IllegalStateException("TC4_NO_JEI FAIL recipes unavailable");
            System.out.println("TC4_NO_JEI ALL PASS native client and original recipes work without JEI");mc.stop();
        }
    }
}
