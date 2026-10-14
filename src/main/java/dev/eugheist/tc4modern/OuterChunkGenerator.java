package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original all-air provider, populated solely from the saved labyrinth's cell diagrams. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterChunkGenerator extends FlatLevelSource {
 public static final MapCodec<OuterChunkGenerator> CODEC=RecordCodecBuilder.mapCodec(i->i.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(OuterChunkGenerator::settings)).apply(i,OuterChunkGenerator::new));
 public OuterChunkGenerator(FlatLevelGeneratorSettings settings){super(settings);}
 @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
 @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent e){e.register(Registries.CHUNK_GENERATOR,h->h.register(AlchemyContent.id("outer_lands"),CODEC));}
 @Override public void applyBiomeDecoration(WorldGenLevel world,ChunkAccess chunk,StructureManager structures){var pos=chunk.getPos();var cell=OuterMazeData.get(world.getLevel()).cell(pos.x,pos.z);if(cell==null)return;generate(world,pos.x,pos.z,cell);}
 static void generate(WorldGenLevel world,int cx,int cz,thaumcraft.common.lib.world.dim.Cell cell){
  var random=new Random(world.getSeed());long xSeed=random.nextLong()/2*2+1,zSeed=random.nextLong()/2*2+1;random.setSeed((cx*xSeed+cz*zSeed)^world.getSeed());
  try{switch(cell.feature){
   case 1->OuterGenPortal.generatePortal(world,random,cx,cz,50,cell);
   case 2,3,4,5->OuterGenBossRoom.generateRoom(world,random,cx,cz,50,cell);
   case 6->OuterGenKeyRoom.generateRoom(world,random,cx,cz,50,cell);
   case 7->OuterGenNestRoom.generateRoom(world,random,cx,cz,50,cell);
   case 8->OuterGenLibraryRoom.generateRoom(world,random,cx,cz,50,cell);
   default->OuterGenPassage.generateDefaultPassage(world,random,cx,cz,50,cell);
  }OuterGenCommon.processDecorations(world);}finally{OuterGenCommon.QUEUES.remove();}
 }
 @Override public int getSpawnHeight(LevelHeightAccessor level){return 53;}
}
