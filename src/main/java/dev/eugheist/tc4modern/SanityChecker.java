package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** ItemSanityChecker: held in the main hand it shows ClientTickEventsFML.renderSanityHud, the three warp buckets on hud.png. */
public final class SanityChecker {
    public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("sanity_checker"));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->h.register(ITEM.getId(),new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))));}
    }
    @EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT) static final class Hud {
        static int rendered;
        static void quad(GuiGraphics g,int x,int y,int u,int v,int w,int h){g.blit(WandHud.HUD,x,y,u,v,w,h,256,256);}
        @SubscribeEvent static void render(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event){
            var mc=Minecraft.getInstance();var player=mc.player;if(player==null||mc.options.hideGui||mc.screen!=null||!player.getMainHandItem().is(ITEM.get()))return;
            var g=event.getGuiGraphics();var warp=PlayerWarp.load(player);float tw=warp.total();int p=warp.get(WarpState.Kind.PERMANENT),s=warp.get(WarpState.Kind.STICKY),t=warp.get(WarpState.Kind.TEMPORARY);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();quad(g,1,1,152,0,20,76);
            float mod=1;if(tw>100){mod=100/tw;tw=100;}
            int gap=(int)((100-tw)/100*48),wt=(int)(t/100f*48*mod),ws=(int)(s/100f*48*mod);
            /* original quads pass wt+gap / wt+ws+gap / 48 as heights, kept as is */
            if(t>0){g.setColor(1,.5f,1,1);quad(g,7,21+gap,200,gap,8,wt+gap);}
            if(s>0){g.setColor(.75f,0,.75f,1);quad(g,7,21+wt+gap,200,wt+gap,8,wt+ws+gap);}
            if(p>0){g.setColor(.5f,0,.5f,1);quad(g,7,21+wt+ws+gap,200,wt+ws+gap,8,48);}
            g.setColor(1,1,1,1);quad(g,1,1,176,0,20,76);if(tw>=100)quad(g,1,1,216,0,20,16);rendered++;
        }
    }
}
