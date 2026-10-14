package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/** Original ItemElementalHoe, including the intentionally stricter tree-growth durability guards. */
public final class ElementalHoe extends HoeItem {
 ElementalHoe(){super(ElementalTools.TIER,ElementalTools.properties(-3,0));}
 // Vanilla 1.7.10 hoes had no mining-speed or melee-durability behavior.
 @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return 1;}
 @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,BlockPos pos,LivingEntity user){return false;}
 @Override public boolean hurtEnemy(ItemStack stack,LivingEntity victim,LivingEntity attacker){return false;}
 @Override public void postHurtEnemy(ItemStack stack,LivingEntity victim,LivingEntity attacker){}
 @Override public int getEnchantmentValue(){return 5;}
 static boolean till(UseOnContext c,BlockPos pos){var p=c.getPlayer();var l=c.getLevel();if(c.getClickedFace()==Direction.DOWN||!l.isEmptyBlock(pos.above())||!p.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand()))return false;var s=l.getBlockState(pos);if(!s.is(Blocks.GRASS_BLOCK)&&!s.is(Blocks.DIRT))return false;l.playSound(p,pos,net.minecraft.sounds.SoundEvents.HOE_TILL,net.minecraft.sounds.SoundSource.BLOCKS,1,1);if(!l.isClientSide){l.setBlock(pos,Blocks.FARMLAND.defaultBlockState(),3);c.getItemInHand().hurtAndBreak(1,p,LivingEntity.getSlotForHand(c.getHand()));}return true;}
 @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();if(p==null)return InteractionResult.PASS;var l=c.getLevel();var at=c.getClickedPos();var stack=c.getItemInHand();if(p.isShiftKeyDown())return till(c,at)?InteractionResult.sidedSuccess(l.isClientSide):InteractionResult.PASS;boolean did=false;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var pos=at.offset(x,0,z);if(till(c,pos)){ElementalToolEffects.sparkle(l,pos,8401408,2);did=true;}}if(did)return InteractionResult.sidedSuccess(l.isClientSide);
  did=BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL),l,at,p);if(did){stack.hurtAndBreak(1,p,LivingEntity.getSlotForHand(c.getHand()));ElementalToolEffects.sparkle(l,at,0,3);}else{var state=l.getBlockState(at);boolean great=state.is(TC4Modern.GREATWOOD_SAPLING.get()),silver=state.is(SilverwoodContent.SAPLING.get());if(great&&stack.getDamageValue()+20<=stack.getMaxDamage()||silver&&stack.getDamageValue()+150<=stack.getMaxDamage()){if(l instanceof ServerLevel server){l.removeBlock(at,false);boolean success;if(great){var ctx=new FeaturePlaceContext<NoneFeatureConfiguration>(java.util.Optional.empty(),server,server.getChunkSource().getGenerator(),server.random,at,NoneFeatureConfiguration.INSTANCE);success=TC4Modern.GREATWOOD_FEATURE.get().grow(ctx,false);}else success=SilverwoodContent.FEATURE.get().grow(server,server.random,at,false,7,5);if(!success)l.setBlock(at,state,2);}stack.hurtAndBreak(great?5:25,p,LivingEntity.getSlotForHand(c.getHand()));ElementalToolEffects.sparkle(l,at,0,2);did=true;}}
  if(did)ElementalTools.sound(l,at,"wand",.75f,.9f+l.random.nextFloat()*.2f);return did?InteractionResult.sidedSuccess(l.isClientSide):InteractionResult.PASS;
 }
}
