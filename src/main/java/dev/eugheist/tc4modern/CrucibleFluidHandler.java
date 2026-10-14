package dev.eugheist.tc4modern;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;

/** Water-only original TileCrucible IFluidHandler, exposed on every direction. */
public final class CrucibleFluidHandler implements IFluidHandler {
    private final CrucibleEntity tile;
    public CrucibleFluidHandler(CrucibleEntity tile){this.tile=tile;}
    public int getTanks(){return 1;}
    public int getTankCapacity(int tank){return tank==0?1000:0;}
    public FluidStack getFluidInTank(int tank){return tank==0&&tile.crucible.water>0?new FluidStack(Fluids.WATER,tile.crucible.water):FluidStack.EMPTY;}
    public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&fluid.is(Fluids.WATER);}
    public int fill(FluidStack resource,FluidAction action){
        if(!isFluidValid(0,resource))return 0;
        int accepted=Math.max(0,Math.min(resource.getAmount(),1000-tile.crucible.water));
        if(action.execute()&&accepted>0){tile.crucible.water+=accepted;tile.changed();}
        return accepted;
    }
    public FluidStack drain(FluidStack resource,FluidAction action){return resource.is(Fluids.WATER)?drain(resource.getAmount(),action):FluidStack.EMPTY;}
    public FluidStack drain(int maxDrain,FluidAction action){
        int amount=Math.max(0,Math.min(maxDrain,tile.crucible.water));
        if(amount==0)return FluidStack.EMPTY;
        if(action.execute()){tile.crucible.water-=amount;tile.changed();}
        return new FluidStack(Fluids.WATER,amount);
    }
}
