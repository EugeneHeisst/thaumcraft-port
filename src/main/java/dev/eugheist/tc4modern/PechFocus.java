package dev.eugheist.tc4modern;
import thaumcraft.api.aspects.*;import net.minecraft.world.item.*;import net.minecraft.world.level.Level;import net.minecraft.world.entity.player.Player;
public final class PechFocus extends FociContent.Focus {
 @Override public AspectList getVisCost(ItemStack focus){var result=new AspectList().add(Aspect.EARTH,10).add(Aspect.ENTROPY,10).add(Aspect.WATER,10);if(getUpgradeLevel(focus,15)>0)result.add(Aspect.AIR,10).add(Aspect.FIRE,10).add(Aspect.ORDER,10);return result;}
 @Override public int getActivationCooldown(ItemStack focus){return 250;}@Override public String getSortingHelperBase(){return "PP";}
 @Override public int[] getPossibleUpgradesByRank(ItemStack focus,int rank){return FociContent.byRank(rank,new int[]{1,0},new int[]{1,0,7},new int[]{1,0},new int[]{1,0,7},new int[]{1,0,15});}@Override public int getFocusColor(ItemStack focus){return 0x229944;}
 @Override public boolean onFocusRightClick(ItemStack wand,Level level,Player player){var focus=FociContent.getFocusItem(wand);if(!level.isClientSide&&FociContent.consumeAllVis(wand,player,getVisCost(wand),true)){var blast=new PechBlast(level,player,FociContent.getFocusPotency(wand),getUpgradeLevel(focus,7),getUpgradeLevel(focus,15)>0);blast.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5f,1);level.addFreshEntity(blast);blast.playSound(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("ice")),.4f,1+level.random.nextFloat()*.1f);}player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);return true;}
}
