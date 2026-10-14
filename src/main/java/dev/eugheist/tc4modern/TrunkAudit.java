package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Visible audit: traveling trunk (EntityTravelingTrunk, ItemTrunkSpawner, bell retrieval, upgrades, dimension follow). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class TrunkAudit {
 static final BlockPos BASE=new BlockPos(600,121,-90);static int ticks;static TravelingTrunk trunk,guard;static net.minecraft.world.entity.monster.Husk husk;static float huskStart;static double farStart;
 static volatile String failed;
 static void check(boolean b,String s){if(!b){failed=s;System.out.println("TC4_TRUNK FAIL "+s);}if(!b)throw new IllegalStateException("TC4_TRUNK FAIL "+s);System.out.println("TC4_TRUNK PASS "+s);}
 static ServerPlayer pl(net.minecraft.server.MinecraftServer s,java.util.UUID id){return s.getPlayerList().getPlayer(id);}
 static TravelingTrunk spawn(ServerPlayer p,ItemStack stack,BlockPos on){p.setItemInHand(InteractionHand.MAIN_HAND,stack);stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(on),Direction.UP,on,false)));
  return p.serverLevel().getEntitiesOfClass(TravelingTrunk.class,new AABB(on.above()).inflate(.6)).stream().findFirst().orElseThrow();}
 static void setup(ServerPlayer p){
  if(p.level()!=p.server.overworld())p.teleportTo(p.server.overworld(),BASE.getX()+.5,BASE.getY(),BASE.getZ()-3.5,0,20);
  var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(48)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-40;x<=40;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
  p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-3.5,0,20);
  var item=new ItemStack(TravelingTrunk.ITEM.get());trunk=spawn(p,item,BASE.below());check(item.isEmpty()&&trunk.ownerName().equals(p.getGameProfile().getName())&&trunk.slots()==27&&trunk.getMaxHealth()==75,"spawner places an owned 27-slot 75 hp trunk and is consumed");
  var copy=new TravelingTrunk(TravelingTrunk.ENTITY.get(),l);trunk.inventory.setItem(5,new ItemStack(Items.DIAMOND,3));var tag=new net.minecraft.nbt.CompoundTag();trunk.saveWithoutId(tag);copy.load(tag);check(copy.inventory.getItem(5).getCount()==3&&copy.ownerName().equals(trunk.ownerName()),"trunk save/load keeps owner and contents");trunk.inventory.clearContent();
  var r=InfusionItemRecipes.all().stream().filter(x->x.key().equals("TravelTrunk")).findFirst().orElseThrow();check(InfusionItemRecipes.match(r.componentStacks(),new ItemStack(r.central().get()),java.util.Set.of("TRAVELTRUNK"))==r&&r.instability()==3&&r.aspects().getAmount(Aspect.VOID)==16&&r.output().get()==TravelingTrunk.ITEM.get(),"original TravelTrunk infusion (hungry chest, iron, 2 greatwood planks, wooden golem; void 16)");
  check(BasicsBookRecipes.researchIcon("TRAVELTRUNK")!=null,"research icon TRAVELTRUNK");
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.trunkAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)s.execute(()->setup(pl(s,id)));
  // follow by hopping
  if(ticks==60)s.execute(()->{var p=pl(s,id);p.connection.teleport(BASE.getX()+12.5,BASE.getY(),BASE.getZ()+.5,90,20);farStart=trunk.distanceTo(p);});
  if(ticks==100)LiquidDeathAudit.shot(mc,"trunk-hopping.png");
  if(ticks==220)s.execute(()->{var p=pl(s,id);check(trunk.distanceTo(p)<6&&farStart>10,"trunk hops after its owner "+farStart+" -> "+trunk.distanceTo(p));p.connection.teleport(BASE.getX()+38.5,BASE.getY(),BASE.getZ()+.5,-90,20);});
  if(ticks==240)s.execute(()->{var p=pl(s,id);check(trunk.distanceTo(p)<5,"owner 25+ blocks away: trunk teleports beside him d="+trunk.distanceTo(p));
   var menu=new TravelingTrunk.Menu(0,p.getInventory(),trunk,trunk.rows());check(trunk.open()&&menu.clickMenuButton(p,1)&&trunk.stay(),"menu opens the lid and button 1 sets stay");menu.removed(p);check(!trunk.open(),"closing lowers the lid");farStart=trunk.getX();p.connection.teleport(BASE.getX()+28.5,BASE.getY(),BASE.getZ()+.5,90,20);});
  if(ticks==320)s.execute(()->{var p=pl(s,id);check(Math.abs(trunk.getX()-farStart)<.01,"staying trunk does not follow");trunk.stay(false);
   trunk.hurt(trunk.damageSources().generic(),30);float before=trunk.getHealth();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD,2));trunk.mobInteract(p,InteractionHand.MAIN_HAND);check(trunk.getHealth()==before+5&&p.getMainHandItem().getCount()==1,"feeding bread heals 5 and consumes one");
   p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Golemancy.upgrade(1)));trunk.mobInteract(p,InteractionHand.MAIN_HAND);check(trunk.upgrade()==1&&trunk.slots()==36&&p.getMainHandItem().isEmpty(),"earth upgrade adds a row (36 slots)");
   p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Golemancy.upgrade(0)));trunk.mobInteract(p,InteractionHand.MAIN_HAND);check(trunk.upgrade()==1&&!p.getMainHandItem().isEmpty(),"second upgrade refused");p.getInventory().clearContent();
   for(int i=0;i<36;i+=4)trunk.inventory.setItem(i,new ItemStack(i%8==0?Items.COBBLESTONE:Items.OAK_LOG,16));p.connection.teleport(trunk.getX()-2.5,trunk.getY(),trunk.getZ(),-90,25);});
  if(ticks==330)s.execute(()->{var p=pl(s,id);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);trunk.mobInteract(p,InteractionHand.MAIN_HAND);});
  if(ticks==345){check(mc.screen instanceof TrunkClient.Screen sc&&sc.getMenu().slots.size()==36+36,"client trunk GUI with 4 rows");LiquidDeathAudit.shot(mc,"trunk-gui-earth.png");mc.player.closeContainer();}
  if(ticks==355)LiquidDeathAudit.shot(mc,"trunk-closeup.png");
  // water: invulnerable & owner only; entropy: pulls items; fire: defends owner
  if(ticks==360)s.execute(()->{var p=pl(s,id);var l=p.serverLevel();
   var w=spawn(p,trunkWith(3,null),BASE.offset(-30,-1,5));check(!w.hurt(w.damageSources().generic(),50)&&w.getHealth()==75,"water upgrade: invulnerable");
   var stranger=net.neoforged.neoforge.common.util.FakePlayerFactory.get(l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Stranger"));stranger.moveTo(w.getX()+1,w.getY(),w.getZ());w.mobInteract(stranger,InteractionHand.MAIN_HAND);check(!(stranger.containerMenu instanceof TravelingTrunk.Menu),"water upgrade: stranger cannot open");
   check(!((Golemancy.Bell)Golemancy.BELL.get()).onLeftClickEntity(new ItemStack(Golemancy.BELL.get()),stranger,w)&&w.isAlive(),"water upgrade: stranger cannot take it with a bell");w.stay(true);
   var e=spawn(p,trunkWith(5,null),BASE.offset(-26,-1,5));e.stay(true);for(int i=0;i<3;i++){var it=new ItemEntity(l,e.getX()+1.5-i*1.5,e.getY()+.3,e.getZ()+2,new ItemStack(Items.APPLE,2));it.setDeltaMovement(Vec3.ZERO);l.addFreshEntity(it);}guard=e;
   husk=EntityType.HUSK.create(l);husk.moveTo(trunk.getX()+3,trunk.getY(),trunk.getZ(),0,0);husk.setNoAi(true);husk.setPersistenceRequired();l.addFreshEntity(husk);huskStart=husk.getHealth();
   var f=spawn(p,trunkWith(2,null),BASE.offset(trunk.blockPosition().getX()-BASE.getX()-1,-1,2));p.setLastHurtByMob(husk);trunk=f;});
  if(ticks==480)s.execute(()->{int apples=0;for(int i=0;i<guard.slots();i++)if(guard.inventory.getItem(i).is(Items.APPLE))apples+=guard.inventory.getItem(i).getCount();check(apples==6,"entropy upgrade pulls in and swallows nearby items apples="+apples);
   check(husk.getHealth()<huskStart||!husk.isAlive(),"fire upgrade: trunk attacks whoever hurt its owner hp "+huskStart+" -> "+husk.getHealth());check(trunk.anger()>0||!husk.isAlive(),"angry while defending");});
  if(ticks==485)LiquidDeathAudit.shot(mc,"trunk-angry.png");
  // bell retrieval: order keeps contents, plain spills them
  if(ticks==500)s.execute(()->{var p=pl(s,id);var l=p.serverLevel();husk.discard();
   var o=spawn(p,trunkWith(4,null),BASE.offset(-20,-1,-5));o.inventory.setItem(0,new ItemStack(Items.EMERALD,7));o.setCustomName(net.minecraft.network.chat.Component.literal("Багаж"));var bell=new ItemStack(Golemancy.BELL.get());
   check(((Golemancy.Bell)bell.getItem()).onLeftClickEntity(bell,p,o)&&o.isRemoved(),"bell retrieves the trunk");var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(o.blockPosition()).inflate(2));
   var spawner=drops.stream().filter(e->e.getItem().is(TravelingTrunk.ITEM.get())).findFirst().orElseThrow().getItem();check(drops.stream().noneMatch(e->e.getItem().is(Items.EMERALD))&&NoteData.tag(spawner).contains("inventory")&&spawner.getHoverName().getString().equals("Багаж"),"order upgrade: item keeps name, upgrade and contents");
   drops.forEach(Entity::discard);var again=spawn(p,spawner.copy(),BASE.offset(-20,-1,-5));check(again.upgrade()==4&&again.inventory.getItem(0).getCount()==7,"re-placed trunk restores contents");
   var plain=spawn(p,new ItemStack(TravelingTrunk.ITEM.get()),BASE.offset(-16,-1,-5));plain.inventory.setItem(3,new ItemStack(Items.GOLD_INGOT,4));((Golemancy.Bell)bell.getItem()).onLeftClickEntity(bell,p,plain);
   check(l.getEntitiesOfClass(ItemEntity.class,new AABB(plain.blockPosition()).inflate(2)).stream().anyMatch(e->e.getItem().is(Items.GOLD_INGOT)&&e.getItem().getCount()==4),"plain trunk spills its contents when retrieved");
   again.stay(true);
   // dimension follow
   trunk.stay(false);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();var nether=s.getLevel(Level.NETHER);p.teleportTo(nether,8.5,100,8.5,0,0);});
  if(ticks==560)s.execute(()->{var p=pl(s,id);var nether=s.getLevel(Level.NETHER);var there=nether.getEntitiesOfClass(TravelingTrunk.class,new AABB(p.blockPosition()).inflate(8));there.forEach(Entity::discard);p.teleportTo(s.overworld(),BASE.getX()+.5,BASE.getY(),BASE.getZ()-3.5,0,20);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);check(!there.isEmpty(),"following trunk travels with its owner to the Nether");
   check(there.stream().noneMatch(TravelingTrunk::stay)&&there.size()==2,"only the two following trunks crossed; staying ones remained "+there.size());});
  // item model checks
  if(ticks==600){var model=mc.getItemRenderer().getModel(new ItemStack(TravelingTrunk.ITEM.get()),mc.level,mc.player,0);check(model!=mc.getModelManager().getMissingModel(),"trunk spawner item model");for(var c:java.util.List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)){var sc=model.getTransforms().getTransform(c).scale;check(sc.x<=1&&sc.y<=1&&sc.z<=1,"bounded item scale "+c);}
   s.execute(()->{var p=pl(s,id);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(TravelingTrunk.ITEM.get()));p.getInventory().selected=0;var drop=new ItemEntity(p.level(),p.getX(),p.getY()+.3,p.getZ()+2,new ItemStack(TravelingTrunk.ITEM.get()));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();p.level().addFreshEntity(drop);p.containerMenu.broadcastChanges();});}
  if(ticks==620)LiquidDeathAudit.shot(mc,"trunk-item-hand.png");
  if(ticks==625)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==640){LiquidDeathAudit.shot(mc,"trunk-item-inventory.png");mc.setScreen(null);check(TrunkClient.rendered>0,"trunk entities rendered");System.out.println(failed==null?"TC4_TRUNK ALL PASS":"TC4_TRUNK FAILED "+failed);mc.stop();}
 }
 static ItemStack trunkWith(int upgrade,String name){var st=new ItemStack(TravelingTrunk.ITEM.get());var t=new net.minecraft.nbt.CompoundTag();t.putByte("upgrade",(byte)upgrade);Golemancy.tag(st,t);return st;}
}
