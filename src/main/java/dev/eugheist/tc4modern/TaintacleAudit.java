package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class TaintacleAudit {
 static final BlockPos BASE=new BlockPos(680,121,-90);static int ticks;static volatile String failed;static Taintacle normal,small,giant,outside;static net.minecraft.world.entity.animal.Cow cow;static net.minecraft.world.entity.animal.Cow distant;static float start;static double x;
 static void check(boolean ok,String message){if(!ok)failed=message;System.out.println("TC4_TAINTACLE "+(ok?"PASS ":"FAIL ")+message);if(!ok)throw new IllegalStateException(message);}
 static Taintacle spawn(net.minecraft.server.level.ServerLevel l,EntityType<Taintacle> type,BlockPos p){var e=type.create(l);e.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);e.setPersistenceRequired();l.addFreshEntity(e);return e;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post ev){
  if(!Boolean.getBoolean("tc4modern.taintacleAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=server.overworld();p.teleportTo(l,BASE.getX()+.5,BASE.getY()+2,BASE.getZ()-10,0,12);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();l.setDayTime(6000);l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(48),e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int dx=-15;dx<=15;dx++)for(int dz=-5;dz<=18;dz++){var pos=BASE.offset(dx,0,dz);TaintBiomes.spread(l,pos);for(int y=-1;y<=9;y++)l.setBlock(pos.offset(0,y,0),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);}
   normal=spawn(l,TaintacleContent.NORMAL.get(),BASE);small=spawn(l,TaintacleContent.SMALL.get(),BASE.offset(-4,0,0));giant=spawn(l,TaintacleContent.GIANT.get(),BASE.offset(6,0,5));outside=spawn(l,TaintacleContent.NORMAL.get(),BASE.offset(25,0,0));outside.setNoGravity(true);x=normal.getX();
   check(ResearchClues.matchesItemTrigger("PRIMPEARL","tc4modern:primordial_pearl")&&BasicsBookRecipes.researchIcon("PRIMPEARL")!=null,"pearl research clue and book icon");check(AspectTable.get("tc4modern:primordial_pearl").values().stream().mapToInt(Integer::intValue).sum()==96,"pearl six primal aspects at 16 each");check(ScanTarget.entity(normal).aspects().get("vitium")==3&&ScanTarget.entity(small).aspects().get("vitium")==1,"original tentacle scan aspects");
   check(normal.getMaxHealth()==50&&small.getMaxHealth()==8&&giant.getMaxHealth()==125,"original health 50/8/125");check(normal.getBbHeight()==3&&small.getBbHeight()==1&&giant.getBbHeight()==6,"original dimensions");
   normal.move(MoverType.SELF,new Vec3(3,2,3));check(normal.getX()==x&&normal.getY()==BASE.getY(),"rooted movement rejects horizontal motion and ascent");
   cow=EntityType.COW.create(l);cow.moveTo(normal.getX(),BASE.getY(),normal.getZ()+1,0,0);cow.setNoAi(true);l.addFreshEntity(cow);start=cow.getHealth();
  });
  if(ticks==100)LiquidDeathAudit.shot(mc,"taintacle-family.png");
  if(ticks==120)server.execute(()->{check(cow.getHealth()<start||!cow.isAlive(),"natural close attack");check(outside.getHealth()<50,"damage outside taint biome");check(normal.getHealth()==50,"tainted biome protects normal from environmental damage");
   var l=server.overworld();cow.discard();giant.setNoAi(true);var target=EntityType.COW.create(l);target.moveTo(BASE.getX()+.5,BASE.getY(),BASE.getZ()+7.5,0,0);target.setNoAi(true);l.addFreshEntity(target);l.setBlock(target.blockPosition(),TC4Modern.TAINT_FIBRES.get().defaultBlockState(),3);target.setOnGround(true);normal.setTarget(target);distant=target;target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);
   giant.hurt(giant.damageSources().generic(),80);check(giant.anger()>0&&giant.getHealth()>=90,"giant caps large hit at 35 and enrages");
  });
  if(ticks==150)server.execute(()->System.out.println("TC4_TAINTACLE DEBUG target="+normal.getTarget()+" ground="+distant.onGround()+" pos="+distant.position()+" block="+distant.level().getBlockState(distant.blockPosition())+" biome="+distant.level().getBiome(distant.blockPosition())+" cooldown="+normal.attackTicks));
  if(ticks==190)server.execute(()->check(!server.overworld().getEntitiesOfClass(Taintacle.class,new AABB(BASE.offset(0,0,7)).inflate(2),Taintacle::small).isEmpty(),"distant grounded target summons small tentacle"));
  if(ticks==210)LiquidDeathAudit.shot(mc,"taintacle-anger.png");
  if(ticks==270)server.execute(()->check(!small.isAlive(),"small lifetime expires after 200 ticks"));
  if(ticks==290)server.execute(()->{var l=server.overworld();var p=server.getPlayerList().getPlayer(id);var g1=spawn(l,TaintacleContent.GIANT.get(),BASE.offset(-10,0,10));var g2=spawn(l,TaintacleContent.GIANT.get(),BASE.offset(-10,0,12));giant.discard();
   g1.setHealth(0);g1.die(g1.damageSources().generic());check(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(BASE).inflate(30),e->e.getItem().is(TaintacleContent.PEARL.get())).isEmpty(),"no pearl while another giant remains");g2.setHealth(0);g2.die(g2.damageSources().generic());check(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(BASE).inflate(30),e->e.getItem().is(TaintacleContent.PEARL.get())).size()==1,"last giant drops exactly one primordial pearl");
   var pos=BASE.offset(-8,1,-3);l.setBlock(pos,TC4Modern.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeEntity)l.getBlockEntity(pos);for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects()){node.reservoir.aspects.add(a,100);node.reservoir.aspectsBase.add(a,100);}node.reservoir.modifier=thaumcraft.api.nodes.NodeModifier.FADING;var stack=new net.minecraft.world.item.ItemStack(TaintacleContent.PEARL.get(),2);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
   var result=stack.getItem().onItemUseFirst(stack,new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false)));check(result.consumesAction()&&stack.getCount()==1,"pearl node use consumes one even in creative");for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())check(node.reservoir.getNodeVisBase(a)>=98&&node.reservoir.getNodeVisBase(a)<=(ResearchProgress.completed(p.getPersistentData()).contains("PRIMNODE")?106:103),"original node base delta "+a.getTag());
   p.getInventory().clearContent();p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(TaintacleContent.PEARL.get()));p.getInventory().selected=0;var drop=new net.minecraft.world.entity.item.ItemEntity(l,p.getX(),p.getY(),p.getZ()+2,new net.minecraft.world.item.ItemStack(TaintacleContent.PEARL.get()));drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);p.containerMenu.broadcastChanges();
  });
  if(ticks==305)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.setDeltaMovement(Vec3.ZERO);p.teleportTo(server.overworld(),BASE.getX()+.5,BASE.getY()+2,BASE.getZ()-10,0,12);p.getAbilities().flying=true;p.onUpdateAbilities();var drop=new net.minecraft.world.entity.item.ItemEntity(p.level(),p.getX(),p.getY()+.5,p.getZ()+2,new net.minecraft.world.item.ItemStack(TaintacleContent.PEARL.get()));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();p.level().addFreshEntity(drop);});
  if(ticks==320)LiquidDeathAudit.shot(mc,"taintacle-pearl-hand.png");
  if(ticks==330)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==345){LiquidDeathAudit.shot(mc,"taintacle-pearl-inventory.png");mc.setScreen(null);}
  if(ticks==350)mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
  if(ticks==355)LiquidDeathAudit.shot(mc,"taintacle-pearl-third-person.png");
  if(ticks==359)mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
  if(ticks==360){check(TaintacleClient.rendered>0,"client renderer used");check(mc.getResourceManager().getResource(AlchemyContent.id("textures/models/taintacle.png")).isPresent(),"original texture imported");System.out.println(failed==null?"TC4_TAINTACLE ALL PASS":"TC4_TAINTACLE FAILED "+failed);mc.stop();}
 }
}
