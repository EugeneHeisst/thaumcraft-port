package dev.eugheist.tc4modern;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeModifier;
/** ItemEldritchObject metadata 3: volatile node enhancement, including creative consumption. */
public final class PrimordialPearl extends Item {
 public PrimordialPearl(){super(new Properties().rarity(Rarity.EPIC));}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){for(int i=5;i<=6;i++)lines.add(net.minecraft.network.chat.Component.translatable("item.ItemEldritchObject.text."+i).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext c){
  if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof AuraNodeEntity node))return InteractionResult.PASS;
  if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
  var level=c.getLevel();var pos=c.getClickedPos();var random=level.random;stack.shrink(1);
  boolean research=c.getPlayer()!=null&&ResearchProgress.completed(c.getPlayer().getPersistentData()).contains("PRIMNODE");
  for(var a:node.reservoir.aspects.getAspects()){int m=node.reservoir.getNodeVisBase(a);if(!a.isPrimal()){if(random.nextBoolean())node.reservoir.setNodeVisBase(a,(short)(m-1));}else node.reservoir.setNodeVisBase(a,(short)(m-2+random.nextInt(research?9:6)));}
  for(var a:Aspect.getPrimalAspects()){int m=node.reservoir.getNodeVisBase(a),r=random.nextInt(research?4:3);if(r>0&&r>m){node.reservoir.setNodeVisBase(a,(short)r);node.reservoir.addToContainer(a,1);}}
  if(node.reservoir.modifier==NodeModifier.FADING&&random.nextBoolean())node.reservoir.modifier=NodeModifier.PALE;
  else if(node.reservoir.modifier==NodeModifier.PALE&&random.nextBoolean())node.reservoir.modifier=null;
  else if(node.reservoir.modifier==null&&random.nextInt(5)==0)node.reservoir.modifier=NodeModifier.BRIGHT;
  node.changed();level.explode(null,pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,3+random.nextFloat()*(research?3:5),true,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
  for(int i=0;i<33;i++){var cell=pos.offset(random.nextInt(6)-random.nextInt(6),random.nextInt(6)-random.nextInt(6),random.nextInt(6)-random.nextInt(6));if(level.isEmptyBlock(cell))level.setBlock(cell,(cell.getY()<pos.getY()?TC4Modern.FLUX_GOO:TC4Modern.FLUX_GAS).get().defaultBlockState().setValue(FluxBlock.AMOUNT,9),3);}
  return InteractionResult.CONSUME;
 }
}
