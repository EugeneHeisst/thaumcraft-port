package thaumcraft.common.lib.world;
/** Original BlockMagicalLeaves four-pass connectivity propagation in the radius-four cube. */
public final class GreatwoodLeafDecay {
    public interface View { boolean sustains(int x,int y,int z); boolean leaves(int x,int y,int z); }
    public static boolean supported(View world){
        int[] cells=new int[32*32*32];
        for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)
            cells[index(x,y,z)]=world.sustains(x,y,z)?0:world.leaves(x,y,z)?-2:-1;
        int[][] directions={{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
        for(int distance=1;distance<=4;distance++)for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++){
            if(cells[index(x,y,z)]!=distance-1)continue;
            for(var direction:directions){int i=index(x+direction[0],y+direction[1],z+direction[2]);if(cells[i]==-2)cells[i]=distance;}
        }
        return cells[index(0,0,0)]>=0;
    }
    private static int index(int x,int y,int z){return (x+16)*1024+(y+16)*32+z+16;}
}
