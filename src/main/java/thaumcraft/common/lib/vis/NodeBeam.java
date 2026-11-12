package thaumcraft.common.lib.vis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
/** Original UtilsFX.drawFloatyLine geometry, returned rather than submitted to GL. */
public final class NodeBeam {
 public record Vertex(float x,float y,float z,float u,float v,float alpha){}
 public static List<List<Vertex>> strips(Vec3 source,Vec3 target,long nanos,int useTicks){
  var strips=new ArrayList<List<Vertex>>();
  float time=nanos/30000000L,dist=(float)source.distanceTo(target),length=Math.round(dist)*8f;
  if(length==0)return strips;
  double dcx=(float)(source.x-target.x),dcy=(float)(source.y-target.y),dcz=(float)(source.z-target.z);
  for(int plane=0;plane<2;plane++){
   var vertices=new ArrayList<Vertex>();
   for(int i=0;i<=length*Math.min(useTicks,10)/10f;i++){
    float f2=i/length,f3=1-Math.abs(i-length/2)/(length/2);
    double dx=dcx+Mth.sin((float)((source.z%16+dist*(1-f2)*8-time%32767/5)/4))*.5f*f3;
    double dy=dcy+Mth.sin((float)((source.x%16+dist*(1-f2)*8-time%32767/5)/3))*.5f*f3;
    double dz=dcz+Mth.sin((float)((source.y%16+dist*(1-f2)*8-time%32767/5)/2))*.5f*f3;
    float u=(1-f2)*dist-time*(-.02f),x=(float)dx*f2,y=(float)dy*f2,z=(float)dz*f2;
    vertices.add(new Vertex(x-(plane==1?.15f:0),y-(plane==0?.15f:0),z,u,1,f3));
    vertices.add(new Vertex(x+(plane==1?.15f:0),y+(plane==0?.15f:0),z,u,0,f3));
   }
   strips.add(vertices);
  }
  return strips;
 }
 public static int smoothColor(int color,int target){
  return (((target>>16&255)+(color>>16&255)*4)/5)<<16 | (((target>>8&255)+(color>>8&255)*4)/5)<<8 | ((target&255)+(color&255)*4)/5;
 }
}
