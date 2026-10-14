package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Greatwood/silverwood in vanilla wood recipes: crafting table, sticks, chest, charcoal; plus their vanilla tags. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class WoodRecipeAudit {
 static int ticks;
 static void check(boolean b,String s){System.out.println((b?"TC4_WOOD PASS ":"TC4_WOOD FAIL ")+s);if(!b)failed=true;}
 static boolean failed;
 static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.woodAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(++ticks!=60)return;
  var server=mc.getSingleplayerServer();server.execute(()->{var level=server.overworld();var rm=level.getRecipeManager();
   for(String wood:List.of("greatwood","silverwood")){
    var planks=new ItemStack(item("tc4modern:"+wood+"_planks"));var log=new ItemStack(item("tc4modern:"+wood+"_log"));
    check(planks.is(ItemTags.PLANKS),wood+" planks in #minecraft:planks");
    check(log.is(ItemTags.LOGS),wood+" log in #minecraft:logs");check(log.is(ItemTags.LOGS_THAT_BURN),wood+" log in #minecraft:logs_that_burn");
    var four=CraftingInput.of(2,2,List.of(planks,planks,planks,planks));
    var r=rm.getRecipeFor(RecipeType.CRAFTING,four,level);check(r.isPresent()&&r.get().value().getResultItem(level.registryAccess()).is(Items.CRAFTING_TABLE),wood+" 2x2 planks -> crafting table ("+r.map(h->h.id().toString()).orElse("none")+")");
    var two=CraftingInput.of(1,2,List.of(planks,planks));var s=rm.getRecipeFor(RecipeType.CRAFTING,two,level);
    check(s.isPresent()&&s.get().value().getResultItem(level.registryAccess()).is(Items.STICK),wood+" 2 planks -> sticks ("+s.map(h->h.id().toString()).orElse("none")+")");
    var ring=new ArrayList<ItemStack>();for(int i=0;i<9;i++)ring.add(i==4?ItemStack.EMPTY:planks);var c=rm.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,ring),level);
    check(c.isPresent()&&c.get().value().getResultItem(level.registryAccess()).is(Items.CHEST),wood+" planks ring -> chest ("+c.map(h->h.id().toString()).orElse("none")+")");
    var f=rm.getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(log),level);
    check(f.isPresent()&&f.get().value().getResultItem(level.registryAccess()).is(Items.CHARCOAL),wood+" log smelts to charcoal ("+f.map(h->h.id().toString()).orElse("none")+")");
    var p=rm.getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(1,1,List.of(log)),level);
    check(p.isPresent(),wood+" log -> planks ("+p.map(h->h.id()+" "+h.value().getResultItem(level.registryAccess())).orElse("none")+")");
   }
   System.out.println(failed?"TC4_WOOD DONE WITH FAILURES":"TC4_WOOD ALL PASS");});
  if(ticks==60)new Thread(()->{try{Thread.sleep(3000);}catch(InterruptedException x){}mc.execute(mc::stop);}).start();}
}
