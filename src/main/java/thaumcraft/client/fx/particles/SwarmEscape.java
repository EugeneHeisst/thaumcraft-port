package thaumcraft.client.fx.particles;
/** Original FXSwarm pushOutOfBlocks; world queries supplied by the modern bridge. */
public final class SwarmEscape {
    public double vx,vy,vz;
    public interface World {
        boolean fibres(int x,int y,int z);
        boolean air(int x,int y,int z);
        boolean liquidInBounds();
        boolean opaqueNormalCube(int x,int y,int z);
    }
    public boolean pushOutOfBlocks(double par1, double par3, double par5, World world, java.util.random.RandomGenerator random) {
        int var7 = net.minecraft.util.Mth.floor((double)par1);
        int var8 = net.minecraft.util.Mth.floor((double)par3);
        int var9 = net.minecraft.util.Mth.floor((double)par5);
        double var10 = par1 - (double)var7;
        double var12 = par3 - (double)var8;
        double var14 = par5 - (double)var9;
        if (!world.fibres(var7, var8, var9) && !world.air(var7, var8, var9) && !world.liquidInBounds()) {
            boolean var16 = !world.opaqueNormalCube(var7 - 1, var8, var9);
            boolean var17 = !world.opaqueNormalCube(var7 + 1, var8, var9);
            boolean var18 = !world.opaqueNormalCube(var7, var8 - 1, var9);
            boolean var19 = !world.opaqueNormalCube(var7, var8 + 1, var9);
            boolean var20 = !world.opaqueNormalCube(var7, var8, var9 - 1);
            boolean var21 = !world.opaqueNormalCube(var7, var8, var9 + 1);
            int var22 = -1;
            double var23 = 9999.0;
            if (var16 && var10 < var23) {
                var23 = var10;
                var22 = 0;
            }
            if (var17 && 1.0 - var10 < var23) {
                var23 = 1.0 - var10;
                var22 = 1;
            }
            if (var18 && var12 < var23) {
                var23 = var12;
                var22 = 2;
            }
            if (var19 && 1.0 - var12 < var23) {
                var23 = 1.0 - var12;
                var22 = 3;
            }
            if (var20 && var14 < var23) {
                var23 = var14;
                var22 = 4;
            }
            if (var21 && 1.0 - var14 < var23) {
                var23 = 1.0 - var14;
                var22 = 5;
            }
            float var25 = random.nextFloat() * 0.05f + 0.025f;
            float var26 = (random.nextFloat() - random.nextFloat()) * 0.1f;
            if (var22 == 0) {
                vx = -var25;
                vy = vz = (double)var26;
            }
            if (var22 == 1) {
                vx = var25;
                vy = vz = (double)var26;
            }
            if (var22 == 2) {
                vy = -var25;
                vx = vz = (double)var26;
            }
            if (var22 == 3) {
                vy = var25;
                vx = vz = (double)var26;
            }
            if (var22 == 4) {
                vz = -var25;
                vy = vx = (double)var26;
            }
            if (var22 == 5) {
                vz = var25;
                vy = vx = (double)var26;
            }
            return true;
        }
        return false;
    }
}
