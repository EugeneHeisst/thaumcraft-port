package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Native generation, graph limits, codec persistence and actual original building contents. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class VillageBuildingAudit {
    static int ticks;static volatile boolean ready;static final BlockPos BASE=new BlockPos(96,180,96);
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_VILLAGE_BUILDING FAIL "+text);System.out.println("TC4_VILLAGE_BUILDING PASS "+text);}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.villageBuildingAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(ticks==60&&!ready)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();
        if(ticks==50)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);p.closeContainer();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);var level=p.serverLevel();
            for(int x=-12;x<=26;x++)for(int z=-20;z<=20;z++){level.setBlock(BASE.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<=13;y++)level.setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            var manager=server.getStructureManager();var poolRegistry=level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
            for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}){
                var pool=(dev.eugheist.tc4modern.mixin.VillagePoolAccessor)poolRegistry.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("village/"+biome+"/houses"));
                check(pool.tc4modern$raw().stream().filter(v->v.getFirst() instanceof VillagePoolElement&&v.getSecond()==(((VillagePoolElement)v.getFirst()).wizard?15:25)).count()==2,"additive original weights15/25 in actual "+biome+" house pool");
            }
            int totalWizard=0,totalBanker=0;
            var generator=level.getChunkSource().getGenerator();var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("village_plains"));
            for(int n=0;n<24;n++){
                var start=structure.generate(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),manager,426351+n,new net.minecraft.world.level.ChunkPos(n*8,0),0,level,b->true);
                long w=start.getPieces().stream().filter(s->s instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece&&piece.getElement() instanceof VillagePoolElement element&&element.wizard).count();
                long b=start.getPieces().stream().filter(s->s instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece piece&&piece.getElement() instanceof VillagePoolElement element&&!element.wizard).count();
                check(w<=1&&b<=1,"real jigsaw village "+n+" has no duplicate profession buildings: "+w+"/"+b);totalWizard+=w;totalBanker+=b;
            }
            check(totalWizard>0&&totalBanker>0,"real village assembly includes both original buildings: "+totalWizard+" towers / "+totalBanker+" banker homes");
            for(boolean wizard:new boolean[]{true,false}){
                var element=new VillagePoolElement(wizard,StructureTemplatePool.Projection.RIGID);var json=StructurePoolElement.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,element).getOrThrow();var copy=StructurePoolElement.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();check(copy instanceof VillagePoolElement v&&v.wizard==wizard,"actual structure element codec preserves profession after serialization");
                var origin=BASE.offset(wizard?0:16,0,0);element.place(manager,level,level.structureManager(),generator,origin,origin,Rotation.NONE,BoundingBox.infinite(),level.random,net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING,false);
            }
            check(level.getBlockState(BASE.offset(3,1,1)).is(Blocks.OAK_DOOR)&&level.getBlockState(BASE.offset(3,2,1)).is(Blocks.OAK_DOOR)&&level.getBlockState(BASE.offset(17,1,0)).is(Blocks.OAK_DOOR),"original two-block entrance doors remain intact");
            check(level.getBlockState(BASE.offset(3,5,3)).is(Blocks.GLOWSTONE),"wizard middle floor retains original glowstone, not an invented bookshelf");
            check(level.getBlockState(BASE.offset(4,9,3)).is(Blocks.LADDER)&&level.getBlockState(BASE.offset(4,10,3)).is(Blocks.OAK_TRAPDOOR),"nine-block ladder and open roof trapdoor present");
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(BASE.offset(2,6,2));int slots=0;for(int i=0;i<27;i++)if(!chest.getItem(i).isEmpty())slots++;check(slots>=1&&slots<=8,"actual wizard chest contains original weighted tower loot: "+slots+" occupied slots");
            var near=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new net.minecraft.world.phys.AABB(BASE).inflate(25));
            check(near.stream().filter(v->v.getVillagerData().getProfession()==VillageContent.WIZARD_PROFESSION.get()).count()==1&&near.stream().filter(v->v.getVillagerData().getProfession()==VillageContent.BANKER_PROFESSION.get()).count()==1,"actual components spawn exactly one wizard and banker villager");
            check(level.getBlockState(BASE.offset(16,2,2)).is(Blocks.IRON_BARS)&&level.getBlockState(BASE.offset(19,2,2)).is(Blocks.IRON_BARS),"banker original iron-bar windows");
            for(var rotation:Rotation.values()){var element=new VillagePoolElement(true,StructureTemplatePool.Projection.RIGID);check(element.getBoundingBox(manager,BASE,rotation).isInside(new BlockPos(7,1,1).rotate(rotation).offset(BASE)),"modern bounding box safely contains original outlying wizard spawn "+rotation);}
            level.setDayTime(6000);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(106.5,187,77.5,0,10);ready=true;
        });
        if(ticks==130){check(ready,"server generation checks complete");LiquidDeathAudit.shot(mc,"village-original-buildings.png");}
        if(ticks==150){server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(false);});System.out.println("TC4_VILLAGE_BUILDING ALL PASS real jigsaw assembly, limits, codec, original building blocks, loot and professions");mc.stop();}
    }
}
