package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.Aspect;

/**
 * ItemVoidRobeArmor (ARMORVOIDFORTRESS): void metal armour fused with thaumaturge robes. Dyeable (default 6961280),
 * self-repairing 1 per 20 ticks, 5% vis discount and 2 warp per piece, the hood works as goggles of revealing, and
 * the ISpecialArmor ratios (defence/25, magic defence/35 at priority 1, none against armour-bypassing damage) go
 * through FortressArmor.absorb.
 */
public final class VoidRobe {
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> MATERIAL=DeferredHolder.create(Registries.ARMOR_MATERIAL,AlchemyContent.id("void_robe"));
    public static final DeferredHolder<Item,Item> HELM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("void_robe_helm")),CHEST=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("void_robe_chest")),LEGS=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("void_robe_legs"));
    static final int DEFAULT_COLOR=6961280;
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(RegisterEvent e){
            /* ThaumcraftApi.armorMatVoid: durability 10, defence 3/7/6/3, enchantability 10; the robe adds a dyed layer */
            e.register(Registries.ARMOR_MATERIAL,h->h.register(MATERIAL.getId(),new ArmorMaterial(Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,7,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3),10,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot"))),List.of(new ArmorMaterial.Layer(AlchemyContent.id("void_robe"),"",true),new ArmorMaterial.Layer(AlchemyContent.id("void_robe"),"_overlay",false)),0,0)));
            e.register(Registries.ITEM,h->{h.register(HELM.getId(),new Piece(ArmorItem.Type.HELMET));h.register(CHEST.getId(),new Piece(ArmorItem.Type.CHESTPLATE));h.register(LEGS.getId(),new Piece(ArmorItem.Type.LEGGINGS));});
        }
    }
    public static boolean piece(ItemStack s){return s.getItem() instanceof Piece;}
    public static final class Piece extends ArmorItem implements thaumcraft.api.IRepairable,thaumcraft.api.IRunicArmor,thaumcraft.api.IVisDiscountGear,thaumcraft.api.IGoggles,thaumcraft.api.nodes.IRevealer,thaumcraft.api.IWarpingGear {
        Piece(Type type){super(MATERIAL,type,new Properties().durability(type.getDurability(10)).rarity(Rarity.EPIC).component(DataComponents.DYED_COLOR,new DyedItemColor(DEFAULT_COLOR,false)));}
        /* type==null (the dyed pass) is the overlay png in the original, the untinted pass is void_robe_armor.png */
        @Override public ResourceLocation getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return AlchemyContent.id("textures/models/void_robe_armor"+(layer.dyeable()?"_overlay":"")+".png");}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack material){return material.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot")))||super.isValidRepairItem(s,material);}
        @Override public void inventoryTick(ItemStack s,Level l,Entity entity,int slot,boolean selected){super.inventoryTick(s,l,entity,slot,selected);if(!l.isClientSide&&s.isDamaged()&&entity.tickCount%20==0&&entity instanceof LivingEntity)s.setDamageValue(s.getDamageValue()-1);}
        public int getRunicCharge(ItemStack s){return 0;}
        public boolean showNodes(ItemStack s,LivingEntity e){return getType()==Type.HELMET;}
        public boolean showIngamePopups(ItemStack s,LivingEntity e){return getType()==Type.HELMET;}
        public int getVisDiscount(ItemStack s,Player p,Aspect a){return 5;}
        public int getWarp(ItemStack s,Player p){return 2;}
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){lines.add(Component.translatable("tc.visdiscount").append(": 5%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));super.appendHoverText(s,c,lines,f);}
        /* onItemUseFirst on a filled cauldron washes the dye out and lowers the water */
        @Override public InteractionResult onItemUseFirst(ItemStack s,UseOnContext c){var l=c.getLevel();var pos=c.getClickedPos();var state=l.getBlockState(pos);if(state.is(Blocks.WATER_CAULDRON)){if(!l.isClientSide){s.set(DataComponents.DYED_COLOR,new DyedItemColor(DEFAULT_COLOR,false));LayeredCauldronBlock.lowerFillLevel(state,l,pos);}return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
    }
}
