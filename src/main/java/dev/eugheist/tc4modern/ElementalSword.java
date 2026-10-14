package dev.eugheist.tc4modern;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original ItemElementalSword: wind while blocking and legacy full-strength secondary attacks. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class ElementalSword extends SwordItem {
 ElementalSword(){super(ElementalTools.TIER,new Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(ElementalTools.TIER,4,-2.4f)));}
 @Override public int getUseDuration(ItemStack s,LivingEntity user){return 72000;}
 @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.BLOCK;}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){p.startUsingItem(hand);return InteractionResultHolder.consume(p.getItemInHand(hand));}
 public static boolean blocking(LivingEntity e){return e.isUsingItem()&&e.getUseItem().getItem() instanceof ElementalSword;}
 @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGH) static void incoming(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){if(e.getEntity() instanceof Player&&blocking(e.getEntity())&&!e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&e.getAmount()>0)e.setAmount((1+e.getAmount())*.5f);}
 @Override public void onUseTick(Level l,LivingEntity user,ItemStack s,int remaining){if(!(user instanceof Player p))return;int ticks=72000-remaining;var v=p.getDeltaMovement();double y=v.y;if(y<0){y/=1.2f;p.fallDistance/=1.2f;}y+=.08f;if(y>.5)y=.2f;p.setDeltaMovement(v.x,y,v.z);if(p instanceof ServerPlayer server)((dev.eugheist.tc4modern.mixin.HoverFloatCounterAccessor)server.connection).tc4modern$floatCounter(0);
  for(var target:l.getEntities(p,p.getBoundingBox().inflate(2.5))){if(target instanceof Player||!target.isAlive()||p.getVehicle()==target)continue;var d=target.position().subtract(p.position());target.setDeltaMovement(target.getDeltaMovement().add(d.scale(1/(2.5*(d.length()+.1)))));}
  if(l.isClientSide)ElementalSwordClient.wind(p);else if(ticks%20==0)ElementalTools.sound(l,p.blockPosition(),"wind",.5f,.9f+l.random.nextFloat()*.2f);if(ticks%20==0)s.hurtAndBreak(1,p,LivingEntity.getSlotForHand(p.getUsedItemHand()));
 }
 @Override public boolean onLeftClickEntity(ItemStack s,Player p,Entity primary){if(!p.level().isClientSide&&primary.isAlive()){var targets=p.level().getEntities(p,primary.getBoundingBox().inflate(1.2,1.1,1.2));int count=0;if(targets.size()>1)for(var target:targets){if(!(target instanceof Mob mob)||target==primary||!target.isAlive()||target instanceof OwnableEntity own&&p.getUUID().equals(own.getOwnerUUID()))continue;attack(p,mob,s);count++;}if(count>0)ElementalTools.sound(p.level(),primary.blockPosition(),"swing",1,.9f+p.getRandom().nextFloat()*.2f);}return false;}
 static void attack(Player p,Mob target,ItemStack s){var event=new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(p,target);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(event.isCanceled()||!target.isAttackable()||target.skipAttackInteraction(p))return;float base=(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE);var source=p.damageSources().playerAttack(p);float ench=BasicsEnchantments.level(p,s,Enchantments.SHARPNESS)*1.25f;if(target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD))ench+=BasicsEnchantments.level(p,s,Enchantments.SMITE)*2.5f;if(target.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD))ench+=BasicsEnchantments.level(p,s,Enchantments.BANE_OF_ARTHROPODS)*2.5f;int knock=BasicsEnchantments.level(p,s,Enchantments.KNOCKBACK)+(p.isSprinting()?1:0);boolean critical=p.fallDistance>0&&!p.onGround()&&!p.onClimbable()&&!p.isInWater()&&!p.hasEffect(MobEffects.BLINDNESS)&&!p.isPassenger();if(critical&&base>0)base*=1.5f;float amount=base+ench;if(amount<=0)return;int fire=BasicsEnchantments.level(p,s,Enchantments.FIRE_ASPECT);boolean lit=fire>0&&!target.isOnFire();if(lit)target.igniteForSeconds(1);boolean success=target.hurt(source,amount);if(success){if(knock>0){double yaw=Math.toRadians(p.getYRot());target.push(-Math.sin(yaw)*knock*.5,.1,Math.cos(yaw)*knock*.5);p.setDeltaMovement(p.getDeltaMovement().multiply(.6,1,.6));p.setSprinting(false);}if(critical)p.crit(target);if(ench>0)p.magicCrit(target);p.setLastHurtMob(target);EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel)p.level(),target,source,null);}
  s.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);if(success&&fire>0)target.igniteForSeconds(fire*4);else if(lit)target.clearFire();p.awardStat(net.minecraft.stats.Stats.DAMAGE_DEALT,Math.round(amount*10));p.causeFoodExhaustion(.3f);
 }
}
