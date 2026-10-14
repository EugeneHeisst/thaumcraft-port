package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemAudit {
    static final BlockPos BASE=new BlockPos(90,121,-90);static StrawGolem golem,fillGolem;static int ticks;static volatile boolean ready,menuReady,done;static ItemEntity reject,outside;static int maxCarried;static final BlockPos HUNGRY=BASE.offset(4,0,-3);
    static void check(boolean b,String what){if(!b)throw new IllegalStateException("TC4_GOLEM FAIL "+what);System.out.println("TC4_GOLEM PASS "+what);}
    static UseOnContext context(ServerPlayer p,BlockPos pos,Direction face){return new UseOnContext(p.level(),p,InteractionHand.MAIN_HAND,p.getMainHandItem(),new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));}
    static int count(ChestBlockEntity c,Item item){int n=0;for(int i=0;i<c.getContainerSize();i++)if(c.getItem(i).is(item))n+=c.getItem(i).getCount();return n;}
    static void setup(ServerPlayer p){
        var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);HoverHarness.STATES.remove(p);p.getAbilities().flying=false;p.onUpdateAbilities();
        l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());l.setDayTime(6000);
        for(var entity:l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(25)))if(!(entity instanceof net.minecraft.world.entity.player.Player))entity.discard();
        for(int x=-20;x<=20;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<=4;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        for(int x=-6;x<=6;x+=3)for(int z=-6;z<=6;z+=3)l.setBlock(BASE.offset(x,-1,z),Blocks.GLOWSTONE.defaultBlockState(),3);
        l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(BASE).inflate(25)).forEach(Entity::discard);
        l.setBlock(BASE,Blocks.CHEST.defaultBlockState(),3);p.connection.teleport(BASE.getX()+5,BASE.getY(),BASE.getZ()-5,40,12);
        for(var key:java.util.List.of("GolemStraw","CoreGather")){var r=CrucibleRecipes.byKey().get(key);check(r!=null&&r.aspects.visSize()==(key.equals("GolemStraw")?12:10),"original crucible recipe "+key);}
        for(var key:java.util.List.of("GolemBell","CoreBlank")){var r=ArcaneRecipes.entries().stream().filter(e->e.key().equals(key)).findFirst().orElseThrow();var grid=new java.util.ArrayList<ItemStack>();for(var ing:r.ingredients())grid.add(ing.isEmpty()?ItemStack.EMPTY:ing.getItems()[0].copy());check(r.matches(grid,java.util.Set.of(r.research()))&&!r.matches(grid,java.util.Set.of()),"arcane recipe and research gate "+key);}
        p.getInventory().setItem(0,new ItemStack(Golemancy.STRAW.get()));p.getInventory().selected=0;
        check(p.getMainHandItem().onItemUseFirst(context(p,BASE,Direction.EAST)).consumesAction()&&p.getMainHandItem().isEmpty(),"survival placer spawns and consumes exactly one");
        golem=l.getEntitiesOfClass(StrawGolem.class,new AABB(BASE).inflate(3)).getFirst();golem.setCustomName(Component.literal("Gather audit"));
        check(golem.core()==-1&&golem.home.equals(BASE.east())&&golem.homeFace==Direction.EAST&&golem.getMaxHealth()==10,"original idle straw stats and side home");
        p.getInventory().setItem(0,new ItemStack(Golemancy.GATHER.get(),2));check(golem.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&golem.core()==2&&p.getMainHandItem().getCount()==1,"install gather core consumes one");
        p.getInventory().setItem(0,ItemStack.EMPTY);golem.mobInteract(p,InteractionHand.MAIN_HAND);check(p.containerMenu instanceof GolemMenu&&golem.paused,"actual interaction opens and pauses filter menu");ready=true;
    }
    static void start(ServerPlayer p){
        check(golem.filters.getItem(0).is(Items.COBBLESTONE)&&p.getInventory().getItem(1).getCount()==6,"ghost filter copies one without consuming real source stack");p.closeContainer();check(!golem.paused,"closing filter menu resumes work");
        var l=p.serverLevel();p.getInventory().setItem(1,ItemStack.EMPTY);
        l.setBlock(HUNGRY,HungryChest.BLOCK.get().defaultBlockState(),3);var food=new ItemEntity(l,HUNGRY.getX()+.5,HUNGRY.getY()+1.5,HUNGRY.getZ()+.5,new ItemStack(Items.EMERALD,9));food.setDeltaMovement(Vec3.ZERO);l.addFreshEntity(food);
        var accept=new ItemEntity(l,BASE.getX()+4.78,BASE.getY(),BASE.getZ()+.95,new ItemStack(Items.COBBLESTONE,4));accept.setDeltaMovement(Vec3.ZERO);accept.setNoPickUpDelay();l.addFreshEntity(accept);
        reject=new ItemEntity(l,BASE.getX()+3.5,BASE.getY(),BASE.getZ()+1.5,new ItemStack(Items.DIAMOND,2));reject.setNoPickUpDelay();l.addFreshEntity(reject);
        outside=new ItemEntity(l,BASE.getX()+19.5,BASE.getY(),BASE.getZ()+.5,new ItemStack(Items.COBBLESTONE,2));outside.setNoPickUpDelay();l.addFreshEntity(outside);menuReady=true;
    }
    static void finish(ServerPlayer p){
        for(var item:p.level().getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(25)))System.out.println("TC4_GOLEM item "+item.getItem()+" at "+item.position()+" delay "+item.hasPickUpDelay());
        var l=p.serverLevel();System.out.println("TC4_GOLEM final chest="+count((ChestBlockEntity)l.getBlockEntity(BASE),Items.COBBLESTONE)+" pos="+golem.position()+" carried="+golem.carried()+" paused="+golem.paused+" home="+golem.home+" cap="+l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,BASE,Direction.EAST));check(count((ChestBlockEntity)l.getBlockEntity(BASE),Items.COBBLESTONE)==4,"natural navigation gathers four items into chest on original attachment face");check(maxCarried==1,"straw carry limit exactly one");check(reject.isAlive()&&reject.getItem().getCount()==2&&outside.isAlive()&&outside.getItem().getCount()==2,"filter rejects diamonds and range excludes distant cobblestone");
        var saved=new net.minecraft.nbt.CompoundTag();golem.carried(new ItemStack(Items.APPLE));golem.addAdditionalSaveData(saved);var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.readAdditionalSaveData(saved);check(copy.core()==2&&copy.home.equals(golem.home)&&copy.homeFace==Direction.EAST&&copy.filters.getItem(0).is(Items.COBBLESTONE)&&copy.carried().is(Items.APPLE),"entity save/load preserves home/core/filter/carried stack");
        var marker=new net.minecraft.nbt.CompoundTag();marker.putLong("pos",BASE.asLong());marker.putByte("side",(byte)1);golem.markers.add(marker);
        p.getInventory().setItem(0,new ItemStack(Golemancy.BELL.get()));p.getInventory().selected=0;var bell=(Golemancy.Bell)Golemancy.BELL.get();check(bell.interactLivingEntity(p.getMainHandItem(),p,golem,InteractionHand.MAIN_HAND).consumesAction(),"bell binds selected golem");Golemancy.Bell.reset(p);check(golem.markers.isEmpty(),"original F action clears markers");
        check(bell.onLeftClickEntity(p.getMainHandItem(),p,golem)&&!golem.isAlive(),"left-click bell retrieves golem");
        var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(golem.blockPosition()).inflate(2));var placer=drops.stream().filter(e->e.getItem().is(Golemancy.STRAW.get())).findFirst().orElseThrow().getItem().copy();check(drops.stream().filter(e->e.getItem().is(Items.APPLE)).mapToInt(e->e.getItem().getCount()).sum()==1,"retrieval drops carried item exactly once");
        p.getInventory().setItem(0,placer);placer.onItemUseFirst(context(p,BASE,Direction.EAST));golem=l.getEntitiesOfClass(StrawGolem.class,new AABB(BASE).inflate(4)).getFirst();check(golem.core()==2&&golem.filters.getItem(0).is(Items.COBBLESTONE)&&golem.getCustomName().getString().equals("Gather audit"),"bell placer roundtrip preserves core/filter/name");
        p.getInventory().setItem(0,new ItemStack(Golemancy.BELL.get()));p.setShiftKeyDown(true);bell.onLeftClickEntity(p.getMainHandItem(),p,golem);p.setShiftKeyDown(false);check(!golem.isAlive()&&l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(4)).stream().anyMatch(e->e.getItem().is(Golemancy.GATHER.get())),"sneak bell separates gather core");expansion(p);done=true;
    }
    static void expansion(ServerPlayer p){
        var l=p.serverLevel();var a=BASE.offset(0,0,4);var b=a.east(3);l.setBlock(a,Blocks.CHEST.defaultBlockState(),3);l.setBlock(b,Blocks.CHEST.defaultBlockState(),3);
        var hungry=(HungryChest.Tile)l.getBlockEntity(HUNGRY);check(count(hungry,Items.EMERALD)==9,"hungry chest naturally eats falling stack");check(hungry.getContainerSize()==27,"hungry chest original 27 slots");var hungerRecipe=ArcaneRecipes.entries().stream().filter(e->e.key().equals("HungryChest")).findFirst().orElseThrow();var hungerGrid=hungerRecipe.ingredients().stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList();check(hungerRecipe.matches(hungerGrid,java.util.Set.of("HUNGRYCHEST"))&&!hungerRecipe.matches(hungerGrid,java.util.Set.of()),"hungry chest original arcane recipe gated");
        for(int slot=0;slot<27;slot++)hungry.setItem(slot,new ItemStack(Items.EMERALD,64));hungry.setItem(0,new ItemStack(Items.EMERALD,62));var leftovers=new ItemEntity(l,HUNGRY.getX()+.5,HUNGRY.getY()+.5,HUNGRY.getZ()+.5,new ItemStack(Items.EMERALD,5));((HungryChest)HungryChest.BLOCK.get()).entityInside(hungry.getBlockState(),l,HUNGRY,leftovers);check(leftovers.isAlive()&&leftovers.getItem().getCount()==3&&count(hungry,Items.EMERALD)==1728,"hungry chest partial insertion preserves leftovers");((HungryChest)HungryChest.BLOCK.get()).entityInside(hungry.getBlockState(),l,HUNGRY,leftovers);check(leftovers.getItem().getCount()==3,"full hungry chest preserves dropped items");var inv=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,HUNGRY,Direction.UP);check(inv!=null&&inv.getSlots()==27,"hungry chest automation capability");var saved=hungry.saveWithFullMetadata(l.registryAccess());var restored=new HungryChest.Tile(HUNGRY,hungry.getBlockState());restored.loadWithComponents(saved,l.registryAccess());check(count(restored,Items.EMERALD)==1728,"hungry chest contents survive save/load");
        var home=(ChestBlockEntity)l.getBlockEntity(a);var source=(ChestBlockEntity)l.getBlockEntity(b);
        for(var m:GolemMaterial.values()){
            var g=new StrawGolem(Golemancy.ENTITY.get(),l);g.material(m,true);check(g.getMaxHealth()==m.health&&g.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==m.armor&&g.carryLimit()==m.carry&&g.fireImmune()==m.fireResist,"material stats "+m);
            g.getEntityData().set(StrawGolem.CORE,(byte)0);g.filters.setItem(0,new ItemStack(Items.COBBLESTONE,12));g.fillAny=true;var tag=new net.minecraft.nbt.CompoundTag();g.addAdditionalSaveData(tag);var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.readAdditionalSaveData(tag);check(copy.material()==m&&copy.filters.getItem(0).getCount()==12&&copy.fillAny&&copy.asItem().is(Golemancy.placer(m).get()),"material/filter/amount save roundtrip "+m);
        }
        var g=new StrawGolem(Golemancy.ENTITY.get(),l);g.material(GolemMaterial.IRON,true);g.home=a.above();g.homeFace=Direction.UP;g.getEntityData().set(StrawGolem.CORE,(byte)0);g.filters.setItem(0,new ItemStack(Items.COBBLESTONE,12));
        var marker=new net.minecraft.nbt.CompoundTag();marker.putLong("pos",b.asLong());marker.putByte("side",(byte)Direction.UP.get3DDataValue());g.markers.add(marker);source.setItem(0,new ItemStack(Items.COBBLESTONE,40));home.setItem(0,new ItemStack(Items.COBBLESTONE,5));
        var job=new GolemInventoryGoal(g);g.moveTo(b.getX()+.5,b.getY()+1,b.getZ()+.5,0,0);job.tick();check(g.carried().getCount()==7&&source.getItem(0).getCount()==33,"fill extracts exact missing amount");g.moveTo(a.getX()+.5,a.getY()+1,a.getZ()+.5,0,0);job.tick();check(count(home,Items.COBBLESTONE)==12&&g.carried().isEmpty(),"fill inserts into home");
        g.fillAny=true;g.moveTo(b.getX()+.5,b.getY()+1,b.getZ()+.5,0,0);job.tick();check(g.carried().getCount()==32,"any amount fills material carry capacity");g.carried(ItemStack.EMPTY);g.getEntityData().set(StrawGolem.CORE,(byte)1);g.moveTo(a.getX()+.5,a.getY()+1,a.getZ()+.5,0,0);job.tick();check(g.carried().getCount()==12&&count(home,Items.COBBLESTONE)==0,"empty extracts matching home stack");g.moveTo(b.getX()+.5,b.getY()+1,b.getZ()+.5,0,0);job.tick();check(count(source,Items.COBBLESTONE)==13&&g.carried().isEmpty(),"empty delivers to marked container");
        var fuzzy=new StrawGolem(Golemancy.ENTITY.get(),l);fuzzy.material(GolemMaterial.TALLOW,true);fuzzy.home=a.above();fuzzy.homeFace=Direction.UP;fuzzy.getEntityData().set(StrawGolem.CORE,(byte)0);check(fuzzy.installUpgrade(3)&&fuzzy.installUpgrade(5),"fire plus entropy fit two material slots");fuzzy.filters.setItem(11,new ItemStack(Items.OAK_PLANKS,7));fuzzy.markers.add(marker.copy());home.clearContent();source.clearContent();home.setItem(0,new ItemStack(Items.BIRCH_PLANKS,2));source.setItem(0,new ItemStack(Items.BIRCH_PLANKS,20));var fuzzyJob=new GolemInventoryGoal(fuzzy);fuzzy.moveTo(b.getX()+.5,b.getY()+1,b.getZ()+.5,0,0);fuzzyJob.tick();check(fuzzy.carried().is(Items.BIRCH_PLANKS)&&fuzzy.carried().getCount()==5&&source.getItem(0).getCount()==15,"entropy fill uses second page and counts equivalent home stock");fuzzy.moveTo(a.getX()+.5,a.getY()+1,a.getZ()+.5,0,0);fuzzyJob.tick();check(count(home,Items.BIRCH_PLANKS)==7&&fuzzy.carried().isEmpty(),"entropy fill delivers real equivalent items without duplication");
        l.setBlock(a,GolemFetter.BLOCK.get().defaultBlockState(),3);g.moveTo(a.getX()+.5,a.getY()+1,a.getZ()+.5,0,0);check(g.working(),"unpowered fetter allows work");l.setBlock(a.north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);check(!g.working(),"real redstone powers fetter and stops work");l.removeBlock(a.north(),false);check(g.working(),"removing redstone resumes work");
        fillGolem=g;g.getEntityData().set(StrawGolem.CORE,(byte)0);g.fillAny=false;g.filters.setItem(0,new ItemStack(Items.COBBLESTONE,12));l.addFreshEntity(g);
        for(var m:GolemMaterial.values()){var display=new StrawGolem(Golemancy.ENTITY.get(),l);display.material(m,true);display.moveTo(BASE.getX()-4+m.ordinal()*1.5,BASE.getY(),BASE.getZ()+3,180,0);l.addFreshEntity(display);}
    }
    @SubscribeEvent static void observe(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){if(Boolean.getBoolean("tc4modern.golemAuditOnly")&&e.getEntity() instanceof StrawGolem g&&!g.level().isClientSide)maxCarried=Math.max(maxCarried,g.carried().getCount());}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("tc4modern.golemAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;var srv=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();
        if(ticks==40)srv.execute(()->setup(srv.getPlayerList().getPlayer(id)));
        if(ticks==80){check(ready&&mc.screen instanceof GolemClient.Screen,"client receives original golem filter GUI");srv.execute(()->{var p=srv.getPlayerList().getPlayer(id);p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,6));p.containerMenu.broadcastChanges();});}
        if(ticks==100){var menu=mc.player.containerMenu;mc.gameMode.handleInventoryMouseClick(menu.containerId,34,0,ClickType.PICKUP,mc.player);mc.gameMode.handleInventoryMouseClick(menu.containerId,0,0,ClickType.PICKUP,mc.player);mc.gameMode.handleInventoryMouseClick(menu.containerId,34,0,ClickType.PICKUP,mc.player);}
        if(ticks==115)LiquidDeathAudit.shot(mc,"golem-filter.png");
        if(ticks==125)srv.execute(()->start(srv.getPlayerList().getPlayer(id)));
        if(ticks==140){mc.setScreen(null);mc.player.closeContainer();}
        if(ticks>150&&ticks%100==0)srv.execute(()->System.out.println("TC4_GOLEM progress chest="+count((ChestBlockEntity)golem.level().getBlockEntity(BASE),Items.COBBLESTONE)+" pos="+golem.position()+" carried="+golem.carried()+" paused="+golem.paused+" nav="+golem.getNavigation().isDone()));
        if(ticks==220){check(menuReady&&GolemClient.rendered>0,"original golem model renders in world");LiquidDeathAudit.shot(mc,"golem-gather.png");}
        if(ticks==600)srv.execute(()->finish(srv.getPlayerList().getPlayer(id)));
        if(ticks==610)LiquidDeathAudit.shot(mc,"golem-materials.png");
        if(ticks==612)srv.execute(()->{var p=srv.getPlayerList().getPlayer(id);p.connection.teleport(HUNGRY.getX()+2,HUNGRY.getY()+1,HUNGRY.getZ()-2,45,20);p.openMenu((HungryChest.Tile)p.level().getBlockEntity(HUNGRY));});
        if(ticks==650){check(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen,"hungry chest actual client GUI (screen="+mc.screen+", menu="+mc.player.containerMenu+")");LiquidDeathAudit.shot(mc,"hungry-chest-gui.png");mc.player.closeContainer();}
        if(ticks==670)LiquidDeathAudit.shot(mc,"hungry-chest-world.png");
        if(ticks==680)srv.execute(()->{var p=srv.getPlayerList().getPlayer(id);p.connection.teleport(fillGolem.getX()+1,fillGolem.getY(),fillGolem.getZ()+1,0,0);p.openMenu(fillGolem,b->b.writeVarInt(fillGolem.getId()));});
        if(ticks==720){check(mc.screen instanceof GolemClient.Screen&&mc.player.containerMenu.getSlot(0).getItem().getCount()==12,"fill GUI synchronizes requested filter count");mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);}
        if(ticks==740){check(((GolemMenu)mc.player.containerMenu).golem.fillAny,"real fill GUI button synchronizes any amount");LiquidDeathAudit.shot(mc,"golem-fill-gui.png");mc.player.closeContainer();}
        if(ticks==760){check(done,"all server golem checks finished");for(var key:java.util.List.of("GOLEMSTRAW","COREGATHER","GOLEMBELL"))check(BasicsBookRecipes.researchIcon(key)!=null,"actual book research icon "+key);System.out.println("TC4_GOLEM ALL PASS");mc.stop();}
    }
}
