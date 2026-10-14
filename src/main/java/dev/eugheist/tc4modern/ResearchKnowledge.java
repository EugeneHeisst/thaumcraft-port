package dev.eugheist.tc4modern;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.PlayerKnowledge;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.lib.research.ResearchManager;

/** NBT/platform bridge to the locally ported TC4 knowledge and scanning classes. */
public final class ResearchKnowledge {
    public static final String KEY = "tc4modern_knowledge";
    private static final String PLAYER = "current";
    private ResearchKnowledge() {}
    public static PlayerKnowledge load(CompoundTag data) {
        PlayerKnowledge player = new PlayerKnowledge();
        CompoundTag knowledge = data.getCompound(KEY);
        AspectList known = new AspectList();
        if (knowledge.contains("Aspects")) known.readFromNBT(knowledge);
        else { // Preserve points from versions 0.1/0.2 while establishing original discovery state.
            for (Aspect aspect : Aspect.aspects.values())
                if (knowledge.contains(aspect.getTag())) known.add(aspect, knowledge.getInt(aspect.getTag()));
        }
        player.aspectsDiscovered.put(PLAYER, known);
        player.addDiscoveredPrimalAspects(PLAYER);
        return player;
    }
    public static void save(CompoundTag data, PlayerKnowledge player) {
        CompoundTag knowledge = data.getCompound(KEY);
        var aspects = player.getAspectsDiscovered(PLAYER);
        aspects.writeToNBT(knowledge);
        for (Aspect aspect : aspects.getAspects()) knowledge.putInt(aspect.getTag(), aspects.getAmount(aspect));
        knowledge.putInt("schema", 3);
        data.put(KEY, knowledge);
    }
    public static void grantAll(CompoundTag data) {
        PlayerKnowledge player=load(data);
        AspectList known=player.getAspectsDiscovered(PLAYER);
        for(Aspect aspect:Aspect.aspects.values()) known.aspects.put(aspect,Math.max(ResearchConfig.aspectCap(),known.getAmount(aspect)));
        save(data,player);
    }
    public static int discoverAll(CompoundTag data) {
        PlayerKnowledge player=load(data);
        AspectList known=player.getAspectsDiscovered(PLAYER);
        int added=0;
        for(Aspect aspect:Aspect.aspects.values()) {
            if(!player.hasDiscoveredAspect(PLAYER,aspect)){known.add(aspect,0);added++;}
        }
        save(data,player);
        return added;
    }
    public static void initialize(CompoundTag data) { save(data, load(data)); }
    public static boolean isScanned(CompoundTag knowledge,String key) {
        return knowledge.getCompound("scanned").getBoolean(key) || knowledge.getBoolean("debugAllItemScans")&&!key.startsWith("entity:")&&!key.startsWith("node:");
    }
    public static void grantAllItemScans(CompoundTag data) {
        var knowledge=data.getCompound(KEY);var scanned=knowledge.getCompound("scanned");
        for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item!=net.minecraft.world.item.Items.AIR) {
            var stack=new net.minecraft.world.item.ItemStack(item);var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
            scanned.putBoolean(StackAspectBridge.scanKey(stack,id),true);
        }
        knowledge.put("scanned",scanned);knowledge.putBoolean("debugAllItemScans",true);data.put(KEY,knowledge);
    }
    public static String scan(CompoundTag data, String objectId, Map<String, Integer> entries) {
        return scan(data, objectId, entries, awarded -> {});
    }
    public static String scan(CompoundTag data, String objectId, Map<String, Integer> entries, java.util.function.Consumer<AspectList> onAwarded) {
        CompoundTag knowledge = data.getCompound(KEY);
        CompoundTag scanned = knowledge.getCompound("scanned");
        if (isScanned(knowledge,objectId)) return "already";
        if (entries.isEmpty()) return "unknown";
        AspectList aspects = new AspectList();
        for (var entry : entries.entrySet()) {
            Aspect aspect = Aspect.getAspect(entry.getKey());
            if (aspect == null) return "unknown";
            aspects.add(aspect, entry.getValue());
        }
        PlayerKnowledge player = load(data);
        if (!ScanManager.validScan(aspects, player, PLAYER)) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                if (player.hasDiscoveredParentAspects(PLAYER, aspect)) continue;
                for (Aspect parent : aspect.getComponents())
                    if (!player.hasDiscoveredAspect(PLAYER, parent)) return "missing:" + parent.getTag();
            }
            return "unknown";
        }
        AspectList awarded = new AspectList();
        for (Aspect aspect : aspects.getAspectsSorted()) {
            int amount = ScanManager.checkAndSyncAspectKnowledge(player, PLAYER, aspect, aspects.getAmount(aspect), ResearchConfig.aspectCap());
            if (amount > 0) awarded.add(aspect, amount);
        }
        scanned.putBoolean(objectId, true);
        knowledge.put("scanned", scanned);
        data.put(KEY, knowledge);
        save(data, player);
        onAwarded.accept(awarded);
        return "scanned";
    }
    public static boolean discover(CompoundTag data, String id, String aspect) {
        return discover(data, id, Map.of(aspect, 1));
    }
    public static boolean discover(CompoundTag data, String id, Map<String, Integer> aspects) {
        return scan(data, id, aspects).equals("scanned");
    }
    public static String combine(CompoundTag data, String first, String second) {
        return combine(data, first, second, new AspectList());
    }
    public static String combine(CompoundTag data, String first, String second, AspectList bonus) {
        Aspect a = Aspect.getAspect(first), b = Aspect.getAspect(second);
        if (a == null || b == null) return "unknown";
        PlayerKnowledge player = load(data);
        if (!player.hasDiscoveredAspect(PLAYER, a) || !player.hasDiscoveredAspect(PLAYER, b)) return "unknown";
        if (player.getAspectPoolFor(PLAYER, a) + bonus.getAmount(a) < (a == b ? 2 : 1)
                || player.getAspectPoolFor(PLAYER, b) + bonus.getAmount(b) < 1) return "insufficient";
        // A research-table experiment spends both points even if no combination exists.
        spend(player, a, bonus);
        spend(player, b, bonus);
        Aspect result = ResearchManager.getCombinationResult(a, b);
        if (result != null) ScanManager.checkAndSyncAspectKnowledge(player, PLAYER, result, 1, ResearchConfig.aspectCap());
        save(data, player);
        return result == null ? "no_combination" : "combined:" + result.getTag();
    }
    public static boolean payCopy(CompoundTag data,AspectList tags,int copies){
        if(copies<0||copies>10000)return false;PlayerKnowledge player=load(data);
        for(Aspect a:tags.getAspects())if(player.getAspectPoolFor(PLAYER,a)<tags.getAmount(a)+copies)return false;
        for(Aspect a:tags.getAspects())player.addAspectPool(PLAYER,a,(short)-(tags.getAmount(a)+copies));save(data,player);return true;
    }
    public static boolean purchase(CompoundTag data, thaumcraft.api.research.ResearchItem research) {
        PlayerKnowledge player=load(data);
        for(Aspect aspect:research.tags.getAspects()) if(!player.hasDiscoveredAspect(PLAYER,aspect) || player.getAspectPoolFor(PLAYER,aspect)<research.tags.getAmount(aspect)) return false;
        for(Aspect aspect:research.tags.getAspects()) player.addAspectPool(PLAYER,aspect,(short)-research.tags.getAmount(aspect));
        save(data,player);return true;
    }
    public static String place(CompoundTag data, thaumcraft.common.lib.research.ResearchNoteData note, int q,int r,Aspect aspect,float chance) {
        return place(data, note, q, r, aspect, chance, new AspectList());
    }
    public static String place(CompoundTag data, thaumcraft.common.lib.research.ResearchNoteData note, int q,int r,Aspect aspect,float chance, AspectList bonus) {
        String cell=q+":"+r;
        var entry=note.hexEntries.get(cell);
        if(note.complete || entry==null || entry.type==1) return "unknown";
        PlayerKnowledge player=load(data);
        var research=ResearchProgress.completed(data);
        if(aspect!=null) {
            if(entry.type!=0 || !player.hasDiscoveredAspect(PLAYER,aspect)) return "unknown";
            boolean free=research.contains("RESEARCHER2") && chance<.1f;
            if(!free && player.getAspectPoolFor(PLAYER,aspect)+bonus.getAmount(aspect)<1) return "insufficient";
            if(!free) spend(player, aspect, bonus);
            note.hexEntries.put(cell,new ResearchManager.HexEntry(aspect,2));
        } else {
            if(entry.type!=2) return "unknown";
            boolean refund=research.contains("RESEARCHER2") ? chance<.5f : research.contains("RESEARCHER1") && chance<.25f;
            if(refund && entry.aspect!=null) player.addAspectPool(PLAYER,entry.aspect,(short)1);
            note.hexEntries.put(cell,new ResearchManager.HexEntry(null,0));
        }
        save(data,player);
        return "placed";
    }
    private static void spend(PlayerKnowledge player, Aspect aspect, AspectList bonus) {
        if (player.getAspectPoolFor(PLAYER, aspect) > 0) player.addAspectPool(PLAYER, aspect, (short)-1);
        else bonus.remove(aspect, 1);
    }
    public static int points(CompoundTag data, String aspect) {
        Aspect key = Aspect.getAspect(aspect);
        return key == null ? 0 : load(data).getAspectPoolFor(PLAYER, key);
    }
    public static void copy(CompoundTag original, CompoundTag replacement) {
        if (original.contains(KEY)) replacement.put(KEY, original.getCompound(KEY).copy());
    }
}
