package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
/** Original ItemBowBone and EventHandlerEntity bow events, including their unusual consumption rules. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class PrimalArrows {
 static final String[] NAMES={"air","fire","water","earth","order","entropy"};
 public static final DeferredHolder<EntityType<?>,EntityType<ArrowEntity>> ENTITY=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("primal_arrow"));
 public static Item bow(){return BuiltInRegistries.ITEM.get(AlchemyContent.id("bone_bow"));}public static Item arrow(int type){return BuiltInRegistries.ITEM.get(AlchemyContent.id(NAMES[type]+"_arrow"));}
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)static final class Registration {
  @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{h.register(AlchemyContent.id("bone_bow"),new BoneBow());for(int i=0;i<6;i++)h.register(AlchemyContent.id(NAMES[i]+"_arrow"),new ElementalArrow(i));});e.register(Registries.ENTITY_TYPE,h->h.register(ENTITY.getId(),EntityType.Builder.<ArrowEntity>of(ArrowEntity::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(10).updateInterval(20).build("tc4modern:primal_arrow")));}
 }
 static ItemStack first(Player p){for(var stack:p.getInventory().items)if(stack.getItem() instanceof ElementalArrow)return stack;return ItemStack.EMPTY;}
 static int enchant(Player p,ItemStack s,ResourceKey<Enchantment> key){return BasicsEnchantments.level(p,s,key);}
 static float power(int ticks,boolean bone){float f=ticks/(bone?10f:20f);return Math.min(1,(f*f+f*2)/3);}
 @SubscribeEvent static void nock(net.neoforged.neoforge.event.entity.player.ArrowNockEvent e){if(first(e.getEntity()).isEmpty())return;e.getEntity().startUsingItem(e.getHand());e.setAction(InteractionResultHolder.consume(e.getBow()));}
 @SubscribeEvent static void loose(net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e){var p=e.getEntity();var ammo=first(p);if(ammo.isEmpty())return;boolean bone=e.getBow().getItem() instanceof BoneBow;float f=power(e.getCharge(),bone);if(f<.1)return;
  if(!e.getLevel().isClientSide){int type=((ElementalArrow)ammo.getItem()).type;var projectile=new ArrowEntity(ENTITY.get(),e.getLevel());projectile.getEntityData().set(ArrowEntity.TYPE,type);projectile.setOwner(p);projectile.pickup=AbstractArrow.Pickup.DISALLOWED;var look=p.getLookAngle();double yaw=Math.toRadians(p.getYRot());projectile.setPos(p.getX()-Math.cos(yaw)*.16+look.x,p.getEyeY()-.10000000014901161+look.y,p.getZ()-Math.sin(yaw)*.16+look.z);projectile.shoot(look.x,look.y,look.z,f*(bone?3.75f:3),1);
   // getDamage applies the elemental multiplier; the original then writes it back, sometimes repeatedly.
   if(bone)projectile.setBaseDamage(projectile.getBaseDamage()+.5);else if(f==1)projectile.setCritArrow(true);int power=enchant(p,e.getBow(),Enchantments.POWER);if(power>0)projectile.setBaseDamage(projectile.getBaseDamage()+power*.5+.5);projectile.knockback=enchant(p,e.getBow(),Enchantments.PUNCH)+(type==3?1:0);if(enchant(p,e.getBow(),Enchantments.FLAME)>0)projectile.igniteForSeconds(100);e.getLevel().addFreshEntity(projectile);e.getBow().hurtAndBreak(1,p,LivingEntity.getSlotForHand(p.getUsedItemHand()));boolean saved=enchant(p,e.getBow(),Enchantments.INFINITY)>0&&p.getRandom().nextFloat()<.33f;
   // Original || condition: survival ALWAYS consumes; creative only saves arrows on a successful Infinity roll.
   if(!p.getAbilities().instabuild||!saved)ammo.shrink(1);
  }e.getLevel().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(e.getLevel().random.nextFloat()*.4f+1.2f)+f*.5f);e.setCanceled(true);
 }
 public static final class ElementalArrow extends Item {final int type;ElementalArrow(int type){super(new Properties());this.type=type;}}
 public static final class BoneBow extends BowItem {
  BoneBow(){super(new Properties().durability(512));}@Override public int getEnchantmentValue(){return 3;}
  @Override public void onUseTick(Level l,LivingEntity entity,ItemStack stack,int remaining){if(entity instanceof Player p&&getUseDuration(stack,entity)-remaining>18)p.releaseUsingItem();}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);boolean ammo=p.getInventory().items.stream().anyMatch(x->x.is(Items.ARROW));var hooked=net.neoforged.neoforge.event.EventHooks.onArrowNock(s,l,p,hand,ammo);if(hooked!=null)return hooked;if(p.getAbilities().instabuild||ammo||enchant(p,s,Enchantments.INFINITY)>0){p.startUsingItem(hand);return InteractionResultHolder.consume(s);}return InteractionResultHolder.fail(s);}
  @Override public void releaseUsing(ItemStack s,Level l,LivingEntity entity,int remaining){if(!(entity instanceof Player p))return;boolean infinite=p.getAbilities().instabuild||enchant(p,s,Enchantments.INFINITY)>0;var ammo=p.getInventory().items.stream().filter(x->x.is(Items.ARROW)).findFirst().orElse(ItemStack.EMPTY);int ticks=net.neoforged.neoforge.event.EventHooks.onArrowLoose(s,l,p,getUseDuration(s,entity)-remaining,infinite||!ammo.isEmpty());if(ticks<0||(!infinite&&ammo.isEmpty()))return;float f=power(ticks,true);if(f<.1)return;if(!l.isClientSide){var arrow=new net.minecraft.world.entity.projectile.Arrow(l,p,new ItemStack(Items.ARROW),null);var look=p.getLookAngle();double yaw=Math.toRadians(p.getYRot());arrow.setPos(p.getX()-Math.cos(yaw)*.16,p.getEyeY()-.10000000014901161,p.getZ()-Math.sin(yaw)*.16);arrow.shoot(look.x,look.y,look.z,f*3.75f,1);arrow.setBaseDamage(2.5);int power=enchant(p,s,Enchantments.POWER);if(power>0)arrow.setBaseDamage(arrow.getBaseDamage()+power*.5+.5);int punch=enchant(p,s,Enchantments.PUNCH);arrow.getPersistentData().putInt("TC4BonePunch",punch);if(enchant(p,s,Enchantments.FLAME)>0)arrow.igniteForSeconds(100);arrow.pickup=infinite?AbstractArrow.Pickup.CREATIVE_ONLY:AbstractArrow.Pickup.ALLOWED;s.hurtAndBreak(1,p,LivingEntity.getSlotForHand(p.getUsedItemHand()));if(!infinite)ammo.shrink(1);l.addFreshEntity(arrow);}l.playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(l.random.nextFloat()*.4f+1.2f)+f*.5f);}
 }
 public static final class ArrowEntity extends AbstractArrow {
  static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(ArrowEntity.class,EntityDataSerializers.INT);int groundTicks,knockback;
  ArrowEntity(EntityType<? extends ArrowEntity> type,Level l){super(type,l);pickup=Pickup.DISALLOWED;}
  public int type(){return entityData.get(TYPE);}
  @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TYPE,0);}
  @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(Items.ARROW);}@Override public void playerTouch(Player p){}
  @Override protected float getWaterInertia(){return .8f;}
  @Override public double getBaseDamage(){return super.getBaseDamage()*(type()==3?1.5:type()==4||type()==5?.8:1);}
  @Override protected void tickDespawn(){if(++groundTicks>=100)discard();}
  @Override public void tick(){super.tick();if(!inGround)groundTicks=0;else if(level().isClientSide)groundTicks++;}
  DamageSource source(){if(type()!=0&&type()!=1&&type()!=4)return damageSources().arrow(this,getOwner()==null?this:getOwner());String name=type()==0?"airarrow":type()==1?"firearrow":"orderarrow";var key=ResourceKey.create(Registries.DAMAGE_TYPE,AlchemyContent.id(name));return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),this,getOwner()==null?this:getOwner());}
  @Override protected void onHitEntity(EntityHitResult hit){var victim=hit.getEntity();int amount=Mth.ceil(getDeltaMovement().length()*getBaseDamage());if(isCritArrow())amount+=random.nextInt(amount/2+2);int fire=(isOnFire()&&type()!=2?5:0)+(type()==1?5:0);if(victim instanceof LivingEntity living){if(type()==2)living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,200,4));if(type()==4)living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,4));if(type()==5)living.addEffect(new MobEffectInstance(MobEffects.WITHER,100));}if(fire>0&&victim.getType()!=EntityType.ENDERMAN)victim.igniteForSeconds(fire);
   var damage=source();if(victim.hurt(damage,amount)){if(victim instanceof LivingEntity living){var movement=getDeltaMovement();double speed=movement.horizontalDistance();if(knockback>0&&speed>0)living.push(movement.x*knockback*.6/speed,.1,movement.z*knockback*.6/speed);if(level() instanceof ServerLevel server)EnchantmentHelper.doPostAttackEffectsWithItemSource(server,living,damage,getWeaponItem());if(living instanceof Player&&getOwner() instanceof net.minecraft.server.level.ServerPlayer shooter)shooter.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(net.minecraft.network.protocol.game.ClientboundGameEventPacket.ARROW_HIT_PLAYER,0));}playSound(SoundEvents.ARROW_HIT,1,1.2f/(random.nextFloat()*.2f+.9f));if(victim.getType()!=EntityType.ENDERMAN)discard();}else{setDeltaMovement(getDeltaMovement().scale(-.1));setYRot(getYRot()+180);yRotO+=180;}}
  @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("type",(byte)type());tag.putInt("TC4Knockback",knockback);tag.putInt("TC4GroundTicks",groundTicks);}
  @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TYPE,Mth.clamp(tag.getByte("type"),0,5));knockback=tag.getInt("TC4Knockback");groundTicks=tag.getInt("TC4GroundTicks");pickup=Pickup.DISALLOWED;}
 }
}
