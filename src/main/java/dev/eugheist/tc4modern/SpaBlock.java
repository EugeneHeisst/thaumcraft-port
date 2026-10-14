package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
/** BlockStoneDevice12 original bucket filling, including partial-bucket loss and creative consumption. */
public final class SpaBlock extends BaseEntityBlock {
 public static final MapCodec<SpaBlock> CODEC=simpleCodec(SpaBlock::new);SpaBlock(Properties p){super(p);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new SpaEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Hygiene.SPA_ENTITY.get(),SpaEntity::serverTick);}
 @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
  if(player.isShiftKeyDown()||!(level.getBlockEntity(p) instanceof SpaEntity tile))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  var container=FluidUtil.getFluidHandler(stack.copyWithCount(1));if(container.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;var handler=container.get();var fluid=handler.drain(Integer.MAX_VALUE,FluidAction.SIMULATE);if(fluid.isEmpty())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  if(!level.isClientSide&&tile.tank.getFluidAmount()<5000&&(tile.tank.isEmpty()||net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(tile.tank.getFluid(),fluid))){tile.tank.fill(fluid,FluidAction.EXECUTE);handler.drain(Integer.MAX_VALUE,FluidAction.EXECUTE);stack.shrink(1);var empty=handler.getContainer();if(!empty.isEmpty()&&!player.getInventory().add(empty))player.drop(empty,false);player.inventoryMenu.broadcastChanges();level.playSound(null,p,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);}
  return ItemInteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(player.isShiftKeyDown())return InteractionResult.PASS;if(!l.isClientSide&&l.getBlockEntity(p) instanceof SpaEntity spa)player.openMenu(spa);return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof SpaEntity spa)Containers.dropContents(l,p,spa);super.onRemove(s,l,p,next,moving);}
}
