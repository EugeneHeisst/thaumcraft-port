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
    static void render(GuiGraphics gui,Font font,ResearchItem research,Set<String> completed,Inventory inventory,int mouseX,int mouseY){
        var elements=new ArrayList<Either<net.minecraft.network.chat.FormattedText,TooltipComponent>>();
        elements.add(Either.left(Component.literal(research.getName())));
        elements.add(Either.left(Component.literal(research.getText())));
        int warp=ResearchWarp.amount(research.key);
        if(warp>0&&ResearchProgress.requisites(completed,research)){
            String severity=Component.translatable("tc.forbidden.level."+Math.min(5,warp)).getString();
            elements.add(Either.left(Component.literal(Component.translatable("tc.forbidden").getString().replace("%n",severity)).withStyle(net.minecraft.ChatFormatting.WHITE)));
        }
        if(!completed.contains(research.key)){
            if(!ResearchProgress.requisites(completed,research))elements.add(Either.left(Component.translatable("tc.researchmissing").withStyle(net.minecraft.ChatFormatting.RED)));
            else if(ResearchConfig.purchaseMode(research,ClientKnowledge.researchDifficulty())){
                boolean ready=enough(research);elements.add(Either.right(new Study(research,Component.translatable(ready?"tc.research.purchase":"tc.research.short").withStyle(ready?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED))));
            }else{
                String hint=noteHint(research,inventory);elements.add(Either.left(Component.translatable(hint).withStyle(hint.equals("tc.research.getprim")?net.minecraft.ChatFormatting.GREEN:hint.equals("tc.research.hasnote")?net.minecraft.ChatFormatting.GOLD:net.minecraft.ChatFormatting.RED)));
            }
        }
        gui.renderComponentTooltipFromElements(font,elements,mouseX,mouseY,ItemStack.EMPTY);
    }
}
