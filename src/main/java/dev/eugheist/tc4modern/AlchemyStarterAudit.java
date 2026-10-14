package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Actual crucible consumption/ejection, alongside the native book-page audit. */
final class AlchemyStarterAudit {
    static volatile boolean passed;
    static final java.util.Set<String> RESEARCH=java.util.Set.of("BATHSALTS","SANESOAP","ARCANESPA","ETHEREALBLOOM","LIQUIDDEATH","BOTTLETAINT","THAUMIUM","PUREIRON","PUREGOLD","PURECOPPER","TRANSIRON","TRANSGOLD","TRANSCOPPER","PHIAL","CRUCIBLE","NITOR","ALUMENTUM","TALLOW","ALCHEMICALDUPLICATION","ALCHEMICALMANUFACTURE","ENTROPICPROCESSING");
    static void check(boolean ok,String msg){if(!ok)throw new IllegalStateException("TC4_ALCHEMY_STARTER FAIL "+msg);System.out.println("TC4_ALCHEMY_STARTER PASS "+msg);}
    static void run(ServerPlayer player){
        var level=player.serverLevel();var pos=FocalAudit.BASE.offset(3,0,2);
        level.setBlock(pos.below(),TC4Modern.NITOR_BLOCK.get().defaultBlockState(),3);
        level.setBlock(pos,TC4Modern.CRUCIBLE.get().defaultBlockState(),3);
        var tile=(CrucibleEntity)level.getBlockEntity(pos);
        for(var entry:CrucibleRecipes.byKey().entrySet()){
            var recipe=entry.getValue();if(!RESEARCH.contains(recipe.key))continue;
            for(var e:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)))e.discard();
            tile.crucible.water=1000;tile.crucible.heat=200;tile.crucible.aspects=recipe.aspects.copy();
            var catalyst=recipe.catalyst.getItems()[0].copyWithCount(1);
            check(CrucibleRecipe.findMatching(java.util.List.of(recipe),java.util.Set.of(),recipe.aspects,catalyst)==null,"research gate "+entry.getKey());
            var dropped=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,catalyst);dropped.setThrower(player);
            tile.attemptSmelt(dropped);
            var outputs=level.getEntitiesOfClass(thaumcraft.common.entities.EntitySpecialItem.class,new net.minecraft.world.phys.AABB(pos).inflate(2),e->!e.isRemoved());
            System.out.println("TC4_ALCHEMY_OUTPUT "+entry.getKey()+" expected="+recipe.getRecipeOutput()+" found="+outputs.stream().map(e->e.getItem().toString()).toList());
            check(dropped.isRemoved()&&tile.crucible.water==950&&tile.crucible.tagAmount()==0,"exact catalyst, water, aspects "+entry.getKey());
            check(outputs.size()==1&&ItemStack.isSameItemSameComponents(outputs.getFirst().getItem(),recipe.getRecipeOutput())&&outputs.getFirst().getItem().getCount()==recipe.getRecipeOutput().getCount(),"ejected output "+entry.getKey());
            var result=outputs.getFirst();tile.attemptSmelt(result);check(result.isAlive()&&tile.crucible.water==950,"output does not dissolve again "+entry.getKey());
        }
        for(String metal:java.util.List.of("Iron","Gold","Copper")){
            var cluster=CrucibleRecipes.byKey().get("Pure"+metal).getRecipeOutput();
            var smelting=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,new net.minecraft.world.item.crafting.SingleRecipeInput(cluster),level);
            var expected=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(metal.toLowerCase(java.util.Locale.ROOT)+"_ingot"));
            check(smelting.isPresent()&&smelting.get().value().getResultItem(level.registryAccess()).is(expected)&&smelting.get().value().getResultItem(level.registryAccess()).getCount()==2,"cluster smelts into two ingots "+metal);
        }
        var recipe=CrucibleRecipes.byKey().get("AltClay");tile.crucible.water=1000;tile.crucible.heat=200;tile.crucible.aspects=new thaumcraft.api.aspects.AspectList().add(Aspect.WATER,4).add(Aspect.EARTH,8);
        var stack=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(net.minecraft.world.item.Items.CLAY_BALL,8));stack.setThrower(player);tile.attemptSmelt(stack);
        check(stack.getItem().getCount()==4&&tile.crucible.water==800&&tile.crucible.tagAmount()==0,"original decreasing stack loop consumes four of eight catalysts");
        tile.crucible.water=1000;tile.crucible.heat=200;tile.crucible.aspects=CrucibleRecipes.byKey().get("Nitor").aspects.copy();tile.changed();passed=true;
    }
}
