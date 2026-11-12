package thaumcraft.common.blocks;
import thaumcraft.common.tiles.InfernalFurnaceStructure.Material;
/** Literal BlockArcaneFurnace neighbour validation and dropped/restored material rules. */
public final class InfernalFurnaceRestoration {
    public interface View {boolean furnace(int x,int y,int z);}
    private InfernalFurnaceRestoration(){}
    public static boolean intact(View world,int x,int y,int z){
        for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++)
            // Preserve source loop termination, which also skips south-centre cells at Y0/Y1.
            for(int zz=-1;zz<=1&&(yy!=1&&yy!=0||zz!=0||xx!=0);zz++)
                if(!world.furnace(x+xx,y+yy,z+zz))return false;
        return true;
    }
    public static Material restored(int metadata){
        return metadata==0?Material.AIR:metadata==10?Material.IRON_BARS:
                metadata%2==0||metadata==5?Material.OBSIDIAN:Material.NETHER_BRICK;
    }
}
