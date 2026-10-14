package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import static dev.eugheist.tc4modern.BasicsBookRecipes.Icon;

/** Original ConfigRecipes presentation, including devices whose gameplay port is still pending. */
final class AlchemyBookRecipes {
    record Page(String output,String pattern,Map<Character,String> inputs,AspectList cost,boolean crucible) {}
    private static final Map<String,Page> PAGES=new LinkedHashMap<>();
    static {
        add("BathSalts","salts","S","S","salis_mundus",cost(Aspect.MIND,6,Aspect.AURA,6,Aspect.ORDER,6,Aspect.HEAL,6),true);
        add("SaneSoap","soap","T","T","@tallowblock",cost(Aspect.MIND,16,Aspect.ELDRITCH,16,Aspect.ORDER,16,Aspect.HEAL,16),true);
        add("ArcaneSpa","@spa","QIQ/SJS/SPS","QISJP","minecraft:quartz_block,minecraft:iron_bars,arcane_stone,warded_jar,minecraft:piston",cost(Aspect.WATER,16,Aspect.ORDER,8,Aspect.EARTH,4),false);
        add("JarVoid","@jar_void","O/J/P","OJP","minecraft:obsidian,warded_jar,minecraft:ender_pearl",cost(Aspect.WATER,5,Aspect.ENTROPY,15),false);
        add("Resonator","resonator","I I/INI/ S ","INS","minecraft:iron_ingot,minecraft:quartz,minecraft:stick",cost(Aspect.WATER,5,Aspect.AIR,5),false);
        add("TubeValve","essentia_valve","TL","TL","essentia_tube,minecraft:lever",cost(Aspect.WATER,5,Aspect.ORDER,5),false);
        add("TubeFilter","essentia_tube_filter","TF","TF","essentia_tube,vis_filter",cost(Aspect.WATER,5,Aspect.ORDER,16),false);
        add("TubeRestrict","essentia_tube_restricted","TS","TS","essentia_tube,minecraft:stone",cost(Aspect.WATER,5,Aspect.EARTH,16),false);
        add("TubeOneway","essentia_tube_oneway","TD","TD","essentia_tube,minecraft:lapis_lazuli",cost(Aspect.WATER,5,Aspect.ORDER,8,Aspect.ENTROPY,8),false);
        add("TubeBuffer","essentia_buffer","PVP/T T/PRP","PVTR","phial,essentia_valve,essentia_tube,essentia_tube_restricted",cost(Aspect.WATER,5,Aspect.ORDER,5),false);
        add("AlchemicalConstruct","@alchemyblock","VTF/TWT/FTV","VTFW","essentia_valve,essentia_tube,vis_filter,greatwood_planks",cost(Aspect.WATER,5,Aspect.ORDER,5),false);
        add("Centrifuge","@centrifuge"," T /ACP/ T ","TACP","essentia_tube,alembic,@alchemyblock,minecraft:piston",cost(Aspect.WATER,5,Aspect.ORDER,5,Aspect.ENTROPY,5),false);
        add("EssentiaCrystalizer","@crystalizer","IDI/QCQ/WTW","IDQCWT","minecraft:iron_ingot,minecraft:dispenser,balanced_shard,@alchemyblock,minecraft:oak_planks,essentia_tube",cost(Aspect.WATER,5,Aspect.EARTH,15,Aspect.ORDER,5),false);
        add("MnemonicMatrix","@brainbox","IAI/ABA/IAI","IAB","minecraft:iron_ingot,amber,zombie_brain",cost(Aspect.FIRE,5,Aspect.WATER,5,Aspect.ORDER,5),false);
    }
    private static AspectList cost(Object... values){var a=new AspectList();for(int i=0;i<values.length;i+=2)a.add((Aspect)values[i],(Integer)values[i+1]);return a;}
    private static void add(String key,String output,String pattern,String letters,String ingredients,AspectList aspects,boolean crucible){var m=new HashMap<Character,String>();var ids=ingredients.split(",");for(int i=0;i<letters.length();i++)m.put(letters.charAt(i),ids[i]);PAGES.put(key,new Page(output,pattern,m,aspects,crucible));}
    static Icon icon(String id){
        if(java.util.Map.of("@centrifuge","centrifuge","@crystalizer","essentia_crystalizer","@jar_void","void_jar","@alchemyblock","alchemical_construct").containsKey(id))return Icon.item("tc4modern:"+java.util.Map.of("@centrifuge","centrifuge","@crystalizer","essentia_crystalizer","@jar_void","void_jar","@alchemyblock","alchemical_construct").get(id),1);
        if(id.equals("@brainbox"))return Icon.item("tc4modern:mnemonic_matrix",1);
        if(id.equals("salts"))return Icon.item("tc4modern:bath_salts",1);
        if(id.equals("soap"))return Icon.item("tc4modern:sanity_soap",1);
        if(id.equals("@spa"))return Icon.item("tc4modern:arcane_spa",1);
        if(id.equals("@tallowblock"))return Icon.item("tc4modern:tallow_block",1);
        if(id.startsWith("@"))return Icon.model(id.substring(1),switch(id.substring(1)){case "spa"->"tile.blockStoneDevice.12.name";case "jar_void"->"tile.blockJar.3.name";case "alchemyblock"->"tile.blockMetalDevice.9.name";case "brainbox"->"tile.blockMetalDevice.12.name";case "tallowblock"->"tile.blockCosmeticSolid.5.name";case "centrifuge"->"tile.blockTube.2.name";case "crystalizer"->"tile.blockTube.7.name";default->"tile.blockTube."+switch(id.substring(1)){case "tube_valve"->1;case "tube_filter"->3;case "tube_buffer"->4;case "tube_restrict"->5;default->6;}+".name";});
        if(id.equals("resonator"))return Icon.sprite("resonator","item.ItemResonator.name");
        return Icon.item(id.contains(":")?id:"tc4modern:"+id,1);
    }
    static Icon researchIcon(String key){return switch(key){case "BATHSALTS"->icon("salts");case "SANESOAP"->icon("soap");case "ARCANESPA"->icon("@spa");case "DISTILESSENTIA"->icon("alembic");case "JARLABEL"->icon("warded_jar");case "JARVOID"->icon("@jar_void");case "TUBES"->icon("essentia_tube");case "TUBEFILTER"->icon("essentia_tube_filter");case "ESSENTIACRYSTAL"->icon("@crystalizer");case "CENTRIFUGE"->icon("@centrifuge");case "THAUMATORIUM"->new Icon(net.minecraft.world.item.ItemStack.EMPTY,"textures/blocks/alchemyblock.png","tc.research_name.THAUMATORIUM",1,null);default->null;};}
    static void audit(){
        var mc=net.minecraft.client.Minecraft.getInstance();
        var icons=new ArrayList<Icon>();for(var p:PAGES.values()){icons.add(icon(p.output));for(var id:p.inputs.values())icons.add(icon(id));}
        for(var icon:icons){
            if(icon.model()!=null){var model=mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResearchBookText.texture("book/"+icon.model())));if(model==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing alchemy model "+icon.model());}
            else if(icon.texture()!=null){if(mc.getResourceManager().getResource(ResearchBookText.texture(icon.texture())).isEmpty())throw new IllegalStateException("Missing alchemy texture "+icon.texture());}
            else if(icon.stack().isEmpty())throw new IllegalStateException("Missing alchemy ingredient "+icon);
        }
        System.out.println("TC4_ALCHEMY_BOOK PASS original recipes="+PAGES.size()+" inputs/outputs="+icons.size());
    }
    static boolean draw(GuiGraphics gui,String key,int x,int y){
        if(java.util.Set.of("BathSalts","SaneSoap","ArcaneSpa").contains(key))return false;
        if(key.equals("JarLabelNull")){RecipeBookBridge.title(gui,"recipe.type.workbenchshapeless",x,y);RecipeBookBridge.background(gui,x,y,2,32,60,15,52,52);RecipeBookBridge.background(gui,x,y,20,12,20,3,16,16);BasicsBookRecipes.icon(gui,icon("jar_label"),x+48,y+32);var marked=icon("jar_label").stack();EssentiaItems.setAspects(marked,new AspectList().add(Aspect.AIR,0));RecipeBookBridge.item(gui,marked,x+16,y+76,false);return true;}
        if(key.equals("JarLabelAspect")){RecipeBookBridge.title(gui,"recipe.type.workbenchshapeless",x,y);RecipeBookBridge.background(gui,x,y,2,32,60,15,52,52);RecipeBookBridge.background(gui,x,y,20,12,20,3,16,16);var a=Aspect.aspects.values().stream().toList().get((int)(System.currentTimeMillis()/1000%Aspect.aspects.size()));var label=icon("jar_label").stack();EssentiaItems.setAspects(label,new AspectList().add(a,0));RecipeBookBridge.item(gui,label,x+48,y+32,true);RecipeBookBridge.item(gui,EssentiaItems.phialOf(a,8),x+16,y+76,false);BasicsBookRecipes.icon(gui,icon("jar_label"),x+48,y+76);return true;}
        var p=PAGES.get(key);if(p==null)return false;
        RecipeBookBridge.title(gui,p.crucible?"recipe.type.crucible":"recipe.type.arcane",x,y);
        if(p.crucible){RecipeBookBridge.background(gui,x,y+28,0,0,0,3,56,17);RecipeBookBridge.background(gui,x,y+28,0,32,0,20,56,48);RecipeBookBridge.background(gui,x,y+28,21,24,100,84,11,13);BasicsBookRecipes.icon(gui,icon(p.output),x+48,y+36);BasicsBookRecipes.icon(gui,icon(p.inputs.values().iterator().next()),x+26,y+72);int n=0;for(var a:p.cost.getAspectsSorted())RecipeBookBridge.drawTag(gui,a,p.cost.getAmount(a),x+28+n%3*20+(n/3==1?20:0),y+118+n++/3*20);}
        else {RecipeBookBridge.background(gui,x,y,2,27,112,15,52,52);RecipeBookBridge.background(gui,x,y,20,7,20,3,16,16);BasicsBookRecipes.icon(gui,icon(p.output),x+48,y+22);var rows=p.pattern.split("/");for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++){var id=p.inputs.get(rows[row].charAt(col));if(id!=null)BasicsBookRecipes.icon(gui,icon(id),x+16+col*32,y+66+row*32);}int n=0;for(var a:p.cost.getAspectsSortedAmount())RecipeBookBridge.drawTag(gui,a,p.cost.getAmount(a),x+14+18*n+++(5-p.cost.size())*8,y+172);}
        return true;
    }
}
