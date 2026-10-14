package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Dedicated checks of original golem marker colours: matching, empty/fill/use targeting, bell cycling, menu buttons and saving. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class GolemColorAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_GOLEM_COLOR FAIL "+text);System.out.println("TC4_GOLEM_COLOR PASS "+text);}
 static CompoundTag mark(BlockPos pos,int color){var m=new CompoundTag();m.putLong("pos",pos.asLong());m.putByte("side",(byte)Direction.UP.get3DDataValue());m.putByte("color",(byte)color);return m;}
 static ChestBlockEntity chest(net.minecraft.server.level.ServerLevel l,BlockPos pos){l.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);var c=(ChestBlockEntity)l.getBlockEntity(pos);c.clearContent();return c;}
 static int count(ChestBlockEntity c){int n=0;for(int i=0;i<c.getContainerSize();i++)n+=c.getItem(i).getCount();return n;}
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.golemColorAudit"))return;var server=e.getServer();try{
  var l=server.overworld();var at=new BlockPos(0,180,0);var home=chest(l,at);var near=chest(l,at.east(2));var far=chest(l,at.east(5));
  var g=new StrawGolem(Golemancy.ENTITY.get(),l);g.material(GolemMaterial.THAUMIUM,true);g.home=at.above();g.homeFace=Direction.UP;g.moveTo(.5,181,.5,0,0);g.getEntityData().set(StrawGolem.CORE,(byte)1);
  check(g.colorsMatching(new ItemStack(Items.COBBLESTONE)).stream().allMatch(c->c==-1)&&g.colorAllows(new ItemStack(Items.COBBLESTONE),9),"no filters keep every slot at any colour, accepting all markers");
  g.filters.setItem(0,new ItemStack(Items.COBBLESTONE));g.colors[0]=5;g.filters.setItem(1,new ItemStack(Items.DIRT));
  check(g.colorAllows(new ItemStack(Items.COBBLESTONE),5)&&!g.colorAllows(new ItemStack(Items.COBBLESTONE),3)&&!g.colorAllows(new ItemStack(Items.COBBLESTONE),-1),"coloured slot only accepts its marker colour, not an any-colour marker (Marker.equalsFuzzy)");
  check(g.colorAllows(new ItemStack(Items.DIRT),3)&&!g.colorAllows(new ItemStack(Items.STONE),3),"any-colour slot accepts every marker, unfiltered items match no colour");
  g.markers.add(mark(at.east(2),3));g.markers.add(mark(at.east(5),5));var job=new GolemInventoryGoal(g);
  var t=job.destination(new ItemStack(Items.COBBLESTONE,4));check(t!=null&&t.pos().equals(at.east(5)),"empty core skips the nearer wrong-colour chest for the lime-filtered item");
  t=job.destination(new ItemStack(Items.DIRT,4));check(t!=null&&t.pos().equals(at.east(2)),"any-colour filtered item still goes to the nearest chest");
  g.carried(new ItemStack(Items.COBBLESTONE,4));g.moveTo(at.east(5).getX()+.5,181,.5,0,0);g.tickCount=GolemConfig.delay()*10;job.tick();check(count(far)==4&&count(near)==0&&g.carried().isEmpty(),"real empty-core delivery puts the coloured load into the matching chest");
  var filler=new StrawGolem(Golemancy.ENTITY.get(),l);filler.material(GolemMaterial.THAUMIUM,true);filler.home=at.above();filler.homeFace=Direction.UP;filler.getEntityData().set(StrawGolem.CORE,(byte)0);filler.filters.setItem(0,new ItemStack(Items.COBBLESTONE,8));filler.colors[0]=5;filler.markers.add(mark(at.east(2),3));filler.markers.add(mark(at.east(5),5));
  near.clearContent();far.clearContent();near.setItem(0,new ItemStack(Items.COBBLESTONE,20));far.setItem(0,new ItemStack(Items.COBBLESTONE,20));home.clearContent();
  var fill=new GolemInventoryGoal(filler);filler.moveTo(at.east(2).getX()+.5,181,.5,0,0);filler.tickCount=GolemConfig.delay()*10;for(int i=0;i<60&&filler.carried().isEmpty();i++){filler.tickCount++;if(filler.distanceToSqr(at.east(5).getX()+.5,181,.5)>3)filler.moveTo(at.east(5).getX()+.5,181,.5,0,0);fill.tick();}
  check(filler.carried().getCount()==8&&count(far)==12&&count(near)==20,"real fill core ignores the nearer wrong-colour source and takes from the matching chest");
  var markers=new net.minecraft.nbt.ListTag();var pos=at.east(9);
  check(Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,false,false)==-2&&markers.size()==1&&markers.getCompound(0).getByte("color")==-1&&Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,false,false)==-2&&markers.isEmpty(),"without the order upgrade the bell only toggles an any-colour marker");
  Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,false);boolean cycle=true;for(int c=0;c<16;c++)cycle&=Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,false)==c&&markers.size()==1&&markers.getCompound(0).getByte("color")==c;
  check(cycle&&Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,false)==-2&&markers.isEmpty(),"order upgrade cycles a marked face through all sixteen colours and removes it after black");
  Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,false);Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,false);check(Golemancy.Bell.changeMarkers(markers,pos,Direction.UP,true,true)==-2&&markers.isEmpty(),"sneaking with the order upgrade removes a coloured marker");
  var p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);var menu=new GolemMenu(0,p.getInventory(),filler);
  check(!menu.clickMenuButton(p,100)&&filler.colors[0]==5,"colour buttons need the order upgrade");check(filler.installUpgrade(4),"order upgrade installs");
  check(menu.clickMenuButton(p,110)&&filler.colors[0]==6&&menu.clickMenuButton(p,101)&&filler.colors[1]==15&&menu.clickMenuButton(p,111)&&filler.colors[1]==-1,"menu buttons step colours up and down with the original -1/15 wrap");
  var saved=filler.itemData();var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.material(GolemMaterial.THAUMIUM,true);copy.restoreData(saved);check(copy.colors[0]==6&&copy.colors[1]==-1&&copy.colors[2]==-1,"filter colours survive golem item save and restore");
  var plain=new StrawGolem(Golemancy.ENTITY.get(),l);plain.restoreData(new CompoundTag());check(java.util.stream.IntStream.range(0,18).allMatch(i->plain.colors[i]==-1),"old saves without colours default to any colour");
  System.out.println("TC4_GOLEM_COLOR ALL PASS original marker colours for empty, fill, bell, menu and saving");
 }finally{server.halt(false);}}
}
