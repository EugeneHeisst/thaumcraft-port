package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Fresh standard world, actual locate/start/chunk generation with the normal biome predicate, then the modern
 * profession mechanics of issue #4: job sites, natural job acquisition, levelled trades and legacy-entity conversion.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class VillageNaturalAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_VILLAGE_NATURAL FAIL "+text);System.out.println("TC4_VILLAGE_NATURAL PASS "+text);}
    static int ticks=-1;static Villager unemployed,trader;static java.util.UUID legacy;static BlockPos lab;
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.villageNaturalAudit"))return;
        var server=event.getServer();try{
            var level=server.overworld();int towers=0,homes=0;
            for(int n=0;n<12&&(towers==0||homes==0);n++){
                var found=level.findNearestMapStructure(StructureTags.VILLAGE,new BlockPos(n*4096,64,0),64,false);check(found!=null,"actual village locate near "+n+" returned "+found);
                var chunk=level.getChunkAt(found);var starts=chunk.getAllStarts().values().stream().filter(s->s.isValid()&&s.getPieces().stream().anyMatch(p->p instanceof PoolElementStructurePiece)).toList();check(!starts.isEmpty(),"actual generated village has persisted structure pieces");
                for(var start:starts){int w=0,b=0;for(var piece:start.getPieces()){
                    if(!(piece instanceof PoolElementStructurePiece pool)||!(pool.getElement() instanceof VillagePoolElement element))continue;
                    var box=piece.getBoundingBox();for(int x=(box.minX()>>4)-1;x<=(box.maxX()>>4)+1;x++)for(int z=(box.minZ()>>4)-1;z<=(box.maxZ()>>4)+1;z++)level.getChunk(x,z);
                    if(element.wizard){
                        var glow=new BlockPos(3,5,3).rotate(pool.getRotation()).offset(pool.getPosition());
                        check(level.getBlockState(glow).is(Blocks.GLOWSTONE),"actual natural tower survived normal multi-chunk placement at "+glow+" rotation="+pool.getRotation());
                        var door=new BlockPos(3,1,1).rotate(pool.getRotation()).offset(pool.getPosition());check(level.getBlockState(door).is(Blocks.OAK_DOOR),"natural tower retains its original entrance door");
                        var step=level.getBlockState(new BlockPos(3,0,0).rotate(pool.getRotation()).offset(pool.getPosition()));
                        if(step.is(Blocks.COBBLESTONE_STAIRS))check(step.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==pool.getRotation().rotate(net.minecraft.core.Direction.SOUTH),"tower entrance step rises towards the door: "+step);
                        var crucible=new BlockPos(2,1,4).rotate(pool.getRotation()).offset(pool.getPosition());check(level.getBlockState(crucible).is(TC4Modern.CRUCIBLE.get()),"tower holds the wizard's Crucible job site at "+crucible);
                        check(!level.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(box.getCenter()).inflate(16),v->v.getVillagerData().getProfession()==VillageContent.WIZARD_PROFESSION.get()).isEmpty(),"tower spawns a vanilla villager with the wizard profession");
                        w++;
                    }else{
                        check(element.template.isPresent(),"new banker home uses a modern vanilla house template: "+element.template);
                        int chests=0,tables=0;for(var at:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())){var s=level.getBlockState(at);if(s.is(HungryChest.BLOCK.get()))chests++;if(s.is(Blocks.CARTOGRAPHY_TABLE))tables++;}
                        check(chests==1&&tables==0,"modern banker home holds exactly one Hungry Chest job site instead of the cartography table: "+chests+"/"+tables+" "+element.template.get());
                        var bankers=level.getEntitiesOfClass(Villager.class,net.minecraft.world.phys.AABB.of(box).inflate(3),v->v.getVillagerData().getProfession()==VillageContent.BANKER_PROFESSION.get()).size();
                        check(bankers==1,"modern banker home spawns exactly one banker villager: "+bankers);
                        b++;
                    }
                }check(w<=1&&b<=1,"actual persisted village respects original zero-or-one quotas: "+w+"/"+b);towers+=w;homes+=b;}
            }
            check(towers>0&&homes>0,"fresh natural world includes both buildings: "+towers+" towers / "+homes+" homes");
            check(net.minecraft.world.entity.ai.village.poi.PoiTypes.forState(TC4Modern.CRUCIBLE.get().defaultBlockState()).map(h->h.is(VillageContent.WIZARD_POI)).orElse(false),"Crucible is the wizard job-site point of interest");
            check(net.minecraft.world.entity.ai.village.poi.PoiTypes.forState(HungryChest.BLOCK.get().defaultBlockState()).map(h->h.is(VillageContent.BANKER_POI)).orElse(false),"Hungry Chest is the banker job-site point of interest");
            for(boolean wizard:new boolean[]{true,false}){var trades=net.minecraft.world.entity.npc.VillagerTrades.TRADES.get((wizard?VillageContent.WIZARD_PROFESSION:VillageContent.BANKER_PROFESSION).get());
                check(trades!=null&&java.util.stream.IntStream.rangeClosed(1,5).allMatch(l->trades.get(l)!=null&&trades.get(l).length>0),(wizard?"wizard":"banker")+" has offers on all five profession levels");}
            // Flat-ground towers in all four rotations always get the original entrance step; it must rise towards the door.
            int r=0;for(var rotation:net.minecraft.world.level.block.Rotation.values()){var base=level.getSharedSpawnPos().offset(-200+r++*40,0,-200);base=new BlockPos(base.getX(),150,base.getZ());
                for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++){level.setBlock(base.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<14;y++)level.setBlock(base.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
                new VillagePoolElement(true,net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection.RIGID).place(server.getStructureManager(),level,level.structureManager(),level.getChunkSource().getGenerator(),base,base,rotation,net.minecraft.world.level.levelgen.structure.BoundingBox.infinite(),level.random,net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING,false);
                var stepPos=new BlockPos(3,0,0).rotate(rotation).offset(base);var step=level.getBlockState(stepPos);
                check(step.is(Blocks.COBBLESTONE_STAIRS),"flat tower "+rotation+" has its entrance step");
                var up=step.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                check(level.getBlockState(stepPos.relative(up).above()).is(Blocks.OAK_DOOR),"flat tower "+rotation+" step rises towards the door (facing "+up+")");
            }
            // Natural job acquisition and trade progression run in a forced chunk near spawn.
            lab=level.getSharedSpawnPos().offset(0,0,48);level.setChunkForced(lab.getX()>>4,lab.getZ()>>4,true);lab=new BlockPos(lab.getX(),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,lab.getX(),lab.getZ())+8,lab.getZ());
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){level.setBlock(lab.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<4;y++)level.setBlock(lab.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            level.setBlock(lab.offset(2,0,0),TC4Modern.CRUCIBLE.get().defaultBlockState(),3);
            unemployed=net.minecraft.world.entity.EntityType.VILLAGER.create(level);unemployed.moveTo(lab.getX()-1.5,lab.getY(),lab.getZ()+.5,0,0);level.addFreshEntity(unemployed);
            check(unemployed.getVillagerData().getProfession()==VillagerProfession.NONE,"fixture villager starts unemployed");
            var old=VillageContent.BANKER.get().create(level);old.moveTo(lab.getX()+.5,lab.getY(),lab.getZ()-3.5,0,0);level.addFreshEntity(old);legacy=old.getUUID();
            ticks=0;
        }catch(RuntimeException ex){server.halt(false);throw ex;}
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(ticks<0)return;ticks++;var server=event.getServer();var level=server.overworld();
        try{
            // Without players the dedicated server leaves the fixture chunk entity-idle; drive the ordinary entity tick.
            for(var e:new net.minecraft.world.entity.Entity[]{legacy==null?null:level.getEntity(legacy),unemployed})if(e!=null&&!e.isRemoved()){e.tickCount++;e.tick();}
            if(legacy!=null&&ticks==400)check(false,"legacy banker entity from old saves is never replaced");
            if(legacy!=null&&level.getEntity(legacy)==null){
                check(true,"legacy banker entity from old saves is replaced after "+ticks+" ticks");legacy=null;
                var converted=level.getEntitiesOfClass(Villager.class,new net.minecraft.world.phys.AABB(lab).inflate(8),v->v.getType()==net.minecraft.world.entity.EntityType.VILLAGER&&v.getVillagerData().getProfession()==VillageContent.BANKER_PROFESSION.get());
                check(converted.size()==1&&converted.getFirst().getVillagerXp()==1,"legacy banker becomes a vanilla villager keeping its banker profession");
                converted.getFirst().discard();
            }
            if(trader==null&&unemployed.getVillagerData().getProfession()==VillageContent.WIZARD_PROFESSION.get()){
                check(true,"unemployed villager took the wizard job from a placed Crucible after "+ticks+" ticks");
                trader=unemployed;var offers=trader.getOffers();check(offers.size()==2&&trader.getVillagerData().getLevel()==1,"novice wizard offers two level-one trades: "+offers.size());
                for(int i=0;i<6;i++){var offer=offers.getFirst();trader.notifyTrade(offer);}
                check(trader.getVillagerXp()>=10,"trading earns profession experience: "+trader.getVillagerXp());ticks=10000;
            }
            if(ticks==2400&&trader==null)check(false,"unemployed villager never took the Crucible job");
            if(ticks==10060){
                check(trader.getVillagerData().getLevel()==2&&trader.getOffers().size()==4,"after enough trades the wizard reaches apprentice and unlocks new offers: level "+trader.getVillagerData().getLevel()+" offers "+trader.getOffers().size());
                System.out.println("TC4_VILLAGE_NATURAL ALL PASS fresh-world villages, modern banker home, tower Crucible, job sites, natural job, levelled trades and legacy conversion");
                ticks=-1;server.halt(false);
            }
        }catch(RuntimeException ex){ticks=-1;server.halt(false);throw ex;}
    }
}
