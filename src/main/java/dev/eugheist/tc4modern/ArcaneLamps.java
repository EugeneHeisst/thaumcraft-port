package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;
/** Original TileArcaneLamp/Growth/Fertility, with modern scheduled random-growth bridge. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ArcaneLamps {
 static final DeferredHolder<Block,LampBlock> LAMP=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_lamp")),GROWTH=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("lamp_growth")),FERTILITY=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("lamp_fertility"));
 static final DeferredHolder<Block,Block> LIGHT=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("lamp_light")),AMBER=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("amber_block")),AMBER_BRICKS=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("amber_bricks"));
 static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LampEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("arcane_lamp"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{for(var b:List.of(LAMP,GROWTH,FERTILITY))h.register(b.getId(),new LampBlock(Block.Properties.of().strength(3,TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(b==LAMP?s->15:s->s.getValue(LampBlock.LIT)?15:8)));h.register(LIGHT.getId(),new LightBlock(Block.Properties.of().noCollission().noOcclusion().replaceable().air().noLootTable().lightLevel(s->15)));for(var amber:List.of(AMBER,AMBER_BRICKS))h.register(amber.getId(),new AmberBlock(Block.Properties.of().strength(1.5f,TC4SoundTypes.res(5)).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));});e.register(Registries.ITEM,h->{for(var b:List.of(LAMP,GROWTH,FERTILITY,AMBER,AMBER_BRICKS))h.register(b.getId(),new BlockItem(b.get(),new Item.Properties()));});e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(LampEntity::new,LAMP.get(),GROWTH.get(),FERTILITY.get()).build(null)));}
 static final class AmberBlock extends HalfTransparentBlock {AmberBlock(Properties p){super(p);}@Override protected int getLightBlock(BlockState s,BlockGetter l,BlockPos p){return 3;}}
 static final class LightBlock extends Block {LightBlock(Properties p){super(p);}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}@Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}}
 static final class LampBlock extends BaseEntityBlock {
  static final MapCodec<LampBlock> CODEC=simpleCodec(LampBlock::new);static final BooleanProperty LIT=BlockStateProperties.LIT;
  LampBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.DOWN).setValue(LIT,false));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.FACING,LIT);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace().getOpposite()).setValue(LIT,this==LAMP.get());}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}@Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(4,2,4,12,14,12);}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new LampEntity(p,s);}@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ENTITY.get(),LampEntity::tick);}
  @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&s.is(LAMP.get())&&!l.isClientSide)for(int x=-15;x<=15;x++)for(int y=-15;y<=15;y++)for(int z=-15;z<=15;z++){var at=p.offset(x,y,z);if(l.getBlockState(at).is(LIGHT.get()))l.removeBlock(at,false);}super.onRemove(s,l,p,next,moving);}
 }
 static final class LampEntity extends BlockEntity implements IEssentiaTransport {
  int charges=-1,count,drawDelay,cursor;boolean reserve;final List<BlockPos> checklist=new ArrayList<>();BlockPos last=BlockPos.ZERO,pending;BlockState lastState=Blocks.AIR.defaultBlockState();long due;
  LampEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);charges=s.is(GROWTH.get())?-1:0;}
  boolean growth(){return getBlockState().is(GROWTH.get());}boolean normal(){return getBlockState().is(LAMP.get());}Direction facing(){return getBlockState().getValue(BlockStateProperties.FACING);}
  static void tick(Level l,BlockPos p,BlockState s,LampEntity t){
   if(t.normal()){t.lightTick();return;}
   if(t.growth()){
    if(t.pending!=null&&l.getGameTime()>=t.due){var target=l.getBlockState(t.pending);if(target.isRandomlyTicking())target.randomTick((ServerLevel)l,t.pending,l.random);t.pending=null;}
    if(t.charges<=0){if(t.reserve){t.charges=100;t.reserve=false;}else if(t.drawEssentia())t.charges=100;}
    if(!t.reserve&&t.drawEssentia())t.reserve=true;if(t.charges==0)t.charges=-1;
    if(t.charges>0)t.updatePlant();
   }else{if(t.charges<4&&t.drawEssentia())t.charges++;if(t.charges>1&&t.count++%300==0)t.updateAnimals();}
   boolean lit=t.charges>0;if(s.getValue(LampBlock.LIT)!=lit)l.setBlock(p,s.setValue(LampBlock.LIT,lit),3);t.setChanged();
  }
  void lightTick(){var r=level.random;int x=worldPosition.getX()+r.nextInt(16)-r.nextInt(16),y=worldPosition.getY()+r.nextInt(16)-r.nextInt(16),z=worldPosition.getZ()+r.nextInt(16)-r.nextInt(16);y=Math.max(5,Math.min(y,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+4));var p=new BlockPos(x,y,z);if(level.isEmptyBlock(p)&&!level.getBlockState(p).is(LIGHT.get())&&level.getMaxLocalRawBrightness(p)<9)level.setBlock(p,LIGHT.get().defaultBlockState(),3);if(!getBlockState().getValue(LampBlock.LIT))level.setBlock(worldPosition,getBlockState().setValue(LampBlock.LIT,true),3);}
  boolean drawEssentia(){if(++drawDelay%5!=0)return false;var face=facing();var source=EssentiaTransport.connectable(level,worldPosition,face);return source!=null&&source.canOutputTo(face.getOpposite())&&source.getSuctionAmount(face.getOpposite())<getSuctionAmount(face)&&source.takeEssentia(growth()?Aspect.PLANT:Aspect.LIFE,1,face.getOpposite())==1;}
  boolean isPlant(BlockPos p){var s=level.getBlockState(p);var b=s.getBlock();return !(b instanceof GrassBlock)&&(b instanceof BonemealableBlock||b instanceof SugarCaneBlock||b instanceof CactusBlock||b instanceof BushBlock||b instanceof ManaPodBlock||b instanceof VineBlock);}
  boolean grown(BlockPos p){var s=level.getBlockState(p);var b=s.getBlock();if(b instanceof ManaPodBlock)return s.getValue(ManaPodBlock.AGE)==7;if(b instanceof CropBlock crop)return crop.isMaxAge(s);if(b instanceof NetherWartBlock)return s.getValue(NetherWartBlock.AGE)>=3;if(b instanceof CocoaBlock)return s.getValue(CocoaBlock.AGE)>=2;if(b instanceof SugarCaneBlock||b instanceof CactusBlock)return level.getBlockState(p.below()).is(b);if(b instanceof StemBlock)return false;return b instanceof BonemealableBlock plant&&!plant.isValidBonemealTarget(level,p,s);}
  void updatePlant(){
   if(lastState!=level.getBlockState(last)){if(level.getNearestPlayer(last.getX(),last.getY(),last.getZ(),32,false)!=null)FociWorld.sparkle((ServerLevel)level,last,0x40ff40,7);lastState=level.getBlockState(last);}
   if(cursor>=checklist.size()){checklist.clear();cursor=0;for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)checklist.add(worldPosition.offset(x,6,z));Collections.shuffle(checklist,new Random(){@Override public int nextInt(int bound){return level.random.nextInt(bound);}});}
   var column=checklist.get(cursor++);
   // Original reads the next entry's identical y after removing the last, throwing at column169. Use its constant y safely.
   for(int y=column.getY();y>=worldPosition.getY()-6;y--){var p=new BlockPos(column.getX(),y,column.getZ());if(level.isEmptyBlock(p)||!isPlant(p)||worldPosition.distSqr(p)>=36||grown(p))continue;charges--;last=p;lastState=level.getBlockState(p);pending=p;due=level.getGameTime()+1;return;}
  }
  void updateAnimals(){var animals=level.getEntitiesOfClass(Animal.class,new AABB(worldPosition).inflate(7));for(var candidate:animals){if(candidate.getAge()!=0||candidate.isInLove())continue;var same=animals.stream().filter(a->a.getClass()==candidate.getClass()).toList();if(same.size()>7)continue;Animal partner=null;for(var animal:same){if(animal.getAge()!=0||animal.isInLove())continue;if(partner==null){partner=animal;continue;}charges-=2;animal.setInLove(null);partner.setInLove(null);return;}}}
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);tag.putInt("orientation",facing().get3DDataValue());tag.putInt("charges",charges);tag.putBoolean("reserve",reserve);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);charges=tag.getInt("charges");reserve=tag.getBoolean("reserve");}
  @Override public boolean isConnectable(Direction f){return !normal()&&f==facing();}@Override public boolean canInputFrom(Direction f){return isConnectable(f);}@Override public boolean canOutputTo(Direction f){return false;}@Override public void setSuction(Aspect a,int n){}@Override public Aspect getSuctionType(Direction f){return growth()?Aspect.PLANT:Aspect.LIFE;}@Override public int getSuctionAmount(Direction f){return normal()||f!=facing()?0:growth()?(!reserve||charges<=0?128:0):128-charges*10;}@Override public int getMinimumSuction(){return 0;}@Override public boolean renderExtendedTube(){return false;}@Override public Aspect getEssentiaType(Direction f){return null;}@Override public int getEssentiaAmount(Direction f){return 0;}@Override public int takeEssentia(Aspect a,int n,Direction f){return 0;}@Override public int addEssentia(Aspect a,int n,Direction f){return 0;}
 }
}
