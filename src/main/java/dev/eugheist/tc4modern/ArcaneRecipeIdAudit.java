package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Issue #11: original arcane recipes have ids; a datapack or a recipe-manager removal (as KubeJS does) disables them. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class ArcaneRecipeIdAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_ARCANE_IDS FAIL "+text);System.out.println("TC4_ARCANE_IDS PASS "+text);}
    static List<ItemStack> grid(ArcaneRecipes.Entry e){var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(ItemStack.EMPTY);
        for(int r=0;r<e.height();r++)for(int c=0;c<e.width();c++){var options=e.ingredients().get(r*e.width()+c).getItems();if(options.length>0)grid.set(r*3+c,options[0].copyWithCount(1));}return grid;}
    static boolean craftable(String name){var e=ArcaneRecipes.byName(name);var found=ArcaneRecipes.find(grid(e),Set.of(e.research(),"CAP_gold"));return found!=null&&ItemStack.isSameItem(found.output(),e.output());}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.arcaneRecipeIdAudit"))return;var server=event.getServer();
        try{
            var manager=server.getRecipeManager();var holders=manager.getAllRecipesFor(ArcaneRecipeIds.TYPE.get());
            check(ArcaneRecipes.names().size()==108,"108 original arcane recipes have names");
            check(holders.size()==107,"datapack condition removed exactly one id: "+holders.size()+" present");
            check(manager.byKey(AlchemyContent.id("arcane/golem_bell")).isPresent(),"recipe id tc4modern:arcane/golem_bell exists");
            check(manager.byKey(AlchemyContent.id("arcane/hungry_chest")).isEmpty(),"datapack-disabled tc4modern:arcane/hungry_chest is absent");
            var bell=manager.byKey(AlchemyContent.id("arcane/golem_bell")).get().value();
            check(ItemStack.isSameItem(bell.getResultItem(server.registryAccess()),ArcaneRecipes.byName("golem_bell").output()),"recipe result is the real output, so removal by output works");
            check(!craftable("hungry_chest"),"disabled Hungry Chest no longer crafts in the arcane workbench");
            check(ArcaneRecipes.entries().stream().noneMatch(e->ItemStack.isSameItem(e.output(),ArcaneRecipes.byName("hungry_chest").output())),"disabled recipe is gone from the list JEI shows");
            check(craftable("golem_bell"),"other recipes keep crafting");
            // What KubeJS does: the recipe manager receives the recipe list without the removed id.
            manager.replaceRecipes(manager.getRecipes().stream().filter(h->!h.id().equals(AlchemyContent.id("arcane/golem_bell"))).toList());
            check(!craftable("golem_bell")&&craftable("golem_fetter"),"recipe removed from the recipe manager stops crafting, others stay");
            // Issue #9/#10: Boots of the Traveller walk on powder snow and keep the wearer from freezing, like leather boots.
            var level=server.overworld();var stand=net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level);
            check(!net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(stand)&&stand.canFreeze(),"control: barefoot entity sinks and freezes");
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(TravellerBoots.item()));
            check(net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(stand),"traveller boots walk on powder snow");
            check(!stand.canFreeze(),"traveller boots keep the wearer from freezing");
            // Issue #9: a wand turns a cauldron into a crucible again with the original sound and sparkle.
            var pos=level.getSharedSpawnPos().above(80);level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);level.setBlock(pos,net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState(),3);
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var wand=new ItemStack(TC4Modern.BASIC_WAND.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
            var result=wand.onItemUseFirst(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false)));
            check(level.getBlockState(pos).is(TC4Modern.CRUCIBLE.get()),"wand turns a water cauldron into a crucible ("+result+")");
            System.out.println("TC4_ARCANE_IDS ALL PASS ids, datapack disable, manager removal, outputs");
        }finally{server.halt(false);}
    }
}
