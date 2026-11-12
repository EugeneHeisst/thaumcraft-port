package thaumcraft.common.lib.research;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original combination and primal-reduction methods; remaining research systems are pending. */
public final class ResearchManager {
    public static Aspect getCombinationResult(Aspect aspect1, Aspect aspect2) {
        Collection<Aspect> aspects = Aspect.aspects.values();
        for (Aspect aspect : aspects) {
            if (aspect.getComponents() == null || (aspect.getComponents()[0] != aspect1 || aspect.getComponents()[1] != aspect2) && (aspect.getComponents()[0] != aspect2 || aspect.getComponents()[1] != aspect1)) continue;
            return aspect;
        }
        return null;
    }
    public static AspectList reduceToPrimals(AspectList al) {
        return ResearchManager.reduceToPrimals(al, false);
    }
    public static AspectList reduceToPrimals(AspectList al, boolean merge) {
        AspectList out = new AspectList();
        for (Aspect aspect : al.getAspects()) {
            if (aspect == null) continue;
            if (aspect.isPrimal()) {
                if (merge) {
                    out.merge(aspect, al.getAmount(aspect));
                    continue;
                }
                out.add(aspect, al.getAmount(aspect));
                continue;
            }
            AspectList send = new AspectList();
            send.add(aspect.getComponents()[0], al.getAmount(aspect));
            send.add(aspect.getComponents()[1], al.getAmount(aspect));
            send = ResearchManager.reduceToPrimals(send, merge);
            for (Aspect a : send.getAspects()) {
                if (merge) {
                    out.merge(a, send.getAmount(a));
                    continue;
                }
                out.add(a, send.getAmount(a));
            }
        }
        return out;
    }

public static boolean checkResearchCompletion(CompoundTag contents, ResearchNoteData note, String username, PlayerKnowledge knowledge) {
        ArrayList<String> checked = new ArrayList<String>();
        ArrayList<String> main = new ArrayList<String>();
        ArrayList<String> remains = new ArrayList<String>();
        for (HexUtils.Hex hex : note.hexes.values()) {
            if (((HexEntry)note.hexEntries.get((Object)hex.toString())).type != 1) continue;
            main.add(hex.toString());
        }
        for (HexUtils.Hex hex : note.hexes.values()) {
            if (((HexEntry)note.hexEntries.get((Object)hex.toString())).type != 1) continue;
            main.remove(hex.toString());
            ResearchManager.checkConnections(note, hex, checked, main, remains, username, knowledge);
            break;
        }
        if (main.size() == 0) {
            ArrayList<String> remove = new ArrayList<String>();
            for (HexUtils.Hex hex : note.hexes.values()) {
                if (((HexEntry)note.hexEntries.get((Object)hex.toString())).type == 1 || remains.contains(hex.toString())) continue;
                remove.add(hex.toString());
            }
            for (String s : remove) {
                note.hexEntries.remove(s);
                note.hexes.remove(s);
            }
            note.complete = true;
            ResearchManager.updateData(contents, note);
            return true;
        }
        return false;
    }

private static void checkConnections(ResearchNoteData note, HexUtils.Hex hex, ArrayList<String> checked, ArrayList<String> main, ArrayList<String> remains, String username, PlayerKnowledge knowledge) {
        checked.add(hex.toString());
        for (int a = 0; a < 6; ++a) {
            HexUtils.Hex target = hex.getNeighbour(a);
            if (checked.contains(target.toString()) || !note.hexEntries.containsKey(target.toString()) || ((HexEntry)note.hexEntries.get((Object)target.toString())).type < 1) continue;
            Aspect aspect1 = ((HexEntry)note.hexEntries.get((Object)hex.toString())).aspect;
            Aspect aspect2 = ((HexEntry)note.hexEntries.get((Object)target.toString())).aspect;
            if (!knowledge.hasDiscoveredAspect(username, aspect1) || !knowledge.hasDiscoveredAspect(username, aspect2) || (aspect1.isPrimal() || aspect1.getComponents()[0] != aspect2 && aspect1.getComponents()[1] != aspect2) && (aspect2.isPrimal() || aspect2.getComponents()[0] != aspect1 && aspect2.getComponents()[1] != aspect1)) continue;
            remains.add(target.toString());
            if (((HexEntry)note.hexEntries.get((Object)target.toString())).type == 1) {
                main.remove(target.toString());
            }
            ResearchManager.checkConnections(note, target, checked, main, remains, username, knowledge);
        }
    }

public static CompoundTag createNote(CompoundTag stack, String key, Random random) {
        ResearchItem rr = ResearchCategories.getResearch((String)key);
        Aspect primaryaspect = rr.getResearchPrimaryTag();
        if (primaryaspect == null) {
            return null;
        }
        stack.putString("key", key);
        stack.putInt("color", primaryaspect.getColor());
        stack.putBoolean("complete", false);
        stack.putInt("copies", 0);
        int radius = 1 + Math.min(3, rr.getComplexity());
        HashMap<String, HexUtils.Hex> hexLocs = HexUtils.generateHexes((int)radius);
        ArrayList<HexUtils.Hex> outerRing = HexUtils.distributeRingRandomly((int)radius, (int)rr.tags.size(), (Random)random);
        HashMap<String, HexEntry> hexEntries = new HashMap<String, HexEntry>();
        HashMap<String, HexUtils.Hex> hexes = new HashMap<String, HexUtils.Hex>();
        for (HexUtils.Hex hex : hexLocs.values()) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new HexEntry(null, 0));
        }
        int count = 0;
        for (HexUtils.Hex hex : outerRing) {
            hexes.put(hex.toString(), hex);
            hexEntries.put(hex.toString(), new HexEntry(rr.tags.getAspects()[count], 1));
            ++count;
        }
        if (rr.getComplexity() > 1) {
            int blanks = rr.getComplexity() * 2;
            HexUtils.Hex[] temp = hexes.values().toArray(new HexUtils.Hex[0]);
            while (blanks > 0) {
                int indx = random.nextInt(temp.length);
                if (hexEntries.get(temp[indx].toString()) == null || ((HexEntry)hexEntries.get((Object)temp[indx].toString())).type != 0) continue;
                boolean gtg = true;
                for (int n = 0; n < 6; ++n) {
                    HexUtils.Hex neighbour = temp[indx].getNeighbour(n);
                    if (!hexes.containsKey(neighbour.toString()) || ((HexEntry)hexEntries.get((Object)neighbour.toString())).type != 1) continue;
                    int cc = 0;
                    for (int q = 0; q < 6; ++q) {
                        if (hexes.containsKey(((HexUtils.Hex)hexes.get(neighbour.toString())).getNeighbour(q).toString())) {
                            ++cc;
                        }
                        if (cc >= 2) break;
                    }
                    if (cc >= 2) continue;
                    gtg = false;
                    break;
                }
                if (!gtg) continue;
                hexes.remove(temp[indx].toString());
                hexEntries.remove(temp[indx].toString());
                temp = hexes.values().toArray(new HexUtils.Hex[0]);
                --blanks;
            }
        }
        ListTag gridtag = new ListTag();
        for (HexUtils.Hex hex : hexes.values()) {
            CompoundTag gt = new CompoundTag();
            gt.putByte("hexq", (byte)hex.q);
            gt.putByte("hexr", (byte)hex.r);
            gt.putByte("type", (byte)((HexEntry)hexEntries.get((Object)hex.toString())).type);
            if (((HexEntry)hexEntries.get((Object)hex.toString())).aspect != null) {
                gt.putString("aspect", ((HexEntry)hexEntries.get((Object)hex.toString())).aspect.getTag());
            }
            gridtag.add(gt);
        }
        stack.put("hexgrid", gridtag);
        return stack;
    }

