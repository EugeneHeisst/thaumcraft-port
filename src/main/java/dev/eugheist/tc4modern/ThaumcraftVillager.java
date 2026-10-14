package dev.eugheist.tc4modern;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

/** Original profession villagers retain seven-use offers and trade-triggered replenishment, without invented workstations. */
public final class ThaumcraftVillager extends Villager {
    int tradeTimer;
    public ThaumcraftVillager(EntityType<? extends Villager> type,Level level){
        super(type,level);setVillagerData(getVillagerData().setProfession(type==VillageContent.WIZARD.get()?VillageContent.WIZARD_PROFESSION.get():VillageContent.BANKER_PROFESSION.get()));
        // Modern unemployed-villager brain must not erase the original permanent profession.
        setVillagerXp(1);
    }
    @Override protected net.minecraft.network.chat.Component getTypeName(){return EntityType.VILLAGER.getDescription();}
    boolean wizard(){return getType()==VillageContent.WIZARD.get();}
    @Override protected void updateTrades(){var candidates=VillageTrades.candidates(wizard(),random);VillageTrades.merge(getOffers(),candidates.get(random.nextInt(candidates.size())));}
    @Override public net.minecraft.world.item.trading.MerchantOffers getOffers(){var offers=super.getOffers();for(var offer:offers)offer.resetSpecialPriceDiff();return offers;}
    @Override protected void rewardTradeXp(MerchantOffer offer){if(!getOffers().isEmpty()&&VillageTrades.same(offer,getOffers().getLast()))tradeTimer=40;}
    @Override public boolean showProgressBar(){return false;}
    @Override public boolean canRestock(){return true;}
    @Override public boolean shouldRestock(){return false;}
    @Override public void tick(){
        super.tick();if(level().isClientSide||isTrading()||tradeTimer<=0)return;
        if(--tradeTimer==0){
            var offers=getOffers();if(offers.size()>1)for(int i=0;i<offers.size();i++){
                var old=offers.get(i);if(old.isOutOfStock())offers.set(i,new MerchantOffer(old.getItemCostA(),old.getItemCostB(),old.getResult().copy(),old.getUses(),old.getMaxUses()+random.nextInt(6)+random.nextInt(6)+2,0,0));
            }
            updateTrades();addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,200,0));
            level().broadcastEntityEvent(this,(byte)14);
        }
    }
}
