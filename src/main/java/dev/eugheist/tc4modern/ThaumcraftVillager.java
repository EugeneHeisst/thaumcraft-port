package dev.eugheist.tc4modern;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

/** Legacy wizard/banker entity from saves before 0.17.182: turns into a vanilla villager with the same profession on its first tick. */
public final class ThaumcraftVillager extends Villager {
    public ThaumcraftVillager(EntityType<? extends Villager> type,Level level){super(type,level);}
    @Override protected net.minecraft.network.chat.Component getTypeName(){return EntityType.VILLAGER.getDescription();}
    boolean wizard(){return getType()==VillageContent.WIZARD.get();}
    @Override public void tick(){
        if(!level().isClientSide&&!isRemoved()){
            boolean wizard=wizard();var converted=convertTo(EntityType.VILLAGER,true);
            if(converted!=null)converted.setVillagerData(converted.getVillagerData().setProfession((wizard?VillageContent.WIZARD_PROFESSION:VillageContent.BANKER_PROFESSION).get()).setLevel(1));
            // Original professions were permanent: one experience point keeps the job until a job site is found.
            if(converted!=null)converted.setVillagerXp(1);
            return;
        }
        super.tick();
    }
}
