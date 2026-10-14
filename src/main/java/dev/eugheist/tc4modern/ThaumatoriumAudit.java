package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class ThaumatoriumAudit {
    static final BlockPos BASE=new BlockPos(-190,121,-190);
    static int ticks;static volatile boolean prepared,finished;
    static void check(boolean ok,String what){if(!ok)throw new IllegalStateException("TC4_THAUMATORIUM FAIL "+what);System.out.println("TC4_THAUMATORIUM PASS "+what);}
    static ThaumatoriumEntity tile(ServerLevel l){return (ThaumatoriumEntity)l.getBlockEntity(BASE);}
    static int output(ServerLevel l){var c=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(BASE.south());return java.util.stream.IntStream.range(0,c.getContainerSize()).map(i->c.getItem(i).is(TC4Modern.NITOR.get())?c.getItem(i).getCount():0).sum();}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("tc4modern.thaumatoriumAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;var srv=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
        if(ticks==40)srv.execute(()->{
            var p=srv.getPlayerList().getPlayer(id);var l=p.serverLevel();l.setDayTime(6000);
            for(var i:l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(10)))i.discard();
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++)l.setBlock(BASE.offset(x,y,z),(y==-3?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().setItem(0,FociClientAudit.wand(ItemStack.EMPTY));p.getInventory().selected=0;
            l.setBlock(BASE.below(),TC4Modern.CRUCIBLE.get().defaultBlockState(),3);l.setBlock(BASE,AlchemyMachines.CONSTRUCT.get().defaultBlockState(),3);l.setBlock(BASE.above(),AlchemyMachines.CONSTRUCT.get().defaultBlockState(),3);
            var c=new UseOnContext(l,p,InteractionHand.MAIN_HAND,p.getMainHandItem(),new BlockHitResult(Vec3.atCenterOf(BASE),Direction.SOUTH,BASE,false));
            p.getPersistentData().getCompound(ResearchKnowledge.KEY).getList("Research",8).removeIf(n->n.getAsString().equals("THAUMATORIUM"));
            check(!Thaumatorium.create(c,p.getMainHandItem()),"construction gated by research");
            DevCommands.grant(p.getPersistentData(),"THAUMATORIUM",new java.util.HashSet<>());DevCommands.grant(p.getPersistentData(),"NITOR",new java.util.HashSet<>());
            var before=new java.util.HashMap<Aspect,Integer>();for(var a:Aspect.getPrimalAspects())before.put(a,WandData.get(p.getMainHandItem(),a));
            check(Thaumatorium.create(c,p.getMainHandItem()),"wand forms lower/top from two constructs above crucible");
            check(l.getBlockState(BASE).is(Thaumatorium.BODY.get())&&l.getBlockState(BASE.above()).getValue(Thaumatorium.TOP),"two native multiblock halves");
            check(WandData.get(p.getMainHandItem(),Aspect.FIRE)<before.get(Aspect.FIRE)&&WandData.get(p.getMainHandItem(),Aspect.ORDER)<before.get(Aspect.ORDER)&&WandData.get(p.getMainHandItem(),Aspect.WATER)<before.get(Aspect.WATER),"construction consumes original Ignis/Ordo/Aqua vis");
            l.setBlock(BASE.south(),Blocks.CHEST.defaultBlockState(),3);
            var required=CrucibleRecipes.byKey().get("Nitor").aspects;int sourceIndex=0;
            for(var a:required.getAspects()){
                var at=BASE.above().relative(new Direction[]{Direction.NORTH,Direction.WEST,Direction.EAST}[sourceIndex++]);
                l.setBlock(at,EssentiaTubes.BUFFER.get().defaultBlockState(),3);
                var b=(EssentiaBufferEntity)l.getBlockEntity(at);b.state.aspects.add(a,required.getAmount(a));b.sync();
                check(b.state.aspects.visSize()<=8,"source buffer respects capacity8");
            }
            tile(l).setItem(0,new ItemStack(Items.GLOWSTONE_DUST,2));
            check(tile(l).addEssentia(Aspect.FIRE,1,Direction.WEST)==0,"safe refusal before recipe selected");
            check(tile(l).toggle("Nitor",p),"select known recipe");
            check(tile(l).state.maxRecipes==1&&!tile(l).toggle("Alumentum",p),"base recipe capacity1 and catalyst/research validation");
            tile(l).toggle("Nitor",p);
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()+3.5,180,0);
            prepared=true;
        });
        if(ticks==70)srv.execute(()->Thaumatorium.open(srv.getPlayerList().getPlayer(id),tile(srv.overworld())));
        if(ticks==90){check(prepared&&mc.player.containerMenu instanceof ThaumatoriumMenu,"real menu opens");var m=(ThaumatoriumMenu)mc.player.containerMenu;var list=m.recipes();check(list.contains("Nitor"),"client recipe list contains studied Nitor");((ThaumatoriumClient.Screen)mc.screen).index=list.indexOf("Nitor");mc.gameMode.handleInventoryButtonClick(m.containerId,list.indexOf("Nitor"));}
        if(ticks==120)srv.execute(()->{var l=srv.overworld();check(tile(l).state.selected.equals(java.util.List.of("Nitor")),"actual client selection packet updates authoritative tile");check(output(l)==0&&tile(l).state.essentia.visSize()==0,"unheated machine does not draw or craft");l.setBlock(BASE.below(2),TC4Modern.NITOR_BLOCK.get().defaultBlockState(),3);l.setBlock(BASE.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);});
        if(ticks==150)srv.execute(()->{var l=srv.overworld();check(output(l)==0,"redstone stops heated machine");l.removeBlock(BASE.west(),false);});
        if(ticks==280)srv.execute(()->{
            var l=srv.overworld();check(output(l)==1&&tile(l).getItem(0).getCount()==1,"real buffer->top suction crafts Nitor into chest, consumes one catalyst");
            var t=tile(l);check(t.state.essentia.visSize()==0,"craft resets essentia reservoir");
            l.setBlock(BASE.east(),Thaumatorium.BRAIN.get().defaultBlockState().setValue(Thaumatorium.FACING,Direction.WEST),3);
        });
        if(ticks==330)srv.execute(()->{
            var l=srv.overworld();var t=tile(l);check(t.state.maxRecipes==3,"one correctly oriented mnemonic matrix adds two slots");
            var n=t.saveWithoutMetadata(l.registryAccess());var reload=new ThaumatoriumEntity(BASE,t.getBlockState());reload.setLevel(l);reload.loadWithComponents(n,l.registryAccess());
            check(reload.getItem(0).getCount()==1&&reload.state.selected.equals(t.state.selected)&&reload.state.maxRecipes==3&&reload.state.players.equals(t.state.players),"inventory/selected recipes/player/upgrade save roundtrip");
            l.setBlock(BASE.east(),Thaumatorium.BRAIN.get().defaultBlockState().setValue(Thaumatorium.FACING,Direction.EAST),3);
        });
        if(ticks==340)LiquidDeathAudit.shot(mc,"thaumatorium-menu.png");
        if(ticks==350)mc.player.closeContainer();
        if(ticks==360)srv.execute(()->{var l=srv.overworld();check(tile(l).state.maxRecipes==1,"reversing matrix removes bonus slots");var p=srv.getPlayerList().getPlayer(id);p.connection.teleport(BASE.getX()+3,BASE.getY()+.5,BASE.getZ()+4,145,10);});
        if(ticks==380)LiquidDeathAudit.shot(mc,"thaumatorium-model.png");
        if(ticks==390)srv.execute(()->{
            var l=srv.overworld();l.destroyBlock(BASE.above(),true);
            check(l.getBlockState(BASE).is(AlchemyMachines.CONSTRUCT.get()),"breaking top restores remaining construct");
            check(l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(3)).stream().anyMatch(i->i.getItem().is(Items.GLOWSTONE_DUST)),"multiblock teardown drops input inventory");finished=true;System.out.println("TC4_THAUMATORIUM ALL PASS");
        });
        if(ticks==410&&finished)mc.stop();if(ticks>600)throw new IllegalStateException("Thaumatorium audit timeout");
    }
}
