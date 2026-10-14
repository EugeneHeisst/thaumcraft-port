package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.tiles.InfernalFurnaceOutput;

/** Native item and XP entities emitted with the original furnace's positions and velocities. */
public final class InfernalFurnaceEjection {
    private InfernalFurnaceEjection(){}
    public static void eject(Level level,BlockPos center,Direction outlet,ItemStack result,
                             ItemStack bonus,int bonusBaseCount,int bellows,float recipeExperience){
        if(level.isClientSide||result.isEmpty())return;
        var random=new java.util.Random(){
            @Override public int nextInt(int bound){return level.random.nextInt(bound);}
            @Override public float nextFloat(){return level.random.nextFloat();}
        };
        int facingX=outlet.getStepX(),facingZ=outlet.getStepZ();
        double x=center.getX()+.5f+facingX*1.2f,y=center.getY()+.4f,z=center.getZ()+.5f+facingZ*1.2f;
        float mx=InfernalFurnaceOutput.horizontalVelocity(facingX,random,false);
        float mz=InfernalFurnaceOutput.horizontalVelocity(facingZ,random,false);
        var item=new ItemEntity(level,x,y,z,result.copy());
        item.setDeltaMovement(mx,0,mz);
        level.addFreshEntity(item);
        if(!bonus.isEmpty()){
            int count=InfernalFurnaceOutput.bonusCount(bonusBaseCount,bellows,random);
            if(count>0){
                mx=InfernalFurnaceOutput.horizontalVelocity(facingX,random,false);
                mz=InfernalFurnaceOutput.horizontalVelocity(facingZ,random,false);
                var extra=new ItemEntity(level,x,y,z,bonus.copyWithCount(count));
                extra.setDeltaMovement(mx,0,mz);
                level.addFreshEntity(extra);
            }
        }
        int remaining=InfernalFurnaceOutput.experience(result.getCount(),recipeExperience,Math.random());
        while(remaining>0){
            int value=ExperienceOrb.getExperienceValue(remaining);remaining-=value;
            var orb=new ExperienceOrb(level,x,y,z,value);
            orb.setDeltaMovement(InfernalFurnaceOutput.horizontalVelocity(facingX,random,true),0,
                    InfernalFurnaceOutput.horizontalVelocity(facingZ,random,true));
            level.addFreshEntity(orb);
        }
    }
}
