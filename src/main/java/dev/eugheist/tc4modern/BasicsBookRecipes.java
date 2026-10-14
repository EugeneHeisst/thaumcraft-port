package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;

/** Original BASICS recipe presentations, including content whose gameplay port is still pending. */
final class BasicsBookRecipes {
    private static final ResourceLocation OVERLAY=ResearchBookText.texture("textures/gui/gui_researchbook_overlay.png");
    static final Set<String> KEYS=Set.of("Thaumometer","Scribe1","Scribe2","Scribe3","KnowFrag","Thaumonomicon","Clusters","PlankGreatwood","PlankSilverwood","NodeJar","Deconstructor","AdvancedDeconstructor");
    record Icon(ItemStack stack,String texture,String name,int count,String model) {
        static Icon item(String id,int count){return new Icon(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),count),null,null,count,null);}
        static Icon sprite(String path,String name){return new Icon(ItemStack.EMPTY,"textures/items/"+path+".png",name,1,null);}
        static Icon model(String path,String name){return new Icon(ItemStack.EMPTY,null,name,1,path);}
        Component label(){return stack.isEmpty()?Component.translatable(name):stack.getHoverName();}
    }
    record Hit(int x,int y,int width,int height,Icon icon) {}
    private static final List<Hit> hits=new ArrayList<>();
    static void begin(){hits.clear();}
    static Icon hovered(double x,double y){for(int i=hits.size()-1;i>=0;i--){var h=hits.get(i);if(x>=h.x&&x<h.x+h.width&&y>=h.y&&y<h.y+h.height)return h.icon;}return null;}
    static void tooltip(GuiGraphics gui,int x,int y){var icon=hovered(x,y);if(icon!=null)gui.renderTooltip(Minecraft.getInstance().font,icon.label(),x,y);}
    private static Icon item(String id){return Icon.item(id.indexOf(':')<0?"tc4modern:"+id:id,1);}
    static void icon(GuiGraphics gui,Icon icon,int x,int y){drawIcon(gui,icon,x,y);hits.add(new Hit(x,y,16,16,icon));}
    static void drawIcon(GuiGraphics gui,Icon icon,int x,int y){
        if(!icon.stack.isEmpty()){gui.renderItem(icon.stack,x,y);gui.renderItemDecorations(Minecraft.getInstance().font,icon.stack,x,y);}
        else if(icon.model!=null)BookDisplayModels.draw(gui,icon.model,x,y);
        else if(icon.texture!=null)gui.blit(ResearchBookText.texture(icon.texture),x,y,0,0,16,16,16,16);
    }
    static Icon researchIcon(String key){
        if(key.equals("PRIMPEARL"))return item("primordial_pearl");
        if(key.equals("SANITYCHECK"))return new Icon(new ItemStack(SanityChecker.ITEM.get()),null,null,1,null);
        if(key.equals("TRAVELTRUNK"))return new Icon(new ItemStack(TravelingTrunk.ITEM.get()),null,null,1,null);
        if(key.equals("ADVANCEDGOLEM")){var stack=new ItemStack(Golemancy.placer(GolemMaterial.STRAW).get());var t=new net.minecraft.nbt.CompoundTag();t.putByte("advanced",(byte)1);Golemancy.tag(stack,t);return new Icon(stack,null,null,1,null);}
        var golemId=switch(key){case "HUNGRYCHEST"->"hungry_chest";case "GOLEMSTRAW"->"golem_straw";case "GOLEMWOOD"->"golem_wood";case "GOLEMTALLOW"->"golem_tallow";case "GOLEMCLAY"->"golem_clay";case "GOLEMFLESH"->"golem_flesh";case "GOLEMSTONE"->"golem_stone";case "GOLEMIRON"->"golem_iron";case "GOLEMTHAUMIUM"->"golem_thaumium";case "COREFILL"->"golem_core_fill";case "COREEMPTY"->"golem_core_empty";case "GOLEMFETTER"->"golem_fetter";case "GOLEMBELL"->"golem_bell";case "CORELIQUID"->"golem_core_liquid";case "COREALCHEMY"->"golem_core_essentia";case "COREUSE"->"golem_core_use";case "CORESORTING"->"golem_core_sorting";case "COREFISHING"->"golem_core_fishing";case "TINYHAT"->"golem_tiny_hat";case "TINYGLASSES"->"golem_tiny_glasses";case "TINYBOWTIE"->"golem_tiny_bowtie";case "TINYFEZ"->"golem_tiny_fez";case "TINYDART"->"golem_tiny_dart";case "TINYVISOR"->"golem_tiny_visor";case "TINYARMOR"->"golem_tiny_armor";case "TINYHAMMER"->"golem_tiny_hammer";case "CORELUMBER"->"golem_core_lumber";case "COREGUARD"->"golem_core_guard";case "COREBUTCHER"->"golem_core_butcher";case "COREHARVEST"->"golem_core_harvest";case "UPGRADEFIRE"->"golem_upgrade_fire";case "UPGRADEORDER"->"golem_upgrade_order";case "UPGRADEENTROPY"->"golem_upgrade_entropy";case "UPGRADEAIR"->"golem_upgrade_air";case "UPGRADEEARTH"->"golem_upgrade_earth";case "UPGRADEWATER"->"golem_upgrade_water";case "COREGATHER"->"golem_core_gather";default->null;};if(golemId!=null)return item(golemId);
        var alchemyIcon=AlchemyBookRecipes.researchIcon(key);if(alchemyIcon!=null)return alchemyIcon;
        if(key.equals("VISCHARGERELAY"))return item("vis_charge_relay");
        // Original advanced-stabilizer research deliberately uses the normal stabilizer icon.
        if(key.equals("NODESTABILIZERADV"))return item("node_stabilizer");
        if(key.startsWith("ROD_")){String rod=key.substring(4);if(rod.equals("greatwood"))return item("greatwood_rod");if(WandParts.RODS.containsKey(rod))return item(BuiltInRegistries.ITEM.getKey(WandParts.RODS.get(rod).get()).getPath());}
        // Addon caps (Thaumic Tinkerer ICHOR) use uppercase keys that are not valid item paths: take the registered cap item itself.
        if(key.startsWith("CAP_")&&WandParts.CAPS.containsKey(key.substring(4)))return item(BuiltInRegistries.ITEM.getKey(WandParts.CAPS.get(key.substring(4)).item().get()).toString());
        if(key.equals("SCEPTRE"))return new Icon(WandParts.sceptre(WandParts.CAPS.get("thaumium"),WandParts.RODDEFS.get("silverwood")),null,null,1,null);
        if(key.startsWith("ROD_")&&key.endsWith("_staff")){var rod=WandParts.RODS.get(key.substring(4));if(rod!=null)return item(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(rod.get()).getPath());}
        return switch(key){
        case "THAUMOMETER"->item("thaumometer");case "BASICARTIFACE"->item("primal_charm");case "ARCANESTONE"->item("arcane_stone");case "TABLE"->item("table");case "ARCTABLE"->Icon.model("arcane_workbench","block.tc4modern.arcane_workbench");case "RESTABLE"->item("table"); /* original ItemStack(blockTable, 1, 1) */case "GOGGLES"->item("goggles");case "BELLOWS"->item("bellows");case "ENCHFABRIC"->item("enchanted_fabric");case "JARBRAIN"->item("brain_jar");
        case "ARMORFORTRESS"->item("fortress_helmet");case "HELMGOGGLES"->item("goggles");case "WARDEDARCANA"->item("arcane_door");case "ARCANEBORE"->item("arcane_bore");case "ELEMENTALSHOVEL"->item("elemental_shovel");case "ELEMENTALHOE"->item("elemental_hoe");case "ELEMENTALSWORD"->item("elemental_sword");case "ELEMENTALAXE"->item("elemental_axe");case "ELEMENTALPICK"->item("elemental_pickaxe");case "BONEBOW"->item("bone_bow");case "PRIMALARROW"->new Icon(new ItemStack(PrimalArrows.arrow((int)(System.currentTimeMillis()/1000%6))),null,null,1,null);case "BANNERS"->new Icon(CrimsonBanner.colored(10),null,null,1,null);case "MIRROR"->item("mirror");case "MIRRORHAND"->item("hand_mirror");case "MIRRORESSENTIA"->item("mirror_essentia");case "HOVERHARNESS"->item("hover_harness");case "HOVERGIRDLE"->item("hover_girdle");case "BOOTSTRAVELLER"->item("boots_traveller");case "RUNICARMOR"->item("runic_ring");case "RUNICCHARGED"->item("runic_ring_charged");case "RUNICHEALING"->item("runic_ring_regen");case "RUNICEMERGENCY"->item("runic_amulet_emergency");case "RUNICKINETIC"->item("runic_girdle_kinetic");case "ARCANELAMP"->item("arcane_lamp");case "LAMPGROWTH"->item("lamp_growth");case "LAMPFERTILITY"->item("lamp_fertility");case "SINSTONE"->Icon.sprite("sinister_stone_active","item.ItemCompassStone.name");case "FLUXSCRUB"->item("flux_scrubber");case "LEVITATOR"->item("levitator");case "ARCANEEAR"->item("arcane_ear");
        case "GRATE"->item("grate");case "PAVETRAVEL"->item("paving_stone_travel");case "PAVEWARD"->item("paving_stone_warding");
        case "INFUSION"->item("runic_matrix");
        case "ETHEREALBLOOM"->item("ethereal_bloom");
        case "LIQUIDDEATH"->item("bucket_death");case "BOTTLETAINT"->item("bottle_taint");
        case "BASICTHAUMATURGY"->item("iron_capped_wooden_wand");
        case "FOCUSFIRE"->item("focus_fire");case "FOCUSFROST"->item("focus_frost");case "FOCUSHELLBAT"->item("focus_hellbat");
        case "FOCUSEXCAVATION"->item("focus_excavation");case "FOCUSWARDING"->item("focus_warding");case "FOCUSSHOCK"->item("focus_shock");
        case "FOCUSTRADE"->item("focus_trade");case "FOCUSPRIMAL"->item("focus_primal");case "CAP_void"->item("void_cap");case "OCULUS"->item("eldritch_eye");case "ADVALCHEMYFURNACE"->item("advanced_alchemical_construct");case "ESSENTIARESERVOIR"->item("essentia_reservoir");case "PRIMALCRUSHER"->item("primal_crusher");case "ARMORVOIDFORTRESS"->item("void_robe_chest");case "VOIDMETAL"->item("void_ingot");case "FOCUSPORTABLEHOLE"->item("focus_portable_hole");case "FOCUSPOUCH"->item("focus_pouch");
        case "WANDPED"->item("wand_pedestal");case "WANDPEDFOC"->item("wand_pedestal_focus");case "VISAMULET"->item("vis_amulet");
        case "NODESTABILIZER"->item("node_stabilizer");case "VISPOWER"->item("node_converter");case "FOCALMANIPULATION"->item("focal_manipulator");
        case "VAMPBAT"->new Icon(ItemStack.EMPTY,"textures/foci/vampirebats.png","tc.research_name.VAMPBAT",1,null);
        case "ALCHEMICALDUPLICATION"->new Icon(ItemStack.EMPTY,"textures/misc/r_alchmult.png","tc.research_name.ALCHEMICALDUPLICATION",1,null);
        case "ALCHEMICALMANUFACTURE"->new Icon(ItemStack.EMPTY,"textures/misc/r_alchman.png","tc.research_name.ALCHEMICALMANUFACTURE",1,null);
        case "ENTROPICPROCESSING"->new Icon(ItemStack.EMPTY,"textures/misc/r_alchent.png","tc.research_name.ENTROPICPROCESSING",1,null);
        case "PUREIRON"->item("iron_cluster");case "PUREGOLD"->item("gold_cluster");case "PURECOPPER"->item("copper_cluster");
        case "TRANSIRON"->item("minecraft:iron_nugget");case "TRANSGOLD"->item("minecraft:gold_nugget");case "TRANSCOPPER"->item("copper_nugget");
        case "PHIAL"->item("phial");case "CRUCIBLE"->item("crucible");case "NITOR"->item("nitor");case "ALUMENTUM"->item("alumentum");case "THAUMIUM"->item("thaumium_ingot");case "TALLOW"->item("tallow");
        case "RESEARCH"->item("scribing_tools");case "KNOWFRAG"->Icon.sprite("knowledgefragment","item.ItemResource.9.name");
        case "THAUMONOMICON"->item("thaumonomicon");case "PLANTS"->item("greatwood_sapling");
        case "ORE"->item(new String[]{"cinnabar_ore","air_infused_stone","fire_infused_stone","water_infused_stone","earth_infused_stone","order_infused_stone","entropy_infused_stone","amber_ore"}[(int)(System.currentTimeMillis()/1000%8)]);
        case "NODEJAR"->Icon.model("node_jar","item.ItemJarNode.name");case "DECONSTRUCTOR"->Icon.model("deconstructor","tile.blockTable.14.name");case "ADVDECONSTRUCTOR"->Icon.model("advanced_deconstructor","block.tc4modern.advanced_deconstructor");
        case "CRIMSON"->Icon.sprite("crimson_rites","item.ItemEldritchObject.1.name");default->null;};}
    static boolean draw(GuiGraphics gui,String key,int x,int y){
        if(key.equals("AdvAlchemyFurnace")){advancedFurnace(gui,x,y);return true;}
        if(key.equals("InfernalFurnace")){infernal(gui,x,y);return true;}
        if(key.equals("Crucible")||key.equals("Thaumatorium")||key.equals("InfusionAltar")||key.equals("ArcTable")||key.equals("ResTable")){construct(gui,key,x,y);return true;}
        if(!KEYS.contains(key))return false;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        if(key.equals("Thaumonomicon")||key.equals("NodeJar")||key.equals("Crucible")){construct(gui,key,x,y);return true;}
        boolean advanced=key.equals("AdvancedDeconstructor"),arcane=key.equals("Deconstructor")||advanced,shapeless=key.startsWith("Scribe")||key.equals("Clusters");
        center(gui,Component.translatable(arcane?"recipe.type.arcane":shapeless?"recipe.type.workbenchshapeless":"recipe.type.workbench"),x+56,y);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(2,2,1);
        gui.blit(OVERLAY,2,arcane?27:32,arcane?112:60,15,52,52,256,256);
        gui.blit(OVERLAY,20,arcane?7:12,20,3,16,16,256,256);gui.pose().popPose();
        List<Icon> input=new ArrayList<>();int width=3;Icon output;
        switch(key){
            case "Scribe1","Scribe2","Scribe3"->{output=item("scribing_tools");input.add(item(key.equals("Scribe1")?"phial":key.equals("Scribe2")?"minecraft:glass_bottle":"scribing_tools"));if(!key.equals("Scribe3"))input.add(item("minecraft:feather"));input.add(item("minecraft:ink_sac"));}
            case "KnowFrag"->{output=Icon.sprite("researchnotes","item.ItemResearchNotes.name");for(int i=0;i<9;i++)input.add(Icon.sprite("knowledgefragment","item.ItemResource.9.name"));}
            case "PlankGreatwood","PlankSilverwood"->{width=1;String wood=key.equals("PlankGreatwood")?"greatwood":"silverwood";input.add(item(wood+"_log"));output=Icon.item("tc4modern:"+wood+"_planks",4);}
            case "Clusters"->{int variant=(int)(System.currentTimeMillis()/1000%7);String[] elements={"air","fire","water","earth","order","entropy"};for(int i=0;i<6;i++)input.add(item(elements[variant==6?i:variant]+"_shard"));output=Icon.model("cluster_"+variant,"tile.blockCrystal."+variant+".name");}
            case "AdvancedDeconstructor"->{input.add(null);input.add(item("thaumometer"));input.add(null);input.add(item("minecraft:netherite_axe"));input.add(item("table"));input.add(item("minecraft:netherite_pickaxe"));output=Icon.model("advanced_deconstructor","block.tc4modern.advanced_deconstructor");}
            case "Deconstructor"->{input.add(null);input.add(item("thaumometer"));input.add(null);input.add(item("minecraft:golden_axe"));input.add(item("table"));input.add(item("minecraft:golden_pickaxe"));output=Icon.model("deconstructor","tile.blockTable.14.name");}
            default->{String[] elements={"air","fire","water","earth","order","entropy","balanced"};var shard=item(elements[(int)(System.currentTimeMillis()/1000%7)]+"_shard");input.add(null);input.add(shard);input.add(null);input.add(item("minecraft:gold_ingot"));input.add(item("minecraft:glass"));input.add(item("minecraft:gold_ingot"));input.add(null);input.add(shard);input.add(null);output=item("thaumometer");}
        }
        icon(gui,output,x+48,y+(arcane?22:32));
        for(int i=0;i<input.size();i++)if(input.get(i)!=null)icon(gui,input.get(i),x+16+i%width*32,y+(arcane?66:76)+i/width*32);
        if(arcane){wandCost(gui,x,y+164);aspect(gui,Aspect.ENTROPY,advanced?60:20,x+46,y+172);}
        return true;
    }
    private static void construct(GuiGraphics gui,String key,int x,int y){
        boolean jar=key.equals("NodeJar"),thaum=key.equals("Thaumatorium"),infusion=key.equals("InfusionAltar"),restable=key.equals("ResTable"),arctable=key.equals("ArcTable");int dx=jar||infusion?3:1,dy=jar?4:thaum||infusion?3:2,dz=jar||infusion?3:restable?2:1,xoff=64-(dx*16+dz*16)/2,yoff=-dy*25;float size=jar?.2f:0,scale=1-size;
        center(gui,Component.translatable("recipe.type.construct"),x+56,y);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(x+xoff*(1+size),y+108+yoff*scale,0);gui.pose().scale(scale,scale,scale);
        gui.pose().pushPose();gui.pose().translate(-8-xoff,-119+Math.max(3-dx,3-dz)*8+dx*4+dz*4+dy*50,0);gui.pose().scale(2,2,1);gui.blit(OVERLAY,0,0,0,72,64,44,256,256);gui.pose().popPose();
        int index=0;
        for(int j=0;j<dy;j++)for(int k=dz-1;k>=0;k--)for(int i=dx-1;i>=0;i--){
            if(restable&&j==0&&k==1)continue;
            boolean corner=i!=1&&k!=1;if(infusion&&!((j==0&&i==1&&k==1)||(j==1&&corner)||(j==2&&(corner||i==1&&k==1))))continue;
            Icon icon=restable||arctable?(j==1?item("table"):item(restable?"scribing_tools":"iron_capped_wooden_wand")):infusion?item(j==0?"runic_matrix":j==1?"arcane_stone":corner?"arcane_stone_bricks":"arcane_pedestal"):thaum?AlchemyBookRecipes.icon(j==2?"crucible":"@alchemyblock"):jar?(j==0?item("minecraft:"+new String[]{"oak","spruce","birch","jungle","acacia","dark_oak"}[(int)(System.currentTimeMillis()/1000%6)]+"_slab"):index==22?Icon.model("aura_node","tile.blockAiry.0.name"):item("minecraft:glass")):j==0?item("iron_capped_wooden_wand"):item(key.equals("Crucible")?"minecraft:cauldron":"minecraft:bookshelf");
            int px=i*16+k*16,py=-i*8+k*8+j*50;int before=hits.size();icon(gui,icon,px,py);hits.remove(before);hits.add(new Hit((int)(x+xoff*(1+size)+px*scale),(int)(y+108+yoff*scale+py*scale),(int)(16*scale),(int)(16*scale),icon));index++;
        }
        gui.pose().popPose();
        if(thaum){wandCost(gui,x,y+174);int i=0;var cost=new thaumcraft.api.aspects.AspectList().add(Aspect.FIRE,15).add(Aspect.ORDER,30).add(Aspect.WATER,30);for(var a:cost.getAspectsSortedAmount())aspect(gui,a,cost.getAmount(a),x+24+18*i++,y+182);}
        if(infusion){wandCost(gui,x,y+174);int i=0;for(var a:Aspect.getPrimalAspects())aspect(gui,a,25,x+6+18*i++,y+182);}
        if(jar){wandCost(gui,x,y+174);int i=0;for(var a:new thaumcraft.api.aspects.AspectList().add(Aspect.FIRE,70).add(Aspect.EARTH,70).add(Aspect.AIR,70).add(Aspect.WATER,70).add(Aspect.ORDER,70).add(Aspect.ENTROPY,70).getAspectsSortedAmount())aspect(gui,a,70,x+6+18*i++,y+182);}
    }
    /** ConfigRecipes InfernalFurnace: top to bottom, middle east opening and fifty Ignis/Terra. */
    private static void infernal(GuiGraphics gui,int x,int y){
        center(gui,Component.translatable("recipe.type.construct"),x+56,y);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(x-16,y+48,0);gui.pose().scale(2,2,1);gui.blit(OVERLAY,0,0,0,72,64,44,256,256);gui.pose().popPose();
        for(int layer=0;layer<3;layer++)for(int z=2;z>=0;z--)for(int xx=2;xx>=0;xx--){
            boolean central=xx==1&&z==1;if(central&&layer==0)continue;
            Icon material=central&&layer==1?Icon.model("lava","block.minecraft.lava"):item("minecraft:"+(xx%2==0&&z%2==0?"nether_bricks":layer==1&&xx==2&&z==1?"iron_bars":"obsidian"));
            icon(gui,material,x+16+xx*16+z*16,y+33-xx*8+z*8+layer*50);
        }
        wandCost(gui,x,y+174);aspect(gui,Aspect.FIRE,50,x+39,y+182);aspect(gui,Aspect.EARTH,50,x+57,y+182);
    }
    private static void advancedFurnace(GuiGraphics gui,int x,int y){
        center(gui,Component.translatable("recipe.type.construct"),x+56,y);
        for(int layer=0;layer<2;layer++)for(int z=2;z>=0;z--)for(int a=2;a>=0;a--){int kind=AdvancedFurnace.BLUEPRINT[1-layer][a][z];if(kind==0)continue;
            icon(gui,item(switch(kind){case 1->"alembic";case 2->"alchemical_construct";case 3->"alchemical_furnace";default->"advanced_alchemical_construct";}),x+8+a*16+z*16,y+48-a*8+z*8+layer*60);}
        wandCost(gui,x,y+174);aspect(gui,Aspect.FIRE,50,x+30,y+182);aspect(gui,Aspect.WATER,50,x+48,y+182);aspect(gui,Aspect.ORDER,50,x+66,y+182);
    }
    private static void wandCost(GuiGraphics gui,int x,int y){com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.4f);gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(2,2,1);gui.blit(OVERLAY,0,0,68,76,12,12,256,256);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
    private static void center(GuiGraphics gui,Component text,int x,int y){var font=Minecraft.getInstance().font;gui.drawString(font,text,x-font.width(text)/2,y,0x505050,false);}
    static void aspect(GuiGraphics gui,Aspect aspect,int count,int x,int y){RecipeBookBridge.drawTag(gui,aspect,count,x,y);}
}