public static ResearchNoteData getData(CompoundTag stack) {
        if (stack == null) {
            return null;
        }
        ResearchNoteData data = new ResearchNoteData();
        if (!stack.contains("key")) return null;
        data.key = stack.getString("key");
        data.color = stack.getInt("color");
        data.complete = stack.getBoolean("complete");
        data.copies = stack.getInt("copies");
        ListTag grid = stack.getList("hexgrid", 10);
        data.hexEntries = new HashMap();
        for (int x = 0; x < grid.size(); ++x) {
            CompoundTag nbt = grid.getCompound(x);
            byte q = nbt.getByte("hexq");
            byte r = nbt.getByte("hexr");
            byte type = nbt.getByte("type");
            String tag = nbt.getString("aspect");
            Aspect aspect = tag != null ? Aspect.getAspect(tag) : null;
            HexUtils.Hex hex = new HexUtils.Hex((int)q, (int)r);
            data.hexEntries.put(hex.toString(), new HexEntry(aspect, (int)type));
            data.hexes.put(hex.toString(), hex);
        }
        return data;
    }

public static void updateData(CompoundTag stack, ResearchNoteData data) {
        stack.putString("key", data.key);
        stack.putInt("color", data.color);
        stack.putBoolean("complete", data.complete);
        stack.putInt("copies", data.copies);
        ListTag gridtag = new ListTag();
        for (HexUtils.Hex hex : data.hexes.values()) {
            CompoundTag gt = new CompoundTag();
            gt.putByte("hexq", (byte)hex.q);
            gt.putByte("hexr", (byte)hex.r);
            gt.putByte("type", (byte)((HexEntry)data.hexEntries.get((Object)hex.toString())).type);
            if (((HexEntry)data.hexEntries.get((Object)hex.toString())).aspect != null) {
                gt.putString("aspect", ((HexEntry)data.hexEntries.get((Object)hex.toString())).aspect.getTag());
            }
            gridtag.add(gt);
        }
        stack.put("hexgrid", gridtag);
    }
public static class HexEntry { public Aspect aspect; public int type; public HexEntry(Aspect aspect, int type) { this.aspect = aspect; this.type = type; } }
}
