package dev.eugheist.tc4modern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
public final class CultistLeader extends ThaumcraftBoss implements RangedAttackMob {
 static final String[] TITLES={"Alberic","Anselm","Bastian","Beturian","Chabier","Chorache","Chuse","Dodorol","Ebardo","Ferrando","Fertus","Guillen","Larpe","Obano","Zelipe"};
 private static final EntityDataAccessor<Integer> TITLE=SynchedEntityData.defineId(CultistLeader.class,EntityDataSerializers.INT);
 public CultistLeader(EntityType<? extends CultistLeader> type,Level level){super(type,level);xpReward=40;}
 public static AttributeSupplier.Builder attributes(){return ThaumcraftBoss.attributes().add(Attributes.MAX_HEALTH,125).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.ATTACK_DAMAGE,5);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TITLE,0);}
 String title(){return TITLES[net.minecraft.util.Mth.clamp(entityData.get(TITLE),0,TITLES.length-1)];}
 @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable(getType().getDescriptionId(),title(),championTitle());}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LongRange(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
 static final class LongRange extends RangedAttackGoal {final CultistLeader mob;LongRange(CultistLeader m){super(m,1,30,40,24);mob=m;}@Override public boolean canUse(){return super.canUse()&&mob.getTarget()!=null&&mob.getTarget().isAlive()&&mob.distanceToSqr(mob.getTarget())>=256;}}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){setItemSlot(EquipmentSlot.HEAD,new ItemStack(LeaderGear.item("cultist_leader_helmet")));setItemSlot(EquipmentSlot.CHEST,new ItemStack(LeaderGear.item("cultist_leader_chestplate")));setItemSlot(EquipmentSlot.LEGS,new ItemStack(LeaderGear.item("cultist_leader_leggings")));setItemSlot(EquipmentSlot.FEET,PechTrades.mod("cultist_boots"));setItemSlot(EquipmentSlot.MAINHAND,PechTrades.mod(world.getDifficulty()==Difficulty.EASY?"void_sword":"crimson_blade"));float tension=difficulty.getSpecialMultiplier();if(!getMainHandItem().isEmpty()&&random.nextFloat()<.5f*tension){var stack=net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(7+tension*random.nextInt(22)),registryAccess(),java.util.Optional.empty());setItemSlot(EquipmentSlot.MAINHAND,stack);}entityData.set(TITLE,random.nextInt(TITLES.length));return super.finalizeSpawn(world,difficulty,reason,data);}
 @Override public boolean isAlliedTo(Entity other){return other instanceof CultistEntity||other instanceof CultistLeader;}
 @Override public boolean canAttackType(EntityType<?> type){return type!=CrimsonContent.KNIGHT.get()&&type!=CrimsonContent.CLERIC.get()&&type!=OuterBossContent.LEADER.get()&&super.canAttackType(type);}
 @Override protected void customServerAiStep(){super.customServerAiStep();for(var cultist:level().getEntitiesOfClass(CultistEntity.class,getBoundingBox().inflate(8)))if(distanceToSqr(cultist)<=64&&!cultist.hasEffect(MobEffects.DAMAGE_RESISTANCE))cultist.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,60,1));}
 @Override public void performRangedAttack(LivingEntity target,float distance){if(!hasLineOfSight(target))return;swing(InteractionHand.MAIN_HAND);getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);var orb=new GolemOrb(level(),this,target,true);var v=orb.getDeltaMovement();orb.setPos(orb.getX()+v.x/2,orb.getY(),orb.getZ()+v.z/2);orb.shoot(target.getX()-getX(),target.getY()+target.getBbHeight()/2-(getY()+getBbHeight()/2)+2,target.getZ()-getZ(),.66f,3);playSound(FociContent.sound("egattack"),1,1+random.nextFloat()*.1f);level().addFreshEntity(orb);}
 @Override protected void dropCustomDeathLoot(ServerLevel world,DamageSource source,boolean recent){spawnAtLocation(new ItemStack(LootBags.ITEMS.get(2).get()),1.5f);}
 @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putByte("title",(byte)(int)entityData.get(TITLE));}@Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);entityData.set(TITLE,net.minecraft.util.Mth.clamp(n.getByte("title"),0,TITLES.length-1));}
}
