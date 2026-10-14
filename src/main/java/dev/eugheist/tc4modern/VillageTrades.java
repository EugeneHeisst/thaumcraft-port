package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** Every original VillageWizardManager / VillageBankerManager offer, in original random-call order. */
final class VillageTrades {
    static ItemStack mod(String key,int count){return PechTrades.mod(key).copyWithCount(count);}
    static MerchantOffer trade(ItemStack buy,ItemStack sell){return new MerchantOffer(new ItemCost(buy.getItem(),buy.getCount()),sell,7,0,0);}
    static void add(List<MerchantOffer> list,ItemStack buy,ItemStack sell){list.add(trade(buy,sell));}
    static List<MerchantOffer> candidates(boolean wizard,RandomSource random){
        var list=new ArrayList<MerchantOffer>();
        add(list,mod("coin",20+random.nextInt(3)),new ItemStack(Items.EMERALD));
        if(wizard){
            add(list,new ItemStack(Items.EMERALD),mod("knowledge_fragment",1));
            add(list,mod("tallow",4+random.nextInt(3)),new ItemStack(Items.EMERALD));
            add(list,new ItemStack(Items.EMERALD),mod("alumentum",1));
            add(list,mod("amber",4+random.nextInt(3)),new ItemStack(Items.EMERALD));
            add(list,new ItemStack(Items.EMERALD),mod("nitor",1));
            add(list,mod("chicken_nugget",24+random.nextInt(8)),new ItemStack(Items.EMERALD));
            add(list,new ItemStack(Items.BOOK,4+random.nextInt(3)),mod("knowledge_fragment",1));
            add(list,mod("beef_nugget",24+random.nextInt(8)),new ItemStack(Items.EMERALD));
            int shards=2+random.nextInt(2);add(list,new ItemStack(Items.EMERALD),new ItemStack(TC4Modern.SHARDS.get(random.nextInt(6)).get(),shards));
            add(list,new ItemStack(Items.EMERALD),mod("mana_bean",1+random.nextInt(2)));
            add(list,new ItemStack(Items.EMERALD,5+random.nextInt(3)),mod("bath_salts",1));
            add(list,new ItemStack(Items.EMERALD,5+random.nextInt(3)),mod("runic_ring_lesser",1));
            add(list,new ItemStack(Items.EMERALD,5+random.nextInt(3)),mod("vis_amulet_lesser",1));
            int rings=5+random.nextInt(3);add(list,new ItemStack(Items.EMERALD,rings),mod("primal_ring_"+thaumcraft.api.aspects.Aspect.getPrimalAspects().get(random.nextInt(6)).getTag(),1));
        }else{
            add(list,mod("coin",2+random.nextInt(2)),new ItemStack(Items.ARROW));
            add(list,mod("coin",6+random.nextInt(3)),new ItemStack(Items.WHITE_WOOL));
            add(list,mod("coin",3+random.nextInt(2)),new ItemStack(Items.PAPER));
            add(list,mod("coin",7+random.nextInt(3)),new ItemStack(Items.BOOK));
            add(list,mod("coin",16+random.nextInt(5)),new ItemStack(Items.EXPERIENCE_BOTTLE));
            add(list,mod("coin",9+random.nextInt(4)),new ItemStack(Items.GLOWSTONE));
            add(list,mod("coin",2+random.nextInt(2)),new ItemStack(Items.COAL));
            add(list,mod("coin",22+random.nextInt(3)),new ItemStack(Items.DIAMOND));
            add(list,mod("coin",6+random.nextInt(3)),new ItemStack(Items.IRON_INGOT));
            add(list,mod("coin",10+random.nextInt(3)),mod("thaumium_ingot",1));
            add(list,mod("coin",25+random.nextInt(8)),new ItemStack(Items.SADDLE));
        }
        return list;
    }
    /** Legacy MerchantRecipe compares item identities but ignores metadata and NBT. */
    static String family(ItemStack stack){
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals(TC4Modern.MODID))return id.toString();
        String name=id.getPath();
        if(java.util.Set.of("coin","knowledge_fragment","tallow","alumentum","amber","nitor","thaumium_ingot").contains(name))return "legacy:ItemResource";
        if(name.endsWith("_shard"))return "legacy:ItemShard";
        if(name.startsWith("primal_ring_"))return "legacy:ItemBaubleBlanks";
        return id.toString();
    }
    static boolean same(MerchantOffer a,MerchantOffer b){return family(a.getBaseCostA()).equals(family(b.getBaseCostA()))&&family(a.getResult()).equals(family(b.getResult()));}
    static void merge(net.minecraft.world.item.trading.MerchantOffers offers,MerchantOffer candidate){
        for(int i=0;i<offers.size();i++)if(same(candidate,offers.get(i))){if(candidate.getBaseCostA().getCount()<offers.get(i).getBaseCostA().getCount())offers.set(i,candidate);return;}
        offers.add(candidate);
    }
}
