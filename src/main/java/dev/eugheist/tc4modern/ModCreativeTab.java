package dev.eugheist.tc4modern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Contents of the single Thaumcraft creative tab (the original also kept everything in Thaumcraft.tabTC):
 * every registered tc4modern item in the original creative order, plus one phial of each aspect like
 * ItemEssence.getSubItems. Items without a sensible creative form are skipped.
 */
public final class ModCreativeTab {
    private ModCreativeTab() {}
    private static final java.util.Set<String> HIDDEN = java.util.Set.of(
        "research_notes_complete");
    private static final java.util.Map<String,Double> ORDER = order();
    private static java.util.Map<String,Double> order() {
        try (var in=ModCreativeTab.class.getResourceAsStream("/creative-tab.json")) {
            var result=new java.util.HashMap<String,Double>();
            for(var entry:com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray()) {
                var row=entry.getAsJsonObject();result.put(row.get("id").getAsString(),row.get("order").getAsDouble());
            }
            return java.util.Map.copyOf(result);
        } catch(java.io.IOException ex){throw new IllegalStateException("Cannot load original creative order",ex);}
    }
    static double rank(Item item){return ORDER.getOrDefault(BuiltInRegistries.ITEM.getKey(item).getPath(),Double.MAX_VALUE);}
    static void fill(CreativeModeTab.Output output) {
        var items=new java.util.ArrayList<Item>();
        BuiltInRegistries.ITEM.forEach(items::add);
        items.sort(java.util.Comparator.comparingDouble(ModCreativeTab::rank));
        for (Item item : items) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(TC4Modern.MODID) || HIDDEN.contains(id.getPath())) continue;
            if (item == TC4Modern.BASIC_WAND.get() || item == TC4Modern.GOLD_GREATWOOD_WAND.get() || item == TC4Modern.THAUMONOMICON_CHEAT.get()) continue;
            // ItemThaumonomicon.getSubItems: the metadata-42 cheat sheet follows the book only while allow_cheat_sheet is on.
            if (item == TC4Modern.THAUMONOMICON.get()) { output.accept(item); if (ResearchConfig.cheatSheet()) output.accept(TC4Modern.THAUMONOMICON_CHEAT.get()); continue; }
            if (item == WandParts.WAND.get()) {
                // ItemWandCasting.getSubItems: iron/wood, gold/greatwood and thaumium/silverwood, all full.
                for (String[] cr : new String[][]{{"iron", "wood"}, {"gold", "greatwood"}, {"thaumium", "silverwood"}}) {
                    var wand = WandParts.make(WandParts.CAPS.get(cr[0]), WandParts.RODDEFS.get(cr[1]));
                    for (var a : thaumcraft.api.aspects.Aspect.getPrimalAspects()) WandData.add(wand, a, WandData.capacity(wand), true);
                    output.accept(wand);
                }
                var sceptre=WandParts.sceptre(WandParts.CAPS.get("thaumium"),WandParts.RODDEFS.get("silverwood"));
                for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())WandData.add(sceptre,a,WandData.capacity(sceptre),true);
                output.accept(sceptre);
                for(String rod:java.util.List.of("silverwood_staff","primal_staff")) {
                    var staff=WandParts.make(WandParts.CAPS.get("thaumium"),WandParts.RODDEFS.get(rod));
                    for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())WandData.add(staff,a,WandData.capacity(staff),true);output.accept(staff);
                }
                continue;
            }
            if (item == EssentiaDevices.ESSENTIA_PHIAL.get()) {
                for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values()) output.accept(EssentiaItems.phialOf(aspect, 8));
                continue;
            }
            if (item == BasicsContent.WISP_ESSENCE.get()) {
                // ItemWispEssence.getSubItems: two of every aspect.
                for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values()) output.accept(WispEssenceItem.of(aspect, 2));
                continue;
            }
            output.accept(new ItemStack(item));
            if(item==CrimsonContent.BANNER.get().asItem())for(int color=0;color<16;color++)output.accept(CrimsonBanner.colored(color));
        }
    }
}
