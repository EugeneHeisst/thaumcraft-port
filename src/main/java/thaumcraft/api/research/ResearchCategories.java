package thaumcraft.api.research;

import org.apache.logging.log4j.LogManager;
import java.util.Collection;
import java.util.LinkedHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Level;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;

public class ResearchCategories {
    public static LinkedHashMap<String, ResearchCategoryList> researchCategories = new LinkedHashMap();

    public static ResearchCategoryList getResearchList(String key) {
        return researchCategories.get(key);
    }

    public static String getCategoryName(String key) {
        return Component.translatable("tc.research_category." + key).getString();
    }

    public static ResearchItem getResearch(String key) {
        Collection<ResearchCategoryList> rc = researchCategories.values();
        for (ResearchCategoryList cat : rc) {
            Collection rl = cat.research.values();
            for (Object ri : rl) {
                if (!((ResearchItem)ri).key.equals(key)) continue;
                return (ResearchItem)ri;
            }
        }
        return null;
    }

    public static void registerCategory(String key, ResourceLocation icon, ResourceLocation background) {
        if (ResearchCategories.getResearchList(key) == null) {
            ResearchCategoryList rl = new ResearchCategoryList(icon, background);
            researchCategories.put(key, rl);
        }
    }

    public static void addResearch(ResearchItem ri) {
        ResearchCategoryList rl = ResearchCategories.getResearchList(ri.category);
        if (rl != null && !rl.research.containsKey(ri.key)) {
            if (!ri.isVirtual()) {
                for (ResearchItem rr : rl.research.values()) {
                    if (rr.displayColumn != ri.displayColumn || rr.displayRow != ri.displayRow) continue;
                    LogManager.getLogger().error("Research overlaps: " + ri.key);
                    return;
                }
            }
            rl.research.put(ri.key, ri);
            if (ri.displayColumn < rl.minDisplayColumn) {
                rl.minDisplayColumn = ri.displayColumn;
            }
            if (ri.displayRow < rl.minDisplayRow) {
                rl.minDisplayRow = ri.displayRow;
            }
            if (ri.displayColumn > rl.maxDisplayColumn) {
                rl.maxDisplayColumn = ri.displayColumn;
            }
            if (ri.displayRow > rl.maxDisplayRow) {
                rl.maxDisplayRow = ri.displayRow;
            }
        }
    }
}
