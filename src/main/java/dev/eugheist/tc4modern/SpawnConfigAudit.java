package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real disabled spawn lists plus deterministic original champion odds through the actual join handler. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class SpawnConfigAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_SPAWN_CONFIG FAIL "+text);System.out.println("TC4_SPAWN_CONFIG PASS "+text);}
    static int count(boolean enabled,boolean outer,boolean spooky,boolean dangerous,Difficulty difficulty,int weight,boolean boss){int count=0;for(int roll=0;roll<100;roll++)if(Champions.chance(roll,difficulty,enabled,outer,spooky,dangerous,weight,boss)<=0)count++;return count;}
    static void actual(ServerLevel level,BlockPos pos,boolean expected,String description){
        var z=EntityType.ZOMBIE.create(level);z.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);z.setNoAi(true);
        long seed=0;while(net.minecraft.util.RandomSource.create(seed).nextInt(100)!=0)seed++;
        level.random.setSeed(seed);level.addFreshEntity(z);check((Champions.type(z)>=0)==expected,description);z.discard();
    }
    static void defaults(ServerLevel level){
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        for(String[] entry:new String[][]{{"minecraft:nether_wastes","firebat","10","1","2"},{"minecraft:nether_wastes","wisp","5","1","1"},{"tc4modern:magical_forest","pech","10","1","1"},{"tc4modern:tainted_land","taintacle","1","1","1"},{"tc4modern:eerie","brainy_zombie","32","1","1"},{"tc4modern:eerie","giant_brainy_zombie","8","1","1"},{"tc4modern:eerie","wisp","3","1","1"},{"tc4modern:eerie","eldritch_guardian","1","1","1"},{"tc4modern:outer_lands","pech","10","1","1"}}){
            var list=registry.get(ResourceLocation.parse(entry[0])).getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            var matches=list.stream().filter(v->net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(v.type).equals(AlchemyContent.id(entry[1]))&&v.getWeight().asInt()==Integer.parseInt(entry[2])&&v.minCount==Integer.parseInt(entry[3])&&v.maxCount==Integer.parseInt(entry[4])).toList();
            check(matches.size()==1,"original default spawn weight/group "+entry[0]+" "+entry[1]);
        }
        System.out.println("TC4_SPAWN_DEFAULTS ALL PASS original default weights, group sizes, conditional eerie entries and magical Outer Lands");
    }
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.spawnConfigAudit"))return;
        var server=event.getServer();try{
            for(String key:new String[]{"spawn_angry_zombies","spawn_fire_bats","spawn_wisps","spawn_taintacles","spawn_taint_spores","spawn_pechs","spawn_eldercreatures","champion_mobs"})check(!SpawnConfig.enabled(key),"loaded false TOML setting "+key);
            var registry=server.registryAccess().registryOrThrow(Registries.BIOME);
            var disabled=java.util.Set.of("brainy_zombie","giant_brainy_zombie","firebat","wisp","taintacle","pech");
            int checked=0;for(var biome:registry){for(var spawn:biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap())check(!disabled.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type).getPath())||!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type).getNamespace().equals(TC4Modern.MODID),"disabled natural spawn absent "+net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(spawn.type));checked++;}
            check(checked>=60,"checked all actual final biome spawn lists: "+checked);
            check(registry.get(AlchemyContent.id("eerie")).getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().noneMatch(v->net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(v.type).equals(AlchemyContent.id("eldritch_guardian"))),"disabled eerie guardians absent while unconditional Outer Lands guardians remain");
            var outerBiome=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME,AlchemyContent.id("outer_lands")));
            check(BiomeAspectBridge.types(outerBiome).containsAll(java.util.List.of("MAGICAL","SPOOKY","END")),"Outer Lands retain all three original biome types");
            check(ConfiguredSpawnModifier.halloween(java.time.LocalDate.of(2026,10,31))&&!ConfiguredSpawnModifier.halloween(java.time.LocalDate.of(2026,10,30))&&!ConfiguredSpawnModifier.halloween(java.time.LocalDate.of(2026,11,1)),"Halloween firebat date is exactly October 31");
            check(count(true,false,false,false,Difficulty.NORMAL,0,false)==1,"normal ordinary champion chance 1/100");
            check(count(true,false,false,false,Difficulty.HARD,0,false)==3,"hard ordinary champion chance 3/100");
            check(count(true,false,false,false,Difficulty.EASY,0,false)==0,"easy ordinary champion chance zero");
            check(count(false,false,false,false,Difficulty.HARD,0,false)==0,"disabled ordinary champions absent even on hard");
            check(count(true,true,true,false,Difficulty.NORMAL,0,false)==6,"Outer Lands corridor chance 6/100");
            check(count(true,true,true,true,Difficulty.NORMAL,0,false)==16,"dangerous Outer Lands chamber chance 16/100");
            check(count(false,true,true,true,Difficulty.NORMAL,0,false)==6,"disabled dangerous-place exception reduced to 6/100");
            check(count(false,false,false,false,Difficulty.NORMAL,3,false)==0,"disabled whitelist weight cannot restore ordinary champions");
            check(count(false,false,false,false,Difficulty.NORMAL,200,true)==100,"boss champion chance remains guaranteed while disabled");
            server.setDifficulty(Difficulty.NORMAL,true);var level=server.overworld();var pos=new BlockPos(320,180,320);
            var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(1){@Override public int nextInt(int bound){return bound==3?1:bound==5?2:0;}};
            var sporePos=pos.offset(16,0,0);level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpore.class,new net.minecraft.world.phys.AABB(pos).inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);TaintBiomes.spread(level,sporePos);level.setBlock(sporePos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);level.setBlock(sporePos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            var fibres=TC4Modern.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibresBlock.VARIANT,3);level.setBlock(sporePos,fibres,3);
            TC4Modern.TAINT_FIBRES.get().tick(fibres,level,sporePos,random);
            check(level.getBlockState(sporePos).getValue(TaintFibresBlock.VARIANT)==3&&level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpore.class,new net.minecraft.world.phys.AABB(sporePos).inflate(2)).isEmpty(),"actual mature-fibre tick cannot spawn spores while disabled");
            SpawnConfig.set("spawn_taint_spores",true);TC4Modern.TAINT_FIBRES.get().tick(fibres,level,sporePos,random);
            check(level.getBlockState(sporePos).getValue(TaintFibresBlock.VARIANT)==4&&level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpore.class,new net.minecraft.world.phys.AABB(sporePos).inflate(2)).size()==1,"actual mature-fibre tick creates exactly one spore while enabled; state="+level.getBlockState(sporePos)+" spores="+level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpore.class,new net.minecraft.world.phys.AABB(sporePos).inflate(2)).size()+" config="+SpawnConfig.spores()+" biome="+level.getBiome(sporePos));
            var crustPos=pos.offset(32,0,0);TaintBiomes.spread(level,crustPos);level.setBlock(crustPos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);level.setBlock(crustPos.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);var crust=TC4Modern.TAINT.get().defaultBlockState();level.setBlock(crustPos,crust,3);SpawnConfig.set("spawn_taint_spores",false);TC4Modern.TAINT.get().tick(crust,level,crustPos,random);
            check(level.getBlockState(crustPos).is(TC4Modern.TAINT.get())&&level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.class,new net.minecraft.world.phys.AABB(crustPos).inflate(2)).isEmpty(),"actual crust tick cannot spawn a swarmer while disabled");
            SpawnConfig.set("spawn_taint_spores",true);TC4Modern.TAINT.get().tick(crust,level,crustPos,random);
            check(level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.class,new net.minecraft.world.phys.AABB(crustPos).inflate(2)).size()==1,"actual crust tick creates a swarmer while enabled");SpawnConfig.set("spawn_taint_spores",false);
            var boss=OuterBossContent.WARDEN.get().create(level);boss.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(boss);check(Champions.type(boss)>=0,"actual boss join still creates a champion while disabled");boss.discard();
            actual(level,pos,false,"actual ordinary zombie join refuses champion while disabled");SpawnConfig.set("champion_mobs",true);actual(level,pos,true,"actual ordinary zombie join accepts zero roll while enabled");SpawnConfig.set("champion_mobs",false);
            var outer=server.getLevel(OuterDimension.KEY);var maze=OuterMazeData.get(outer);maze.create(512,512,15,15,426351);
            BlockPos danger=null,corridor=null;
            for(int x=500;x<=524;x++)for(int z=500;z<=524;z++){var cell=maze.cell(x,z);if(cell==null)continue;var point=new BlockPos(x*16+8,54,z*16+8);if(cell.feature==6||cell.feature==8)danger=point;else corridor=point;}
            check(danger!=null&&corridor!=null,"saved original maze has dangerous and ordinary cells");check(Champions.dangerous(outer,danger)&&!Champions.dangerous(outer,corridor)&&!Champions.dangerous(level,danger),"actual saved-cell lookup restricts dangerous locations to Outer Lands");
            actual(outer,danger,true,"actual dangerous-room zombie can become champion with toggle disabled");
            System.out.println("TC4_SPAWN_CONFIG ALL PASS loaded switches, final biome lists, original champion odds, dangerous maze cells and disabled-mode exception");
        }catch(Exception ex){throw new IllegalStateException("Spawn configuration audit failed",ex);}finally{server.halt(false);}
    }
}
