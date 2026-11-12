package thaumcraft.common.items.baubles;

import thaumcraft.api.aspects.Aspect;
/** ItemAmuletVis.onWornTick: transfer to held wand first, then refill from a nearby relay. */
public final class AmuletVisRules {
 private AmuletVisRules(){}
 public interface Store {
  int capacity();
  int get(Aspect aspect);
  void set(Aspect aspect,int amount);
 }
 public interface Relay {
  int consume(Aspect aspect,int amount);
 }
 public static int capacity(boolean greater){return greater?25000:2500;}
 public static void wornTick(int ticks,Store amulet,Store heldWand,Relay nearbyRelay){
  if(ticks%5!=0)return;
  if(heldWand!=null)for(var aspect:Aspect.getPrimalAspects()){
   int stored=amulet.get(aspect);
   if(stored<=0)continue;
   int amount=Math.min(5,heldWand.capacity()-heldWand.get(aspect));
   amount=Math.min(amount,stored);
   amulet.set(aspect,stored-amount);
   heldWand.set(aspect,heldWand.get(aspect)+amount);
  }
  if(nearbyRelay!=null)for(var aspect:Aspect.getPrimalAspects()){
   int room=amulet.capacity()-amulet.get(aspect);
   if(room<=0)continue;
   int taken=nearbyRelay.consume(aspect,Math.min(5,room));
   if(taken>0)amulet.set(aspect,Math.min(amulet.capacity(),amulet.get(aspect)+taken));
  }
 }
}
