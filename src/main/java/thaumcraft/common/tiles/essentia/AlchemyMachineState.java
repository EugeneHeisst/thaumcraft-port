package thaumcraft.common.tiles.essentia;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.*;
/** TileCentrifuge / TileEssentiaCrystalizer update and transport rules. */
public final class AlchemyMachineState {
    public final boolean crystalizer;
    public Aspect input, output;
    public Direction facing=Direction.DOWN;
    public int count, progress;
    public float rotation, speed, cr=1,cg=1,cb=1;
    public AlchemyMachineState(boolean crystalizer){this.crystalizer=crystalizer;}
    public int suction(Direction side,boolean power){return crystalizer?(power?0:(side==facing&&input==null?128:64)):(side==Direction.DOWN?(power?0:(input==null?128:64)):0);}
    public int add(Aspect aspect,int amount,Direction side){
        if(crystalizer){if(side!=facing||input!=null||amount==0)return 0;input=aspect;return 1;}
        /* Original addEssentia ignores face and requested amount for the centrifuge. */
        if(input==null&&!aspect.isPrimal()){input=aspect;progress=39;return 1;}return 0;
    }
    public boolean tick(boolean power,IEssentiaTransport source,java.util.function.IntUnaryOperator earth,java.util.function.Consumer<Aspect> eject,net.minecraft.util.RandomSource random){
        boolean changed=false;
        if(crystalizer){
            if(++count%5==0&&!power){
                if(input==null){changed=draw(source,power);progress=0;}
                else progress+=1+earth.applyAsInt(Math.min(20,Math.max(1,(200-progress)/2)))*2;
            }
            if(input!=null&&progress>=200){eject.accept(input);input=null;progress=0;changed=true;}
        }else if(!power){
            if(output==null&&input==null&&++count%5==0)changed=draw(source,power);
            if(progress>0)--progress;
            if(output==null&&input!=null&&progress==0){output=input.getComponents()[random.nextInt(2)];input=null;changed=true;}
        }
        return changed;
    }
    boolean draw(IEssentiaTransport source,boolean power){
        Direction side=crystalizer?facing:Direction.DOWN,other=side.getOpposite();
        if(source==null||!source.canOutputTo(other))return false;
        if(source.getEssentiaAmount(other)<=0||source.getSuctionAmount(other)>=suction(side,power)||suction(side,power)<source.getMinimumSuction())return false;
        Aspect a=source.getEssentiaType(other);
        if(a==null||(!crystalizer&&a.isPrimal())||source.takeEssentia(a,1,other)!=1)return false;
        input=a;if(!crystalizer)progress=39;return true;
    }
    public void animate(boolean power){
        if(crystalizer){
            float r=input==null?1:((input.getColor()>>16)&255)/220f,g=input==null?1:((input.getColor()>>8)&255)/220f,b=input==null?1:(input.getColor()&255)/220f;
            cr=approach(cr,r);cg=approach(cg,g);cb=approach(cb,b);
            rotation+=speed;if(rotation>360)rotation-=360;
            if(input!=null&&!power)speed=Math.min(20,speed+.1f);else speed=Math.max(0,speed-.2f);
        }else{
            if(input!=null&&!power&&speed<20)speed+=2;
            if((input==null||power)&&speed>0)speed-=.5f;
            rotation+=speed;
        }
    }
    static float approach(float a,float b){if(a<b)a+=.05f;if(a>b)a-=.05f;return a;}
}
