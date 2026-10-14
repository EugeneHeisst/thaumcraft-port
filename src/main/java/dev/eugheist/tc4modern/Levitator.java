package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** Original BlockLifter/TileLifter; the global client shift key affects every target, as in TC4. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class Levitator {
 public static final DeferredHolder<Block,LifterBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("levitator"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<LifterEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("levitator"));
 static java.util.function.BooleanSupplier shift=()->false;
 static java.util.function.BiConsumer<Level,BlockPos> sparkle=(l,p)->{};
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new LifterBlock(Block.Properties.of().strength(2.5f,TC4SoundTypes.res(15)).sound(SoundType.WOOD).noOcclusion())));e.register(Registries.ITEM,h->h.register(BLOCK.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(LifterEntity::new,BLOCK.get()).build(null)));}
 public static final class LifterBlock extends BaseEntityBlock {
  static final MapCodec<LifterBlock> CODEC=simpleCodec(LifterBlock::new);static final BooleanProperty POWERED=BooleanProperty.create("powered");
  LifterBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(POWERED,false));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(POWERED);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new LifterEntity(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,ENTITY.get(),LifterEntity::tick);}
  @Override public boolean canConnectRedstone(BlockState s,BlockGetter l,BlockPos p,Direction side){return side!=null&&side.getAxis().isHorizontal();}
  static void updateBelow(Level l,BlockPos p){for(var at=p.below();l.getBlockState(at).is(BLOCK.get());at=at.below())if(l.getBlockEntity(at) instanceof LifterEntity t)t.requiresUpdate=true;}
  @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){super.onPlace(s,l,p,old,moving);updateBelow(l,p);}
  @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock()))updateBelow(l,p);super.onRemove(s,l,p,next,moving);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(l.getBlockEntity(p) instanceof LifterEntity t&&t.gettingPower()!=t.lastPowerState){t.requiresUpdate=true;updateBelow(l,p);}}
  @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){if(l.getBlockEntity(p) instanceof LifterEntity t&&!t.gettingPower()&&t.rangeAbove>0)sparkle.accept(l,p);}
 }
 public static final class LifterEntity extends BlockEntity {
  int counter,rangeAbove;boolean requiresUpdate=true,lastPowerState;
  LifterEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
  boolean gettingPower(){return level.hasNeighborSignal(worldPosition)||level.hasNeighborSignal(worldPosition.above());}
  static void tick(Level l,BlockPos p,BlockState s,LifterEntity t){
   t.counter++;if(t.requiresUpdate||t.counter%100==0){t.lastPowerState=t.gettingPower();t.requiresUpdate=false;int max=10;for(var at=p.below();l.getBlockState(at).is(BLOCK.get())&&!l.hasNeighborSignal(at);at=at.below())max+=10;t.rangeAbove=0;while(t.rangeAbove<max&&p.getY()+1+t.rangeAbove<l.getMaxBuildHeight()&&!l.getBlockState(p.above(1+t.rangeAbove)).isSolidRender(l,p.above(1+t.rangeAbove)))t.rangeAbove++;
    if(!l.isClientSide&&s.getValue(LifterBlock.POWERED)!=t.lastPowerState)l.setBlock(p,s.setValue(LifterBlock.POWERED,t.lastPowerState),2);
   }
   if(t.rangeAbove<=0||t.gettingPower())return;
   for(var target:l.getEntities(null,new AABB(p.getX(),p.getY()+1,p.getZ(),p.getX()+1,p.getY()+1+t.rangeAbove,p.getZ()+1))){
    if(!(target instanceof ItemEntity)&&!target.isPushable()&&!(target instanceof Horse))continue;
    var velocity=target.getDeltaMovement();double y=velocity.y;
    // CommonProxy.isShiftKeyDown is always false on the server in original TC4.
    if(l.isClientSide&&shift.getAsBoolean()){if(y<0)y*=.9f;}else if(y<.35f)y+=.1f;
    target.setDeltaMovement(velocity.x,y,velocity.z);target.fallDistance=0;
   }
  }
 }
}
