package dev.eugheist.tc4modern;
import net.minecraft.core.registries.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original cult leader's fortress-grade armour and Crimson Blade. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class LeaderGear {
 public static Item item(String name){return BuiltInRegistries.ITEM.get(AlchemyContent.id(name));}
 static final Tier TIER=new net.neoforged.neoforge.common.SimpleTier(net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL,200,8,3.5f,20,()->net.minecraft.world.item.crafting.Ingredient.of(item("void_ingot")));
 @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent e){e.register(Registries.ITEM,h->{for(var type:java.util.List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS))h.register(AlchemyContent.id("cultist_leader_"+type.getName()),new Piece(type));h.register(AlchemyContent.id("crimson_blade"),new Blade());});}
 public static final class Piece extends ArmorItem implements thaumcraft.api.IRepairable,thaumcraft.api.IRunicArmor {
  Piece(Type type){super(FortressArmor.MATERIAL,type,new Properties().durability(type.getDurability(40)).rarity(Rarity.RARE));}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack repair){return repair.is(Items.GOLD_INGOT)||super.isValidRepairItem(s,repair);}
  public int getRunicCharge(ItemStack s){return 0;}
  @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return AlchemyContent.id("textures/models/cultist_leader_armor.png");}
 }
 public static final class Blade extends SwordItem implements thaumcraft.api.IRepairable,thaumcraft.api.IWarpingGear {
  Blade(){super(TIER,new Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(TIER,4,-2.4f)));}
  public int getWarp(ItemStack s,net.minecraft.world.entity.player.Player player){return 2;}
  @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){super.inventoryTick(s,l,e,slot,selected);VoidMetal.repair(s,l,e);}
  @Override public boolean hurtEnemy(ItemStack s,LivingEntity target,LivingEntity hitter){if(!target.level().isClientSide&&(!(target instanceof net.minecraft.world.entity.player.Player)||!(hitter instanceof net.minecraft.world.entity.player.Player)||target.level().getServer().isPvpAllowed())){target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,60));target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,120));}return super.hurtEnemy(s,target,hitter);}
  @Override public void appendHoverText(ItemStack s,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("enchantment.special.sapgreat").withStyle(net.minecraft.ChatFormatting.GOLD));super.appendHoverText(s,context,lines,flag);}
 }
}
