package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class AdvancedFurnaceAudit {
    static final BlockPos BASE=new BlockPos(840,121,-90),CORE=BASE.offset(0,0,3);static int ticks;static volatile String failed;
    static void check(boolean yes,String message){if(!yes)failed=message;System.out.println("TC4_ADV_FURNACE "+(yes?"PASS ":"FAIL ")+message);}
    static void blueprint(net.minecraft.world.level.Level l){for(int a=-1;a<=1;a++)for(int b=0;b<=1;b++)for(int c=-1;c<=1;c++){
        int kind=AdvancedFurnace.BLUEPRINT[b][a+1][c+1];var block=switch(kind){case 1->EssentiaDevices.ALEMBIC.get();case 2->AlchemyMachines.CONSTRUCT.get();case 3->EssentiaDevices.FURNACE.get();case 4->AdvancedFurnace.CONSTRUCT.get();default->Blocks.AIR;};l.setBlock(CORE.offset(a,b,c),block.defaultBlockState(),3);}}
    static AdvancedFurnace.CoreEntity core(net.minecraft.world.level.Level l){return (AdvancedFurnace.CoreEntity)l.getBlockEntity(CORE);}
    static void setup(ServerPlayer p){var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();l.setDayTime(6000);p.getInventory().clearContent();
        l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(20)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        for(int x=-6;x<=6;x++)for(int z=-4;z<=9;z++)for(int y=-1;y<6;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
        blueprint(l);var wand=FociClientAudit.wand(ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var knowledge=p.getPersistentData().getCompound(ResearchKnowledge.KEY);var researches=knowledge.getList("Research",8);researches.add(StringTag.valueOf("ADVALCHEMYFURNACE"));knowledge.put("Research",researches);p.getPersistentData().put(ResearchKnowledge.KEY,knowledge);
        var hit=CORE.east();var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(hit),Direction.UP,hit,false));
        check(AdvancedFurnace.create(context,wand),"wand assembles the original 3x3x2 blueprint");
        int parts=0,nozzles=0;for(var at:BlockPos.betweenClosed(CORE.offset(-1,0,-1),CORE.offset(1,1,1))){if(l.getBlockState(at).is(AdvancedFurnace.BLOCK.get()))parts++;if(l.getBlockEntity(at) instanceof AdvancedFurnace.NozzleEntity)nozzles++;}
        check(parts==17&&nozzles==4,"17 parts including four output nozzles: "+parts+" / "+nozzles);
        var f=core(l);f.heat=500;f.power1=500;f.power2=500;f.sync();var tags=f.tags(new ItemStack(Items.COAL));check(tags!=null&&tags.visSize()>0,"coal has original smelting aspects");
        var item=new ItemEntity(l,CORE.getX()+.5,CORE.getY()+1.2,CORE.getZ()+.5,new ItemStack(Items.COAL,2));item.setNeverPickUp();l.addFreshEntity(item);
        var recipe=ArcaneRecipes.entries().stream().filter(r->r.key().equals("AdvAlchemyConstruct")).findFirst().orElseThrow();check(recipe.output().getCount()==4&&recipe.cost().getAmount(Aspect.ORDER)==30,"original construct arcane recipe");
        p.connection.teleport(BASE.getX()+4.5,BASE.getY()+2.2,BASE.getZ()-1.5,45,20);p.containerMenu.broadcastChanges();
    }
    static void processed(ServerPlayer p){var l=p.serverLevel();var f=core(l);check(f.vis>0&&f.aspects.visSize()==f.vis,"dropped coal melts into essentia: "+f.vis);check(f.heat<500&&f.power1<500&&f.power2<500,"processing consumes heat and both vis powers");
        var copy=new AdvancedFurnace.CoreEntity(CORE,l.getBlockState(CORE));copy.loadWithComponents(f.saveWithoutMetadata(l.registryAccess()),l.registryAccess());check(copy.vis==f.vis&&copy.heat==f.heat&&copy.power1==f.power1&&copy.aspects.visSize()==f.vis,"NBT preserves all energy and essentia");
        var n=(AdvancedFurnace.NozzleEntity)l.getBlockEntity(CORE.east());check(n.facing==Direction.EAST&&n.canOutputTo(Direction.EAST)&&!n.canInputFrom(Direction.EAST),"east nozzle exposes outward output only");var a=f.aspects.getAspects()[0];int before=f.aspects.getAmount(a);check(n.takeEssentia(a,1,Direction.EAST)==1&&f.aspects.getAmount(a)==before-1,"output nozzle drains actual core contents");
        f.aspects=new AspectList().add(Aspect.FIRE,500);f.vis=500;f.processed=0;check(!f.process(new ItemStack(Items.COAL)),"500 capacity rejects overflow");f.aspects=new AspectList().add(Aspect.FIRE,200);f.vis=200;f.heat=500;f.sync();
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.advancedFurnaceAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
        if(ticks==100)s.execute(()->{try{setup(s.getPlayerList().getPlayer(id));}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ticks==220)s.execute(()->{try{processed(s.getPlayerList().getPlayer(id));}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ticks==240)s.execute(()->{try{var l=s.getPlayerList().getPlayer(id).serverLevel();var f=core(l);f.heat=0;f.power1=0;f.power2=0;var at=CORE.west(4).above();l.setBlock(at,TC4Modern.ENERGIZED_NODE.get().defaultBlockState(),3);var node=(EnergizedNodeEntity)l.getBlockEntity(at);for(var a:new Aspect[]{Aspect.FIRE,Aspect.WATER,Aspect.ENTROPY}){node.auraBase.add(a,100);node.energy.visBase.add(a,50);node.energy.vis.add(a,50);}VisNetwork.register(l,node);VisNetwork.invalidate();}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ticks==320)s.execute(()->{var f=core(s.getPlayerList().getPlayer(id).serverLevel());check(f.heat>100&&f.power1>100&&f.power2>100,"actual energized node feeds all three powers: "+f.heat+" / "+f.power1+" / "+f.power2);});
        if(ticks==350)LiquidDeathAudit.shot(mc,"advanced-furnace-world.png");
        if(ticks==360)s.execute(()->{try{var l=s.getPlayerList().getPlayer(id).serverLevel();l.destroyBlock(CORE.east(),true);int restored=0;for(int a=-1;a<=1;a++)for(int b=0;b<=1;b++)for(int c=-1;c<=1;c++){if(a==1&&b==0&&c==0||a==0&&b==1&&c==0)continue;int kind=AdvancedFurnace.BLUEPRINT[b][a+1][c+1];if(AdvancedFurnace.matches(l,CORE.offset(a,b,c),kind))restored++;}check(restored==16,"breaking one part restores the other sixteen without recursion: "+restored);var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(CORE.east()).inflate(1)).stream().filter(it->it.getItem().is(AdvancedFurnace.CONSTRUCT_ITEM.get())).mapToInt(it->it.getItem().getCount()).sum();check(drops==1,"broken part drops exactly one construct: "+drops);}catch(Exception ex){failed=ex.toString();ex.printStackTrace();}});
        if(ticks==380){System.out.println(failed==null?"TC4_ADV_FURNACE ALL PASS":"TC4_ADV_FURNACE FAILED "+failed);mc.stop();}
    }
}
