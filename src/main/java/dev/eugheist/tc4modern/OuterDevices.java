package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterDevices {
 public static final DeferredHolder<Block,DeviceBlock> TRAP=block("runed_stone"),VENT=block("crusted_opening");
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Device>> TYPE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("outer_device"));
 static DeferredHolder<Block,DeviceBlock> block(String name){return DeferredHolder.create(Registries.BLOCK,AlchemyContent.id(name));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{h.register(TRAP.getId(),new DeviceBlock(false));h.register(VENT.getId(),new DeviceBlock(true));});e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(TYPE.getId(),BlockEntityType.Builder.of(Device::new,TRAP.get(),VENT.get()).build(null)));}
 public static final class DeviceBlock extends BaseEntityBlock {
  final boolean vent;
  /* BlockEldritch metadata 9/10 override hardness to 15 and explosion resistance to 30. */
  DeviceBlock(boolean vent){super(Properties.of().strength(15,30).sound(SoundType.STONE).noOcclusion().noLootTable().lightLevel(s->vent?4:0).isValidSpawn((s,l,p,t)->false));this.vent=vent;registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.DOWN));}
  @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new DeviceBlock(vent));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING);}
  @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
  @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Device(pos,state);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TYPE.get(),Device::tick);}
  @Override public int getExpDrop(BlockState state,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockEntity be,net.minecraft.world.entity.Entity breaker,net.minecraft.world.item.ItemStack tool){return vent?6+level.getRandom().nextInt(5):1+level.getRandom().nextInt(4);}
 }
 public static final class Device extends BlockEntity {
  public int count,ticks,venting;boolean initialized;
  Device(BlockPos pos,BlockState state){super(TYPE.get(),pos,state);count=state.is(VENT.get())?150:20;}
  public Direction facing(){return getBlockState().getValue(BlockStateProperties.FACING);}
  public void face(Direction direction){level.setBlock(worldPosition,getBlockState().setValue(BlockStateProperties.FACING,direction),3);}
  static void tick(Level level,BlockPos pos,BlockState state,Device device){boolean vent=state.is(VENT.get());if(!device.initialized){device.initialized=true;device.ticks=level.random.nextInt(500);}device.ticks++;
   if(level.isClientSide){if(vent){if(device.venting>0){device.venting--;for(int i=0;i<3;i++)OuterDevicesClient.vent(device);}else if(level.random.nextInt(20)==0)OuterDevicesClient.vent(device);}return;}
   var server=(ServerLevel)level;
   if(!vent){if(device.count--<=0){device.count=10+level.random.nextInt(25);var p=level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,false);if(p!=null){p.hurt(level.damageSources().magic(),2);if(level.random.nextBoolean()&&p instanceof net.minecraft.server.level.ServerPlayer player)PlayerWarp.add(player,WarpState.Kind.TEMPORARY,1+level.random.nextInt(2));net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server,null,pos.getX(),pos.getY(),pos.getZ(),32,new TrapZap(pos,p.getId()));}}return;}
   if(--device.count<0){device.count=50+level.random.nextInt(50);return;}
   boolean active=level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16,false)!=null;
   boolean full=level.getEntitiesOfClass(EldritchCrab.class,new net.minecraft.world.phys.AABB(pos).inflate(32)).size()>5;
   if(active&&!full&&device.count==15){level.blockEvent(pos,state.getBlock(),1,0);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);}
   if(active&&!full&&device.count<=0){device.count=150+level.random.nextInt(100);var direction=device.facing();var crab=CrabContent.CRAB.get().create(level);var at=pos.relative(direction);crab.moveTo(at.getX()+.5,at.getY()+.5,at.getZ()+.5,0,0);crab.finalizeSpawn(server,server.getCurrentDifficultyAt(at),net.minecraft.world.entity.MobSpawnType.SPAWNER,null);crab.setHelm(false);crab.setDeltaMovement(direction.getStepX()*.2,direction.getStepY()*.2,direction.getStepZ()*.2);level.addFreshEntity(crab);level.playSound(null,pos,FociContent.sound("gore"),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);}
  }
  @Override public boolean triggerEvent(int id,int value){if(id==1){venting=20;return true;}return super.triggerEvent(id,value);}
 }
 public record TrapZap(BlockPos from,int entity) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
  static final Type<TrapZap> TYPE=new Type<>(AlchemyContent.id("outer_trap_zap"));
  static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf,TrapZap> CODEC=net.minecraft.network.codec.StreamCodec.of((b,p)->{b.writeBlockPos(p.from);b.writeVarInt(p.entity);},b->new TrapZap(b.readBlockPos(),b.readVarInt()));
  public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
 }
 @SubscribeEvent static void payloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("tc4-outer-1").playToClient(TrapZap.TYPE,TrapZap.CODEC,(p,c)->c.enqueueWork(()->OuterDevicesClient.zap(p)));}
}
