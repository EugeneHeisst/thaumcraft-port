package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class BottleTaint {
 public static final DeferredHolder<Item,Item> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("bottle_taint"));
 public static final DeferredHolder<EntityType<?>,EntityType<Projectile>> ENTITY=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("bottle_taint"));
 public static java.util.function.Consumer<Projectile> burst=p->{};
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(Registries.ITEM,h->h.register(ITEM.getId(),new Bottle()));
  event.register(Registries.ENTITY_TYPE,h->h.register(ENTITY.getId(),EntityType.Builder.<Projectile>of(Projectile::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build(ENTITY.getId().toString())));
 }
 // The original item unexpectedly implements IScribeTools too; keep that behaviour.
 public static final class Bottle extends Item implements thaumcraft.api.IScribeTools {
  Bottle(){super(new Properties().stacksTo(8));}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
   var stack=player.getItemInHand(hand);
   level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
   if(!level.isClientSide){var projectile=new Projectile(ENTITY.get(),level);projectile.setOwner(player);double yaw=Math.toRadians(player.getYRot()),pitch=Math.toRadians(player.getXRot());projectile.setPos(player.getX()-Math.cos(yaw)*.16,player.getEyeY()-.1,player.getZ()-Math.sin(yaw)*.16);
    // Legacy EntityThrowable normalizes first, adds Gaussian spread, and does not inherit player motion.
    var direction=new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch-Math.toRadians(20)),Math.cos(yaw)*Math.cos(pitch)).normalize();
    projectile.setDeltaMovement(direction.add(level.random.nextGaussian()*.0075,level.random.nextGaussian()*.0075,level.random.nextGaussian()*.0075).scale(.5));level.addFreshEntity(projectile);}
   if(!player.getAbilities().instabuild)stack.shrink(1);
   return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
 }
 public static final class Projectile extends ThrowableItemProjectile {
  public Projectile(EntityType<? extends Projectile> type,Level level){super(type,level);}
  @Override protected Item getDefaultItem(){return ITEM.get();}
  @Override protected double getDefaultGravity(){return .05;}
  @Override protected void onHit(HitResult hit){super.onHit(hit);if(level().isClientSide)return;impact();}
  public void impact(){
   if(!(level() instanceof net.minecraft.server.level.ServerLevel level))return;
   for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(position(),position()).inflate(5)))
    if(!(entity instanceof thaumcraft.api.entities.ITaintedMob)&&!entity.isInvertedHealAndHarm())entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,100,0,false,true));
   // Original truncates negative coordinates towards zero, rather than flooring.
   int x=(int)getX(),y=(int)getY(),z=(int)getZ();
   for(int a=0;a<10;a++){
    var pos=new BlockPos(x+(int)((random.nextFloat()-random.nextFloat())*5),y,z+(int)((random.nextFloat()-random.nextFloat())*5));
    if(!level.random.nextBoolean()||TaintBiomes.isTainted(level,pos))continue;
    TaintBiomes.spread(level,pos);
    if(level.getBlockState(pos.below()).isSolidRender(level,pos.below())&&level.getBlockState(pos).canBeReplaced())level.setBlock(pos,TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);
   }
   level.broadcastEntityEvent(this,(byte)3);discard();
  }
  @Override public void handleEntityEvent(byte event){if(event==3)burst.accept(this);else super.handleEntityEvent(event);}
 }
}
