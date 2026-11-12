package thaumcraft.common.lib.vis;
import java.util.List;
import java.util.ArrayList;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
/** Original TileNodeRenderer animation math; GL submission replaced by returned layers. */
public final class NodeVisuals {
    public record Layer(float scale,float angle,float alpha,int strip,int frame,int color,boolean additive){}
    public static List<Layer> layers(long nt,int x,int ticks,double distance,double viewDistance,boolean visible,AspectList aspects,NodeType type,NodeModifier modifier){
        List<Layer> result=new ArrayList<>();int frame=(int)((nt/40000000L+x)%32);
        if(aspects.size()==0 || !visible){result.add(new Layer(.5f,0,.1f,1,frame,0xFFFFFF,true));return result;}
        if(distance>viewDistance)return result;
        float alpha=(float)((viewDistance-distance)/viewDistance);
        if(modifier!=null)switch(modifier){case BRIGHT:alpha*=1.5f;break;case PALE:alpha*=.66f;break;case FADING:alpha*=net.minecraft.util.Mth.sin(ticks/3f)*.25f+.33f;}
        long time=nt/5000000L;int count=0;float average=0,angle=0;
        for(Aspect aspect:aspects.getAspects()){
            if(aspect.getBlend()==771)alpha*=1.5f;
            average+=aspects.getAmount(aspect);
            float scale=net.minecraft.util.Mth.sin(ticks/(14f-count))*.25f+.5f;
            scale=.2f+scale*(aspects.getAmount(aspect)/50f);
            angle=(time%(5000+500*count))/(5000f+500*count)*(float)Math.PI*2;
            result.add(new Layer(scale,angle,alpha/Math.max(1,aspects.size()/2f),0,frame,aspect.getColor(),aspect.getBlend()!=771));
            count++;if(aspect.getBlend()==771)alpha/=1.5f;
        }
        float scale=.1f+(average/aspects.size())/150f;int strip=1;boolean additive=true;
        switch(type){case UNSTABLE:strip=6;angle=0;break;case DARK:strip=2;additive=false;break;case TAINTED:strip=5;additive=false;break;case PURE:strip=4;break;case HUNGRY:strip=3;scale*=.75f;break;default:break;}
        result.add(new Layer(scale,angle,alpha,strip,frame,0xFFFFFF,additive));return result;
    }
}
