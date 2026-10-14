package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/**
 * ItemPrimalCrusher: PRIMALVOID material (harvest 5, 500 uses, speed 8, damage 4, enchantability 20), pickaxe and shovel
 * at once, 3x3 on the struck face unless sneaking, clusters like the pickaxe of the core, self-repair and 2 warp.
 */
public final class PrimalCrusher {
    public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("primal_crusher"));
    static final Tier TIER=new net.neoforged.neoforge.common.SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,500,8,4,20,()->Ingredient.of(WandPedestal.PRIMAL_CHARM.get()));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->h.register(ITEM.getId(),new Crusher()));}
    }
    /** isEffective list of the original plus everything a pickaxe or shovel mines. */
    static boolean effective(BlockState s){return s.is(BlockTags.MINEABLE_WITH_PICKAXE)||s.is(BlockTags.MINEABLE_WITH_SHOVEL)||s.is(TC4Modern.TAINT.get())||s.is(TC4Modern.TAINT_FIBRES.get())||s.is(Blocks.OBSIDIAN);}
    public static final class Crusher extends DiggerItem implements thaumcraft.api.IRepairable,thaumcraft.api.IWarpingGear {
        Crusher(){super(TIER,BlockTags.MINEABLE_WITH_PICKAXE,new Properties().rarity(Rarity.EPIC).attributes(DiggerItem.createAttributes(TIER,3.5f,-2.8f)));}
        @Override public float getDestroySpeed(ItemStack s,BlockState state){return effective(state)?TIER.getSpeed():1;}
        /* canHarvestBlock: everything except wood, leaves and plants */
        @Override public boolean isCorrectToolForDrops(ItemStack s,BlockState state){return !(state.is(BlockTags.LOGS)||state.is(BlockTags.PLANKS)||state.is(BlockTags.LEAVES)||state.is(BlockTags.REPLACEABLE_BY_TREES)||state.is(BlockTags.FLOWERS)||state.is(BlockTags.SAPLINGS)||state.is(BlockTags.CROPS))&&(effective(state)||!state.requiresCorrectToolForDrops());}
        @Override public boolean canPerformAction(ItemStack s,net.neoforged.neoforge.common.ItemAbility a){return net.neoforged.neoforge.common.ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(a)||net.neoforged.neoforge.common.ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(a);}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack material){return material.is(WandPedestal.PRIMAL_CHARM.get())||super.isValidRepairItem(s,material);}
        public int getWarp(ItemStack s,net.minecraft.world.entity.player.Player p){return 2;}
        @Override public void inventoryTick(ItemStack s,Level l,Entity entity,int slot,boolean selected){super.inventoryTick(s,l,entity,slot,selected);if(s.isDamaged()&&entity.tickCount%20==0&&entity instanceof LivingEntity)s.setDamageValue(s.getDamageValue()-1);}
        @Override public boolean mineBlock(ItemStack s,Level l,BlockState state,BlockPos pos,LivingEntity miner){
            if(miner.isShiftKeyDown()||!(miner instanceof ServerPlayer p))return super.mineBlock(s,l,state,pos,miner);
            if(!(effective(state)||state.is(BlockTags.MINEABLE_WITH_PICKAXE)))return true;
            /* onBlockStartBreak remembers the struck side; recompute it from the player's view */
            var hit=p.pick(p.blockInteractionRange(),0,false);Direction side=hit instanceof BlockHitResult b&&hit.getType()==HitResult.Type.BLOCK?b.getDirection():Direction.UP;
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){var at=switch(side.getAxis()){case Y->pos.offset(a,0,b);case Z->pos.offset(a,b,0);default->pos.offset(0,b,a);};
                var st=l.getBlockState(at);if(st.isAir()||st.getDestroySpeed(l,at)<0||!effective(st))continue;
                s.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);ElementalTools.harvest(p,at,2);if(s.isEmpty())break;}
            return true;
        }
    }
}
