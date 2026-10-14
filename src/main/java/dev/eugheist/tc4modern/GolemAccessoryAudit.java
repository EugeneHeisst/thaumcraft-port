package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Hidden native audit: golem accessories (ItemGolemDecoration), advanced golems and the remaining golemancy recipes. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemAccessoryAudit {
 static final BlockPos BASE=new BlockPos(360,121,-90);static int ticks;static volatile boolean done;static volatile int darts,dartHits;static volatile float dartDamage;static StrawGolem shooter,display;static net.minecraft.world.entity.monster.Husk husk;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_ACCESSORY FAIL "+s);System.out.println("TC4_ACCESSORY PASS "+s);}
 static boolean near(double a,double b){return Math.abs(a-b)<1e-4;}
 static boolean give(StrawGolem g,ServerPlayer p,int deco){var stack=new ItemStack(Golemancy.decoration(deco),2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);String before=g.decoration();g.mobInteract(p,InteractionHand.MAIN_HAND);boolean added=!g.decoration().equals(before);check(stack.getCount()==(added?1:2),"accessory "+Golemancy.DECO.charAt(deco)+" consumed only when added");return added;}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.connection.teleport(BASE.getX()+.5,BASE.getY()+1.2,BASE.getZ()-3.5,0,20);l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(20)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);for(int x=-8;x<9;x++)for(int z=-8;z<9;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
  var a=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(-2,0,2),-1);
  check(give(a,p,0),"top hat added");check(!give(a,p,3),"fez refused next to top hat");check(give(a,p,1),"spectacles added");check(!give(a,p,5),"visor refused next to spectacles");check(give(a,p,2),"bowtie added");check(!give(a,p,6),"plating refused next to bowtie");check(give(a,p,4)&&give(a,p,7),"dart launcher and mace added");check(!give(a,p,7),"duplicate mace refused");
  check(a.decoration().equals("HGBRM"),"decoration string HGBRM="+a.decoration());
  check(near(a.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),40),"top hat +5 health");check(near(a.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),.31f*1.1f),"bowtie speed x1.1");check(near(a.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),2+4+2),"mace +2 damage");check(near(a.range(),17.6f),"spectacles range 16 -> 17.6");check(near(a.getAttribute(Attributes.ARMOR).getBaseValue(),15),"no armour bonus without visor/plating");
  var b=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(2,0,2),-1);give(b,p,3);give(b,p,5);give(b,p,6);check(b.decoration().equals("FVP"),"fez visor plating");check(near(b.getAttribute(Attributes.ARMOR).getBaseValue(),20),"visor+plating armour 15+1+4 capped 20");check(near(b.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),.31f*.88f),"plating speed x0.88");b.regen=0;b.setHealth(10);b.aiStep();check(b.regen==(int)(125*.66f),"fez regeneration delay x0.66 regen="+b.regen);
  var zombie=EntityType.ZOMBIE.create(l);zombie.moveTo(b.getX()+1,b.getY(),b.getZ(),0,0);zombie.setNoAi(true);l.addFreshEntity(zombie);check(b.doHurtTarget(zombie)&&zombie.getKillCredit() instanceof net.minecraft.world.entity.player.Player,"visor hit counts as player kill credit");zombie.discard();
  var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.restoreItem(a.asItem());check(copy.decoration().equals("HGBRM"),"decorations survive bell item");
  // advanced golems
  var placer=new ItemStack(Golemancy.placer(GolemMaterial.IRON).get());var t=new net.minecraft.nbt.CompoundTag();t.putByte("core",(byte)2);Golemancy.tag(placer,t);
  var components=java.util.List.of(new ItemStack(Items.REDSTONE),new ItemStack(Items.GLOWSTONE_DUST),new ItemStack(Items.GUNPOWDER),new ItemStack(EssentiaDevices.JAR.get()),new ItemStack(ZombieBrainContent.ITEM.get()));
  var r=InfusionItemRecipes.match(components,placer,java.util.Set.of("ADVANCEDGOLEM"));check(r!=null&&r.key().equals("AdvancedGolem")&&r.instability()==3&&r.aspects().visSize()==24,"AdvancedGolem infusion matches iron placer");var adv=r.outputStack(placer);check(NoteData.tag(adv).contains("advanced")&&NoteData.tag(adv).getByte("core")==2&&adv.is(placer.getItem()),"advanced output keeps material and core");
  check(InfusionItemRecipes.match(components,new ItemStack(Golemancy.placer(GolemMaterial.STRAW).get()),java.util.Set.of("ADVANCEDGOLEM"))!=null,"any material accepted");check(InfusionItemRecipes.match(components,placer,java.util.Set.of())==null,"needs ADVANCEDGOLEM research");
  var c=new StrawGolem(Golemancy.ENTITY.get(),l);c.material(GolemMaterial.IRON,true);c.home=BASE;c.restoreItem(adv);check(c.advanced()&&near(c.range(),19.2f)&&c.attackSpeed()==18&&near(c.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),.31f*1.1f),"advanced range +20%, attack 18 ticks, speed x1.1");
  check(c.installUpgrade(0)&&c.installUpgrade(1)&&!c.installUpgrade(2),"advanced iron golem holds 1+1 upgrades");var back=new StrawGolem(Golemancy.ENTITY.get(),l);back.restoreItem(c.asItem());check(back.advanced()&&back.upgradeCount()==2,"advanced flag and extra upgrade survive bell item");
  // recipes
  int[][] costs={{8,8},{4,4},{4,4},{4,4},{4,4},{4,4},{8},{4,4}};String[] keys={"TinyHat","TinyGlasses","TinyBowtie","TinyFez","TinyDart","TinyVisor","TinyArmor","TinyHammer"};
  for(int i=0;i<8;i++){String key=keys[i];var e=ArcaneRecipes.entries().stream().filter(x->x.key().equals(key)).findFirst().orElse(null);check(e!=null&&e.output().is(Golemancy.decoration(i))&&e.research().equals(key.toUpperCase(java.util.Locale.ROOT))&&e.cost().visSize()==java.util.Arrays.stream(costs[i]).sum(),"original arcane "+key);}
  for(var key:java.util.List.of("CoreUse","CoreSorting","CoreFishing")){var recipe=InfusionItemRecipes.all().stream().filter(x->x.key().equals(key)).findFirst().orElseThrow();check(InfusionItemRecipes.match(recipe.componentStacks(),new ItemStack(recipe.central().get()),java.util.Set.of(recipe.research()))==recipe&&recipe.instability()==3,"original infusion "+key);}
  for(var key:java.util.List.of("COREUSE","CORESORTING","COREFISHING","ADVANCEDGOLEM","TINYHAT","TINYGLASSES","TINYBOWTIE","TINYFEZ","TINYDART","TINYVISOR","TINYARMOR","TINYHAMMER"))check(BasicsBookRecipes.researchIcon(key)!=null,"research icon "+key);
  for(int i=0;i<8;i++)p.getInventory().setItem(i,new ItemStack(Golemancy.decoration(i)));p.getInventory().setItem(8,adv);p.getInventory().selected=8;p.containerMenu.broadcastChanges();
  for(int i=0;i<4;i++){var drop=new ItemEntity(l,BASE.getX()-1.2+i*.8,BASE.getY()+1,BASE.getZ()-1,new ItemStack(i==3?Golemancy.SORTING.get():Golemancy.decoration(i*2)));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);}
  // dart launcher: a guard golem shoots a target 8 blocks away, the display golem shows advanced+core accessories
  shooter=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(-6,0,6),4);give(shooter,p,4);husk=EntityType.HUSK.create(l);husk.moveTo(BASE.getX()+2.5,BASE.getY(),BASE.getZ()+6.5,90,0);husk.setNoAi(true);husk.setPersistenceRequired();l.addFreshEntity(husk);
  display=GolemHarvestAudit.golem(p,GolemMaterial.CLAY,BASE.offset(0,0,2),2);display.getEntityData().set(StrawGolem.ADVANCED,true);give(display,p,1);give(display,p,2);give(display,p,7);display.setNoAi(true);display.setYRot(180);display.yBodyRot=180;display.yHeadRot=180;
  p.getInventory().clearContent();for(int i=0;i<8;i++)p.getInventory().setItem(i,new ItemStack(Golemancy.decoration(i)));p.getInventory().setItem(8,adv);p.containerMenu.broadcastChanges();
  done=true;
 }
 @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e){if(Boolean.getBoolean("tc4modern.golemAccessoryAuditOnly")&&!e.getLevel().isClientSide&&e.getEntity() instanceof GolemDart d&&d.getOwner()==shooter){darts++;check(d.pickup==net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED&&near(d.getBaseDamage(),shooter.getAttributeValue(Attributes.ATTACK_DAMAGE)*.4),"dart unpickable, damage 40% of melee "+d.getBaseDamage());}}
 @SubscribeEvent static void hurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post e){if(Boolean.getBoolean("tc4modern.golemAccessoryAuditOnly")&&e.getEntity()==husk&&e.getSource().getDirectEntity() instanceof GolemDart&&e.getSource().getEntity()==shooter){dartHits++;dartDamage=e.getNewDamage();}}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemAccessoryAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->setup(server.getPlayerList().getPlayer(id)));
  if(ticks==120){check(done,"server accessory checks finished");var items=new java.util.ArrayList<Item>();for(int i=0;i<8;i++)items.add(Golemancy.decoration(i));items.addAll(java.util.List.of(Golemancy.USE.get(),Golemancy.SORTING.get(),Golemancy.FISHING.get()));for(var item:items){var model=mc.getItemRenderer().getModel(new ItemStack(item),mc.level,mc.player,0);check(model!=mc.getModelManager().getMissingModel()&&!model.isGui3d(),"flat item model "+item);for(var context:java.util.List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)){var scale=model.getTransforms().getTransform(context).scale;check(scale.x<=1&&scale.y<=1&&scale.z<=1,"bounded scale "+item+" "+context);}}
   var adv=mc.player.getInventory().getItem(8);var plain=new ItemStack(Golemancy.placer(GolemMaterial.IRON).get());check(mc.getItemRenderer().getModel(adv,mc.level,mc.player,0)!=mc.getItemRenderer().getModel(plain,mc.level,mc.player,0),"advanced placer uses overlay model");LiquidDeathAudit.shot(mc,"golem-accessories-items.png");}
  if(ticks==130){mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
  if(ticks==145){LiquidDeathAudit.shot(mc,"golem-accessories-inventory.png");mc.setScreen(null);server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(BASE.getX()+.5,BASE.getY()+.3,BASE.getZ()+.4,0,12);});}
  if(ticks==175){check(GolemClient.Model.accessoryParts>0,"accessory model parts rendered "+GolemClient.Model.accessoryParts);LiquidDeathAudit.shot(mc,"golem-accessories-closeup.png");server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(BASE.getX()-3.2,BASE.getY()+.6,BASE.getZ()+.6,-50,12);});}
  if(ticks==200)LiquidDeathAudit.shot(mc,"golem-accessories-side.png");
  if(ticks==205)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(BASE.getX()-1.8,BASE.getY()+1.2,BASE.getZ()+1.5,0,18);});
  if(ticks>205&&ticks<500&&GolemClient.DartRenderer.rendered>0&&dartHits>0&&ticks%5==0){LiquidDeathAudit.shot(mc,"golem-accessories-dart.png");check(darts>=1&&dartHits>=1&&dartDamage>0,"guard golem with dart launcher hit husk 8 blocks away darts="+darts+" hits="+dartHits+" dmg="+dartDamage);ticks=500;}
  if(ticks==499)check(false,"no dart hit within timeout darts="+darts+" hits="+dartHits+" rendered="+GolemClient.DartRenderer.rendered);
  if(ticks==510){server.execute(()->{shooter.discard();husk.discard();});System.out.println("TC4_ACCESSORY ALL PASS");mc.stop();}
 }
}
