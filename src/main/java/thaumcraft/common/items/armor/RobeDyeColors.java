package thaumcraft.common.items.armor;
/** Original RecipesRobeArmorDyes brightness-preserving blend with original EntitySheep palette. */
public final class RobeDyeColors {
    private static final float[][] SHEEP = new float[][]{{1.0f, 1.0f, 1.0f}, {0.85f, 0.5f, 0.2f}, {0.7f, 0.3f, 0.85f}, {0.4f, 0.6f, 0.85f}, {0.9f, 0.9f, 0.2f}, {0.5f, 0.8f, 0.1f}, {0.95f, 0.5f, 0.65f}, {0.3f, 0.3f, 0.3f}, {0.6f, 0.6f, 0.6f}, {0.3f, 0.5f, 0.6f}, {0.5f, 0.25f, 0.7f}, {0.2f, 0.3f, 0.7f}, {0.4f, 0.3f, 0.2f}, {0.4f, 0.5f, 0.2f}, {0.6f, 0.2f, 0.2f}, {0.1f, 0.1f, 0.1f}};
    public static int blend(int base,int[] dyes){
        int[] a=new int[3];int brightness=0,count=1;
        float r=(base>>16&255)/255f,g=(base>>8&255)/255f,b=(base&255)/255f;
        brightness=(int)(Math.max(r,Math.max(g,b))*255f);
        a[0]=(int)(r*255f);a[1]=(int)(g*255f);a[2]=(int)(b*255f);
        for(int dye:dyes){float[] rgb=SHEEP[dye];int rr=(int)(rgb[0]*255f),gg=(int)(rgb[1]*255f),bb=(int)(rgb[2]*255f);brightness+=Math.max(rr,Math.max(gg,bb));a[0]+=rr;a[1]+=gg;a[2]+=bb;count++;}
        int rr=a[0]/count,gg=a[1]/count,bb=a[2]/count;float avg=(float)brightness/count,max=Math.max(rr,Math.max(gg,bb));
        rr=(int)(rr*avg/max);gg=(int)(gg*avg/max);bb=(int)(bb*avg/max);
        return (rr<<16)|(gg<<8)|bb;
    }
}
