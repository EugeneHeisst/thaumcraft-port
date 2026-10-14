package dev.eugheist.tc4modern;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Crimson Cult content: EntityCultistKnight/Cleric, EntityGolemOrb, blockEldritch metadata 0-3 (altar, obelisk,
 * obelisk body, capstone), the cult banner (blockWoodenDevice 8), WorldGenEldritchRing and the cultist armour.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CrimsonContent {
    private CrimsonContent() {}
    public static final DeferredHolder<EntityType<?>, EntityType<CultistEntity.Knight>> KNIGHT =
        DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("cultist_knight"));
    public static final DeferredHolder<EntityType<?>, EntityType<CultistEntity.Cleric>> CLERIC =
        DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("cultist_cleric"));
    public static final DeferredHolder<EntityType<?>, EntityType<GolemOrb>> GOLEM_ORB =
        DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("golem_orb"));

    public static final DeferredHolder<Block, Block> ALTAR = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("eldritch_altar"));
    public static final DeferredHolder<Block, Block> OBELISK = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("eldritch_obelisk"));
    public static final DeferredHolder<Block, Block> OBELISK_BODY = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("eldritch_obelisk_body"));
    public static final DeferredHolder<Block, Block> CAP = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("eldritch_cap"));
    /** blockCosmeticSolid metadata 1, the obsidian tile ("Obsidian Base") of eldritch rings. */
    public static final DeferredHolder<Block, Block> OBSIDIAN_TILE = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("obsidian_tile"));
    public static final DeferredHolder<Block, Block> BANNER = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("crimson_banner"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrimsonAltarEntity>> ALTAR_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("eldritch_altar"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EldritchBlock.ObeliskEntity>> OBELISK_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("eldritch_obelisk"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EldritchBlock.CapEntity>> CAP_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("eldritch_cap"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrimsonBanner.Entity>> BANNER_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("crimson_banner"));

    public static final DeferredHolder<Feature<?>, CrimsonRingFeature> RING = DeferredHolder.create(Registries.FEATURE, AlchemyContent.id("eldritch_ring"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PLATE = DeferredHolder.create(Registries.ARMOR_MATERIAL, AlchemyContent.id("cultist_plate"));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROBE = DeferredHolder.create(Registries.ARMOR_MATERIAL, AlchemyContent.id("cultist_robe"));

    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE, h -> {
            h.register(KNIGHT.getId(), EntityType.Builder.of(CultistEntity.Knight::new, MobCategory.MONSTER).sized(.6f, 1.8f).clientTrackingRange(8).build(KNIGHT.getId().toString()));
            h.register(CLERIC.getId(), EntityType.Builder.of(CultistEntity.Cleric::new, MobCategory.MONSTER).sized(.6f, 1.8f).clientTrackingRange(8).build(CLERIC.getId().toString()));
            // EntityGolemOrb: EntityThrowable default .25 box, registered with tracking range 64 and update every tick.
            h.register(GOLEM_ORB.getId(), EntityType.Builder.<GolemOrb>of(GolemOrb::new, MobCategory.MISC).sized(.25f, .25f).clientTrackingRange(4).updateInterval(1).build(GOLEM_ORB.getId().toString()));
        });
        event.register(Registries.ARMOR_MATERIAL, h -> {
            for (var ref : List.of(PLATE, ROBE)) {
                var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.BOOTS, 2);
                defense.put(ArmorItem.Type.LEGGINGS, ref == PLATE ? 5 : 3);
                defense.put(ArmorItem.Type.CHESTPLATE, ref == PLATE ? 6 : 4);
                defense.put(ArmorItem.Type.HELMET, ref == PLATE ? 3 : 2);
                defense.put(ArmorItem.Type.BODY, 0);
                h.register(ref.getId(), new ArmorMaterial(defense, 10, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT), List.of(new ArmorMaterial.Layer(ref.getId())), 0, 0));
            }
        });
        event.register(Registries.BLOCK, h -> {
            for (var kind : EldritchBlock.Kind.values()) h.register(AlchemyContent.id(kind.id), kind==EldritchBlock.Kind.OBELISK_BODY?new EldritchBlock.Body():new EldritchBlock(kind));
            // BlockCosmeticSolid: stone sound, metadata 1 hardness 30 and explosion resistance 999.
            h.register(OBSIDIAN_TILE.getId(), new Block(Block.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(30, 999).sound(SoundType.STONE).requiresCorrectToolForDrops()));
            // BlockWoodenDevice: wood, hardness 2.5, resistance 10.
            h.register(BANNER.getId(), new CrimsonBanner(Block.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, 6).sound(SoundType.WOOD).noCollission().noOcclusion()));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            h.register(ALTAR_ENTITY.getId(), BlockEntityType.Builder.of(CrimsonAltarEntity::new, ALTAR.get()).build(null));
            h.register(OBELISK_ENTITY.getId(), BlockEntityType.Builder.of(EldritchBlock.ObeliskEntity::new, OBELISK.get()).build(null));
            h.register(CAP_ENTITY.getId(), BlockEntityType.Builder.of(EldritchBlock.CapEntity::new, CAP.get()).build(null));
            h.register(BANNER_ENTITY.getId(), BlockEntityType.Builder.of(CrimsonBanner.Entity::new, BANNER.get()).build(null));
        });
        event.register(Registries.FEATURE, h -> h.register(RING.getId(), new CrimsonRingFeature()));
        event.register(Registries.ITEM, h -> {
            h.register(AlchemyContent.id("cultist_knight_spawn_egg"), new net.neoforged.neoforge.common.DeferredSpawnEggItem(KNIGHT, 0x8B1010, 0x352225, new Item.Properties()));
            h.register(AlchemyContent.id("cultist_cleric_spawn_egg"), new net.neoforged.neoforge.common.DeferredSpawnEggItem(CLERIC, 0x8B1010, 0x352225, new Item.Properties()));
            for (var material : List.of(PLATE, ROBE))
                for (var type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS))
                    h.register(AlchemyContent.id((material == PLATE ? "cultist_plate_" : "cultist_robe_") + type.getName()),
                        new CultistArmor(material, type, new Item.Properties().durability(type.getDurability(12)), material == PLATE ? "cultist_plate_armor" : "cultist_robe_armor"));
            h.register(AlchemyContent.id("cultist_boots"), new CultistArmor(PLATE, ArmorItem.Type.BOOTS, new Item.Properties().durability(12 * 13), "cultistboots"));
            h.register(OBSIDIAN_TILE.getId(), new BlockItem(OBSIDIAN_TILE.get(), new Item.Properties()));
            h.register(BANNER.getId(), new CrimsonBanner.Item(BANNER.get(), new Item.Properties()));
        });
        if (event.getRegistryKey() == Registries.BLOCK) {
            // Rings from 0.17.11/0.17.12 used eldritch_stone for the perimeter capstones.
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.addAlias(AlchemyContent.id("eldritch_stone"), CAP.getId());
        }
    }

    @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(KNIGHT.get(), CultistEntity.attributes(36).build());
        event.put(CLERIC.get(), CultistEntity.attributes(30).build());
    }

    /** getArmorTexture: every cultist piece uses one sheet for both layers (cultist_robe_armor, cultist_plate_armor, cultistboots). */
    public static final class CultistArmor extends ArmorItem {
        private final net.minecraft.resources.ResourceLocation texture;
        CultistArmor(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Properties properties, String texture) {
            super(material, type, properties);
            this.texture = AlchemyContent.id("textures/models/" + texture + ".png");
        }
        @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
                net.minecraft.world.entity.EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) { return texture; }
    }
}
