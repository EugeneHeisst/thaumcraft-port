package dev.eugheist.compat;

import dev.eugheist.tc4modern.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import thaumcraft.api.aspects.*;
import java.nio.file.*;
import java.util.*;

/** Disposable native server: actual right clicks, spawn placement predicates and working villager brains. */
@EventBusSubscriber(modid="compat_audit")
public final class IssuesNov06Audit {
    static final Map<String,Boolean> results=new LinkedHashMap<>();
    static final List<Trial> trials=new ArrayList<>();
    static final BlockPos JAR_POS=new BlockPos(64,110,64);
    static MinecraftServer server;
    static int ticks, phase;
    static boolean captureConversion;
    static Villager converted;
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e){
        if(captureConversion&&e.getEntity() instanceof Villager v&&v.getType()==EntityType.VILLAGER)converted=v;
    }
    static boolean old(){return "old".equals(System.getProperty("issuesnov06.variant"));}
    static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("tc4modern",name);}
    static Block block(String name){return BuiltInRegistries.BLOCK.get(id(name));}
    static ItemStack jar(boolean empty, boolean isVoid, Aspect aspect,int amount){
        var s=new ItemStack(block(isVoid?"void_jar":"warded_jar"));
        if(!empty)EssentiaItems.setAspects(s,new AspectList().add(aspect,amount));
        return s;
    }
    static void check(boolean good,String name){results.put(name,good);System.out.println("ISSUESNOV06 "+(good?"PASS ":"FAIL ")+name);}
    static int amount(ItemStack s,Aspect a){var c=EssentiaItems.getAspects(s);return c==null?0:c.getAmount(a);}
    static void fill(String name,ItemStack held,Aspect source,int quantity,int remaining,int expected,boolean split,boolean offhand,boolean creative){
        var l=server.overworld();var p=FakePlayerFactory.getMinecraft(l);p.getInventory().clearContent();p.getInventory().selected=0;
        p.gameMode.changeGameModeForPlayer(creative?GameType.CREATIVE:GameType.SURVIVAL);
        p.setShiftKeyDown(false);p.moveTo(JAR_POS.getX()+.5,JAR_POS.getY(),JAR_POS.getZ()+2.5);
        var hand=offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        p.setItemInHand(hand,held);l.removeBlock(JAR_POS,false);l.setBlockAndUpdate(JAR_POS,block("alembic").defaultBlockState());
        var a=(AlembicEntity)l.getBlockEntity(JAR_POS);a.state.aspect=source;a.state.amount=quantity;a.state.aspectFilter=source;a.changed();
        var hit=new BlockHitResult(Vec3.atCenterOf(JAR_POS).add(0,0,.5),Direction.SOUTH,JAR_POS,false);
        var result=p.gameMode.useItemOn(p,l,held,hand,hit);
        ItemStack filled=p.getItemInHand(hand);
        if(split)for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(amount(s,source)>0){filled=s;break;}}
        check(a.state.amount==remaining&&amount(filled,source)==expected,name+"_native_transfer");
        check(a.state.aspectFilter==source&&(remaining>0?a.state.aspect==source:a.state.aspect==null),name+"_alembic_filter_and_type");
        if(split)check(held.getCount()==2&&filled.getCount()==1,name+"_stack_splits_exactly_one");
        if(name.contains("label"))check(filled.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getString("AspectFilter").equals(source.getTag()),name+"_label_preserved");
        System.out.println("ISSUESNOV06 CLICK "+name+" result="+result+" source="+a.state.amount+" jar="+amount(filled,source));
    }
    static void refused(String name,ItemStack held){
        var l=server.overworld();var p=FakePlayerFactory.getMinecraft(l);p.getInventory().clearContent();p.getInventory().selected=0;p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,held);var copy=held.copy();
        l.removeBlock(JAR_POS,false);l.removeBlock(JAR_POS.south(),false);l.setBlockAndUpdate(JAR_POS,block("alembic").defaultBlockState());
        var a=(AlembicEntity)l.getBlockEntity(JAR_POS);a.state.aspect=Aspect.FIRE;a.state.amount=32;a.changed();
        p.gameMode.useItemOn(p,l,held,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(JAR_POS),Direction.SOUTH,JAR_POS,false));
        check(a.state.amount==32&&ItemStack.matches(copy,p.getMainHandItem())&&l.isEmptyBlock(JAR_POS.south()),name+"_no_loss_or_placement");
    }
    static void jars(){
        fill("empty",jar(true,false,Aspect.FIRE,0),Aspect.FIRE,32,0,32,false,false,false);
        fill("partial",jar(false,false,Aspect.FIRE,60),Aspect.FIRE,32,28,64,false,false,false);
        var labelled=jar(false,false,Aspect.FIRE,48);CustomData.update(DataComponents.CUSTOM_DATA,labelled,t->t.putString("AspectFilter","ignis"));
        fill("label",labelled,Aspect.FIRE,24,8,64,false,false,false);
        fill("void_full",jar(false,true,Aspect.FIRE,64),Aspect.FIRE,32,0,64,false,false,false);
        fill("void_partial",jar(false,true,Aspect.FIRE,60),Aspect.FIRE,32,0,64,false,false,false);
        var stack=jar(true,false,Aspect.FIRE,0);stack.setCount(3);fill("stack",stack,Aspect.FIRE,24,0,24,true,false,false);
        fill("offhand",jar(true,false,Aspect.FIRE,0),Aspect.FIRE,12,0,12,false,true,false);
        stack=jar(true,false,Aspect.FIRE,0);stack.setCount(3);fill("creative_stack",stack,Aspect.FIRE,16,0,16,true,false,true);
        refused("wrong_aspect",jar(false,false,Aspect.WATER,12));
        refused("full_regular",jar(false,false,Aspect.FIRE,64));
        var wrongLabel=jar(true,false,Aspect.FIRE,0);CustomData.update(DataComponents.CUSTOM_DATA,wrongLabel,t->t.putString("AspectFilter","aqua"));refused("wrong_label",wrongLabel);
    }
    record Trial(String name,Villager villager,BlockPos job){}
    static void villager(String name,boolean wizard,Block workstation,int x)throws Exception{
        var l=server.overworld();var pos=new BlockPos(x,110,160);l.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int y=-1;y<=3;y++)l.setBlock(pos.offset(dx,y,dz),(y==-1||Math.abs(dx)==3||Math.abs(dz)==3?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        l.setBlockAndUpdate(pos,workstation.defaultBlockState());
        Villager v;
        if(name.startsWith("legacy_")){
            var legacy=(Villager)BuiltInRegistries.ENTITY_TYPE.get(id(wizard?"wizard":"banker")).create(l);
            legacy.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+1.3);l.addFreshEntity(legacy);
            converted=null;captureConversion=true;legacy.tick();captureConversion=false;v=Objects.requireNonNull(converted,"native conversion must add its vanilla villager");
            check(legacy.isRemoved(),name+"_native_conversion");
        }else{
            var factory=Class.forName("dev.eugheist.tc4modern.VillageContent").getDeclaredMethod("villager",Level.class,boolean.class);factory.setAccessible(true);
            v=(Villager)factory.invoke(null,l,wizard);v.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+1.3);l.addFreshEntity(v);
        }
        v.setVillagerXp(1);
        if(name.startsWith("loaded_")){var saved=new net.minecraft.nbt.CompoundTag();v.saveWithoutId(saved);v.load(saved);}
        for(var offer:v.getOffers())offer.setToOutOfStock();trials.add(new Trial(name,v,pos));
    }
    static final List<ServerLevel> spawnLevels=new ArrayList<>();static final List<BlockPos> spawnPositions=new ArrayList<>();
    static final List<Boolean> spawnExpected=new ArrayList<>();static final List<String> spawnNames=new ArrayList<>();
    static void spawnRoom(ServerLevel l,BlockPos pos,String biome,boolean expected,String name){
        l.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=4;y++)l.setBlock(pos.offset(x,y,z),(Math.abs(x)==3||Math.abs(z)==3||y==-1||y==4?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
        if(biome!=null)server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(l).withSuppressedOutput(),"fillbiome "+(pos.getX()-4)+" 100 "+(pos.getZ()-4)+" "+(pos.getX()+4)+" 120 "+(pos.getZ()+4)+" tc4modern:"+biome);
        spawnLevels.add(l);spawnPositions.add(pos);spawnExpected.add(expected);spawnNames.add(name);
    }
    static void spawns(){
        @SuppressWarnings("unchecked") var type=(EntityType<PechEntity>)BuiltInRegistries.ENTITY_TYPE.get(id("pech"));
        for(int i=0;i<spawnNames.size();i++){
            var l=spawnLevels.get(i);var pos=spawnPositions.get(i);var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(0){@Override public int nextInt(int bound){return 0;}};
            boolean nativeAllowed=net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type,l,MobSpawnType.NATURAL,pos,random);
            check(nativeAllowed,spawnNames.get(i)+"_fixture_passes_vanilla_monster_rules");
            check(SpawnPlacements.checkSpawnRules(type,l,MobSpawnType.NATURAL,pos,random)==spawnExpected.get(i),spawnNames.get(i)+"_native_spawn_placement");
        }
        var outer=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id("outer_lands")));
        var existing=type.create(outer);existing.setNoAi(true);existing.moveTo(spawnPositions.get(0).getX(),110,160);outer.addFreshEntity(existing);existing.tick();check(existing.isAlive()&&!existing.isRemoved(),"existing_or_command_pechs_are_not_deleted");existing.discard();
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){
        if(System.getProperty("issuesnov06.variant")==null)return;
        try{
            server=e.getServer();server.setDifficulty(Difficulty.NORMAL,true);server.tickRateManager().setTickRate(200);
            var l=server.overworld();l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.setDayTime(2000);
            l.setChunkForced(4,4,true);jars();
            check(net.minecraft.world.entity.ai.village.poi.PoiTypes.forState(block("arcane_workbench").defaultBlockState()).map(h->h.is(id("wizard"))).orElse(false),"arcane_workbench_registered_as_wizard_job_site");
            villager("arcane_wizard",true,block("arcane_workbench"),160);villager("crucible_wizard",true,block("crucible"),240);villager("hungry_chest_banker",false,block("hungry_chest"),320);
            villager("loaded_crucible_wizard",true,block("crucible"),400);villager("loaded_banker",false,block("hungry_chest"),480);
            villager("legacy_wizard",true,block("arcane_workbench"),560);villager("legacy_banker",false,block("hungry_chest"),640);
            var outer=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id("outer_lands")));
            spawnRoom(outer,new BlockPos(800,110,800),null,false,"outer_lands");
            spawnRoom(l,new BlockPos(880,110,800),"magical_forest",true,"overworld_magical_forest");
            spawnRoom(l,new BlockPos(960,110,800),"tainted_land",false,"overworld_tainted_land");
            spawnRoom(server.getLevel(Level.NETHER),new BlockPos(1040,110,800),"magical_forest",true,"other_dimension_original_forest_exception");
        }catch(Throwable ex){finish(ex);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(server==null)return;
        try{
            ticks++;
            // Dedicated servers without players leave forced chunks entity-idle. Drive normal entity ticks.
            for(var t:trials)if(!t.villager.isRemoved()){t.villager.tickCount++;t.villager.tick();}
            if(ticks==100||ticks==1000)for(var t:trials)System.out.println("ISSUESNOV06 VILLAGER "+t.name+" pos="+t.villager.position()+" job="+t.villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)+" potential="+t.villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)+" actualPoi="+server.overworld().getPoiManager().getType(t.job)+" activity="+t.villager.getBrain().getActiveNonCoreActivity());
            if(ticks==100)spawns();
            boolean all=trials.stream().allMatch(t->t.villager.getOffers().stream().noneMatch(o->o.isOutOfStock()));
            if(ticks>=100&&(all||ticks>=2400)){
                for(var t:trials){
                    var memory=t.villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                    check(memory.isPresent()&&memory.get().pos().equals(t.job),t.name+"_naturally_claims_job_site_day"+phase);
                    check(t.villager.getOffers().stream().noneMatch(o->o.isOutOfStock())&&t.villager.getBrain().hasMemoryValue(MemoryModuleType.LAST_WORKED_AT_POI),t.name+"_brain_restock_day"+phase);
                }
                if(phase++==0){
                    server.overworld().setDayTime(26000);for(var t:trials)for(var offer:t.villager.getOffers())offer.setToOutOfStock();ticks=0;
                }else finish(null);
            }
        }catch(Throwable ex){finish(ex);}
    }
    static void finish(Throwable ex){
        try{
            if(ex!=null){ex.printStackTrace();results.put("audit_exception",false);}
            var path=Path.of(System.getProperty("issuesnov06.out"));Files.createDirectories(path);Files.writeString(path.resolve("results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
            long failed=results.values().stream().filter(x->!x).count();System.out.println("ISSUESNOV06 DONE "+System.getProperty("issuesnov06.variant")+" checks="+results.size()+" failures="+failed);
            System.out.flush();Runtime.getRuntime().halt(ex!=null||(!old()&&failed>0)?3:0);
        }catch(Throwable failure){failure.printStackTrace();Runtime.getRuntime().halt(4);}
    }
}
