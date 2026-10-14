package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** BlockMetalDevice5/6, BlockCosmeticSolid2/3 and TileWardingStone/Fence. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ArtificeDevices {
 public static java.util.function.BiConsumer<Level,BlockPos> travelSparkles=(l,p)->{},wardRunes=(l,p)->{};
 public static final DeferredHolder<Item,Item> MIRROR_GLASS=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("mirror_glass"));
 public static final DeferredHolder<Block,GrateBlock> GRATE=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("grate"));
 public static final DeferredHolder<Block,Block> TRAVEL=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("paving_stone_travel"));
 public static final DeferredHolder<Block,WardBlock> WARD=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("paving_stone_warding"));
 public static final DeferredHolder<Block,FenceBlock> FENCE=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("warding_barrier"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<GrateEntity>> GRATE_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("grate"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<WardEntity>> WARD_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("paving_stone_warding"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<FenceEntity>> FENCE_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("warding_barrier"));
 static Block.Properties stone(){return Block.Properties.of().strength(2,TC4SoundTypes.res(10)).sound(SoundType.STONE).requiresCorrectToolForDrops();}
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(Registries.BLOCK,h->{h.register(GRATE.getId(),new GrateBlock(Block.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape()));h.register(TRAVEL.getId(),new TravelBlock(stone().lightLevel(s->9).isValidSpawn((s,l,p,t)->false)));h.register(WARD.getId(),new WardBlock(stone().isValidSpawn((s,l,p,t)->false)));h.register(FENCE.getId(),new FenceBlock(Block.Properties.of().replaceable().noOcclusion().dynamicShape().noLootTable().strength(-1,0)));});
  event.register(Registries.ITEM,h->{h.register(MIRROR_GLASS.getId(),new Item(new Item.Properties()));for(var b:java.util.List.of(GRATE,TRAVEL,WARD))h.register(b.getId(),new BlockItem(b.get(),new Item.Properties()));});
  event.register(Registries.BLOCK_ENTITY_TYPE,h->{h.register(GRATE_ENTITY.getId(),BlockEntityType.Builder.of(GrateEntity::new,GRATE.get()).build(null));h.register(WARD_ENTITY.getId(),BlockEntityType.Builder.of(WardEntity::new,WARD.get()).build(null));h.register(FENCE_ENTITY.getId(),BlockEntityType.Builder.of(FenceEntity::new,FENCE.get()).build(null));});
 }
 @SubscribeEvent static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,GRATE_ENTITY.get(),(tile,side)->side==Direction.UP?tile.handler:null);}
 static final class TravelBlock extends Block {
  TravelBlock(Properties p){super(p);}
  @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){if(entity instanceof LivingEntity living){if(level.isClientSide)travelSparkles.accept(level,pos);living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1));living.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0));}super.stepOn(level,pos,state,entity);}
 }
 public static final class GrateBlock extends BaseEntityBlock {
  public static final BooleanProperty CLOSED=BooleanProperty.create("closed");public static final MapCodec<GrateBlock> CODEC=simpleCodec(GrateBlock::new);static final VoxelShape SHAPE=Block.box(0,13,0,16,16,16);
  GrateBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(CLOSED,false));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CLOSED);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
  @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return !s.getValue(CLOSED)&&c instanceof EntityCollisionContext ec&&ec.getEntity() instanceof ItemEntity?Shapes.empty():SHAPE;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GrateEntity(p,s);}
  void toggle(Level l,BlockPos p,BlockState s,boolean closed){if(s.getValue(CLOSED)!=closed){l.setBlock(p,s.setValue(CLOSED,closed),3);l.levelEvent(1003,p,0);}}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide)toggle(l,p,s,!s.getValue(CLOSED));return InteractionResult.sidedSuccess(l.isClientSide);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide)toggle(l,p,s,l.hasNeighborSignal(p));}
 }
 public static final class GrateEntity extends BlockEntity {
  GrateEntity(BlockPos p,BlockState s){super(GRATE_ENTITY.get(),p,s);}
  final net.neoforged.neoforge.items.IItemHandler handler=new net.neoforged.neoforge.items.IItemHandler(){
   public int getSlots(){return 1;}public ItemStack getStackInSlot(int slot){return ItemStack.EMPTY;}public int getSlotLimit(int slot){return 64;}public boolean isItemValid(int slot,ItemStack stack){return !getBlockState().getValue(GrateBlock.CLOSED);}
   public ItemStack extractItem(int slot,int count,boolean simulate){return ItemStack.EMPTY;}
   public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(stack.isEmpty()||!isItemValid(slot,stack))return stack;int n=Math.min(64,stack.getCount());if(!simulate&&!level.isClientSide){var item=new ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY()+.6,worldPosition.getZ()+.5,stack.copyWithCount(n));item.setDeltaMovement(0,-.1,0);level.addFreshEntity(item);}return stack.copyWithCount(stack.getCount()-n);}
  };
 }
 public static final class WardBlock extends BaseEntityBlock {
  static final MapCodec<WardBlock> CODEC=simpleCodec(WardBlock::new);WardBlock(Properties p){super(p);}
  @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource random){wardRunes.accept(l,p);}@Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WardEntity(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,WARD_ENTITY.get(),WardEntity::tick);}
 }
 public static final class WardEntity extends BlockEntity {
  int count;WardEntity(BlockPos p,BlockState s){super(WARD_ENTITY.get(),p,s);}
  static void tick(Level l,BlockPos p,BlockState s,WardEntity tile){if(tile.count==0)tile.count=l.random.nextInt(100);if(tile.count%5==0&&!l.hasNeighborSignal(p))for(var e:l.getEntitiesOfClass(LivingEntity.class,new AABB(p.getX(),p.getY(),p.getZ(),p.getX()+1,p.getY()+3,p.getZ()+1).inflate(.1))){if(e.onGround()||e instanceof Player)continue;float angle=(e.getYRot()+180)*(float)Math.PI/180;e.push(-net.minecraft.util.Mth.sin(angle)*.2f,-.1,net.minecraft.util.Mth.cos(angle)*.2f);}
   // Original compares Airy metadata3 then places4; replacement checks make this idempotent.
   if(++tile.count%100==0)for(int y=1;y<=2;y++){var to=p.above(y);if(l.getBlockState(to).canBeReplaced())l.setBlock(to,FENCE.get().defaultBlockState(),3);}
  }
 }
 public static final class FenceBlock extends BaseEntityBlock {
  static final MapCodec<FenceBlock> CODEC=simpleCodec(FenceBlock::new);FenceBlock(Properties p){super(p);}@Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}@Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(c instanceof EntityCollisionContext ec&&ec.getEntity() instanceof LivingEntity&&!(ec.getEntity() instanceof Player)){var below=p.below(l.getBlockState(p.below()).is(WARD.get())?1:2);if(l instanceof Level level&&!level.hasNeighborSignal(below))return Shapes.block();}return Shapes.empty();}
  @Override public net.minecraft.world.level.pathfinder.PathType getBlockPathType(BlockState state,BlockGetter l,BlockPos p,net.minecraft.world.entity.Mob mob){for(int y=1;y<=2;y++)if(l.getBlockState(p.below(y)).is(WARD.get())&&l instanceof Level level&&level.hasNeighborSignal(p.below(y)))return net.minecraft.world.level.pathfinder.PathType.OPEN;return net.minecraft.world.level.pathfinder.PathType.BLOCKED;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new FenceEntity(p,s);}@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,FENCE_ENTITY.get(),FenceEntity::tick);}
 }
 public static final class FenceEntity extends BlockEntity {
  int count;FenceEntity(BlockPos p,BlockState s){super(FENCE_ENTITY.get(),p,s);}static void tick(Level l,BlockPos p,BlockState s,FenceEntity tile){if(tile.count==0)tile.count=l.random.nextInt(100);if(++tile.count%100==0&&!l.getBlockState(p.below()).is(WARD.get())&&!l.getBlockState(p.below(2)).is(WARD.get()))l.removeBlock(p,false);}
 }
}
