package thaumcraft.common.tiles;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.research.ResearchManager;
/** TileDeconstructionTable server algorithm; inventory/platform outside this local original port. */
public final class DeconstructionState {
 public Aspect aspect; public int breaktime;
 public boolean tick(AspectList tags,java.util.function.IntUnaryOperator random,Runnable consume){
  boolean can=aspect==null&&tags!=null&&tags.size()>0,changed=false;
  if(breaktime==0&&can){breaktime=40;changed=true;}
  if(breaktime>0&&can){if(--breaktime==0){var primals=ResearchManager.reduceToPrimals(tags);if(random.applyAsInt(80)<primals.visSize())aspect=primals.getAspects()[random.applyAsInt(primals.size())];consume.run();changed=true;}}
  else breaktime=0;
  return changed;
 }
}
