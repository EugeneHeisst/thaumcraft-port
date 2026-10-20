package dev.eugheist.tc4modern;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.*;
/** Original ItemBootsTraveller and EventHandlerEntity jump/step handling. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class TravellerBoots {
 public static Item item(){return BuiltInRegistries.ITEM.get(AlchemyContent.id("boots_traveller"));}
 public static boolean worn(Player p){return p.getItemBySlot(EquipmentSlot.FEET).is(item());}
 public static boolean moving(Player p){return worn(p)&&!p.getAbilities().flying&&p.zza>0;}
 public static float airSpeed(Player p){return p.getPersistentData().getBoolean("TC4Hover")?.03f:.05f;}
 static final java.util.Map<Player,Double> oldStep=new java.util.WeakHashMap<>();
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)static class Registration {
  @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent e){e.register(Registries.ITEM,h->h.register(AlchemyContent.id("boots_traveller"),new Boots()));}
 }
 public static final class Boots extends ArmorItem implements IRunicArmor,IRepairable {
  Boots(){super(TC4Modern.SPECIAL_MATERIAL,Type.BOOTS,new Properties().durability(350).rarity(Rarity.RARE));}
  @Override public int getRunicCharge(ItemStack s){return 0;}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack repair){return false;}
  /** Issue #9/#10: crafted from leather boots, so they walk on powder snow like them (freeze immunity via the vanilla item tag). */
  @Override public boolean canWalkOnPowderedSnow(ItemStack s,LivingEntity wearer){return true;}
  @Override public ResourceLocation getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return AlchemyContent.id("textures/models/bootstraveler.png");}
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){var p=e.getEntity();var step=p.getAttribute(Attributes.STEP_HEIGHT);
  if(p.level().isClientSide&&step!=null){if((p.isShiftKeyDown()||!worn(p))&&oldStep.containsKey(p)){step.setBaseValue(oldStep.remove(p));}if(moving(p)&&!p.isShiftKeyDown()){oldStep.putIfAbsent(p,step.getBaseValue());step.setBaseValue(1);}}
  if(!worn(p))return;if(moving(p)&&p.onGround())p.moveRelative(p.isInWater()?.055f/4:.055f,new net.minecraft.world.phys.Vec3(0,0,1));if(p.fallDistance>0)p.fallDistance-=.25f;
 }
 @SubscribeEvent static void jump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof Player p&&worn(p))p.setDeltaMovement(p.getDeltaMovement().add(0,.275f,0));}
}
