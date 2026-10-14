package dev.eugheist.tc4modern;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Thaumium equipment from ThaumcraftApi.toolMatThaumium (harvest 3, 400 uses, speed 7, damage 2, enchantability 22)
 * and armorMatThaumium (durability factor 25, protection 2/6/5/2, enchantability 25), repaired with thaumium ingots;
 * plus the thaumium nugget and block. All items are EnumRarity.uncommon as in the original.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ThaumiumContent {
    private ThaumiumContent() {}
    private static DeferredHolder<Item, Item> item(String n) { return DeferredHolder.create(Registries.ITEM, AlchemyContent.id(n)); }
    public static final DeferredHolder<Block, Block> BLOCK = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("thaumium_block"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMOR = DeferredHolder.create(Registries.ARMOR_MATERIAL, AlchemyContent.id("thaumium"));
    public static final List<String> PIECES = List.of("thaumium_helmet", "thaumium_chestplate", "thaumium_leggings", "thaumium_boots", "thaumium_sword", "thaumium_pickaxe", "thaumium_axe", "thaumium_shovel", "thaumium_hoe", "thaumium_nugget", "thaumium_block");
    static Item ingot() { return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_ingot")); }
    /** Modern tiers express "harvest level 3" as diamond-equivalent mining. */
    public static final Tier TIER = new net.neoforged.neoforge.common.SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 400, 7.0f, 2.0f, 22, () -> Ingredient.of(ingot()));

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.ARMOR_MATERIAL, h -> {
            var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.HELMET, 2); defense.put(ArmorItem.Type.CHESTPLATE, 6); defense.put(ArmorItem.Type.LEGGINGS, 5); defense.put(ArmorItem.Type.BOOTS, 2); defense.put(ArmorItem.Type.BODY, 6);
            // textures/models/thaumium_1/2.png are imported as models/armor/thaumium_layer_1/2.png.
            h.register(ARMOR.getId(), new ArmorMaterial(defense, 25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ingot()), List.of(new ArmorMaterial.Layer(AlchemyContent.id("thaumium"))), 0f, 0f));
        });
        event.register(Registries.BLOCK, h -> h.register(BLOCK.getId(), new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4f, 20f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
        event.register(Registries.ITEM, h -> {
            Holder<ArmorMaterial> mat = ARMOR;
            var p = new Item.Properties().rarity(Rarity.UNCOMMON);
            h.register(AlchemyContent.id("thaumium_helmet"), new ArmorItem(mat, ArmorItem.Type.HELMET, p.durability(ArmorItem.Type.HELMET.getDurability(25))));
            h.register(AlchemyContent.id("thaumium_chestplate"), new ArmorItem(mat, ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.CHESTPLATE.getDurability(25))));
            h.register(AlchemyContent.id("thaumium_leggings"), new ArmorItem(mat, ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.LEGGINGS.getDurability(25))));
            h.register(AlchemyContent.id("thaumium_boots"), new ArmorItem(mat, ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.UNCOMMON).durability(ArmorItem.Type.BOOTS.getDurability(25))));
            // Original 1.7.10 ItemSword/ItemTool add 4/2/3/1 to material damage.
            // Attack speed is the modern host requirement; original damage is retained.
            var tool = new Item.Properties().rarity(Rarity.UNCOMMON);
            h.register(AlchemyContent.id("thaumium_sword"), new SwordItem(TIER, tool.attributes(SwordItem.createAttributes(TIER, 4, -2.4f))));
            h.register(AlchemyContent.id("thaumium_pickaxe"), new PickaxeItem(TIER, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(TIER, 2f, -2.8f))));
            h.register(AlchemyContent.id("thaumium_axe"), new AxeItem(TIER, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(TIER, 3f, -3.0f))));
            h.register(AlchemyContent.id("thaumium_shovel"), new ShovelItem(TIER, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(TIER, 1f, -3.0f))));
            h.register(AlchemyContent.id("thaumium_hoe"), new HoeItem(TIER, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(TIER, -2f, 0f))));
            h.register(AlchemyContent.id("thaumium_nugget"), new Item(new Item.Properties()));
            h.register(BLOCK.getId(), new BlockItem(BLOCK.get(), new Item.Properties()));
        });
    }
}
