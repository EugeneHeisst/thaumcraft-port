package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterPortal {
 public static final DeferredHolder<Block,PortalBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_portal"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Portal>> TYPE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("eldritch_portal"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new PortalBlock()));e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(TYPE.getId(),BlockEntityType.Builder.of(Portal::new,BLOCK.get()).build(null)));}
 public static final class PortalBlock extends BaseEntityBlock {
  PortalBlock(){super(Properties.of().strength(-1,200000).noCollission().noOcclusion().noLootTable().lightLevel(s->15).isValidSpawn((s,l,p,t)->false));}
  @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PortalBlock());}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Portal(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,TYPE.get(),Portal::tick);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moved){if(!EldritchBlock.isStructure(l.getBlockState(p.above()).getBlock())||!EldritchBlock.isStructure(l.getBlockState(p.below()).getBlock()))l.removeBlock(p,false);}
 }
 public static final class Portal extends BlockEntity {
  public int opencount=-1;int count;
  Portal(BlockPos p,BlockState s){super(TYPE.get(),p,s);}
  static void tick(Level world,BlockPos pos,BlockState state,Portal tile){tile.count++;
   if(world.isClientSide){if(tile.opencount<30)tile.opencount++;if(tile.count%250==0)world.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,FociContent.sound("evilportal"),net.minecraft.sounds.SoundSource.BLOCKS,1,1,false);return;}
   if(tile.count%5!=0)return;
   for(var player:world.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(.5,1,.5))){if(player.isPassenger()||player.isVehicle())continue;if(player.isOnPortalCooldown()){player.setPortalCooldown(100);continue;}transfer(player,pos);}
  }
 }
 static void transfer(ServerPlayer player,BlockPos source){
  boolean entering=player.level().dimension()!=OuterDimension.KEY;var destination=player.server.getLevel(entering?OuterDimension.KEY:Level.OVERWORLD);if(destination==null)return;
  BlockPos target;
  if(entering){var cp=new ChunkPos(source);var cell=OuterMazeData.get(destination).cell(cp.x,cp.z);if(cell==null||cell.feature!=1)return;target=new BlockPos(cp.getMinBlockX()+8,53,cp.getMinBlockZ()+8);destination.getChunk(cp.x,cp.z);if(!destination.getBlockState(target).is(BLOCK.get())){OuterGenPortal.generatePortal(destination,new java.util.Random(destination.getSeed()^cp.toLong()),cp.x,cp.z,50,cell);OuterGenCommon.processDecorations(destination);}}
  else {target=findReturnPortal(destination,source);if(target==null){var spawn=destination.getSharedSpawnPos();player.setPortalCooldown(100);player.teleportTo(destination,spawn.getX()+.5,spawn.getY()+1,spawn.getZ()+.5,player.getYRot(),player.getXRot());return;}}
  player.setPortalCooldown(100);player.setDeltaMovement(Vec3.ZERO);player.teleportTo(destination,target.getX()+.5+(destination.random.nextBoolean()?1:-1),target.getY(),target.getZ()+.5+(destination.random.nextBoolean()?1:-1),player.getYRot(),player.getXRot());
  if(entering&&ResearchProgress.complete(player,"ENTEROUTER")){ThaumometerItem.sync(player,"research_complete");net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new ResearchCompletedPayload("ENTEROUTER"));}
 }
 private static BlockPos findReturnPortal(ServerLevel level,BlockPos source){BlockPos closest=null;double distance=Double.MAX_VALUE;var cp=new ChunkPos(source);
  // Scan only the original 128-block search area, using each chunk's block entities instead of 17 million block reads.
  for(int x=cp.x-8;x<=cp.x+8;x++)for(int z=cp.z-8;z<=cp.z+8;z++)for(var entry:level.getChunk(x,z).getBlockEntities().entrySet())if(entry.getValue() instanceof Portal){var p=entry.getKey();if(Math.abs(p.getX()-source.getX())>128||Math.abs(p.getZ()-source.getZ())>128)continue;double d=p.distSqr(source);if(d<distance){distance=d;closest=p;}}
  return closest;
 }
}
