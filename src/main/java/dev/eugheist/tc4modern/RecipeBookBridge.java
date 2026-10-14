package dev.eugheist.tc4modern;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import thaumcraft.api.aspects.Aspect;

/** Resolves original research recipe keys to recipes synchronized by the modern server. */
final class RecipeBookBridge {
    private static final Map<String,String> IDS=Map.ofEntries(Map.entry("BlockFlesh","flesh_block"),Map.entry("VoidHelm","void_helmet"),Map.entry("VoidChest","void_chestplate"),Map.entry("VoidLegs","void_leggings"),Map.entry("VoidBoots","void_boots"),Map.entry("VoidAxe","void_axe"),Map.entry("VoidSword","void_sword"),Map.entry("VoidPick","void_pickaxe"),Map.entry("VoidShovel","void_shovel"),Map.entry("VoidHoe","void_hoe"),Map.entry("BlockTallow","tallow_block"),Map.entry("BlockThaumium","thaumium_block"),Map.entry("MundaneAmulet","mundane_amulet"),Map.entry("MundaneRing","mundane_ring"),Map.entry("MundaneBelt","mundane_belt"),Map.entry("Grate","grate"),Map.entry("ThaumiumAxe","thaumium_axe"),Map.entry("ThaumiumSword","thaumium_sword"),Map.entry("ThaumiumPick","thaumium_pickaxe"),Map.entry("ThaumiumShovel","thaumium_shovel"),Map.entry("ThaumiumHoe","thaumium_hoe"),Map.entry("ThaumiumHelm","thaumium_helmet"),Map.entry("ThaumiumChest","thaumium_chestplate"),Map.entry("ThaumiumLegs","thaumium_leggings"),Map.entry("ThaumiumBoots","thaumium_boots"),Map.entry("SalisMundus","salis_mundus"),Map.entry("TallowCandle","tallow_candle"),Map.entry("JarLabel","jar_label"),Map.entry("JarLabelNull","jar_label_clear"),Map.entry("Phial","phial"),Map.entry("Table","table"),Map.entry("Scribe2","scribing_tools"),Map.entry("Scribe3","scribing_tools_refill"),Map.entry("WandCapIron","iron_cap"),Map.entry("WandBasic","iron_capped_wooden_wand"),Map.entry("Thaumometer","thaumometer"),Map.entry("ArcaneStone2","arcane_stone_bricks"),Map.entry("ArcaneStone3","arcane_stone_stairs"),Map.entry("ArcaneStone4","arcane_stone_slab"));
    private RecipeBookBridge() {}
    private static final ResourceLocation OVERLAY=ResearchBookText.texture("textures/gui/gui_researchbook_overlay.png");
    private static final java.text.DecimalFormat FORMAT=new java.text.DecimalFormat("#######.##");

