package dev.eugheist.tc4modern;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;

/**
 * WandCap / WandRod definitions (ConfigItems.init and postInit) and the generic composite wand
 * (ItemWandCasting with "cap"/"rod" NBT). Silver caps stay disabled because, as in the original,
 * they only exist when a silver ingot is registered (none in vanilla 1.21); copper is enabled
 * because vanilla 1.21 has copper ingots.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WandParts {
    private WandParts() {}
    public record Cap(String tag, float baseCostModifier, List<Aspect> specialAspects, float specialCostModifier, Supplier<Item> item, int craftCost) {
        public float modifier(Aspect a) { return specialAspects.contains(a) ? specialCostModifier : baseCostModifier; }
        public String research() { return "CAP_" + tag; }
    }
    public record Rod(String tag, int capacity, Supplier<Item> item, int craftCost, Aspect regen, boolean glowing) {
        public String research() { return "ROD_" + tag; }
        public boolean staff() { return tag.endsWith("_staff"); }
        public boolean runes() { return tag.equals("primal_staff"); }
        public String baseTag() { return staff()?tag.substring(0,tag.length()-6):tag; }
    }
    private static DeferredHolder<Item, Item> item(String n) { return DeferredHolder.create(Registries.ITEM, AlchemyContent.id(n)); }
    public static final DeferredHolder<Item, Item> WAND = item("wand");
    public static final DeferredHolder<Item, Item> COPPER_CAP = item("copper_cap"), THAUMIUM_CAP_INERT = item("thaumium_cap_inert"), THAUMIUM_CAP = item("thaumium_cap"), COPPER_NUGGET = item("copper_nugget"), VOID_CAP_INERT = item("void_cap_inert"), VOID_CAP = item("void_cap"), VOID_NUGGET = item("void_nugget");
    public static final Map<String, DeferredHolder<Item, Item>> RODS = new LinkedHashMap<>();
    static {
        for (String r : List.of("obsidian", "ice", "quartz", "reed", "blaze", "bone", "silverwood")) RODS.put(r, item(r + "_rod"));
        for (String r : List.of("greatwood", "obsidian", "ice", "quartz", "reed", "blaze", "bone", "silverwood", "primal")) RODS.put(r+"_staff",item(r+"_staff_rod"));
    }

    public static final Map<String, Cap> CAPS = new LinkedHashMap<>();
    public static final Map<String, Rod> RODDEFS = new LinkedHashMap<>();
    static {
        CAPS.put("iron", new Cap("iron", 1.1f, List.of(), 1f, () -> TC4Modern.IRON_CAP.get(), 1));
        CAPS.put("gold", new Cap("gold", 1.0f, List.of(), 1f, () -> TC4Modern.GOLD_CAP.get(), 3));
        CAPS.put("thaumium", new Cap("thaumium", .9f, List.of(), 1f, () -> THAUMIUM_CAP.get(), 6));
        /* ConfigItems: WAND_CAP_VOID = new WandCap("void", 0.8f, itemWandCap:7, 9) */
        CAPS.put("void", new Cap("void", .8f, List.of(), 1f, () -> VOID_CAP.get(), 9));
        CAPS.put("copper", new Cap("copper", 1.1f, List.of(Aspect.ORDER, Aspect.ENTROPY), 1.0f, () -> COPPER_CAP.get(), 2));
        RODDEFS.put("wood", new Rod("wood", 25, () -> Items.STICK, 1, null, false));
        RODDEFS.put("greatwood", new Rod("greatwood", 50, () -> TC4Modern.GREATWOOD_ROD.get(), 3, null, false));
        RODDEFS.put("obsidian", new Rod("obsidian", 75, () -> RODS.get("obsidian").get(), 6, Aspect.EARTH, false));
        RODDEFS.put("blaze", new Rod("blaze", 75, () -> RODS.get("blaze").get(), 6, Aspect.FIRE, true));
        RODDEFS.put("ice", new Rod("ice", 75, () -> RODS.get("ice").get(), 6, Aspect.WATER, false));
        RODDEFS.put("quartz", new Rod("quartz", 75, () -> RODS.get("quartz").get(), 6, Aspect.ORDER, false));
        RODDEFS.put("bone", new Rod("bone", 75, () -> RODS.get("bone").get(), 6, Aspect.ENTROPY, false));
        RODDEFS.put("reed", new Rod("reed", 75, () -> RODS.get("reed").get(), 6, Aspect.AIR, false));
        RODDEFS.put("silverwood", new Rod("silverwood", 100, () -> RODS.get("silverwood").get(), 9, null, false));
        for (String base : List.of("greatwood","obsidian","ice","quartz","reed","blaze","bone","silverwood","primal")) {
            String key=base+"_staff";var normal=RODDEFS.get(base);
            int capacity=base.equals("greatwood")?125:base.equals("silverwood")||base.equals("primal")?250:175;
            int cost=base.equals("greatwood")?8:base.equals("silverwood")?24:base.equals("primal")?32:14;
            RODDEFS.put(key,new Rod(key,capacity,()->RODS.get(key).get(),cost,normal==null?null:normal.regen(),base.equals("blaze")));
        }
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.ITEM, h -> {
            // ItemWandCasting: capacity and cost come from the stack's rod and cap.
            h.register(WAND.getId(), new BasicWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 0, 1f));
            h.register(COPPER_CAP.getId(), new Item(new Item.Properties()));
            h.register(THAUMIUM_CAP_INERT.getId(), new Item(new Item.Properties()));
            h.register(THAUMIUM_CAP.getId(), new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
            h.register(COPPER_NUGGET.getId(), new Item(new Item.Properties()));
            h.register(VOID_NUGGET.getId(), new Item(new Item.Properties()));
            h.register(VOID_CAP_INERT.getId(), new Item(new Item.Properties()));
            h.register(VOID_CAP.getId(), new Item(new Item.Properties().rarity(Rarity.RARE)));
            for (var e : RODS.entrySet()) h.register(e.getValue().getId(), new Item(new Item.Properties()));
        });
    }

    /** Legacy fixed wands keep working as iron/wood and gold/greatwood. */
    public static Cap cap(ItemStack wand) {
        var data = wand.get(DataComponents.CUSTOM_DATA);
        if (data != null) { var c = CAPS.get(data.copyTag().getString("cap")); if (c != null) return c; }
        return wand.is(TC4Modern.GOLD_GREATWOOD_WAND.get()) ? CAPS.get("gold") : CAPS.get("iron");
    }
    public static Rod rod(ItemStack wand) {
        var data = wand.get(DataComponents.CUSTOM_DATA);
        if (data != null) { var r = RODDEFS.get(data.copyTag().getString("rod")); if (r != null) return r; }
        return wand.is(TC4Modern.GOLD_GREATWOOD_WAND.get()) ? RODDEFS.get("greatwood") : RODDEFS.get("wood");
    }
    public static ItemStack make(Cap cap, Rod rod) {
        var out = new ItemStack(WAND.get());
        CustomData.update(DataComponents.CUSTOM_DATA, out, t -> { t.putString("cap", cap.tag()); t.putString("rod", rod.tag()); });
        if(rod.staff())out.set(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(AlchemyContent.id("staff_attack"),6,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build());
        return out;
    }
    public static boolean isStaff(ItemStack wand) { return rod(wand).staff(); }
    public static boolean isSceptre(ItemStack wand) { return NoteData.tag(wand).contains("sceptre"); }
    public static ItemStack sceptre(Cap cap,Rod rod) { var stack=make(cap,rod);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putByte("sceptre",(byte)1));return stack; }
    /** item.Wand.name = %CAP %ROD %OBJ */
    public static Component name(ItemStack wand) {
        var lang = net.minecraft.locale.Language.getInstance();
        String s = lang.getOrDefault("item.Wand.name", "%CAP %ROD %OBJ")
            .replace("%CAP", lang.getOrDefault("item.Wand." + cap(wand).tag() + ".cap", cap(wand).tag()))
            .replace("%ROD", lang.getOrDefault("item.Wand." + rod(wand).baseTag() + ".rod", rod(wand).baseTag()))
            .replace("%OBJ", lang.getOrDefault(isStaff(wand)?"item.Wand.staff.obj":isSceptre(wand)?"item.Wand.sceptre.obj":"item.Wand.wand.obj", "Wand"));
        return Component.literal(s);
    }
    /** ArcaneWandRecipe: caps at slots 2 and 6, rod at 4, all else empty; never wood+iron; cost cap*rod of every primal. */
    static ArcaneRecipes.Entry recipe(List<ItemStack> grid, java.util.Set<String> completed) {
        if (grid.size() < 9) return null;
        var sceptre=sceptreRecipe(grid,completed);if(sceptre!=null)return sceptre;
        for (int i : new int[]{0, 1, 3, 5, 7, 8}) if (!grid.get(i).isEmpty()) return null;
        ItemStack c1 = grid.get(6), c2 = grid.get(2), r = grid.get(4);
        if (c1.isEmpty() || c2.isEmpty() || r.isEmpty() || !c1.is(c2.getItem())) return null;
        Cap cap = null; Rod rod = null;
        for (Cap c : CAPS.values()) if (c1.is(c.item().get()) && (c.tag().equals("iron") || completed.contains(c.research()))) { cap = c; break; }
        for (Rod d : RODDEFS.values()) if (r.is(d.item().get()) && (d.tag().equals("wood") || completed.contains(d.research()))) { rod = d; break; }
        if (cap == null || rod == null || cap.tag().equals("iron") && rod.tag().equals("wood")) return null;
        int cost = cap.craftCost() * rod.craftCost();
        var al = new thaumcraft.api.aspects.AspectList();
        for (Aspect a : Aspect.getPrimalAspects()) al.add(a, cost);
        final Cap fc = cap; final Rod fr = rod;
        List<java.util.function.Predicate<ItemStack>> preds = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i == 2 || i == 6) preds.add(s -> s.is(fc.item().get()));
            else if (i == 4) preds.add(s -> s.is(fr.item().get()));
            else preds.add(null);
        }
        var shape = new thaumcraft.api.crafting.ArcanePattern<ItemStack>("", 3, 3, preds).setMirrored(false);
        List<net.minecraft.world.item.crafting.Ingredient> ing = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) ing.add(i == 2 || i == 6 ? net.minecraft.world.item.crafting.Ingredient.of(cap.item().get()) : i == 4 ? net.minecraft.world.item.crafting.Ingredient.of(rod.item().get()) : net.minecraft.world.item.crafting.Ingredient.EMPTY);
        return new ArcaneRecipes.Entry("WandArcane", "", 3, 3, ing, make(cap, rod), al, shape);
    }
    /** ArcaneSceptreRecipe: three equal caps, any researched rod, primal charm; floor(1.5*capCost*rodCost). CAP_iron and ROD_wood are auto-unlocked research in the original, so iron caps and the wooden rod always qualify. */
    static ArcaneRecipes.Entry sceptreRecipe(List<ItemStack> grid,java.util.Set<String> completed) {
        if(grid.size()<9 || !completed.contains("SCEPTRE"))return null;
        for(int i:new int[]{0,3,7,8})if(!grid.get(i).isEmpty())return null;
        if(!grid.get(2).is(WandPedestal.PRIMAL_CHARM.get()))return null;
        Cap cap=null;Rod rod=null;
        for(var c:CAPS.values())if(grid.get(1).is(c.item().get())&&grid.get(5).is(c.item().get())&&grid.get(6).is(c.item().get())&&(c.tag().equals("iron")||completed.contains(c.research()))){cap=c;break;}
        for(var r:RODDEFS.values())if(grid.get(4).is(r.item().get())&&(r.tag().equals("wood")||completed.contains(r.research()))){rod=r;break;}
        return cap==null||rod==null?null:sceptreEntry(cap,rod);
    }
    static ArcaneRecipes.Entry sceptreEntry(Cap cap,Rod rod) {
        var ing=new java.util.ArrayList<net.minecraft.world.item.crafting.Ingredient>();var preds=new java.util.ArrayList<java.util.function.Predicate<ItemStack>>();
        for(int i=0;i<9;i++){
            var ingredient=i==1||i==5||i==6?net.minecraft.world.item.crafting.Ingredient.of(cap.item().get()):i==4?net.minecraft.world.item.crafting.Ingredient.of(rod.item().get()):i==2?net.minecraft.world.item.crafting.Ingredient.of(WandPedestal.PRIMAL_CHARM.get()):net.minecraft.world.item.crafting.Ingredient.EMPTY;
            ing.add(ingredient);preds.add(ingredient.isEmpty()?null:ingredient::test);
        }
        var cost=new thaumcraft.api.aspects.AspectList();for(var a:Aspect.getPrimalAspects())cost.add(a,(int)(cap.craftCost()*rod.craftCost()*1.5f));
        return new ArcaneRecipes.Entry("Sceptre","SCEPTRE",3,3,ing,sceptre(cap,rod),cost,new thaumcraft.api.crafting.ArcanePattern<ItemStack>("SCEPTRE",3,3,preds).setMirrored(false));
    }

}
