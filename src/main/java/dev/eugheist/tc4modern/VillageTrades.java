package dev.eugheist.tc4modern;

import java.util.List;
import java.util.function.Function;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Every original VillageWizardManager / VillageBankerManager offer, spread over the five modern profession levels
 * (issue #4): trading earns experience and each new level unlocks the next original offers. Counts keep the original ranges.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class VillageTrades {
    static final int[] XP={2,10,20,30,30};
    static ItemStack mod(String key,int count){return PechTrades.mod(key).copyWithCount(count);}
    static MerchantOffer trade(ItemStack buy,ItemStack sell){return new MerchantOffer(new ItemCost(buy.getItem(),buy.getCount()),sell,7,0,0);}
    /** One offer whose stacks are rolled per villager, like the original random ranges. */
    record Offer(int level,Function<RandomSource,ItemStack> buy,Function<RandomSource,ItemStack> sell) implements VillagerTrades.ItemListing {
        @Override public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader,RandomSource random){
            var cost=buy.apply(random);return new MerchantOffer(new ItemCost(cost.getItem(),cost.getCount()),sell.apply(random),12,XP[level-1],.05f);
        }
    }
    static Function<RandomSource,ItemStack> mod(String key,int min,int extra){return r->mod(key,min+(extra>0?r.nextInt(extra):0));}
    static Function<RandomSource,ItemStack> item(net.minecraft.world.item.Item item,int min,int extra){return r->new ItemStack(item,min+(extra>0?r.nextInt(extra):0));}
    static Function<RandomSource,ItemStack> emerald(int min,int extra){return item(Items.EMERALD,min,extra);}
    static List<Offer> offers(boolean wizard){
        if(wizard)return List.of(
            new Offer(1,mod("coin",20,3),emerald(1,0)),new Offer(1,mod("tallow",4,3),emerald(1,0)),
            new Offer(1,emerald(1,0),r->new ItemStack(TC4Modern.SHARDS.get(r.nextInt(6)).get(),2+r.nextInt(2))),
            new Offer(2,mod("amber",4,3),emerald(1,0)),new Offer(2,emerald(1,0),mod("knowledge_fragment",1,0)),new Offer(2,emerald(1,0),mod("alumentum",1,0)),
            new Offer(3,mod("chicken_nugget",24,8),emerald(1,0)),new Offer(3,mod("beef_nugget",24,8),emerald(1,0)),
            new Offer(3,item(Items.BOOK,4,3),mod("knowledge_fragment",1,0)),new Offer(3,emerald(1,0),mod("nitor",1,0)),new Offer(3,emerald(1,0),mod("mana_bean",1,2)),
            new Offer(4,emerald(5,3),mod("bath_salts",1,0)),new Offer(4,emerald(5,3),mod("runic_ring_lesser",1,0)),
            new Offer(5,emerald(5,3),mod("vis_amulet_lesser",1,0)),
            new Offer(5,emerald(5,3),r->mod("primal_ring_"+thaumcraft.api.aspects.Aspect.getPrimalAspects().get(r.nextInt(6)).getTag(),1)));
        return List.of(
            new Offer(1,mod("coin",20,3),emerald(1,0)),new Offer(1,mod("coin",2,2),item(Items.ARROW,1,0)),new Offer(1,mod("coin",2,2),item(Items.COAL,1,0)),new Offer(1,mod("coin",3,2),item(Items.PAPER,1,0)),
            new Offer(2,mod("coin",6,3),item(Items.WHITE_WOOL,1,0)),new Offer(2,mod("coin",6,3),item(Items.IRON_INGOT,1,0)),new Offer(2,mod("coin",7,3),item(Items.BOOK,1,0)),
            new Offer(3,mod("coin",9,4),item(Items.GLOWSTONE,1,0)),new Offer(3,mod("coin",10,3),mod("thaumium_ingot",1,0)),
            new Offer(4,mod("coin",16,5),item(Items.EXPERIENCE_BOTTLE,1,0)),new Offer(4,mod("coin",25,8),item(Items.SADDLE,1,0)),
            new Offer(5,mod("coin",22,3),item(Items.DIAMOND,1,0)));
    }
    @SubscribeEvent static void trades(VillagerTradesEvent event){
        for(boolean wizard:new boolean[]{true,false})if(event.getType()==(wizard?VillageContent.WIZARD_PROFESSION:VillageContent.BANKER_PROFESSION).get())
            for(var offer:offers(wizard))event.getTrades().get(offer.level()).add(offer);
    }
}
