package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.research.*;
import thaumcraft.common.config.ConfigResearchData;

/** Modern input/render adapter for original categories, coordinates, prerequisites and pages. */
public class ThaumonomiconScreen extends AbstractContainerScreen<ThaumonomiconMenu> {
    private static final ResourceLocation FRAME=texture("textures/gui/gui_research.png");
    private static final ResourceLocation BOOK=texture("textures/gui/gui_researchbook.png");
    private static final ResourceLocation HIGHLIGHT=texture("textures/misc/particles.png");
    private final List<String> categories;
    private int category, page;
    private static String lastCategory;private static double lastPanX=140,lastPanY=115;
    private double panX=140,panY=115;
    private ResearchItem opened;
    private long nextRequest;
    private boolean panning;
    private double dragX,dragY;
    public ThaumonomiconScreen(ThaumonomiconMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);ConfigResearchData.init();categories=new ArrayList<>(ResearchCategories.researchCategories.keySet().stream().filter(c->!c.equals("ELDRITCH")||completed().contains("ELDRITCHMINOR")).toList());imageWidth=256;imageHeight=230;
        // GuiResearchBrowser kept selectedCategory and the map position in static fields: reopen where the book was closed.
        int last=categories.indexOf(lastCategory);if(last>=0){category=last;panX=lastPanX;panY=lastPanY;}
    }
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,path); }
    private Set<String> completed() { var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge.copy());return ResearchProgress.completed(data); }
    private List<ResearchItem> visible() {
        var done=completed();
        return ResearchCategories.getResearchList(categories.get(category)).research.values().stream().filter(r -> ResearchProgress.visible(done,r)).toList();
    }
    private int x(ResearchItem item) { return leftPos+(int)panX+item.displayColumn*24; }
    private int y(ResearchItem item) { return topPos+(int)panY+item.displayRow*24; }
    List<ResearchPage> readablePages() {
        var result=new ArrayList<ResearchPage>();
        if(opened==null || opened.getPages()==null)return result;
        for(var entry:opened.getPages())if(entry.type!=ResearchPage.PageType.TEXT_CONCEALED || completed().contains(entry.research))result.add(entry);
        if(opened.key.equals("RUNICAUGMENTATION")){var recipe=new ResearchPage("");recipe.type=ResearchPage.PageType.INFUSION_CRAFTING;recipe.recipeKey="RunicAugmentation";result.add(Math.min(1,result.size()),recipe);}
        if(opened.key.equals("SCEPTRE")){var recipe=new ResearchPage("");recipe.type=ResearchPage.PageType.ARCANE_CRAFTING;recipe.recipeKey="Sceptre";result.add(recipe);}
        if(opened.key.equals("ASPECTS")) {
            var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge.copy());
            var known=ResearchKnowledge.load(data).getAspectsDiscovered("current").getAspectsSorted();
            for(int i=0;i<known.length;i+=4){var aspects=new thaumcraft.api.aspects.AspectList();for(int j=i;j<Math.min(i+4,known.length);j++)aspects.add(known[j],0);result.add(new ResearchPage(aspects));}
        }
        return result;
    }
    private int bookX(){return (width-256)/2;}
    private int bookY(){return (height-181)/2;}
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY) {
        BasicsBookRecipes.begin();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        if(opened!=null) {
            int sw=bookX(),sh=bookY();
            gui.pose().pushPose();gui.pose().translate((width-256*1.3f)/2,(height-181*1.3f)/2,0);gui.pose().scale(1.3f,1.3f,1);
            gui.blit(BOOK,0,0,0,0,256,181,256,256);gui.pose().popPose();
            var pages=readablePages();
            for(int side=0;side<2;side++)if(page+side<pages.size()) {
                var entry=pages.get(page+side);int y=sh;
                if(page==0&&side==0){gui.blit(BOOK,sw+4,y-13,24,184,96,4,256,256);gui.blit(BOOK,sw+4,y+4,24,184,96,4,256,256);
                    var title=Component.literal(opened.getName());float scale=Math.min(1,130f/font.width(title));gui.pose().pushPose();gui.pose().translate(sw+52-font.width(title)*scale/2,y-6*scale,0);gui.pose().scale(scale,scale,1);gui.drawString(font,title,0,0,0x303030,false);gui.pose().popPose();y+=25;
                }
                if(entry.recipeKey!=null){int x=sw-4+side*152;if(!BasicsBookRecipes.draw(gui,entry.recipeKey,x,y-8)&&!RecipeBookBridge.draw(gui,entry.recipeKey,x,y-8)&&!GolemancyBookAudit.missing(opened.key,entry.recipeKey)&&(Boolean.getBoolean("tc4modern.artificeBookAuditOnly")||Boolean.getBoolean("tc4modern.furnaceAuditOnly")||Boolean.getBoolean("tc4modern.fortressAuditOnly")||Boolean.getBoolean("tc4modern.arcanaAuditOnly")||Boolean.getBoolean("tc4modern.boreAuditOnly")||Boolean.getBoolean("tc4modern.earthAuditOnly")||Boolean.getBoolean("tc4modern.swordAuditOnly")||Boolean.getBoolean("tc4modern.elementalAuditOnly")||Boolean.getBoolean("tc4modern.bowAuditOnly")||Boolean.getBoolean("tc4modern.bannerAuditOnly")||Boolean.getBoolean("tc4modern.mirrorAuditOnly")||Boolean.getBoolean("tc4modern.hoverAuditOnly")||Boolean.getBoolean("tc4modern.bootsAuditOnly")||Boolean.getBoolean("tc4modern.runicAuditOnly")||Boolean.getBoolean("tc4modern.enchantAuditOnly")||Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")||Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly")))throw new IllegalStateException("Missing book recipe: "+opened.key+" / "+entry.recipeKey);}
                else if(entry.type==ResearchPage.PageType.ASPECTS)drawAspects(gui,entry.aspects,sw-8+side*152,y-8,mouseX,mouseY);
                else ResearchBookText.page(gui,entry.getTranslatedText(),sw-15+side*152,y-10,sh+186-(y-10));
            }
            if(page>0)arrow(gui,sw-16,sh+190,0,mouseX,mouseY);
            if(page+2<pages.size())arrow(gui,sw+262,sh+190,12,mouseX,mouseY);
            if(!history.isEmpty())gui.blit(BOOK,sw+118,sh+189,38,202,20,12,256,256);
            BasicsBookRecipes.tooltip(gui,mouseX,mouseY);
            return;
        }
        var categoryData=ResearchCategories.getResearchList(categories.get(category));
        double minX=categoryData.minDisplayColumn*24-85,maxX=categoryData.maxDisplayColumn*24-112,minY=categoryData.minDisplayRow*24-112,maxY=categoryData.maxDisplayRow*24-61;
        double mapX=16-panX,mapY=17-panY;
        int u=(int)((mapX-minX)/Math.max(1,maxX-minX)*288),v=(int)((mapY-minY)/Math.max(1,maxY-minY)*316);
        gui.pose().pushPose();gui.pose().translate(leftPos+16,topPos+17,0);gui.pose().scale(2,2,1);gui.blit(categoryData.background,0,0,u/2,v/2,112,98,256,256);gui.pose().popPose();
        gui.enableScissor(leftPos+16,topPos+17,leftPos+240,topPos+213);
        var entries=visible();var done=completed();
        for(var item:entries) if(item.parents!=null) for(String key:item.parents) {
            var parent=ResearchCategories.getResearch(key);
            if(parent!=null && parent.category.equals(item.category) && entries.contains(parent)) ResearchBrowserLines.draw(gui,x(item)+11,y(item)+11,x(parent)+11,y(parent)+11,done.contains(item.key)?0x191919:done.contains(parent.key)?0x00FF00:0x0000FF,!done.contains(item.key),animationTicks());
        }
        for(var item:entries) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            boolean available=ResearchProgress.requisites(done,item);
            if(ResearchWarp.amount(item.key)>0)drawForbidden(gui,x(item)+11,y(item)+11);
            float brightness=done.contains(item.key)?1:available?pulse():.3f;
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness,brightness,brightness,1);
            gui.blit(FRAME,x(item)-2,y(item)-2,item.isRound() ? 54:item.isHidden() ? ResearchConfig.purchaseMode(item,ClientKnowledge.researchDifficulty()) ? 230:86:ResearchConfig.purchaseMode(item,ClientKnowledge.researchDifficulty()) ? 110:0,230,26,26,256,256);
            if(item.isSpecial())gui.blit(FRAME,x(item)-2,y(item)-2,26,230,26,26,256,256);
            if(ResearchPopup.highlighted.contains(item.key))drawHighlight(gui,x(item)-5,y(item)-5);
            drawResearchIcon(gui,item,x(item)+3,y(item)+3,true);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);


        }
        gui.disableScissor();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        // GuiResearchBrowser order: tab, highlight, icon, unselected shade, then the pane frame over the icons' right edge.
        for(int i=0;i<categories.size();i++) {
            boolean selected=i==category;
            gui.blit(FRAME,leftPos-24,topPos+i*24,selected ? 152:176,232,24,24,256,256);
            if(ResearchPopup.highlighted.contains(categories.get(i))) drawHighlight(gui,leftPos-27,topPos+i*24);
            gui.blit(ResearchCategories.getResearchList(categories.get(i)).icon,leftPos-19+(selected?0:8),topPos+4+i*24,0,0,16,16,16,16);
            if(!selected)gui.blit(FRAME,leftPos-24,topPos+i*24,200,232,24,24,256,256);
        }
        gui.blit(FRAME,leftPos,topPos,0,0,256,230,256,256);
    }
    private static final long ANIMATION_START=System.nanoTime();
    /** Original GuiResearchBrowser pulse: sin over a 600 ms period, brightness 0.5..1.0. */
    static float pulse(){return (float)Math.sin(System.currentTimeMillis()%600L/600.0*Math.PI*2)*.25f+.75f;}
    private static float animationTicks(){return (System.nanoTime()-ANIMATION_START)/50_000_000f;}
    private void drawForbidden(GuiGraphics gui,int x,int y){
        int frame=31-(minecraft.player.tickCount%32);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(68/255f,0,85/255f,.66f);
        gui.blit(texture("textures/misc/nodes.png"),x-40,y-40,80,80,frame*32f,5*32f,32,32,1024,1024);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
    }
    private void drawHighlight(GuiGraphics gui,int x,int y) {
        int frame=(int)(animationTicks()/2)%16;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.65f);
        gui.blit(HIGHLIGHT,x,y,32,32,frame*16,80,16,16,256,256);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
    }
    @Override public void removed() { if(category>=0&&category<categories.size()){lastCategory=categories.get(category);lastPanX=panX;lastPanY=panY;} super.removed(); }
    @Override public void onClose() { ResearchPopup.highlighted.clear(); super.onClose(); }
    @Override protected void renderLabels(GuiGraphics gui,int mouseX,int mouseY) {}
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial) {
        super.render(gui,mouseX,mouseY,partial);
        // GuiResearchBrowser: hovering a tab writes its category name, white with shadow, just above the cursor.
        if(opened==null)for(int i=0;i<categories.size();i++)if(mouseX>=leftPos-24&&mouseX<leftPos&&mouseY>=topPos+i*24&&mouseY<topPos+i*24+24){gui.pose().pushPose();gui.pose().translate(0,0,400);gui.drawString(font,ResearchCategories.getCategoryName(categories.get(i)),mouseX,mouseY-8,0xFFFFFF,true);gui.pose().popPose();}
        if(opened==null && mouseX>=leftPos+16 && mouseX<leftPos+240 && mouseY>=topPos+16 && mouseY<topPos+214) for(var item:visible()) if(mouseX>=x(item)-2 && mouseX<x(item)+24 && mouseY>=y(item)-2 && mouseY<y(item)+24) {
            ResearchBrowserTooltip.render(gui,font,item,completed(),minecraft.player.getInventory(),mouseX,mouseY);
        }
        if(!ClientKnowledge.status.isEmpty() && System.nanoTime()<ClientKnowledge.messageUntil) gui.drawCenteredString(font,Component.translatable("hud.tc4modern."+ClientKnowledge.status),leftPos+128,topPos+238,0xEADCB4);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(opened!=null) {
            int sw=bookX(),sh=bookY();
            if(my>=sh+189&&my<sh+201) {
                if(mx>=sw-17&&mx<sw-3&&page>0){page-=2;pageSound();return true;}
                if(mx>=sw+261&&mx<sw+275&&page+2<readablePages().size()){page+=2;pageSound();return true;}
                if(mx>=sw+118&&mx<sw+138&&!history.isEmpty()){var previous=history.remove(history.size()-1);opened=previous.research();page=previous.page();pageSound();return true;}
            }
            var icon=BasicsBookRecipes.hovered(mx,my);
            if(icon!=null){var reference=reference(icon);if(reference!=null&&!reference.research().key.equals(opened.key)){history.add(new Previous(opened,page));opened=reference.research();page=reference.page()/2*2;pageSound();}}
            return true;
        }
        if(mx>=leftPos-24 && mx<leftPos) for(int i=0;i<categories.size();i++) if(my>=topPos+i*24 && my<topPos+24+i*24) {category=i;panX=140;panY=115;return true;}
        if(mx>=leftPos+16 && mx<leftPos+240 && my>=topPos+16 && my<topPos+214) for(var item:visible()) if(mx>=x(item)-2 && mx<x(item)+24 && my>=y(item)-2 && my<y(item)+24) {
            if(completed().contains(item.key)) {opened=item;page=0;}
            else if(button==0 && ResearchProgress.requisites(completed(),item) && System.nanoTime()>nextRequest) {nextRequest=System.nanoTime()+250_000_000L;PacketDistributor.sendToServer(new ResearchRequestPayload(menu.containerId,item.key));}
            return true;
        }
        if(button==0 && mx>=leftPos+16 && mx<leftPos+240 && my>=topPos+16 && my<topPos+214) { panning=true;dragX=mx;dragY=my;setDragging(true);return true; }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(opened==null && panning) {panX+=x-dragX;panY+=y-dragY;clampPan();dragX=x;dragY=y;return true;}
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button) {panning=false;return super.mouseReleased(x,y,button);}
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(opened==null) {
            if(key==263) {panX+=16;return true;} if(key==262) {panX-=16;return true;}
            if(key==265) {panY+=16;return true;} if(key==264) {panY-=16;return true;}
        }
        if((key==256 || minecraft.options.keyInventory.matches(key,scan)) && opened!=null) {opened=null;page=0;history.clear();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    private record Previous(ResearchItem research,int page) {}
    private final List<Previous> history=new ArrayList<>();
    private Previous reference(BasicsBookRecipes.Icon icon){String key=null;int index=0;
        if(!icon.stack().isEmpty())key=switch(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.stack().getItem()).getPath()){case "scribing_tools"->"RESEARCH";case "thaumometer"->"RESEARCH";case "thaumonomicon"->"THAUMONOMICON";case "greatwood_planks","silverwood_planks"->"PLANTS";default->null;};
        else if("knowledgefragment".equals(icon.texture()==null?null:icon.texture().replace("textures/items/","").replace(".png","")))key="KNOWFRAG";
        else if("deconstructor".equals(icon.model()))key="DECONSTRUCTOR";
        if(key==null||!completed().contains(key))return null;var research=ResearchCategories.getResearch(key);
        String recipeKey=!icon.stack().isEmpty()?switch(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(icon.stack().getItem()).getPath()){case "thaumometer"->"Thaumometer";case "scribing_tools"->"Scribe1";case "greatwood_planks"->"PlankGreatwood";case "silverwood_planks"->"PlankSilverwood";case "thaumonomicon"->"Thaumonomicon";default->null;}:"deconstructor".equals(icon.model())?"Deconstructor":null;
        if(recipeKey!=null)for(int i=0;i<research.getPages().length;i++)if(recipeKey.equals(research.getPages()[i].recipeKey)){index=i;break;}
        return new Previous(research,index);
    }
    private void pageSound(){if(minecraft.player!=null)minecraft.player.playSound(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(texture("page")),.66f,1);}
    private void arrow(GuiGraphics gui,int x,int y,int u,int mx,int my){
        boolean hovered=mx>=x-1&&mx<x+13&&my>=y-1&&my<y+9;
        float offset=hovered?(float)Math.sin(animationTicks()*.12f)*.6f:0;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(offset,y,0);gui.blit(BOOK,x,0,u,184,12,8,256,256);gui.pose().popPose();
    }

    private void clampPan(){var data=ResearchCategories.getResearchList(categories.get(category));double minX=data.minDisplayColumn*24-85,maxX=data.maxDisplayColumn*24-112,minY=data.minDisplayRow*24-112,maxY=data.maxDisplayRow*24-61;panX=16-Math.clamp(16-panX,minX,Math.max(minX,maxX-1));panY=17-Math.clamp(17-panY,minY,Math.max(minY,maxY-1));}
    private void drawAspects(GuiGraphics gui,thaumcraft.api.aspects.AspectList aspects,int x,int y,int mx,int my){int i=0;for(var aspect:aspects.getAspectsSorted()){
        int row=y+i++*50;gui.pose().pushPose();gui.pose().translate(x,row,0);gui.pose().scale(2,2,1);BasicsBookRecipes.aspect(gui,aspect,0,0,0);gui.pose().popPose();ResearchBookText.string(gui,aspect.getName(),x+16-ResearchBookText.layout().width(aspect.getName())/2,row+33,0x505050);
        var components=aspect.getComponents();if(components==null)ResearchBookText.string(gui,Component.translatable("tc.aspect.primal").getString(),x+48,row+12,0x444444);
        else{for(int j=0;j<2;j++){int offset=j*42;gui.pose().pushPose();gui.pose().translate(x+54+offset,row+4,0);gui.pose().scale(1.5f,1.5f,1);BasicsBookRecipes.aspect(gui,components[j],0,0,0);gui.pose().popPose();ResearchBookText.string(gui,"§o"+components[j].getName(),x+66+offset-ResearchBookText.layout().width(components[j].getName())/2,row+30,0x505050);}gui.drawString(font,"=",x+39,row+12,0x999999,false);gui.drawString(font,"+",x+83,row+12,0x999999,false);}
        if(mx>=x&&mx<x+40&&my>=row&&my<row+40)drawAspectSources(gui,aspect,mx,my);
    }}
    private void drawAspectSources(GuiGraphics gui,thaumcraft.api.aspects.Aspect aspect,int mx,int my){var scanned=ClientKnowledge.knowledge.getCompound("scanned");var sources=new ArrayList<net.minecraft.world.item.ItemStack>();for(String id:scanned.getAllKeys()){if(!scanned.getBoolean(id)||id.startsWith("entity:")||id.contains("#"))continue;var name=ResourceLocation.tryParse(id);if(name==null||!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(name))continue;var stack=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(name));int amount=RecipeAspects.get(stack,minecraft.level).getOrDefault(aspect.getTag(),0);if(amount>0)sources.add(stack.copyWithCount(amount));}for(int i=0;i<sources.size();i++){int bx=mx+8+i%8*17,by=my-4-sources.size()/8*8+i/8*17;gui.renderItem(sources.get(i),bx,by);gui.renderItemDecorations(font,sources.get(i),bx,by);}}
    void auditBrowser(int column,int row){auditBrowser("THAUMATURGY",column,row);}
    public void auditBrowser(String cat,int column,int row){category=categories.indexOf(cat);opened=null;panX=128-column*24;panY=115-row*24;}
    int auditLeft(){return leftPos;}int auditTop(){return topPos;}String auditCategory(){return categories.get(category);}double[] auditPan(){return new double[]{panX,panY};}
    void auditOpen(ResearchItem item,int page){opened=item;this.page=page;}
    int auditPageCount(){return readablePages().size();}

    @org.jetbrains.annotations.Nullable static net.minecraft.world.item.ItemStack specialIcon(String key){
        return switch(key){
            case "THAUMOMETER"->new net.minecraft.world.item.ItemStack(TC4Modern.THAUMOMETER.get());
            case "BASICTHAUMATURGY"->new net.minecraft.world.item.ItemStack(TC4Modern.BASIC_WAND.get());
            case "ROD_greatwood"->new net.minecraft.world.item.ItemStack(TC4Modern.GREATWOOD_ROD.get());
            case "CAP_gold"->new net.minecraft.world.item.ItemStack(TC4Modern.GOLD_CAP.get());
            case "BELLOWS"->new net.minecraft.world.item.ItemStack(TC4Modern.BELLOWS_ITEM.get());
            case "NODESTABILIZER"->new net.minecraft.world.item.ItemStack(TC4Modern.NODE_STABILIZER_ITEM.get());
            case "VISPOWER"->new net.minecraft.world.item.ItemStack(TC4Modern.NODE_CONVERTER_ITEM.get());
            case "FOCALMANIPULATION"->new net.minecraft.world.item.ItemStack(FocalManipulator.BLOCK.get());
            default->null;
        };
    }
    /** Research icon in the browser and popups: book icons by key (or the key an addon stub mirrors), then the entry's own item or texture. */
    public static void drawResearchIcon(GuiGraphics gui,thaumcraft.api.research.ResearchItem item,int x,int y){drawResearchIcon(gui,item,x,y,false);}
    static void drawResearchIcon(GuiGraphics gui,thaumcraft.api.research.ResearchItem item,int x,int y,boolean hover){
        var source=thaumcraft.api.research.ResearchCategories.getResearch(dev.eugheist.tc4modern.api.TC4Addons.researchIconKey(item.key));
        if(source==null)source=item;
        var basicsIcon=BasicsBookRecipes.researchIcon(source.key);
        var special=specialIcon(source.key);
        if(basicsIcon!=null){if(hover)BasicsBookRecipes.icon(gui,basicsIcon,x,y);else BasicsBookRecipes.drawIcon(gui,basicsIcon,x,y);}
        else if(special!=null)gui.renderItem(special,x,y);
        // ResearchItem(…, ItemStack icon): addon entries (Thaumic Tinkerer) carry their original item icon.
        else if(source.icon_item!=null&&!source.icon_item.isEmpty())gui.renderItem(source.icon_item,x,y);
        else if(source.icon_resource!=null)gui.blit(source.icon_resource,x,y,0,0,16,16,16,16);
    }
    /** Audit: what the icon of a research resolves to, null when nothing visible would be drawn. */
    @org.jetbrains.annotations.Nullable public static String researchIconProblem(net.minecraft.client.Minecraft mc,thaumcraft.api.research.ResearchItem item){
        var source=thaumcraft.api.research.ResearchCategories.getResearch(dev.eugheist.tc4modern.api.TC4Addons.researchIconKey(item.key));
        if(source==null)source=item;
        var icon=BasicsBookRecipes.researchIcon(source.key);
        if(icon!=null){
            if(!icon.stack().isEmpty())return mc.getItemRenderer().getModel(icon.stack(),mc.level,mc.player,0)==mc.getModelManager().getMissingModel()?"missing item model "+icon.stack():null;
            if(icon.model()!=null&&java.util.Set.of("aura_node","deconstructor").contains(icon.model()))return null;
            if(icon.model()!=null)return mc.getModelManager().getModel(BookDisplayModels.id(icon.model()))==mc.getModelManager().getMissingModel()?"missing block model "+icon.model():null;
            if(icon.texture()!=null)return mc.getResourceManager().getResource(ResearchBookText.texture(icon.texture())).isPresent()?null:"missing sprite "+icon.texture();
            return "empty book icon";
        }
        if(specialIcon(source.key)!=null)return null;
        if(source.icon_item!=null&&!source.icon_item.isEmpty())return mc.getItemRenderer().getModel(source.icon_item,mc.level,mc.player,0)==mc.getModelManager().getMissingModel()?"missing item model "+source.icon_item:null;
        if(source.icon_resource!=null)return mc.getResourceManager().getResource(source.icon_resource).isPresent()?null:"missing texture "+source.icon_resource;
        return "no icon";
    }
}
