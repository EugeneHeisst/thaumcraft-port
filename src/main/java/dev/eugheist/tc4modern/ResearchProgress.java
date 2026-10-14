package dev.eugheist.tc4modern;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import thaumcraft.common.config.ConfigResearchData;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

/** Modern per-player storage bridge for original research keys and prerequisites. */
public final class ResearchProgress {
    private ResearchProgress() {}
    /** ResearchItem.getName: addons (Thaumic Tinkerer ttresearch.name/lore) override the tc.research_* keys like the original. */
    public static net.minecraft.network.chat.Component name(String key){var research=thaumcraft.api.research.ResearchCategories.getResearch(key);return research==null?net.minecraft.network.chat.Component.translatable("tc.research_name."+key):net.minecraft.network.chat.Component.literal(research.getName());}
    public static Set<String> completed(CompoundTag playerData) {
        ConfigResearchData.init();
        Set<String> result=new HashSet<>();
        ListTag entries=playerData.getCompound(ResearchKnowledge.KEY).getList("Research",8);
        for(int i=0;i<entries.size();i++) result.add(entries.getString(i));
        for(var category:ResearchCategories.researchCategories.values()) for(ResearchItem item:category.research.values()) if(item.isAutoUnlock()) result.add(item.key);
        return result;
    }
    /** Original browser icons: a clue bypasses concealed visibility, but not unlock prerequisites. */
    public static boolean visible(Set<String> completed, ResearchItem item) {
        if (item == null || item.isVirtual()) return false;
        if (completed.contains(item.key) || completed.contains("@" + item.key)) return true;
        return !item.isLost() && !item.isHidden() && (!item.isConcealed() || requisites(completed, item));
    }
    public static boolean requisites(Set<String> completed, ResearchItem item) {
        if(item==null) return false;
        if(item.parents!=null) for(String parent:item.parents) if(!completed.contains(parent)) return false;
        if(item.parentsHidden!=null) for(String parent:item.parentsHidden) if(!completed.contains(parent)) return false;
        return true;
    }
    public static boolean complete(net.minecraft.world.entity.player.Player player,String key) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer)) return false;
        var before=completed(player.getPersistentData());
        if (!complete(player.getPersistentData(), key)) return false;
        ResearchWarp.award(player, key);
        var research=ResearchCategories.getResearch(key);
        if(research.siblings!=null)for(String sibling:java.util.Arrays.stream(research.siblings).distinct().toList())if(!sibling.equals(key)&&!before.contains(sibling)&&completed(player.getPersistentData()).contains(sibling))ResearchWarp.award(player,sibling);
        return true;
    }
    /** Original book/note completion: notify each newly learned sibling once, with the original learn sound. */
    public static boolean completeAndNotify(net.minecraft.server.level.ServerPlayer player,String key){
        var before=completed(player.getPersistentData());
        if(!complete(player,key))return false;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new ResearchCompletedPayload(key));
        var research=ResearchCategories.getResearch(key);
        if(research.siblings!=null)for(String sibling:java.util.Arrays.stream(research.siblings).distinct().toList())if(!sibling.equals(key)&&!before.contains(sibling)&&completed(player.getPersistentData()).contains(sibling))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new ResearchCompletedPayload(sibling));
        player.level().playSound(null,player.blockPosition(),TC4Modern.LEARN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.75f,1);
        return true;
    }
    /** ResearchManager.completeResearchUnsaved: record a key without prerequisite checks, as the book's sibling repair does. */
    public static boolean add(CompoundTag playerData,String key) {
        Set<String> completed=completed(playerData);
        if(ResearchCategories.getResearch(key)==null||!completed.add(key)) return false;
        ListTag list=new ListTag();completed.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s)));
        CompoundTag knowledge=playerData.getCompound(ResearchKnowledge.KEY);knowledge.put("Research",list);playerData.put(ResearchKnowledge.KEY,knowledge);
        return true;
    }
    public static boolean complete(CompoundTag playerData,String key) {
        Set<String> completed=completed(playerData);
        ResearchItem item=ResearchCategories.getResearch(key);
        if(!requisites(completed,item) || !completed.add(key)) return false;
        if(item.siblings!=null) for(String sibling:item.siblings) if(requisites(completed,ResearchCategories.getResearch(sibling))) completed.add(sibling);
        ListTag list=new ListTag();completed.stream().sorted().forEach(s -> list.add(StringTag.valueOf(s)));
        CompoundTag knowledge=playerData.getCompound(ResearchKnowledge.KEY);knowledge.put("Research",list);playerData.put(ResearchKnowledge.KEY,knowledge);
        return true;
    }
}
