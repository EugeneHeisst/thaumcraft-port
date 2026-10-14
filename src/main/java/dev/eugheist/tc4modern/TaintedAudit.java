package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import thaumcraft.api.aspects.Aspect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class TaintedAudit {
 static final BlockPos BASE=new BlockPos(740,121,-90);static int ticks;static volatile String failed;static TaintedMob cow,sheep,creeper;static net.minecraft.world.entity.animal.Pig target;static float targetHealth;static AspectOrb expired,orb,natural;static net.minecraft.world.item.ItemStack wand;
 static void check(boolean ok,String m){if(!ok)failed=m;System.out.println("TC4_TAINTED "+(ok?"PASS ":"FAIL ")+m);if(!ok)throw new IllegalStateException(m);}
 static TaintedMob spawn(net.minecraft.server.level.ServerLevel l,String species,BlockPos pos){var e=TaintedContent.TYPES.get(species).get().create(l);e.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);l.addFreshEntity(e);return e;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post ev){
  if(!Boolean.getBoolean("tc4modern.taintedAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=s.overworld();p.teleportTo(l,BASE.getX()+.5,BASE.getY()+2,BASE.getZ()-8,0,12);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(40),e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int x=-16;x<=16;x++)for(int z=-4;z<=24;z++)for(int y=-1;y<9;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
   int i=0;for(var entry:java.util.Map.<String,EntityType<?>>of("chicken",EntityType.CHICKEN,"cow",EntityType.COW,"creeper",EntityType.CREEPER,"pig",EntityType.PIG,"sheep",EntityType.SHEEP,"villager",EntityType.VILLAGER).entrySet()){
    var pos=BASE.offset(-10+i*4,0,3);var original=(LivingEntity)entry.getValue().create(l);original.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);l.addFreshEntity(original);original.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,100,0));original.hurt(original.damageSources().genericKill(),1000);
    var replacements=l.getEntitiesOfClass(TaintedMob.class,new AABB(pos).inflate(1));check(original.isRemoved()&&replacements.size()==1&&replacements.getFirst().species().equals(entry.getKey()),"death conversion "+entry.getKey());replacements.getFirst().setNoAi(true);i++;
   }
   var zombie=EntityType.ZOMBIE.create(l);zombie.moveTo(BASE.getX(),BASE.getY(),BASE.getZ()+16,0,0);l.addFreshEntity(zombie);zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(TC4Modern.FLUX_TAINT,100,0));zombie.hurt(zombie.damageSources().genericKill(),1000);var slimes=l.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityThaumicSlime.class,new AABB(BASE.offset(0,0,16)).inflate(2));check(zombie.isRemoved()&&slimes.size()==1&&slimes.getFirst().getSlimeSize()==3,"other tainted death produces slime size 1+maxHP/10");slimes.forEach(Entity::discard);
   cow=spawn(l,"cow",BASE.offset(-8,0,10));target=EntityType.PIG.create(l);target.moveTo(cow.getX()+2,cow.getY(),cow.getZ(),0,0);target.setNoAi(true);l.addFreshEntity(target);targetHealth=target.getHealth();cow.setTarget(target);
   sheep=spawn(l,"sheep",BASE.offset(-3,0,10));sheep.setNoAi(true);check(sheep.isShearable(p,new ItemStack(Items.SHEARS),l,sheep.blockPosition()),"tainted sheep is shearable");var wool=sheep.onSheared(p,new ItemStack(Items.SHEARS),l,sheep.blockPosition());check(wool.size()==1&&wool.getFirst().is(Items.PURPLE_WOOL)&&wool.getFirst().getCount()>=1&&wool.getFirst().getCount()<=3&&!sheep.isShearable(p,ItemStack.EMPTY,l,sheep.blockPosition()),"shearing gives 1-3 purple wool once");var tag=new net.minecraft.nbt.CompoundTag();sheep.saveWithoutId(tag);var copy=TaintedContent.TYPES.get("sheep").get().create(l);copy.load(tag);check(copy.sheared(),"sheared state persists");
   var chicken=spawn(l,"chicken",BASE.offset(2,0,10));check(!chicken.causeFallDamage(20,1,chicken.damageSources().fall()),"chicken is immune to fall damage");chicken.setNoAi(true);
   expired=new AspectOrb(l,BASE.getX()+10,BASE.getY()+.5,BASE.getZ()+18,Aspect.FIRE,2);l.addFreshEntity(expired);
   orb=new AspectOrb(l,p.getX(),p.getY(),p.getZ(),Aspect.AIR,2);orb.setNoGravity(true);l.addFreshEntity(orb);orb.playerTouch(p);check(!orb.isRemoved(),"orb cannot be collected without a hotbar wand");wand=new ItemStack(TC4Modern.BASIC_WAND.get());p.getInventory().setItem(10,wand);orb.playerTouch(p);check(!orb.isRemoved(),"wand in backpack does not collect vis");p.getInventory().setItem(10,ItemStack.EMPTY);p.getInventory().setItem(0,wand);orb.playerTouch(p);check(orb.isRemoved()&&WandData.get(wand,Aspect.AIR)==200,"hotbar wand absorbs orb as 2 vis / 200 centivis");
  });
  if(ticks==100)LiquidDeathAudit.shot(mc,"tainted-family.png");
  if(ticks==140)s.execute(()->{check(!target.isAlive()||target.getHealth()<targetHealth,"tainted cow naturally attacks animals");cow.setNoAi(true);target.discard();var l=s.overworld();var goal=sheep.new ConvertGrassGoal();l.setBlock(sheep.blockPosition().below(),Blocks.GRASS_BLOCK.defaultBlockState(),3);goal.start();for(int i=0;i<36;i++)goal.tick();goal.stop();check(l.getBlockState(sheep.blockPosition()).is(TC4Modern.TAINT_FIBRES.get())&&l.getBiome(sheep.blockPosition()).is(TaintBiomes.TAINTED_LAND),"sheep grazing turns grass column to taint with fibres pos="+sheep.blockPosition()+" state="+l.getBlockState(sheep.blockPosition())+" biome="+l.getBiome(sheep.blockPosition()));
   creeper=spawn(l,"creeper",BASE.offset(8,0,14));creeper.setNoAi(true);var victim=EntityType.COW.create(l);victim.moveTo(creeper.getX()+4,creeper.getY(),creeper.getZ(),0,0);victim.setNoAi(true);l.addFreshEntity(victim);creeper.explode();check(creeper.isRemoved()&&victim.hasEffect(TC4Modern.FLUX_TAINT),"creeper taint explosion poisons a nearby living victim");
   var p=s.getPlayerList().getPlayer(id);natural=new AspectOrb(l,p.getX(),p.getY()+1,p.getZ()+2,Aspect.AIR,1);natural.setDeltaMovement(Vec3.ZERO);l.addFreshEntity(natural);
  });
  if(ticks==210)s.execute(()->{check(expired.isRemoved(),"uncollected orb expires after 150 ticks");check(natural.isRemoved()&&WandData.get(wand,Aspect.AIR)==300,"natural orb homing and collision recharge hotbar wand");var p=s.getPlayerList().getPlayer(id);var l=s.overworld();p.takeXpDelay=0;WandData.add(wand,Aspect.AIR,WandData.capacity(wand),true);var full=new AspectOrb(l,p.getX(),p.getY(),p.getZ(),Aspect.AIR,3);l.addFreshEntity(full);full.playerTouch(p);check(!full.isRemoved(),"full wand refuses orb");full.discard();
   int initial=l.getEntitiesOfClass(AspectOrb.class,new AABB(BASE).inflate(40)).size();for(int i=0;i<30;i++){var mob=EntityType.COW.create(l);mob.moveTo(BASE.getX()+10,BASE.getY(),BASE.getZ()+6,0,0);l.addFreshEntity(mob);mob.lastHurtByPlayerTime=100;mob.hurt(mob.damageSources().genericKill(),1000);}check(l.getEntitiesOfClass(AspectOrb.class,new AABB(BASE).inflate(40)).size()>initial,"player-recent kills drop primal aspect orbs");p.getInventory().clearContent();
  });
  if(ticks==235)LiquidDeathAudit.shot(mc,"tainted-orbs.png");
  if(ticks==245)s.execute(()->{var p=s.getPlayerList().getPlayer(id);p.takeXpDelay=0;var partial=new ItemStack(TC4Modern.BASIC_WAND.get());WandData.add(partial,Aspect.AIR,24,true);p.getInventory().setItem(0,partial);var extra=new AspectOrb(p.level(),p.getX(),p.getY(),p.getZ(),Aspect.AIR,2);p.level().addFreshEntity(extra);extra.playerTouch(p);check(extra.isRemoved()&&WandData.get(partial,Aspect.AIR)==2500,"partially empty wand takes one vis and excess orb vis vanishes");
   p.getInventory().clearContent();int slot=0;for(var entry:TaintedContent.TYPES.entrySet()){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("taint_"+entry.getKey()+"_spawn_egg"));p.getInventory().setItem(slot++,new ItemStack(item));}p.getInventory().selected=0;p.containerMenu.broadcastChanges();var drop=new ItemEntity(p.level(),p.getX(),p.getY()+.5,p.getZ()+2,p.getMainHandItem().copy());drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();p.level().addFreshEntity(drop);
  });
  if(ticks==260)LiquidDeathAudit.shot(mc,"tainted-eggs-hand.png");
  if(ticks==270)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==280){LiquidDeathAudit.shot(mc,"tainted-eggs-inventory.png");mc.setScreen(null);}
  if(ticks==290)mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
  if(ticks==295)LiquidDeathAudit.shot(mc,"tainted-eggs-third.png");
  if(ticks==299)mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
  if(ticks==310){check(TaintedClient.rendered>0&&TaintedClient.orbs>0,"animal and aspect orb renderers used");System.out.println(failed==null?"TC4_TAINTED ALL PASS":"TC4_TAINTED FAILED "+failed);mc.stop();}
 }
}
