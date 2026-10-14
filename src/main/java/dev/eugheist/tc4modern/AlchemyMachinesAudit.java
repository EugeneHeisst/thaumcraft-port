package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class AlchemyMachinesAudit {
    static final BlockPos BASE=new BlockPos(-160,120,-160);
    static int ticks;static volatile boolean done;
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("TC4_ALCHEMY_MACHINES FAIL "+message);System.out.println("TC4_ALCHEMY_MACHINES PASS "+message);}
    static BlockPos at(int x,int y,int z){return BASE.offset(x,y,z);}
    static AlchemyMachineEntity machine(ServerLevel l,int x){return (AlchemyMachineEntity)l.getBlockEntity(at(x,1,0));}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        if(!Boolean.getBoolean("tc4modern.alchemyMachinesAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var srv=mc.getSingleplayerServer();var uuid=mc.player.getUUID();ticks++;
        if(ticks==40)srv.execute(()->{
            var p=srv.getPlayerList().getPlayer(uuid);var l=p.serverLevel();l.setDayTime(6000);
            for(var i:l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(20)))i.discard();
            for(int x=-2;x<16;x++)for(int z=-3;z<5;z++)for(int y=-1;y<4;y++)l.setBlock(at(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            for(var name:java.util.List.of("Centrifuge","EssentiaCrystalizer","JarVoid","AlchemicalConstruct")){
                var r=ArcaneRecipes.entries().stream().filter(a->a.key().equals(name)).findFirst().orElseThrow();
                var grid=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));
                for(int y=0;y<r.height();y++)for(int x=0;x<r.width();x++){var ing=r.ingredients().get(y*r.width()+x);if(!ing.isEmpty())grid.set(y*3+x,ing.getItems()[0]);}
                check(r.matches(grid,java.util.Set.of(r.research()))&&!r.matches(grid,java.util.Set.of()),"recipe and research gate "+name);
            }
            var slow=new thaumcraft.common.tiles.essentia.AlchemyMachineState(true);slow.input=Aspect.WATER;var produced=new java.util.concurrent.atomic.AtomicInteger();
            for(int i=0;i<999;i++)slow.tick(false,null,n->0,a->produced.incrementAndGet(),l.random);
            check(produced.get()==0&&slow.progress==199,"crystallizer without vis takes 200 steps at five-tick intervals");
            slow.tick(false,null,n->0,a->produced.incrementAndGet(),l.random);check(produced.get()==1&&slow.input==null,"unaccelerated crystal completes on tick1000");
            var fast=new thaumcraft.common.tiles.essentia.AlchemyMachineState(true);fast.input=Aspect.WATER;
            for(int i=0;i<25;i++)fast.tick(false,null,n->n,a->produced.incrementAndGet(),l.random);
            check(produced.get()==2,"Terra-vis accelerates crystallization with original remaining-progress clamp");
            TubeAudit.jar(l,at(0,0,0),Aspect.MAGIC,64,null);l.setBlock(at(0,1,0),AlchemyMachines.CENTRIFUGE.get().defaultBlockState(),3);
            check(machine(l,0).getSuctionAmount(Direction.DOWN)==128&&!machine(l,0).canInputFrom(Direction.UP)&&machine(l,0).canOutputTo(Direction.UP),"centrifuge input below/output above/suction128");
            check(machine(l,0).addEssentia(Aspect.FIRE,1,Direction.DOWN)==0,"centrifuge rejects primal input");
            TubeAudit.jar(l,at(3,0,0),Aspect.WATER,64,null);l.setBlock(at(3,1,0),AlchemyMachines.CRYSTALIZER.get().defaultBlockState(),3);
            l.setBlock(at(3,2,0),Blocks.CHEST.defaultBlockState(),3);
            TubeAudit.jar(l,at(6,0,0),Aspect.MAGIC,64,null);l.setBlock(at(6,1,0),AlchemyMachines.CENTRIFUGE.get().defaultBlockState(),3);l.setBlock(at(6,1,1),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            l.setBlock(at(9,0,0),AlchemyMachines.VOID_JAR.get().defaultBlockState(),3);var v=(WardedJarEntity)l.getBlockEntity(at(9,0,0));
            v.state.aspectFilter=Aspect.FIRE;check(v.getSuctionAmount(Direction.UP)==48&&v.getMinimumSuction()==48,"labeled void jar suction48");
            check(v.addEssentia(Aspect.FIRE,80,Direction.UP)==80&&v.state.amount==64,"void jar consumes overflow and caps64");
            check(v.getSuctionAmount(Direction.UP)==32&&v.addEssentia(Aspect.FIRE,9,Direction.UP)==9&&v.state.amount==64,"full void jar still consumes matching excess at suction32");
            check(v.addEssentia(Aspect.WATER,1,Direction.UP)==0,"filled void jar refuses mismatched aspect");v.changed();
            var saved=v.saveWithoutMetadata(l.registryAccess());var reload=new WardedJarEntity(at(9,0,0),v.getBlockState());reload.loadWithComponents(saved,l.registryAccess());
            check(reload.state.amount==64&&reload.state.aspectFilter==Aspect.FIRE&&reload.state instanceof thaumcraft.common.tiles.essentia.VoidJarState,"void behavior and label persist");
            var label=new ItemStack(EssentiaDevices.LABEL.get());var phial=EssentiaItems.phialOf(Aspect.FIRE,8);
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(phial,label));var recipe=new LabelRecipes.Aspect(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            check(recipe.matches(input,l)&&EssentiaItems.getAspects(recipe.assemble(input,l.registryAccess())).getAspects()[0]==Aspect.FIRE,"JARLABEL eight essentia -> labeled aspect");
            var marked=recipe.assemble(input,l.registryAccess());var clear=new LabelRecipes.Clear(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
            check(clear.matches(net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(marked)),l),"JARLABEL clear marked label");
            l.setBlock(at(12,0,0),EssentiaDevices.FURNACE.get().defaultBlockState(),3);l.setBlock(at(12,1,0),EssentiaDevices.ALEMBIC.get().defaultBlockState(),3);l.setBlock(at(12,2,0),EssentiaDevices.ALEMBIC.get().defaultBlockState(),3);
            var f=(AlchemicalFurnaceEntity)l.getBlockEntity(at(12,0,0));f.setItem(0,new ItemStack(Items.COBBLESTONE,2));f.setItem(1,new ItemStack(Items.COAL));
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+4,BASE.getY()+3,BASE.getZ()-4,0,20);
        });
        if(ticks==150)srv.execute(()->{
            var l=srv.overworld();var c=machine(l,0);
            check(c.state.output==Aspect.VOID||c.state.output==Aspect.ENERGY,"centrifuge outputs one original component of Praecantatio: "+c.state.output);
            check(c.getEssentiaAmount(Direction.UP)==1&&c.state.input==null,"centrifuge holds exactly one output");
            check(machine(l,6).state.input==null&&machine(l,6).getSuctionAmount(Direction.DOWN)==0,"redstone halts centrifuge draw");
            check(machine(l,3).state.input==Aspect.WATER&&machine(l,3).state.progress>0,"crystallizer draws one water and progresses");
            var f=(AlchemicalFurnaceEntity)l.getBlockEntity(at(12,0,0));var a=(AlembicEntity)l.getBlockEntity(at(12,1,0));
            check(f.getItem(0).getCount()<2&&a.state.amount>0,"DISTILESSENTIA real furnace fuel+smelt feeds alembic ("+a.state.amount+")");
            machine(l,3).state.progress=199;
        });
        if(ticks==180)srv.execute(()->{
            var l=srv.overworld();var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(at(3,2,0));
            check(java.util.stream.IntStream.range(0,chest.getContainerSize()).mapToObj(chest::getItem).anyMatch(s->s.is(LiquidDeath.CRYSTAL.get())&&EssentiaItems.getAspects(s).getAspects()[0]==Aspect.WATER),"crystallizer inserts correct crystal into inventory opposite input");
            var c=machine(l,0);var a=c.state.output;check(c.takeEssentia(a,1,Direction.DOWN)==0&&c.takeEssentia(a,1,Direction.UP)==1,"centrifuge extraction only above");
            System.out.println("TC4_ALCHEMY_MACHINES ALL PASS");done=true;
        });
        if(ticks==195){mc.options.guiScale().set(2);mc.resizeDisplay();}
        if(ticks==205)LiquidDeathAudit.shot(mc,"alchemy-machines.png");
        if(ticks==210){AlchemyBookRecipes.audit();if(done)mc.stop();}
        if(ticks>400)throw new IllegalStateException("Alchemy machines audit timeout");
    }
}
