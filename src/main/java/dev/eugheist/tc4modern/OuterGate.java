package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterGate {
 public static final DeferredHolder<Block,Block> FRAME=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_gate_frame")),BARRIER=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_gate_barrier"));
 public static final DeferredHolder<Block,LockBlock> LOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("eldritch_lock"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Lock>> TYPE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("eldritch_lock"));
 static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.of().strength(50,TC4SoundTypes.res(20000)).sound(SoundType.STONE).noLootTable().isValidSpawn((s,l,p,t)->false);}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{h.register(FRAME.getId(),new Block(properties().strength(-1,Float.MAX_VALUE).lightLevel(s->12)));h.register(BARRIER.getId(),new Block(properties().strength(-1,TC4SoundTypes.res(6000000)).noOcclusion()){@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.empty();}@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return net.minecraft.world.phys.shapes.Shapes.block();}});h.register(LOCK.getId(),new LockBlock());});e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(TYPE.getId(),BlockEntityType.Builder.of(Lock::new,LOCK.get()).build(null)));}
 public static final class LockBlock extends BaseEntityBlock {
  LockBlock(){super(OuterGate.properties().strength(-1,Float.MAX_VALUE).lightLevel(s->5));registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
  @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new LockBlock());}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){if(stack.is(OuterItems.TABLET.get())&&level.getBlockEntity(pos) instanceof Lock lock&&lock.count<0){if(!level.isClientSide){lock.count=0;stack.shrink(1);lock.setChanged();level.sendBlockUpdated(pos,state,state,3);level.playSound(null,pos,FociContent.sound("runicshieldcharge"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TYPE.get(),Lock::tick);}
  @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.util.RandomSource random){if(level.getBlockEntity(pos) instanceof Lock lock&&lock.count>=0)OuterBossClient.lockEffects(pos);}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Lock(p,s);}
 }
 public static final class Lock extends BlockEntity {public int count=-1;Lock(BlockPos p,BlockState s){super(TYPE.get(),p,s);}static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,Lock lock){if(lock.count<0)return;if(++lock.count%5==0)level.playSound(null,pos,FociContent.sound("pump"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);if(lock.count>100&&!level.isClientSide){var server=(net.minecraft.server.level.ServerLevel)level;var data=OuterMazeData.get(server);int cx=pos.getX()>>4,cz=pos.getZ()>>4,centerx=cx,centerz=cz,exit=0;for(int a=-2;a<=2;a++)for(int b=-2;b<=2;b++){var cell=data.cell(cx+a,cz+b);if(cell==null)continue;if(cell.feature==2){centerx=cx+a;centerz=cz+b;}if(cell.feature>=2&&cell.feature<=5&&(cell.north||cell.south||cell.east||cell.west))exit=cell.feature;}OuterBossRooms.create(server,pos,centerx,centerz,exit,data.nextBoss(server.random));for(var p:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2)))if(level.getBlockState(p).is(BARRIER.get()))level.removeBlock(p,false);level.removeBlock(pos,false);level.playSound(null,pos,FociContent.sound("ice"),net.minecraft.sounds.SoundSource.BLOCKS,1,1);}}
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag n,HolderLookup.Provider lookup){super.saveAdditional(n,lookup);n.putShort("count",(short)count);}
 @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag n,HolderLookup.Provider lookup){super.loadAdditional(n,lookup);count=n.contains("count")?n.getShort("count"):-1;}
 @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
 @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 public Direction facing(){return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);}}
}
