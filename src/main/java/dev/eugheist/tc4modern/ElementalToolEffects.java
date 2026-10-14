package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
/** ClientProxy.blockSparkle: original 112..120 generic twinkles; server proxy is empty. */
final class ElementalToolEffects {
 static void sparkle(Level level,BlockPos pos,int color,int count){if(!level.isClientSide)return;var r=level.random;float red=(color>>16&255)/255f,green=(color>>8&255)/255f,blue=(color&255)/255f;for(int i=0;i<FociFx.particleCount(count);i++)FociFx.generic(pos.getX()-.1f+r.nextFloat()*1.2f,pos.getY()-.1f+r.nextFloat()*1.2f,pos.getZ()-.1f+r.nextFloat()*1.2f,0,r.nextFloat()*.02,0,Mth.clamp(red-.2f+r.nextFloat()*.4f,0,1),Mth.clamp(green-.2f+r.nextFloat()*.4f,0,1),Mth.clamp(blue-.2f+r.nextFloat()*.4f,0,1),.9f,false,112,9,1,5+r.nextInt(8),r.nextInt(10),.7f+r.nextFloat()*.4f);}
}
