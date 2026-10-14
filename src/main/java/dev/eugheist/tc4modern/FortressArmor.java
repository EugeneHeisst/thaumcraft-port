package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.*;
import thaumcraft.api.nodes.IRevealer;
/** ItemFortressArmor and the three original EventHandlerRunic mask effects. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class FortressArmor {
 public static final DeferredHolder<ArmorMaterial,ArmorMaterial> MATERIAL=DeferredHolder.create(Registries.ARMOR_MATERIAL,AlchemyContent.id("fortress"));
 public static Item item(String piece){return BuiltInRegistries.ITEM.get(AlchemyContent.id("fortress_"+piece));}
 public static int mask(LivingEntity e){var stack=e.getItemBySlot(EquipmentSlot.HEAD);return stack.getItem() instanceof Piece&&NoteData.tag(stack).contains("mask")?NoteData.tag(stack).getInt("mask"):-1;}
 static ItemStack withGoggles(ItemStack s){var data=NoteData.tag(s);data.putByte("goggles",(byte)1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return s;}
 static double multiplier(LivingEntity e){double value=.875;if(!(e instanceof Player))return 1;for(var slot:List.of(EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD)){var s=e.getItemBySlot(slot);if(s.getItem() instanceof Piece){value+=.125;if(NoteData.tag(s).contains("mask"))value+=.05;}}return value;}
 @SubscribeEvent static void masks(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event){var victim=event.getEntity();if(victim.level().isClientSide)return;float amount=event.getAmount();var source=event.getSource().getEntity();if(source instanceof Player player&&mask(player)==2&&player.level().random.nextFloat()<amount/12)player.heal(1);if(victim instanceof Player player&&source instanceof LivingEntity attacker&&mask(player)==1&&player.level().random.nextFloat()<amount/10)attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,80,0));}
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)static final class Registration {
  @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ARMOR_MATERIAL,h->h.register(MATERIAL.getId(),new ArmorMaterial(Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,7,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3),25,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,()->net.minecraft.world.item.crafting.Ingredient.of(ThaumiumContent.ingot()),List.of(new ArmorMaterial.Layer(AlchemyContent.id("fortress"))),0,0)));e.register(Registries.ITEM,h->{h.register(AlchemyContent.id("fortress_helmet"),new Piece(ArmorItem.Type.HELMET,440));h.register(AlchemyContent.id("fortress_chestplate"),new Piece(ArmorItem.Type.CHESTPLATE,640));h.register(AlchemyContent.id("fortress_leggings"),new Piece(ArmorItem.Type.LEGGINGS,600));});}
 }
 public static final class Piece extends ArmorItem implements IRepairable,IRunicArmor,IGoggles,IRevealer {
  Piece(Type type,int durability){super(MATERIAL,type,new Properties().durability(durability).rarity(Rarity.RARE));}
  public int getRunicCharge(ItemStack s){return 0;}
  public boolean showNodes(ItemStack s,LivingEntity e){return NoteData.tag(s).contains("goggles");}
  public boolean showIngamePopups(ItemStack s,LivingEntity e){return showNodes(s,e);}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack material){return material.is(ThaumiumContent.ingot());}
  @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return AlchemyContent.id("textures/models/fortress_armor.png");}
  @Override public void appendHoverText(ItemStack s,TooltipContext context,List<Component> lines,TooltipFlag flag){var data=NoteData.tag(s);if(data.contains("goggles"))lines.add(Component.translatable("item.ItemGoggles.name").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));if(data.contains("mask"))lines.add(Component.translatable("item.HelmetFortress.mask."+data.getInt("mask")).withStyle(net.minecraft.ChatFormatting.GOLD));super.appendHoverText(s,context,lines,flag);}
 }
 public static boolean equipped(LivingEntity e){for(var s:e.getArmorSlots())if(s.getItem() instanceof Piece||VoidRobe.piece(s))return true;return false;}
 public static float absorb(LivingEntity entity,DamageSource source,float amount){
  var props=new ArrayList<LegacyFortressProtection.Property>();double multiplier=multiplier(entity);
  for(var slot:List.of(EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD)){var stack=entity.getItemBySlot(slot);if(!(stack.getItem() instanceof ArmorItem armor))continue;int priority=0;double ratio=armor.getDefense()/25d;
   if(armor instanceof Piece){if(magic(source)){priority=1;ratio=armor.getDefense()/35d;}else if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION)){priority=1;ratio=armor.getDefense()/20d;}else if(source.is(DamageTypeTags.BYPASSES_ARMOR))ratio=0;ratio*=multiplier;}
   else if(armor instanceof VoidRobe.Piece){/* ItemVoidRobeArmor.getProperties */if(magic(source)){priority=1;ratio=armor.getDefense()/35d;}else if(source.is(DamageTypeTags.BYPASSES_ARMOR))ratio=0;}
   else if(source.is(DamageTypeTags.BYPASSES_ARMOR))continue;
   props.add(new LegacyFortressProtection.Property(priority,ratio,stack.getMaxDamage()+1-stack.getDamageValue(),stack,slot));
  }return LegacyFortressProtection.apply(entity,source,amount,props);
 }
 static boolean magic(DamageSource s){return s.is(DamageTypes.MAGIC)||s.is(DamageTypes.INDIRECT_MAGIC)||s.is(DamageTypes.WITHER)||s.is(DamageTypes.THORNS)||s.is(DamageTypes.DRAGON_BREATH);}
}
