package dev.eugheist.tc4modern.api;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.AspectList;
import dev.eugheist.tc4modern.ArcaneRecipes;
import dev.eugheist.tc4modern.InfusionItemRecipes;

/**
 * Registration points for addons (Thaumic Energistics and others), the modern counterpart of the ThaumcraftApi calls they used:
 * registerObjectTag, recipe registration and generated object tags. Book pages also draw any vanilla recipe by its full id
 * ("modid:path" as the page recipe key).
 */
public final class TC4Addons {
    private TC4Addons() {}
    private static final Map<String, AspectList> OBJECT_TAGS = new ConcurrentHashMap<>();
    private static final List<AspectProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ArcaneRecipes.Entry> ARCANE = new CopyOnWriteArrayList<>();
    private static final List<InfusionItemRecipes.Recipe> INFUSION = new CopyOnWriteArrayList<>();

    /** Computes tags for items the addon knows about (e.g. from its own recipe types); null when it does not handle the item. */
    @FunctionalInterface public interface AspectProvider { @Nullable AspectList aspects(ItemStack stack, Level level); }

    /** ThaumcraftApi.registerObjectTag: fixed tags for an item id, taking precedence over generated ones. */
    /** Golem hooks (Thaumic Energistics GolemHooks equivalent): extra goals, interactions before the golem GUI, bell pick-up/dismantle. */
    public interface GolemHook {
        /** Called from registerGoals (constructor); goals must check the golem state themselves. */
        default void goals(dev.eugheist.tc4modern.StrawGolem golem, java.util.function.BiConsumer<Integer, net.minecraft.world.entity.ai.goal.Goal> add) {}
        /** Called on both sides before the built-in interactions; anything but PASS stops them. */
        default net.minecraft.world.InteractionResult interact(dev.eugheist.tc4modern.StrawGolem golem, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) { return net.minecraft.world.InteractionResult.PASS; }
        /** Server side, golem bell left click just before the golem is removed. */
        default void removed(dev.eugheist.tc4modern.StrawGolem golem, net.minecraft.world.entity.player.Player player, boolean dismantled) {}
    }
    private static final List<GolemHook> GOLEM_HOOKS = new CopyOnWriteArrayList<>();
    /** 1.7.10 Item.onBlockStartBreak, invoked before modern ServerPlayerGameMode removes a block. True handles the break. */
    @FunctionalInterface public interface BlockBreakHook { boolean start(ItemStack stack, net.minecraft.core.BlockPos pos, net.minecraft.server.level.ServerPlayer player); }
    private static final List<BlockBreakHook> BLOCK_BREAK_HOOKS = new CopyOnWriteArrayList<>();
    public static void registerBlockBreakHook(BlockBreakHook hook) { BLOCK_BREAK_HOOKS.add(hook); }
    public static boolean beginBlockBreak(ItemStack stack, net.minecraft.core.BlockPos pos, net.minecraft.server.level.ServerPlayer player) { for (var hook:BLOCK_BREAK_HOOKS) if(hook.start(stack,pos,player)) return true;return false; }
    /** Energy-backed pickaxes accepted by the arcane bore; payment runs once per protected, successful mining attempt. */
    public interface BoreTool {
        boolean accepts(ItemStack stack);
        boolean canUse(ItemStack stack);
        boolean consume(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity actor);
    }
    private static final List<BoreTool> BORE_TOOLS = new CopyOnWriteArrayList<>();
    public static void registerBoreTool(BoreTool hook) { BORE_TOOLS.add(hook); }
    @Nullable public static BoreTool boreTool(ItemStack stack) { for (var hook : BORE_TOOLS) if (hook.accepts(stack)) return hook; return null; }
    /** Block entities holding a wand a vis charge relay above them may recharge (the arcane workbench is built in). */
    @FunctionalInterface public interface WandHolder { @Nullable ItemStack wand(net.minecraft.world.level.block.entity.BlockEntity tile); }
    private static final List<WandHolder> WAND_HOLDERS = new CopyOnWriteArrayList<>();
    public static void registerWandHolder(WandHolder holder) { WAND_HOLDERS.add(holder); }
    /** The live wand stack of a registered holder, or null. */
    @Nullable public static ItemStack heldWand(net.minecraft.world.level.block.entity.BlockEntity tile) {
        for (var holder : WAND_HOLDERS) { var wand = holder.wand(tile); if (wand != null && !wand.isEmpty()) return wand; }
        return null;
    }
    public static void registerGolemHook(GolemHook hook) { GOLEM_HOOKS.add(hook); }
    private static final Map<String, String> ICON_ALIASES = new ConcurrentHashMap<>();
    /** Book icon of a research taken from another research (addon stubs mirroring an entry of another tab). */
    public static void registerResearchIconAlias(String key, String iconKey) { ICON_ALIASES.put(key, iconKey); }
    public static String researchIconKey(String key) { return ICON_ALIASES.getOrDefault(key, key); }
    public static List<GolemHook> golemHooks() { return GOLEM_HOOKS; }
    public static void registerObjectTag(ResourceLocation item, AspectList tags) { OBJECT_TAGS.put(item.toString(), tags.copy()); }
    public static void registerAspectProvider(AspectProvider provider) { PROVIDERS.add(provider); }
    public static void addArcaneRecipe(ArcaneRecipes.Entry recipe) { ARCANE.add(recipe); }
    private static final Map<String, java.util.function.Function<ItemStack,ItemStack>> INFUSION_OUTPUTS = new ConcurrentHashMap<>();
    /** Addon infusion results with components (charge, dye or state) beyond a plain item. */
    public static void registerInfusionOutput(String key, java.util.function.Function<ItemStack,ItemStack> output) { INFUSION_OUTPUTS.put(key,output); }
    @Nullable public static ItemStack infusionOutput(String key, ItemStack central) { var output=INFUSION_OUTPUTS.get(key);return output==null?null:output.apply(central); }
    private static final Map<String,java.util.function.Supplier<List<ItemStack>>> INFUSION_COMPONENTS = new ConcurrentHashMap<>();
    /** Addon ingredients with tag alternatives; expected stacks persist through the native matrix crafting lifecycle. */
    public static void registerInfusionComponents(String key, java.util.function.Supplier<List<ItemStack>> components) { INFUSION_COMPONENTS.put(key,components); }
    @Nullable public static List<ItemStack> infusionComponents(String key) { var source=INFUSION_COMPONENTS.get(key);return source==null?null:source.get().stream().map(ItemStack::copy).toList(); }
    public static ItemStack tagIngredient(ItemStack display, ResourceLocation tag) { var copy=display.copyWithCount(1);net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,copy,data->data.putString("TC4IngredientTag",tag.toString()));return copy; }
    public static void addInfusionRecipe(InfusionItemRecipes.Recipe recipe) { INFUSION.add(recipe); }
    private static final Map<String, thaumcraft.api.crafting.CrucibleRecipe> CRUCIBLE = new java.util.concurrent.ConcurrentHashMap<>();
    /** ThaumcraftApi.addCrucibleRecipe under a book page key; usable by crucibles, the Thaumatorium, JEI and research pages. */
    public static void addCrucibleRecipe(String key, thaumcraft.api.crafting.CrucibleRecipe recipe) { CRUCIBLE.put(key, recipe); }
    public static Map<String, thaumcraft.api.crafting.CrucibleRecipe> crucibleRecipes() { return Collections.unmodifiableMap(CRUCIBLE); }

    public static List<ArcaneRecipes.Entry> arcaneRecipes() { return Collections.unmodifiableList(ARCANE); }
    public static List<InfusionItemRecipes.Recipe> infusionRecipes() { return Collections.unmodifiableList(INFUSION); }
    @Nullable public static AspectList objectTag(String itemId) { var tags = OBJECT_TAGS.get(itemId); return tags == null ? null : tags.copy(); }
    /** Thaumometer knowledge in a player's persistent data (server) or the synced client copy: has this item been scanned? */
    public static boolean scanned(net.minecraft.nbt.CompoundTag playerData, ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return dev.eugheist.tc4modern.ResearchKnowledge.isScanned(playerData.getCompound(dev.eugheist.tc4modern.ResearchKnowledge.KEY), dev.eugheist.tc4modern.AddonKnowledge.scanKey(stack, id));
    }
    /** PlayerKnowledge.hasDiscoveredAspect for a player's persistent data. */
    public static boolean discovered(net.minecraft.nbt.CompoundTag playerData, thaumcraft.api.aspects.Aspect aspect) {
        return dev.eugheist.tc4modern.ResearchKnowledge.load(playerData).hasDiscoveredAspect("current", aspect);
    }
    @Nullable public static AspectList provided(ItemStack stack, Level level) {
        for (var provider : PROVIDERS) { var tags = provider.aspects(stack, level); if (tags != null && tags.size() > 0) return tags; }
        return null;
    }
}
