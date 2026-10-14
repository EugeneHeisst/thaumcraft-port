package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Original tainted animals are monsters, without vanilla breeding, trading or milking. */
public final class TaintedMob extends Monster implements thaumcraft.api.entities.ITaintedMob,net.neoforged.neoforge.common.IShearable {
 private static final EntityDataAccessor<Boolean> SHEARED=SynchedEntityData.defineId(TaintedMob.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Integer> FUSE=SynchedEntityData.defineId(TaintedMob.class,EntityDataSerializers.INT);
 public static java.util.function.Consumer<TaintedMob> spawnFx=e->{};
 public int sheepTimer,fuseO;public float flap,flapO,flapSpeed,flapSpeedO,flapping=1;
 public TaintedMob(EntityType<? extends TaintedMob> t,Level l){super(t,l);if(!species().equals("cow"))setPersistenceRequired();if(species().equals("villager"))((net.minecraft.world.entity.ai.navigation.GroundPathNavigation)getNavigation()).setCanOpenDoors(true);}
 public String species(){return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().substring(6);}
 public static AttributeSupplier.Builder attributes(String species){double[] stats=switch(species){case "chicken"->new double[]{8,3,.4,2};case "cow"->new double[]{40,6,.27,0};case "pig"->new double[]{20,4,.275,2};case "sheep"->new double[]{20,3,.25,1};case "villager"->new double[]{30,4,.3,0};default->new double[]{30,2,.25,0};};return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,stats[0]).add(Attributes.ATTACK_DAMAGE,stats[1]).add(Attributes.MOVEMENT_SPEED,stats[2]).add(Attributes.ARMOR,stats[3]);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SHEARED,false);b.define(FUSE,0);}
 public boolean sheared(){return entityData.get(SHEARED);}
 public int fuse(){return entityData.get(FUSE);}
 @Override protected void registerGoals(){
  goalSelector.addGoal(0,new FloatGoal(this));
  if(species().equals("chicken"))goalSelector.addGoal(2,new LeapAtTargetGoal(this,.3f));
  if(species().equals("creeper"))goalSelector.addGoal(2,new Goal(){{setFlags(java.util.EnumSet.of(Flag.MOVE));}@Override public boolean canUse(){return fuse()>0||getTarget()!=null&&distanceToSqr(getTarget())<9;}@Override public void start(){getNavigation().stop();}});
  if(species().equals("sheep"))goalSelector.addGoal(2,new ConvertGrassGoal());
  goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
  if(species().equals("villager"))goalSelector.addGoal(4,new OpenDoorGoal(this,true));
  targetSelector.addGoal(0,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
  if(!species().equals("villager")){targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Villager.class,false));if(!species().equals("sheep"))targetSelector.addGoal(8,new NearestAttackableTargetGoal<>(this,Animal.class,false));}
 }
 @Override public boolean doHurtTarget(Entity target){return species().equals("creeper")||super.doHurtTarget(target);}
 @Override public void tick(){
  fuseO=fuse();super.tick();
  if(species().equals("chicken")){flapO=flap;flapSpeedO=flapSpeed;flapSpeed=net.minecraft.util.Mth.clamp(flapSpeed+(onGround()?-.3f:1.2f),0,1);if(!onGround()&&flapping<1)flapping=1;flapping*=.9f;flap+=flapping*2;if(!onGround()&&getDeltaMovement().y<0)setDeltaMovement(getDeltaMovement().multiply(1,.9,1));}
  if(level().isClientSide){if(sheepTimer>0)sheepTimer--;if(tickCount<5)spawnFx.accept(this);return;}
  if(species().equals("creeper")&&!isNoAi()&&isAlive()){var t=getTarget();boolean swell=t!=null&&distanceToSqr(t)<(fuse()>0?49:9)&&hasLineOfSight(t);if(swell&&fuse()==0)playSound(net.minecraft.sounds.SoundEvents.CREEPER_PRIMED,1,.5f);entityData.set(FUSE,net.minecraft.util.Mth.clamp(fuse()+(swell?1:-1),0,30));if(fuse()>=30)explode();}
 }
 public void explode(){if(!(level() instanceof net.minecraft.server.level.ServerLevel l))return;l.explode(this,getX(),getY()+getBbHeight()/2,getZ(),1.5f,false,Level.ExplosionInteraction.MOB);for(var e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(6),e->e!=this&&!(e instanceof thaumcraft.api.entities.ITaintedMob)&&!e.isInvertedHealAndHarm()))e.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,100,0,false,true));
  int x=(int)getX(),y=(int)getY(),z=(int)getZ();for(int i=0;i<10;i++){var p=new BlockPos(x+(int)((random.nextFloat()-random.nextFloat())*5),y,z+(int)((random.nextFloat()-random.nextFloat())*5));if(random.nextBoolean()&&!l.getBiome(p).is(TaintBiomes.TAINTED_LAND)){TaintBiomes.spread(l,p);if(l.getBlockState(p.below()).isSolidRender(l,p.below())&&l.getBlockState(p).canBeReplaced())l.setBlock(p,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);}}discard();}
 @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return !species().equals("chicken")&&super.causeFallDamage(distance,multiplier,source);}
 @Override protected float getSoundVolume(){return species().equals("cow")?.4f:super.getSoundVolume();}
 @Override public float getVoicePitch(){return .7f;}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return switch(species()){case "cow"->net.minecraft.sounds.SoundEvents.COW_AMBIENT;case "pig"->net.minecraft.sounds.SoundEvents.PIG_AMBIENT;case "sheep"->net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;case "chicken"->net.minecraft.sounds.SoundEvents.CHICKEN_AMBIENT;case "villager"->net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT;default->null;};}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return switch(species()){case "cow"->net.minecraft.sounds.SoundEvents.COW_HURT;case "pig"->net.minecraft.sounds.SoundEvents.PIG_AMBIENT;case "sheep"->net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT;case "chicken"->net.minecraft.sounds.SoundEvents.CHICKEN_HURT;case "villager"->net.minecraft.sounds.SoundEvents.VILLAGER_HURT;default->net.minecraft.sounds.SoundEvents.CREEPER_HURT;};}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return switch(species()){case "pig"->net.minecraft.sounds.SoundEvents.PIG_DEATH;case "creeper"->net.minecraft.sounds.SoundEvents.CREEPER_DEATH;default->getHurtSound(damageSources().generic());};}
 @Override public boolean isShearable(Player p,ItemStack item,Level l,BlockPos pos){return species().equals("sheep")&&!sheared();}
 @Override public java.util.List<ItemStack> onSheared(Player p,ItemStack item,Level l,BlockPos pos){if(!l.isClientSide)entityData.set(SHEARED,true);return java.util.List.of(new ItemStack(Items.PURPLE_WOOL,1+random.nextInt(3)));}
 @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource source,boolean recent){boolean drop=switch(species()){case "pig","sheep"->random.nextInt(3)==0;case "villager"->random.nextBoolean();default->true;};if(species().equals("villager")){int looting=source.getEntity() instanceof LivingEntity killer?net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),killer):0;if(random.nextInt(13)<1+looting)spawnAtLocation(PechTrades.mod("coin"),1.5f);}
  if(drop)spawnAtLocation(species().equals("chicken")?(random.nextInt(4)==0?TC4Modern.TAINTED_GOO.get():TC4Modern.TAINTED_TENDRIL.get()):(random.nextBoolean()?TC4Modern.TAINTED_GOO.get():TC4Modern.TAINTED_TENDRIL.get()));}
 @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.addAdditionalSaveData(t);t.putBoolean("Sheared",sheared());}
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.readAdditionalSaveData(t);entityData.set(SHEARED,t.getBoolean("Sheared"));}
 @Override public void handleEntityEvent(byte e){if(e==10)sheepTimer=40;else super.handleEntityEvent(e);}
 final class ConvertGrassGoal extends Goal {
  int timer;ConvertGrassGoal(){setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.JUMP,Flag.LOOK));}
  @Override public boolean canUse(){return random.nextInt(250)==0&&(level().getBlockState(blockPosition()).is(Blocks.SHORT_GRASS)||level().getBlockState(blockPosition().below()).is(Blocks.GRASS_BLOCK));}
  @Override public boolean canContinueToUse(){return timer>0;}
  @Override public boolean requiresUpdateEveryTick(){return true;}
  @Override public void start(){timer=40;level().broadcastEntityEvent(TaintedMob.this,(byte)10);getNavigation().stop();}
  @Override public void stop(){timer=0;}
  @Override public void tick(){timer=Math.max(0,timer-1);if(timer==4){var pos=blockPosition();if(level().getBlockState(pos).is(Blocks.SHORT_GRASS)||level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)){level().levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));level().setBlock(pos,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);TaintBiomes.spread((net.minecraft.server.level.ServerLevel)level(),pos);}}}
 }
}