    /**
     * GuiResearchRecipe page layouts. Coordinates are relative to x = page left - 4 (side offset included) and y = top - 8,
     * exactly the values drawPage hands to drawCraftingPage, drawArcaneCraftingPage, drawCruciblePage and drawInfusionPage.
     */
    static boolean draw(GuiGraphics gui,String key,int x,int y) {
        Minecraft mc=Minecraft.getInstance();
        // ResearchPage(IRecipe[]) / (IArcaneRecipe[]): one page cycling through several recipes once per second; addons join keys with '|'.
        if(key.indexOf('|')>0){var keys=key.split("\\|");return draw(gui,keys[(int)(System.currentTimeMillis()/1000%keys.length)],x,y);}
        if(AlchemyBookRecipes.draw(gui,key,x,y))return true;
        if(key.equals("PrimalArrows"))return drawArcane(gui,"PrimalArrow_"+(System.currentTimeMillis()/1000%6),x,y);
        if(key.equals("Banners")||key.equals("Banner"))return drawArcane(gui,"Banner_"+(System.currentTimeMillis()/1000%16),x,y);
        if(key.equals("BalancedShard"))return drawCrucible(gui,"BalancedShard_"+(System.currentTimeMillis()/1000%6),x,y);
        String id=IDS.get(key);
        if(mc.getConnection()==null || mc.level==null)return false;
        // TC4Addons: a namespaced page key ("modid:path") is a vanilla recipe id drawn like the original crafting pages.
        ResourceLocation addonRecipe=id==null&&key.indexOf(':')>0?ResourceLocation.tryParse(key):null;
        if(addonRecipe!=null&&mc.getConnection().getRecipeManager().byKey(addonRecipe).isEmpty())addonRecipe=null;
        if(key.equals("RunicAugmentation")){var input=new ItemStack(TC4Modern.ROBE_CHEST.get());int level=(int)(System.currentTimeMillis()/1000%5);if(level>0){var tag=NoteData.tag(input);tag.putByte("RS.HARDEN",(byte)level);input.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}infusion(gui,x,y,RunicAugmentation.output(input),input,RunicAugmentation.components(input),RunicAugmentation.cost(input),RunicAugmentation.instability(input));return true;}
        if(key.startsWith("InfEnch"))return drawEnchantment(gui,key,x,y);
        if (key.equals("JarBrain")) return drawBrainJar(gui, x, y);
        if(addonRecipe==null&&id==null&&InfusionItemRecipes.all().stream().anyMatch(r->r.key().equals(key)))return drawInfusion(gui,key,x,y);
        if(addonRecipe==null&&id==null)return CrucibleRecipes.byKey().containsKey(key)?drawCrucible(gui,key,x,y):drawArcane(gui,key,x,y);
        var holder=mc.getConnection().getRecipeManager().byKey(addonRecipe!=null?addonRecipe:ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,id));
        if(holder.isEmpty()) return false;
        var recipe=holder.get().value();var ingredients=recipe.getIngredients();
        if(recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe){
            title(gui,"recipe.type.smelting",x,y);background(gui,x,y+28,0,0,0,192,56,64);
            item(gui,cycle(ingredients.getFirst()),x+48,y+64,false);item(gui,recipe.getResultItem(mc.level.registryAccess()),x+48,y+144,true);return true;
        }
        boolean shaped=recipe instanceof ShapedRecipe;
        int width=recipe instanceof ShapedRecipe s ? s.getWidth():3;
        // drawCraftingPage: 2x overlay grid (60,15) and output frame (20,3), output at (48,32), inputs every 32 px from (16,76).
        background(gui,x,y,2,32,60,15,52,52);background(gui,x,y,20,12,20,3,16,16);
        item(gui,recipe.getResultItem(mc.level.registryAccess()),x+48,y+32,true);
        title(gui,shaped?"recipe.type.workbench":"recipe.type.workbenchshapeless",x,y);
        for(int i=0;i<ingredients.size()&&i<9;i++)item(gui,cycle(ingredients.get(i)),x+16+(i%width)*32,y+76+(i/width)*32,false);
        return true;
    }
    static ItemStack cycle(net.minecraft.world.item.crafting.Ingredient ingredient){
        var stacks=ingredient.getItems();return stacks.length==0?ItemStack.EMPTY:stacks[(int)(System.currentTimeMillis()/1000%stacks.length)];
    }
    /** RenderItem with tooltip hit; inputs are drawn as single items (splitStack(1)), outputs with their count. */
    static void item(GuiGraphics gui,ItemStack stack,int x,int y,boolean count){
        if(stack.isEmpty())return;
        var shown=count?stack:stack.copyWithCount(1);
        BasicsBookRecipes.icon(gui,new BasicsBookRecipes.Icon(shown,null,null,shown.getCount(),null),x,y);
    }
    static void title(GuiGraphics gui,String key,int x,int y){
        var font=Minecraft.getInstance().font;var text=net.minecraft.network.chat.Component.translatable(key);
        gui.drawString(font,text,x+56-font.width(text)/2,y,0x505050,false);
    }
    private static void fitted(GuiGraphics gui,net.minecraft.network.chat.Component text,int center,int y,int width,int color){var font=Minecraft.getInstance().font;float scale=Math.min(1,width/(float)Math.max(1,font.width(text)));gui.pose().pushPose();gui.pose().translate(center,y,0);gui.pose().scale(scale,scale,1);gui.drawString(font,text,-font.width(text)/2,0,color,false);gui.pose().popPose();}
    /** drawTexturedModalRect from the overlay at twice its size, at (x + dx*2, y + dy*2). */
    static void background(GuiGraphics gui,int x,int y,int dx,int dy,int u,int v,int w,int h){
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(2,2,1);gui.blit(OVERLAY,dx,dy,u,v,w,h,256,256);gui.pose().popPose();
    }
    /**
     * UtilsFX.drawTag(x, y, aspect, amount, 0, z, 771, 1, false): the aspect tinted in its colour, the amount at half size in
     * the bottom-right corner, white over a one-pixel black outline.
     */
    static void drawTag(GuiGraphics gui,Aspect aspect,float amount,int x,int y){drawTag(gui,aspect,amount,x,y,1);}
    /** UtilsFX.drawTag with alpha: the research browser pulses aspects the player cannot yet afford. */
    static void drawTag(GuiGraphics gui,Aspect aspect,float amount,int x,int y,float alpha){
        if(aspect==null)return;
        var font=Minecraft.getInstance().font;int color=aspect.getColor();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);
        gui.blit(aspect.getImage(),x,y,0,0,16,16,16,16);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        if(amount>0){
            String am=FORMAT.format(amount);int sw=font.width(am);
            gui.pose().pushPose();gui.pose().translate(0,0,1);gui.pose().scale(.5f,.5f,.5f);
            int bx=32-sw+x*2,by=32-font.lineHeight+y*2;
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)if((a==0)!=(b==0))gui.drawString(font,am,bx+a,by+b,Math.max(4,(int)(alpha*255))<<24,false);
            gui.drawString(font,am,bx,by,(Math.max(4,(int)(alpha*255))<<24)|0xFFFFFF,false);
            gui.pose().popPose();
        }
    }
    private static boolean drawBrainJar(GuiGraphics gui, int x, int y) {
        infusion(gui,x,y,BrainJarRecipe.output(),BrainJarRecipe.input(),BrainJarRecipe.ingredients(),BrainJarRecipe.cost(),BrainJarRecipe.INSTABILITY);
        return true;
    }
    /** drawInfusionPage: altar overlay, output on top, central item and components on a 40 px circle, aspects in rows of five. */
    private static boolean drawInfusion(GuiGraphics gui,String key,int x,int y){
        var mc=Minecraft.getInstance();
        var recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals(key)).findFirst().orElse(null);
        if(recipe==null)return false;
        infusion(gui,x,y,recipe.outputStack(new ItemStack(recipe.central().get())),new ItemStack(recipe.central().get()),recipe.componentStacks(),recipe.aspects(),recipe.instability());
        return true;
    }
    /** Original drawInfusionEnchantingPage: cycles levels, scales base essentia and XP, no dummy central/output item. */
    private static boolean drawEnchantment(GuiGraphics gui,String key,int x,int y){
        var mc=Minecraft.getInstance();var recipe=thaumcraft.common.tiles.infusion.InfusionEnchantments.all(()->InfusionContent.SALIS.get()).stream().filter(r->r.key().equals(key)).findFirst().orElse(null);if(recipe==null)return false;
        var enchantment=thaumcraft.common.tiles.infusion.InfusionEnchantments.holder(mc.level.registryAccess(),recipe.enchantment());int level=1+(int)(System.currentTimeMillis()/1000%enchantment.value().getMaxLevel());
        title(gui,"recipe.type.infusionenchantment",x,y);var inst=net.minecraft.network.chat.Component.translatable("tc.inst").append(" ").append(net.minecraft.network.chat.Component.translatable("tc.inst."+Math.min(5,recipe.instability()/2)));
        var font=mc.font;fitted(gui,inst,x+56,y+190,128,0x505050);var name=net.minecraft.world.item.enchantment.Enchantment.getFullname(enchantment,level);gui.drawString(font,name,x+56-font.width(name)/2,y+24,7360656,false);
        String xp=recipe.baseXP(enchantment)*level+" levels";gui.drawString(font,xp,x+56-font.width(xp)/2,y+40,0x508850,false);background(gui,x,y+20,0,19,200,77,60,44);
        var aspects=recipe.aspects();int total=0,rows=(aspects.size()-1)/5,shift=(5-aspects.size()%5)*10;for(var a:aspects.getAspectsSorted()){int m=total/5>=rows&&(rows>1||aspects.size()<5)?1:0;drawTag(gui,a,aspects.getAmount(a)*level,x+8+total%5*20+shift*m,y+164-10*rows+total/5*20);total++;}
        float rotation=-90;float slice=360/recipe.components().size();for(var c:recipe.components()){int xx=(int)(net.minecraft.util.Mth.cos(rotation/180f*(float)Math.PI)*40)-8,yy=(int)(net.minecraft.util.Mth.sin(rotation/180f*(float)Math.PI)*40)-8;rotation+=slice;item(gui,new ItemStack(c.get()),x+56+xx,y+102+yy,false);}return true;
    }
    private static void infusion(GuiGraphics gui,int x,int y,ItemStack output,ItemStack central,java.util.List<ItemStack> components,thaumcraft.api.aspects.AspectList aspects,int instability){
        var mc=Minecraft.getInstance();
        title(gui,"recipe.type.infusion",x,y);
        var inst=net.minecraft.network.chat.Component.translatable("tc.inst").append(" ").append(net.minecraft.network.chat.Component.translatable("tc.inst."+Math.min(5,instability/2)));
        fitted(gui,inst,x+56,y+190,128,0x505050);
        background(gui,x,y+20,0,0,0,3,56,17);background(gui,x,y+20,0,19,200,77,60,44);
        int total=0,rows=(aspects.size()-1)/5,shift=(5-aspects.size()%5)*10,sx=x+8,sy=y+164-10*rows;
        for(var tag:aspects.getAspectsSorted()){
            int m=total/5>=rows&&(rows>1||aspects.size()<5)?1:0;
            drawTag(gui,tag,aspects.getAmount(tag),sx+total%5*20+shift*m,sy+total/5*20);total++;
        }
        item(gui,output,x+48,y+28,true);
        item(gui,central,x+48,y+94,false);
        int le=components.size();float pieSlice=360/le,rot=-90;
        for(var component:components){
            int xx=(int)(net.minecraft.util.Mth.cos(rot/180f*(float)Math.PI)*40)-8,yy=(int)(net.minecraft.util.Mth.sin(rot/180f*(float)Math.PI)*40)-8;rot+=pieSlice;
            item(gui,component,x+56+xx,y+102+yy,false);
        }
    }
    /** drawCruciblePage: crucible overlay with the catalyst dropping in, output above, aspects centred in rows of three. */
    private static boolean drawCrucible(GuiGraphics gui,String key,int x,int y) {
        var recipe=CrucibleRecipes.byKey().get(key);
        if(recipe==null)return false;
        title(gui,"recipe.type.crucible",x,y);
        background(gui,x,y+28,0,0,0,3,56,17);background(gui,x,y+28,0,32,0,20,56,48);background(gui,x,y+28,21,24,100,84,11,13);
        var aspects=recipe.aspects;int total=0,rows=(aspects.size()-1)/3,shift=(3-aspects.size()%3)*10,sx=x+28,sy=y+96+32-10*rows;
        for(var tag:aspects.getAspectsSorted()){
            int m=total/3>=rows&&(rows>1||aspects.size()<3)?1:0;
            drawTag(gui,tag,aspects.getAmount(tag),sx+total%3*20+shift*m,sy+total/3*20);total++;
        }
        item(gui,recipe.getRecipeOutput(),x+48,y+36,true);
        item(gui,cycle(recipe.catalyst),x+26,y+72,false);
        return true;
    }
    /** drawArcaneCraftingPage: arcane grid overlay, faded wand behind the vis cost, cost tags sorted by amount. */
    private static boolean drawArcane(GuiGraphics gui,String key,int x,int y){
        var recipe=ArcaneRecipes.entries().stream().filter(e->e.key().equals(key)).findFirst().orElse(null);if(key.equals("Sceptre")){int idx=(int)(System.currentTimeMillis()/2000%3);recipe=WandParts.sceptreEntry(WandParts.CAPS.get(new String[]{"iron","gold","thaumium"}[idx]),WandParts.RODDEFS.get(new String[]{"wood","greatwood","silverwood"}[idx]));}if(recipe==null)return false;
        background(gui,x,y,2,27,112,15,52,52);background(gui,x,y,20,7,20,3,16,16);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.4f);background(gui,x,y+164,0,0,68,76,12,12);com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        var tags=recipe.cost();int count=0;
        for(var tag:tags.getAspectsSortedAmount())drawTag(gui,tag,tags.getAmount(tag),x+14+18*count+++(5-tags.size())*8,y+172);
        item(gui,recipe.output(),x+48,y+22,true);
        title(gui,"recipe.type.arcane",x,y);
        for(int i=0;i<recipe.ingredients().size()&&i<9;i++)item(gui,cycle(recipe.ingredients().get(i)),x+16+(i%recipe.width())*32,y+66+(i/recipe.width())*32,false);
        return true;
    }
}
