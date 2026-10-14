package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden audit: void wand caps and nuggets, void thaumaturge robes, sanity checker recipe (ELDRITCH tab). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class EldritchGearAudit {
 static final BlockPos BASE=new BlockPos(720,121,-90);static int ticks;static volatile String failed;
 static void check(boolean b,String s){if(!b){failed=s;System.out.println("TC4_ELDGEAR FAIL "+s);}else System.out.println("TC4_ELDGEAR PASS "+s);}
 static Item item(String n){return BuiltInRegistries.ITEM.get(AlchemyContent.id(n));}
 static ItemStack stack(String n){return new ItemStack(item(n));}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();if(p.level()!=p.server.overworld())p.teleportTo(p.server.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);
  p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(16)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
  // void caps and nuggets
  var cap=WandParts.CAPS.get("void");check(cap!=null&&Math.abs(cap.baseCostModifier()-.8f)<1e-6&&cap.craftCost()==9&&cap.research().equals("CAP_void"),"void cap: cost x0.8, craft cost 9, research CAP_void");
  var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(i<3||i==3||i==5?stack("void_nugget"):ItemStack.EMPTY);var inert=ArcaneRecipes.find(grid,Set.of("CAP_void"));check(inert!=null&&inert.output().is(item("void_cap_inert"))&&inert.cost().visSize()==90,"arcane inert void cap from 5 void nuggets (90 vis)");check(ArcaneRecipes.find(grid,Set.of())==null,"needs CAP_void research");
  var capRecipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals("WandCapVoid")).findFirst().orElseThrow();check(capRecipe.instability()==8&&capRecipe.aspects().visSize()==72&&InfusionItemRecipes.match(capRecipe.componentStacks(),new ItemStack(capRecipe.central().get()),Set.of("CAP_void"))==capRecipe,"infusion charged void cap (instability 8, 4x18)");
  var wand=WandParts.make(cap,WandParts.RODDEFS.get("greatwood"));check(WandParts.cap(wand)==cap,"wand made with void caps");
  var rm=l.getRecipeManager();var in1=net.minecraft.world.item.crafting.CraftingInput.of(1,1,List.of(stack("void_ingot")));check(rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,in1,l).map(r->r.value().assemble(in1,l.registryAccess())).filter(s->s.is(item("void_nugget"))&&s.getCount()==9).isPresent(),"void ingot -> 9 nuggets");
  var nine=new ArrayList<ItemStack>();for(int i=0;i<9;i++)nine.add(stack("void_nugget"));var in9=net.minecraft.world.item.crafting.CraftingInput.of(3,3,nine);check(rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,in9,l).map(r->r.value().assemble(in9,l.registryAccess())).filter(s->s.is(item("void_ingot"))).isPresent(),"9 nuggets -> void ingot");
  // void robes
  for(var key:List.of("VoidRobeHelm","VoidRobeChest","VoidRobeLegs","SanityCheck")){var r=InfusionItemRecipes.all().stream().filter(x->x.key().equals(key)).findFirst().orElse(null);check(r!=null&&InfusionItemRecipes.match(r.componentStacks(),new ItemStack(r.central().get()),Set.of(r.research()))==r,"original infusion "+key);}
  var helm=new ItemStack(VoidRobe.HELM.get());var chest=new ItemStack(VoidRobe.CHEST.get());var legs=new ItemStack(VoidRobe.LEGS.get());p.setItemSlot(EquipmentSlot.HEAD,helm);p.setItemSlot(EquipmentSlot.CHEST,chest);p.setItemSlot(EquipmentSlot.LEGS,legs);
  check(((VoidRobe.Piece)helm.getItem()).showNodes(helm,p)&&!((VoidRobe.Piece)chest.getItem()).showNodes(chest,p),"hood reveals nodes like goggles, robe does not");
  check(Math.abs(thaumcraft.common.lib.vis.VisGearDiscount.armor(p,null)-.15f)<1e-4,"three pieces give 15% vis discount");
  check(WarpGear.total(p,List.of())==6,"three pieces carry 6 warp");
  float magic=FortressArmor.absorb(p,l.damageSources().magic(),10),phys=FortressArmor.absorb(p,l.damageSources().mobAttack(new net.minecraft.world.entity.monster.Zombie(l)),10),bypass=FortressArmor.absorb(p,l.damageSources().starve(),10);
  check(Math.abs(magic-10*(1-16/35f))<.05,"magic damage reduced by defence/35: "+magic);check(Math.abs(phys-10*(1-16/25f))<.05,"physical damage reduced by defence/25: "+phys);check(Math.abs(bypass-10)<1e-4,"armour-bypassing damage untouched: "+bypass);
  chest.setDamageValue(20);legs.setDamageValue(20);
  // dye and wash
  var dyeIn=net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(new ItemStack(VoidRobe.CHEST.get()),new ItemStack(Items.RED_DYE)));var dyed=rm.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,dyeIn,l).map(r->r.value().assemble(dyeIn,l.registryAccess())).orElse(ItemStack.EMPTY);
  check(dyed.is(VoidRobe.CHEST.get())&&thaumcraft.common.items.armor.ItemRobeArmor.color(dyed)!=VoidRobe.DEFAULT_COLOR,"robe dye recipe recolours the void robe "+Integer.toHexString(thaumcraft.common.items.armor.ItemRobeArmor.color(dyed)));
  var stand=EntityType.ARMOR_STAND.create(l);stand.moveTo(BASE.getX()+1.5,BASE.getY(),BASE.getZ()+.5,180,0);stand.setItemSlot(EquipmentSlot.HEAD,new ItemStack(VoidRobe.HELM.get()));stand.setItemSlot(EquipmentSlot.CHEST,dyed);stand.setItemSlot(EquipmentSlot.LEGS,new ItemStack(VoidRobe.LEGS.get()));l.addFreshEntity(stand);
  var stand2=EntityType.ARMOR_STAND.create(l);stand2.moveTo(BASE.getX()-.5,BASE.getY(),BASE.getZ()+.5,180,0);stand2.setItemSlot(EquipmentSlot.HEAD,new ItemStack(VoidRobe.HELM.get()));stand2.setItemSlot(EquipmentSlot.CHEST,new ItemStack(VoidRobe.CHEST.get()));stand2.setItemSlot(EquipmentSlot.LEGS,new ItemStack(VoidRobe.LEGS.get()));l.addFreshEntity(stand2);
  String[] hot={"void_cap","void_cap_inert","void_nugget","void_robe_helm","void_robe_chest","void_robe_legs","sanity_checker","primal_crusher"};for(int i=0;i<hot.length;i++)p.getInventory().setItem(i,stack(hot[i]));p.getInventory().setItem(8,dyed.copy());
  for(int i=0;i<3;i++){var drop=new ItemEntity(l,BASE.getX()-1+i*.9+.5,BASE.getY()+.4,BASE.getZ()-1.5,stack(hot[i*2]));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);}
  p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-3,0,10);p.containerMenu.broadcastChanges();
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.eldritchGearAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==100)s.execute(()->{try{setup(s.getPlayerList().getPlayer(id));}catch(RuntimeException ex){failed=String.valueOf(ex);ex.printStackTrace();}});
  if(ticks==150)s.execute(()->{var p=s.getPlayerList().getPlayer(id);check(Math.abs(p.getArmorValue()-16)<1e-4,"defence 3+7+6 = 16 armour "+p.getArmorValue());check(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()<20&&p.getItemBySlot(EquipmentSlot.LEGS).getDamageValue()<20,"worn void robes repair themselves");});
  if(ticks==160){for(var n:new String[]{"void_cap","void_cap_inert","void_nugget","void_robe_helm","void_robe_chest","void_robe_legs","sanity_checker","primal_crusher"}){var m=mc.getItemRenderer().getModel(stack(n),mc.level,mc.player,0);check(m!=mc.getModelManager().getMissingModel(),"item model "+n);for(var c:List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)){var sc=m.getTransforms().getTransform(c).scale;check(sc.x<=1&&sc.y<=1&&sc.z<=1,"bounded "+c+" "+n);}}LiquidDeathAudit.shot(mc,"eldritch-gear-stands.png");}
  if(ticks==170)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
   var r=InfusionItemRecipes.all().stream().filter(x->x.key().equals("PrimalCrusher")).findFirst().orElseThrow();check(r.instability()==6&&InfusionItemRecipes.match(r.componentStacks(),new ItemStack(r.central().get()),Set.of("PRIMALCRUSHER"))==r,"original infusion PrimalCrusher");
   var wall=BASE.offset(0,0,4);for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)l.setBlock(wall.offset(x,y,0),Blocks.STONE.defaultBlockState(),3);
   var crusher=new ItemStack(PrimalCrusher.ITEM.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,crusher);p.connection.teleport(wall.getX()+.5,wall.getY(),wall.getZ()-1.6,0,0);p.moveTo(wall.getX()+.5,wall.getY(),wall.getZ()-1.6,0,-20);
   var center=wall.above();p.gameMode.destroyBlock(center);int cleared=0;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)if(l.isEmptyBlock(center.offset(x,y,0)))cleared++;
   check(cleared==9&&!l.isEmptyBlock(wall.offset(2,0,0)),"crusher breaks a 3x3 on the struck face cleared="+cleared);check(crusher.getDamageValue()>=8&&crusher.getDamageValue()<=10,"one use per extra block damage="+crusher.getDamageValue());
   p.setShiftKeyDown(true);var single=wall.offset(-2,1,0);p.gameMode.destroyBlock(single);p.setShiftKeyDown(false);check(l.isEmptyBlock(single)&&!l.isEmptyBlock(single.above())&&!l.isEmptyBlock(single.below()),"sneaking mines a single block");
   check(((PrimalCrusher.Crusher)crusher.getItem()).getWarp(crusher,p)==2&&crusher.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())&&crusher.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState())&&!crusher.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()),"harvests ores and dirt, not wood; 2 warp");});
  if(ticks==175)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==190){LiquidDeathAudit.shot(mc,"eldritch-gear-inventory.png");mc.setScreen(null);System.out.println(failed==null?"TC4_ELDGEAR ALL PASS":"TC4_ELDGEAR FAILED "+failed);mc.stop();}
 }
}
