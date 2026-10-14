package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Original ItemElementalShovel: fixed-face digging plane, three placement orientations and sneak-only preview. */
public final class ElementalShovel extends ShovelItem implements Warding.Architect {
 int side;
 ElementalShovel(){super(ElementalTools.TIER,ElementalTools.properties(1,-3));}
 static byte orientation(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getByte("or");}
 static void toggle(ItemStack stack){byte next=(byte)((orientation(stack)+1)%3);CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putByte("or",next));}
 static BlockPos offset(int a,int b,int side,int orientation,Player p){if(orientation==1||orientation==2&&side<2){if(side<2){int facing=Mth.floor(p.getYRot()*4/360+.5)&3;return facing==0||facing==2?new BlockPos(a,b,0):new BlockPos(0,b,a);}return side<4?new BlockPos(0,b,a):new BlockPos(a,b,0);}if(orientation==2)return new BlockPos(a,0,b);return side<2?new BlockPos(a,0,b):side<4?new BlockPos(a,b,0):new BlockPos(0,b,a);}
 static boolean replaceable(Level l,BlockPos at){var s=l.getBlockState(at);return s.isAir()||s.canBeReplaced()||s.is(Blocks.SNOW)||s.is(BlockTags.FLOWERS)||s.getFluidState().is(FluidTags.WATER);}
 static ItemStack material(Player p,Item item){for(var s:p.getInventory().items)if(s.is(item))return s;return ItemStack.EMPTY;}
 @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();if(p==null)return InteractionResult.PASS;var l=c.getLevel();var origin=c.getClickedPos();var state=l.getBlockState(origin);if(l.getBlockEntity(origin)!=null)return InteractionResult.PASS;int face=c.getClickedFace().get3DDataValue();boolean placedAny=false;for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){var at=origin.offset(offset(a,b,face,orientation(c.getItemInHand()),p)).relative(c.getClickedFace());if(!replaceable(l,at)||!l.mayInteract(p,at)||!p.mayUseItemAt(at,c.getClickedFace(),c.getItemInHand()))continue;var use=material(p,state.getBlock().asItem());var placed=state;int color=8401408;if(!p.getAbilities().instabuild&&use.isEmpty()){if(!state.is(Blocks.GRASS_BLOCK))continue;use=material(p,Items.DIRT);if(use.isEmpty())continue;placed=Blocks.DIRT.defaultBlockState();color=3;}placedAny=true;var sound=state.getSoundType(l,origin,p);l.playSound(p,at,sound.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,.9f+l.random.nextFloat()*.2f);ElementalToolEffects.sparkle(l,at,color,4);
  // Only the server places: client-predicted blocks around the clicked one were rolled back to air by the block-change ack (ghost blocks).
  if(l.isClientSide)continue;if(!p.getAbilities().instabuild)use.shrink(1);l.setBlock(at,placed,3);c.getItemInHand().hurtAndBreak(1,p,LivingEntity.getSlotForHand(c.getHand()));}return placedAny?InteractionResult.sidedSuccess(l.isClientSide):InteractionResult.PASS;}
 public void capture(Player p,BlockPos pos){var hit=p.pick(p.blockInteractionRange(),0,true);if(hit instanceof net.minecraft.world.phys.BlockHitResult b)side=b.getDirection().get3DDataValue();}
 @Override public boolean mineBlock(ItemStack stack,Level l,BlockState state,BlockPos origin,LivingEntity user){if(user.isShiftKeyDown())return super.mineBlock(stack,l,state,origin,user);if(user instanceof ServerPlayer p&&state.is(BlockTags.MINEABLE_WITH_SHOVEL))for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){var at=origin.offset(offset(a,b,side,0,p));if(at.equals(origin))continue;var extra=l.getBlockState(at);if(extra.is(BlockTags.MINEABLE_WITH_SHOVEL)&&extra.getDestroySpeed(l,at)>=0&&l.mayInteract(p,at)){stack.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);ElementalTools.harvest(p,at,3);}}return true;}
 @Override public List<BlockPos> architectBlocks(ItemStack s,Level l,BlockPos pos,int side,Player p){var out=new ArrayList<BlockPos>();if(!p.isShiftKeyDown())return out;for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){var at=pos.offset(offset(a,b,side,orientation(s),p)).relative(Direction.from3DDataValue(side));if(replaceable(l,at))out.add(at);}return out;}
 @Override public boolean showAxis(ItemStack s,Level l,Player p,int side,Warding.Axis axis){return false;}
}
