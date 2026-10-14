package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class HungryChest extends ChestBlock {
 public static final DeferredHolder<Block,Block> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("hungry_chest"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Tile>> TILE=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("hungry_chest"));
 HungryChest(){super(Properties.of().strength(2.5f).sound(SoundType.WOOD),()->TILE.get());}
 @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);}
 @Override protected BlockState updateShape(BlockState s,Direction d,BlockState next,LevelAccessor l,BlockPos p,BlockPos q){return super.updateShape(s,d,next,l,p,q).setValue(TYPE,ChestType.SINGLE);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
 @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){
  if(!l.isClientSide&&entity instanceof ItemEntity item&&item.isAlive()&&l.getBlockEntity(p) instanceof Tile tile){var original=item.getItem();var left=ItemHandlerHelper.insertItemStacked(new InvWrapper(tile),original.copy(),false);if(left.getCount()!=original.getCount()){l.playSound(null,p,net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.BLOCKS,.25f,1);l.blockEvent(p,this,2,2);if(left.isEmpty())item.discard();else item.setItem(left);tile.setChanged();}}
 }
 public static final class Tile extends ChestBlockEntity {
  int chew;public float chewLid,oldChewLid;
  Tile(BlockPos p,BlockState s){super(TILE.get(),p,s);}
  @Override protected Component getDefaultName(){return Component.translatable("block.tc4modern.hungry_chest");}
  @Override public boolean triggerEvent(int id,int value){if(id==2){chew=4;return true;}return super.triggerEvent(id,value);}
  @Override public float getOpenNess(float partial){return Math.max(super.getOpenNess(partial),net.minecraft.util.Mth.lerp(partial,oldChewLid,chewLid));}
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?createTickerHelper(t,TILE.get(),(level,pos,state,tile)->{ChestBlockEntity.lidAnimateTick(level,pos,state,tile);tile.oldChewLid=tile.chewLid;tile.chewLid=tile.chew>0?.2f:Math.max(0,tile.chewLid-.1f);if(tile.chew>0)tile.chew--;}):null;}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new HungryChest()));e.register(Registries.ITEM,h->h.register(BLOCK.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(TILE.getId(),BlockEntityType.Builder.of(Tile::new,BLOCK.get()).build(null)));}
 @SubscribeEvent static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,TILE.get(),(tile,side)->new InvWrapper(tile));}
}
