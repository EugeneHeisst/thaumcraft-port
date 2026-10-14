package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.*;

/** Native fixtures exercising original tower rolls, embedded nodes, harvesting and world texture selection. */
final class TotemAudit {
    static void check(boolean good,String message){if(!good)throw new IllegalStateException("TC4_TOTEM FAIL "+message);System.out.println("TC4_TOTEM PASS "+message);}
    static void setup(ServerPlayer player,BlockPos base) {
        var level=player.serverLevel();var heights=new HashSet<Integer>();
        for(int seed=0;seed<16;seed++) {
            var ground=base.offset(seed*2-16,-30,30);for(int y=0;y<=6;y++)level.setBlock(ground.above(y),y==0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            check(ObsidianTotems.TotemFeature.build(level,ground,net.minecraft.util.RandomSource.create(seed)),"actual tower generated seed "+seed);
            check(level.getBlockState(ground).is(CrimsonContent.OBSIDIAN_TILE.get()),"original tile foundation "+seed);
            int charged=0,height=0;
            for(int y=1;y<=5;y++)if(ObsidianTotems.isTotem(level.getBlockState(ground.above(y)))) {
                height=y;if(level.getBlockState(ground.above(y)).is(ObsidianTotems.CHARGED.get())) {
                    charged++;var node=(AuraNodeEntity)level.getBlockEntity(ground.above(y));
                    check(node!=null&&node.nodeType==thaumcraft.api.nodes.NodeType.DARK&&node.reservoir.aspects.visSize()>0,"charged body retains real eerie node "+seed);
                    var saved=node.saveWithoutMetadata(level.registryAccess());var copy=new AuraNodeEntity(node.getBlockPos(),node.getBlockState());copy.loadWithComponents(saved,level.registryAccess());
                    check(copy.nodeId.equals(node.nodeId)&&copy.reservoir.aspects.aspects.equals(node.reservoir.aspects.aspects),"embedded node NBT "+seed);
                }
            }
            check(charged==1&&height>=2&&height<=5,"exactly one terminal charged node "+seed);heights.add(height);
            for(int y=0;y<=6;y++)level.removeBlock(ground.above(y),false);
        }
        check(heights.size()>1&&heights.contains(5),"original early-node rolls and forced five-block cap exercised");
        var blocked=base.offset(20,-30,30);level.setBlock(blocked,Blocks.STONE.defaultBlockState(),3);level.setBlock(blocked.above(),Blocks.STONE.defaultBlockState(),3);
        check(!ObsidianTotems.TotemFeature.build(level,blocked,net.minecraft.util.RandomSource.create(1))&&level.getBlockState(blocked).is(Blocks.STONE),"blocked tower refuses without replacing foundation");
        var high=base.offset(64,0,64);for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-1;y<=5;y++)level.setBlock(high.offset(x,y,z),y==-1?Blocks.SAND.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(high.getY()>level.getSeaLevel()&&ObsidianTotems.TotemFeature.placeOnSurface(level,high,net.minecraft.util.RandomSource.create(8)),"actual surface generation accepts a totem above sea level");
        for(int y=-1;y<=6;y++)level.setBlock(high.above(y),y==-1?Blocks.SAND.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var desert=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.DESERT);
        for(int cx=(high.getX()>>4)-1;cx<=(high.getX()>>4)+1;cx++)for(int cz=(high.getZ()>>4)-1;cz<=(high.getZ()>>4)+1;cz++)level.getChunk(cx,cz).fillBiomesFromNoise((x,y,z,sampler)->desert,level.getChunkSource().randomState().sampler());
        check(level.getBiome(high).is(net.minecraft.world.level.biome.Biomes.DESERT),"desert fixture includes neighbouring biome samples");
        boolean flowers=false;
        for(int seed=0;seed<150&&!flowers;seed++)flowers=new CinderpearlFeature().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(seed),high,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        check(flowers,"actual cinderpearl feature generates hot-sand flowers above sea level");
        var nodePos=base.offset(0,0,6);level.setBlock(nodePos,ObsidianTotems.CHARGED.get().defaultBlockState(),3);var node=(AuraNodeEntity)level.getBlockEntity(nodePos);
        node.reservoir.setAspects(new AspectList().add(Aspect.AIR,20).add(Aspect.FIRE,4));
        var ordinary=base.offset(10,0,6);level.removeBlock(ordinary,false);
        check(AuraNodeFeature.createRandomNodeAt(level,ordinary,net.minecraft.util.RandomSource.create(9),false,false,false)&&level.getBlockState(ordinary).is(TC4Modern.AURA_NODE.get()),"ordinary air node generation still places an airy node");level.removeBlock(ordinary,false);level.setBlock(ordinary,Blocks.STONE.defaultBlockState(),3);
        check(!AuraNodeFeature.createRandomNodeAt(level,ordinary,net.minecraft.util.RandomSource.create(9),false,false,false)&&level.getBlockState(ordinary).is(Blocks.STONE),"node helper preserves occupied non-node blocks");level.removeBlock(ordinary,false);
        player.teleportTo(level,nodePos.getX()+.5,nodePos.getY()-1.12,nodePos.getZ()-2,0,0);
        var scan=ScanTarget.look(player);check(scan!=null&&scan.knowledgeKey().equals("node:"+node.nodeId)&&scan.aspects().equals(Map.of("aer",4,"ignis",4)),"actual raycast scans embedded reservoir instead of stone item");
        var wand=WandParts.make(WandParts.CAPS.get("iron"),WandParts.RODDEFS.get("wood"));player.getInventory().selected=0;player.getInventory().setItem(0,wand);
        var research=ResearchProgress.completed(player.getPersistentData());int tap=1+(research.contains("NODETAPPER1")?1:0)+(research.contains("NODETAPPER2")?1:0);
        int before=node.reservoir.aspects.visSize();wand.getItem().use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);wand.getItem().onUseTick(level,player,wand,72000);player.stopUsingItem();
        check(Aspect.getPrimalAspects().stream().mapToInt(a->WandData.get(wand,a)).sum()==tap*100&&node.reservoir.aspects.visSize()==before-tap,"actual wand use drains embedded node and charges exact vis");
        node.reservoir.setAspects(new AspectList().add(Aspect.AIR,20).add(Aspect.FIRE,4));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var state=level.getBlockState(nodePos);state.getBlock().playerWillDestroy(level,nodePos,state,player);
        var essences=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(nodePos).inflate(1)).stream().filter(e->e.getItem().is(BasicsContent.WISP_ESSENCE.get())).toList();
        check(essences.stream().mapToInt(e->e.getItem().getCount()).sum()==3&&essences.stream().allMatch(e->EssentiaItems.getAspects(e.getItem()).getAmount(Aspect.AIR)==2),"charged node harvest uses original 20 Air -> three essences, four Fire -> none");for(var e:essences)e.discard();
        var drops=Block.getDrops(state,level,nodePos,null);check(drops.size()==1&&drops.getFirst().is(CrimsonContent.OBSIDIAN_TILE.get().asItem()),"charged totem drops obsidian tile");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);state.getBlock().playerWillDestroy(level,nodePos,state,player);
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(nodePos).inflate(1)).stream().noneMatch(e->e.getItem().is(BasicsContent.WISP_ESSENCE.get())),"creative charged break does not release essentia");
        for(int y=0;y<4;y++)level.setBlock(base.offset(-6,y,5),y==3?ObsidianTotems.CHARGED.get().defaultBlockState():ObsidianTotems.PLAIN.get().defaultBlockState(),3);
        var cap=(AuraNodeEntity)level.getBlockEntity(base.offset(-6,3,5));cap.reservoir.setAspects(new AspectList().add(Aspect.ENTROPY,30));cap.nodeType=thaumcraft.api.nodes.NodeType.DARK;cap.changed();
        level.setBlock(base.offset(6,0,5),ObsidianTotems.PLAIN.get().defaultBlockState(),3);level.removeBlock(nodePos,false);
        var plain=BlockAspectBridge.get(ObsidianTotems.PLAIN.get().defaultBlockState(),level);check(plain.equals(Map.of("terra",4,"tenebrae",2,"alienis",2)),"original ordinary totem aspects");
    }
    static void render(Minecraft mc,BlockPos base) {
        for(var pos:List.of(base.offset(-6,0,5),base.offset(-6,3,5),base.offset(6,0,5))) {
            var state=mc.level.getBlockState(pos);var model=mc.getBlockRenderer().getBlockModel(state);
            var data=model.getModelData(mc.level,pos,state,net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
            for(var face:Direction.values()) {
                String texture="obsidiantile";
                if(face.getAxis()!=Direction.Axis.Y)texture=ObsidianTotems.isTotem(mc.level.getBlockState(pos.above()))?"obsidiantotembaseshaded":!ObsidianTotems.isTotem(mc.level.getBlockState(pos.below()))?"obsidiantotembase":"obsidiantotem"+(1+Math.abs((face.get3DDataValue()+pos.getX()%4+pos.getZ()%4+pos.getY()%4)%4));
                var wanted=AlchemyContent.id("blocks/"+texture);var quads=model.getQuads(state,face,net.minecraft.util.RandomSource.create(),data,null);
                check(!quads.isEmpty()&&quads.stream().allMatch(q->q.getSprite().contents().name().equals(wanted)),"original world texture "+pos+" "+face);
            }
        }
        check(mc.level.getBlockEntity(base.offset(-6,3,5)) instanceof AuraNodeEntity,"charged node arrives and persists on client");
        System.out.println("TC4_TOTEM ALL PASS generation, embedded-node NBT, harvest, original drops and context textures");
    }
}
