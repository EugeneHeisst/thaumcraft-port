package dev.eugheist.tc4modern;

import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;

/** Original 150-tick vis orb. A hotbar wand may accept part of the orb; the remainder vanishes. */
public final class AspectOrb extends Entity {
 private static final EntityDataAccessor<String> ASPECT=SynchedEntityData.defineId(AspectOrb.class,EntityDataSerializers.STRING);
 private static final EntityDataAccessor<Integer> VALUE=SynchedEntityData.defineId(AspectOrb.class,EntityDataSerializers.INT);
 private int age,health=5;private Player closest;
 public AspectOrb(EntityType<? extends AspectOrb> t,Level l){super(t,l);}
 public AspectOrb(Level l,double x,double y,double z,Aspect aspect,int value){this(TaintedContent.ORB.get(),l);setPos(x,y,z);entityData.set(ASPECT,aspect.getTag());entityData.set(VALUE,value);setDeltaMovement((random.nextFloat()*.2-.1)*2,random.nextFloat()*.4,(random.nextFloat()*.2-.1)*2);}
 @Override public boolean shouldRenderAtSqrDistance(double d){return d<1024;}
 public Aspect aspect(){var a=Aspect.getAspect(entityData.get(ASPECT));return a==null?Aspect.AIR:a;}
 public int value(){return entityData.get(VALUE);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(ASPECT,"aer");b.define(VALUE,1);}
 static int wandSlot(Player p,Aspect a,int amount){for(int i=0;i<9;i++){var st=p.getInventory().getItem(i);if(WandData.isWand(st)&&WandData.add(st,a,amount,false)<amount)return i;}return -1;}
 @Override public void tick(){
  super.tick();if(++age>=150){discard();return;}setDeltaMovement(getDeltaMovement().add(0,-.03f,0));
  if(isInLava())setDeltaMovement((random.nextFloat()-random.nextFloat())*.2,.2,(random.nextFloat()-random.nextFloat())*.2);
  if(!level().isClientSide){
   if(tickCount%5==0&&closest==null)closest=level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(8),p->!p.isSpectator()&&wandSlot(p,aspect(),value())>=0).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
   if(closest!=null){var toward=closest.getEyePosition().subtract(position()).scale(1.0/8);double distance=toward.length(),pull=1-distance;if(pull>0&&distance>1e-6)setDeltaMovement(getDeltaMovement().add(toward.scale(pull*pull*.1/distance)));}
  }
  move(MoverType.SELF,getDeltaMovement());float friction=onGround()?level().getBlockState(blockPosition().below()).getFriction(level(),blockPosition().below(),this)*.98f:.98f;var velocity=getDeltaMovement();setDeltaMovement(velocity.x*friction,velocity.y*.98*(onGround()?-.9:1),velocity.z*friction);
  if(!level().isClientSide)for(var p:level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(.1)))playerTouch(p);
 }
 @Override public void playerTouch(Player p){if(level().isClientSide||isRemoved()||p.takeXpDelay>0||!aspect().isPrimal())return;int slot=wandSlot(p,aspect(),value());if(slot<0)return;WandData.add(p.getInventory().getItem(slot),aspect(),value(),true);p.takeXpDelay=2;playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.1f,.5f*((random.nextFloat()-random.nextFloat())*.7f+1.8f));discard();}
 @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){if(isInvulnerableTo(source))return false;health=(int)(health-amount);if(health<=0)discard();return false;}
 @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){t.putShort("Age",(short)age);t.putShort("Health",(short)health);t.putString("Aspect",aspect().getTag());t.putShort("Value",(short)value());}
 @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){age=t.getShort("Age");health=t.getShort("Health");entityData.set(ASPECT,t.getString("Aspect"));entityData.set(VALUE,(int)t.getShort("Value"));}
}
