package thaumcraft.common.tiles;

/** Numeric ejection rules extracted from TileArcaneFurnace.ejectItem. */
public final class InfernalFurnaceOutput {
    private InfernalFurnaceOutput(){}
    public static int bonusCount(int originalCount,int bellows,java.util.Random random){
        int result=originalCount;
        if(bellows==0){if(random.nextInt(4)==0)++result;}
        else for(int i=0;i<bellows;i++)if(random.nextFloat()<.44f)++result;
        return result;
    }
    public static int experience(int outputCount,float recipeExperience,double roundingRandom){
        if(recipeExperience==0)return 0;
        // Original only scales fractional experience; rates >=1 retain output count.
        if(recipeExperience>=1)return outputCount;
        float total=outputCount*recipeExperience;
        int value=(int)Math.floor(total);
        if(value<(int)Math.ceil(total)&&(float)roundingRandom<total-value)++value;
        return value;
    }
    public static float horizontalVelocity(int facing,java.util.Random random,boolean experienceOrb){
        return facing==0?(random.nextFloat()-random.nextFloat())*(experienceOrb?.025f:.03f):facing*.13f;
    }
}
