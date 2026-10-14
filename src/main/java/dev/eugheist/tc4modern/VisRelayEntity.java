package dev.eugheist.tc4modern;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNearbyRules;
import thaumcraft.api.visnet.VisParentRules;
import thaumcraft.common.tiles.VisRelayState;
/** Modern lifecycle host for original TileVisRelay. Registration/rendering supplied by relay block. */
public class VisRelayEntity extends BlockEntity implements VisNetwork.RelayNode {
 public final VisRelayState relay=new VisRelayState();
 private WeakReference<VisParentRules.Node> parent;
 private static final java.util.Map<ServerLevel,java.util.Map<Integer,WeakReference<VisRelayEntity>>> NEARBY_PLAYERS=new java.util.WeakHashMap<>();
 /** Original TileVisRelay radius-five nearest-player registry, scoped to the modern world. */
 public static VisRelayEntity nearbyRelay(net.minecraft.world.entity.player.Player player){
  if(!(player.level() instanceof ServerLevel server))return null;
  var entries=NEARBY_PLAYERS.get(server);var ref=entries==null?null:entries.get(player.getId());
  var relay=ref==null?null:ref.get();return relay!=null&&relay.valid()?relay:null;
 }
 private void registerNearbyPlayers(ServerLevel server){
  var entries=NEARBY_PLAYERS.computeIfAbsent(server,k->new java.util.HashMap<>());
  entries.entrySet().removeIf(e->{var tile=e.getValue().get();return tile==null||!tile.valid()||server.getEntity(e.getKey())==null;});
  for(var player:server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new net.minecraft.world.phys.AABB(worldPosition).inflate(5))){
   var old=nearbyRelay(player);
   if(old!=null&&old.getBlockPos().distToCenterSqr(player.position())<worldPosition.distToCenterSqr(player.position()))continue;
   entries.put(player.getId(),new WeakReference<>(this));
  }
 }
 private int counter;
 public static java.util.function.Consumer<VisRelayEntity> clientBeamTick=tile->{};
 Object clientBeam;
 int clientBeamAge;
 private boolean clientBeamConnected;
 private boolean refresh=true;
 public VisRelayEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
 public int x(){return worldPosition.getX();}public int y(){return worldPosition.getY();}public int z(){return worldPosition.getZ();}
 public int range(){return 8;}public int attunement(){return relay.color;}
 public boolean loaded(){return level!=null&&level.hasChunkAt(worldPosition);}
 public boolean valid(){return !isRemoved()&&loaded()&&level.getBlockEntity(worldPosition)==this;}
 public Iterable<VisNearbyRules.Node> children(){return level instanceof ServerLevel server?VisNetwork.children(server,this):java.util.List.of();}
 public VisParentRules.Node parent(){var node=parent==null?null:parent.get();return node!=null&&node.valid()?node:null;}
 public void disconnected(){parent=null;relay.px=relay.py=relay.pz=0;refresh=true;changed();}
 private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 public void cycleAttunement(){if(level instanceof ServerLevel server){relay.cycleColor();VisNetwork.detach(server,this);changed();crystalSound();}}
 public void setShardAttunement(int color){if(level instanceof ServerLevel server){relay.color=(byte)(color==relay.color||color==6?-1:color);VisNetwork.detach(server,this);changed();crystalSound();}}
 private void crystalSound(){level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"crystal")),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1f);}
 public int consumeVis(Aspect aspect,int amount){
  var node=parent();if(!valid()||node==null)return 0;
  int taken=node.consumeVis(aspect,amount);
  if(taken>0){int color=relay.consumePulse(aspect);if(color>=0)level.blockEvent(worldPosition,getBlockState().getBlock(),0,color);}
  return taken;
 }
 public static void tick(Level level,BlockPos pos,BlockState state,VisRelayEntity tile){
  short facing=(short)state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING).get3DDataValue();
  if(tile.relay.orientation!=facing){tile.relay.orientation=facing;tile.changed();}
  tile.relay.tick(level.isClientSide);
  if(level.isClientSide&&tile.refresh){
   tile.parent=null;
   if(tile.relay.px!=0||tile.relay.py!=0||tile.relay.pz!=0){
    var parentPos=pos.offset(-tile.relay.px,-tile.relay.py,-tile.relay.pz);
    if(level.hasChunkAt(parentPos)&&level.getBlockEntity(parentPos) instanceof VisParentRules.Node node)tile.parent=new WeakReference<>(node);
   }
   tile.refresh=false;
  }
  if(level.isClientSide){
   clientBeamTick.accept(tile);
   boolean connected=tile.parent()!=null;
   if(connected){if(!tile.clientBeamConnected)tile.clientBeamAge=0;else tile.clientBeamAge++;}
   tile.clientBeamConnected=connected;
  }
  if(level instanceof ServerLevel server&&tile.counter%20==0)tile.registerNearbyPlayers(server);
  if(level instanceof ServerLevel server&&(tile.counter++%40==0||tile.refresh)){
   var oldParent=tile.parent();
   if(oldParent!=null&&!VisNetwork.visible(server,tile,oldParent))VisNetwork.detach(server,tile);
   for(var child:tile.children())if(!child.valid()||!VisNetwork.visible(server,tile,child)){VisNetwork.detach(server,tile);break;}
   if(tile.refresh||tile.parent()==null){
    tile.parent=VisNetwork.attach(server,tile);var parent=tile.parent();
    if(parent!=null){tile.relay.px=tile.x()-parent.x();tile.relay.py=tile.y()-parent.y();tile.relay.pz=tile.z()-parent.z();}
    tile.refresh=false;tile.changed();
   }
  }
 }
 @Override public void setRemoved(){if(level instanceof ServerLevel server)VisNetwork.detach(server,this);super.setRemoved();}
 @Override public void onChunkUnloaded(){if(level instanceof ServerLevel server)VisNetwork.detach(server,this);super.onChunkUnloaded();}
 @Override public boolean triggerEvent(int id,int value){if(id==0){if(level!=null&&level.isClientSide){
   relay.receivePulse(value);
   var visited=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<VisRelayEntity,Boolean>());
   visited.add(this);var ancestor=parent();
   while(ancestor instanceof VisRelayEntity upstream&&upstream.relay.pulse==0&&visited.add(upstream)){
    upstream.relay.receivePulse(value);ancestor=upstream.parent();
   }
  }return true;}return super.triggerEvent(id,value);}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);relay.write(tag);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);relay.read(tag);parent=null;refresh=true;VisNetwork.invalidate();}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
