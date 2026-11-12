package thaumcraft.common.tiles;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
/** Original TileVisRelay attunement, pulse and persisted attachment presentation. */
public final class VisRelayState {
 public short orientation=1;
 public byte color=-1;
 public int pulse,px,py,pz;
 public float red=.5f,green=.5f,blue=.5f;
 /** FXBeamPower opacity has its own twenty-tick decay, independent of pulse throttling. */
 public float beamOpacity=.3f;
 public static final int[] COLORS={0xFFFF7E,16727041,37119,40960,0xEECCFF,0x555577};
 public void cycleColor(){color++;if(color>5)color=-1;}
 public int consumePulse(Aspect aspect){
  int c=aspect==Aspect.AIR?0:aspect==Aspect.FIRE?1:aspect==Aspect.WATER?2:aspect==Aspect.EARTH?3:aspect==Aspect.ORDER?4:aspect==Aspect.ENTROPY?5:-1;
  if(c<0||pulse!=0)return -1;
  pulse=5;return c;
 }
 public void receivePulse(int c){
  if(c<0||c>=COLORS.length)return;
  pulse=5;beamOpacity=.8f;int rgb=COLORS[c];red=(rgb>>16&255)/255f;green=(rgb>>8&255)/255f;blue=(rgb&255)/255f;
 }
 public void tick(boolean client){
  if(client){beamOpacity=Math.max(.3f,(pulse>0?.8f:beamOpacity)-.025f);red=Math.min(1,red+.025f);green=Math.min(1,green+.025f);blue=Math.min(1,blue+.025f);}
  if(pulse>0)pulse--;
 }
 public void read(CompoundTag tag){orientation=tag.getShort("orientation");color=tag.getByte("color");px=tag.getByte("px");py=tag.getByte("py");pz=tag.getByte("pz");}
 public void write(CompoundTag tag){tag.putShort("orientation",orientation);tag.putByte("color",color);tag.putByte("px",(byte)px);tag.putByte("py",(byte)py);tag.putByte("pz",(byte)pz);}
}
