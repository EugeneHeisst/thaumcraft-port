package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.Set;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchItem;

/** Original study hints and aspect costs inside the modern positioned tooltip. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class ResearchBrowserTooltip {
    record Study(ResearchItem research,Component hint) implements TooltipComponent {}
    @SubscribeEvent static void register(net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent event){
        event.register(Study.class,study->new ClientTooltipComponent(){
            public int getHeight(){return 28;}
            public int getWidth(Font font){return Math.max(study.research.tags.size()*18,font.width(study.hint));}
            public void renderImage(Font font,int x,int y,GuiGraphics gui){
                int column=0;
                for(Aspect aspect:study.research.tags.getAspectsSortedAmount()){
                    boolean known=known(aspect);int cost=study.research.tags.getAmount(aspect);
                    if(known)RecipeBookBridge.drawTag(gui,aspect,cost,x+column*18,y,ClientKnowledge.knowledge.getInt(aspect.getTag())<cost?ThaumonomiconScreen.pulse():1);
                    else {com.mojang.blaze3d.systems.RenderSystem.setShaderColor(.5f,.5f,.5f,.5f);gui.blit(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/aspects/_unknown.png"),x+column*18,y,0,0,16,16,16,16);com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
                    column++;
                }
                gui.drawString(font,study.hint,x,y+19,0xFFFFFF,false);
            }
        });
    }
    static boolean known(Aspect aspect){var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge);return ResearchKnowledge.load(data).hasDiscoveredAspect("current",aspect);}
    static boolean enough(ResearchItem research){for(Aspect aspect:research.tags.getAspects())if(!known(aspect)||ClientKnowledge.knowledge.getInt(aspect.getTag())<research.tags.getAmount(aspect))return false;return true;}
    static String noteHint(ResearchItem research,Inventory inventory){
        boolean paper=false,ink=false;
        for(int i=0;i<inventory.getContainerSize();i++){
            ItemStack stack=inventory.getItem(i);var note=NoteData.read(stack);
            if(note!=null&&note.key.equals(research.key))return "tc.research.hasnote";
            paper|=stack.is(Items.PAPER);ink|=ScribingToolsItem.hasInk(stack);
        }
        return paper&&ink?"tc.research.getprim":"tc.research.shortprim";
    }
    /** GuiResearchBrowser.drawScreen hover: its own gradient box, half-size lines and original colours, not a vanilla tooltip. */
    static void render(GuiGraphics gui,Font font,ResearchItem research,Set<String> completed,Inventory inventory,int mouseX,int mouseY){
        String name=research.getName();int x=mouseX+6,y=mouseY-4;
        boolean done=completed.contains(research.key),canUnlock=ResearchProgress.requisites(completed,research);
        // Locked, unresearched entries name themselves in the galactic alphabet (galFontRenderer).
        var nameStyle=!done&&!canUnlock?net.minecraft.network.chat.Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("alt")):net.minecraft.network.chat.Style.EMPTY;
        var title=Component.literal(nameStyle.getFont().equals(net.minecraft.network.chat.Style.DEFAULT_FONT)?name:galactic(name)).withStyle(nameStyle);
        gui.pose().pushPose();gui.pose().translate(0,0,400);
        if(canUnlock){
            String text=research.getText();
            boolean secondary=!done&&research.tags!=null&&research.tags.size()>0&&ResearchConfig.purchaseMode(research,ClientKnowledge.researchDifficulty());
            boolean primary=!secondary&&!done;
            int width=(int)Math.max(font.width(title),font.width(text)/1.9f);
            int height=font.wordWrapHeight(title,width)+5,extra=0;
            if(primary){extra+=9;width=(int)Math.max(width,font.width(Component.translatable("tc.research.shortprim"))/1.9f);}
            if(secondary){extra+=29;width=(int)Math.max(width,font.width(Component.translatable("tc.research.short"))/1.9f);}
            int warp=Math.min(5,ResearchWarp.amount(research.key));
            String forbidden=Component.translatable("tc.forbidden").getString().replace("%n",Component.translatable("tc.forbidden.level."+warp).getString());
            if(warp>0){extra+=9;width=(int)Math.max(width,font.width(forbidden)/1.9f);}
            gui.fillGradient(x-3,y-3,x+width+3,y+height+6+extra,0xC0000000,0xC0000000);
            half(gui,font,text,x,y+height-1,0xFF9090FF);
            if(warp>0){half(gui,font,forbidden,x,y+height+8,0xFFFFFFFF);height+=9;}
            if(primary){
                String hint=noteHint(research,inventory);
                half(gui,font,Component.translatable(hint).getString(),x,y+height+8,hint.equals("tc.research.hasnote")?0xFFFFA500:hint.equals("tc.research.getprim")?0xFF87CEEB:0xFFDC143C);
            }else if(secondary){
                int column=0;
                for(Aspect aspect:research.tags.getAspectsSortedAmount()){
                    int cost=research.tags.getAmount(aspect);
                    if(known(aspect))RecipeBookBridge.drawTag(gui,aspect,cost,x+column*16,y+height+8,ClientKnowledge.knowledge.getInt(aspect.getTag())<cost?ThaumonomiconScreen.pulse():1);
                    else{com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(.5f,.5f,.5f,.5f);gui.blit(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/aspects/_unknown.png"),x+column*16,y+height+8,0,0,16,16,16,16);com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
                    column++;
                }
                boolean ready=enough(research);
                half(gui,font,Component.translatable(ready?"tc.research.purchase":"tc.research.short").getString(),x,y+height+27,ready?0xFF87CEEB:0xFFDC143C);
            }
        }else{
            String missing=Component.translatable("tc.researchmissing").getString();
            int width=(int)Math.max(font.width(title),font.width(missing)/1.5f);
            int lines=font.wordWrapHeight(missing,width*2);
            gui.fillGradient(x-3,y-3,x+width+3,y+lines+10,0xC0000000,0xC0000000);
            gui.pose().pushPose();gui.pose().translate(x,y+12,0);gui.pose().scale(.5f,.5f,1);
            gui.drawWordWrap(font,Component.literal(missing),0,0,width*2,0xFF705050);
            gui.pose().popPose();
        }
        int color=canUnlock?(research.isSpecial()?0xFFFFFF80:0xFFFFFFFF):(research.isSpecial()?0xFF808040:0xFF808080);
        gui.drawString(font,title,x,y,color,true);
        gui.pose().popPose();
    }
    private static final String CYRILLIC="абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String[] LATIN={"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"};
    /** The galactic alphabet font only has Latin glyphs: spell other scripts in Latin letters first. */
    static String galactic(String name){
        var out=new StringBuilder();
        for(char c:name.toCharArray()){int i=CYRILLIC.indexOf(Character.toLowerCase(c));if(i<0){out.append(c);continue;}
            String l=LATIN[i];out.append(Character.isUpperCase(c)&&!l.isEmpty()?Character.toUpperCase(l.charAt(0))+l.substring(1):l);}
        return out.toString();
    }
    /** Half-scale fontRenderer.drawStringWithShadow line; legacy section codes in the text keep working. */
    private static void half(GuiGraphics gui,Font font,String text,int x,int y,int color){
        gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(.5f,.5f,1);gui.drawString(font,text,0,0,color,true);gui.pose().popPose();
    }
}
