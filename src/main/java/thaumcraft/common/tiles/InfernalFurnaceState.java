package thaumcraft.common.tiles;

/** Tick ordering extracted from TC4 TileArcaneFurnace; inventory/world operations belong to the host. */
public final class InfernalFurnaceState {
    public int cookTime,maxCookTime,speedyTime;
    public interface Host {
        int drainFireVis(int requested);
        int activeBellows();
        boolean hasSmeltableInput();
        void smeltFirstInput();
    }
    public static int cookDuration(boolean speedy,int bellows){
        return (speedy?80:140)-20*Math.min(3,bellows);
    }
    public void tick(Host host){
        boolean cooked=false;
        if(cookTime>0){--cookTime;cooked=true;}
        if(cooked&&speedyTime>0)--speedyTime;
        if(speedyTime<=0)speedyTime=host.drainFireVis(5);
        if(maxCookTime==0)maxCookTime=cookDuration(speedyTime>0,host.activeBellows());
        if(cookTime>maxCookTime)cookTime=maxCookTime;
        if(cookTime==0&&cooked)host.smeltFirstInput();
        if(cookTime==0&&!cooked&&host.hasSmeltableInput()){
            cookTime=maxCookTime=cookDuration(speedyTime>0,host.activeBellows());
        }
    }
}
