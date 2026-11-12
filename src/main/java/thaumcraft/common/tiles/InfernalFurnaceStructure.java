package thaumcraft.common.tiles;

/** Original WandManager.fitArcaneFurnace/replaceArcaneFurnace blueprint, bottom-relative coordinates. */
public final class InfernalFurnaceStructure {
    public enum Material { AIR,NETHER_BRICK,OBSIDIAN,LAVA,IRON_BARS,OTHER }
    public interface View { Material at(int x,int y,int z); }
    private InfernalFurnaceStructure(){}
    public static boolean fits(View world,int x,int y,int z){
        boolean fenceFound=false;
        for(int layer=0;layer<3;layer++)for(int xx=0;xx<3;xx++)for(int zz=0;zz<3;zz++){
            Material expected=xx==1&&zz==1?(layer==0?Material.AIR:layer==1?Material.LAVA:Material.OBSIDIAN)
                    :xx%2==0&&zz%2==0?Material.NETHER_BRICK:Material.OBSIDIAN;
            Material actual=world.at(x+xx,y-layer+2,z+zz);
            if(actual==expected)continue;
            if(layer==1&&!fenceFound&&actual==Material.IRON_BARS&&xx!=zz&&(xx==1||zz==1)){
                fenceFound=true;continue;
            }
            return false;
        }
        return fenceFound;
    }
    /** Layer-independent original metadata encodes each X/Z cell; lava0, exit10, air unconverted. */
    public static int convertedMetadata(Material original,int localX,int localZ){
        if(original==Material.AIR)return -1;
        if(original==Material.LAVA)return 0;
        if(original==Material.IRON_BARS)return 10;
        return 1+localX+3*localZ;
    }
}
