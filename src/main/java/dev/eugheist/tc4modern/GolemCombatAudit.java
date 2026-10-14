package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemCombatAudit {
 static final BlockPos BASE=new BlockPos(190,121,-90);static StrawGolem guard,butcher;static Zombie zombie;static int ticks;static volatile boolean done;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_COMBAT FAIL "+s);System.out.println("TC4_COMBAT PASS "+s);}
 static <T extends LivingEntity>T spawn(ServerPlayer p,EntityType<T> type,BlockPos pos){var e=type.create(p.level());e.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);if(e instanceof Mob m){m.setNoAi(true);m.setPersistenceRequired();}p.level().addFreshEntity(e);return e;}
 static void setup(ServerPlayer p){var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.connection.teleport(BASE.getX()+8,BASE.getY()+4,BASE.getZ()-7,40,20);p.getAbilities().flying=true;p.onUpdateAbilities();l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());l.setDayTime(18000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(35)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-5;x<30;x++)for(int z=-5;z<8;z++)for(int y=-1;y<4;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
  guard=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE,4);guard.owner=p.getUUID();zombie=spawn(p,EntityType.ZOMBIE,BASE.offset(4,0,0));var pig=spawn(p,EntityType.PIG,BASE.offset(1,0,3));var creeper=spawn(p,EntityType.CREEPER,BASE.offset(2,0,3));var rules=new GolemCombatGoal(guard);
  check(rules.valid(zombie)&&!rules.valid(pig)&&!rules.valid(creeper)&&!rules.valid(p),"guard default hostiles only, owner safe");guard.installUpgrade(4);check(rules.valid(pig)&&rules.valid(creeper),"Order enables animals and creepers");var menu=new GolemMenu(99,p.getInventory(),guard);for(int i=0;i<4;i++)check(menu.clickMenuButton(p,80+i),"guard toggle "+i);check(!rules.valid(zombie)&&!rules.valid(pig)&&!rules.valid(creeper),"disabled attack categories enforced");menu.removed(p);guard.getEntityData().set(StrawGolem.ATTACK_FLAGS,0);guard.restoreUpgrades(0);
  zombie.moveTo(BASE.getX()+20,BASE.getY(),BASE.getZ(),0,0);check(!rules.valid(zombie),"home radius bounds target");zombie.moveTo(BASE.getX()+4,BASE.getY(),BASE.getZ()+.5,0,0);
  butcher=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(22,0,0),9);for(int i=0;i<3;i++)spawn(p,EntityType.COW,BASE.offset(23+i,0,1));var baby=spawn(p,EntityType.COW,BASE.offset(24,0,3));baby.setBaby(true);check(!new GolemCombatGoal(butcher).valid(baby),"butcher excludes babies");var wolf=spawn(p,EntityType.WOLF,BASE.offset(23,0,3));wolf.setTame(true,false);check(!new GolemCombatGoal(butcher).valid(wolf),"butcher excludes tame animals");
  var tag=new CompoundTag();butcher.addAdditionalSaveData(tag);var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.readAdditionalSaveData(tag);check(copy.core()==9,"butcher core persists");guard.getEntityData().set(StrawGolem.ATTACK_FLAGS,6);var saved=guard.asItem();copy.restoreItem(saved);check(copy.core()==4&&copy.getEntityData().get(StrawGolem.ATTACK_FLAGS)==6,"guard item preserves attack toggles");guard.getEntityData().set(StrawGolem.ATTACK_FLAGS,0);
  for(String key:java.util.List.of("CoreGuard","CoreButcher"))check(CrucibleRecipes.byKey().get(key)!=null,"recipe "+key);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!Boolean.getBoolean("tc4modern.golemCombatAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();if(ticks==40)server.execute(()->setup(server.getPlayerList().getPlayer(id)));if(ticks==400)server.execute(()->{check(!zombie.isAlive(),"guard naturally navigates and kills hostile");long adults=butcher.level().getEntitiesOfClass(Cow.class,new AABB(BASE.offset(22,0,0)).inflate(10)).stream().filter(e->e.isAlive()&&!e.isBaby()).count();check(adults==2,"butcher leaves two adult cows");done=true;});if(ticks==420){check(done,"server checks complete");LiquidDeathAudit.shot(mc,"golem-combat.png");server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(guard.getX()+1,guard.getY(),guard.getZ()-1,0,0);p.openMenu(guard,b->b.writeVarInt(guard.getId()));});}if(ticks==470){check(mc.screen instanceof GolemClient.Screen,"guard actual client menu");mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,80);}if(ticks==490){check((mc.player.containerMenu instanceof GolemMenu m)&&(m.golem.getEntityData().get(StrawGolem.ATTACK_FLAGS)&1)!=0,"network guard toggle synchronized");LiquidDeathAudit.shot(mc,"golem-guard-settings.png");mc.player.closeContainer();System.out.println("TC4_COMBAT ALL PASS");mc.stop();}}
}
