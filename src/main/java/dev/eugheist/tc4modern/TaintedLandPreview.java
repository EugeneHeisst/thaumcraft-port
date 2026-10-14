package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden native screenshot of newly generated natural taint, with no manual biome/block fixture. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class TaintedLandPreview {
    /** Biome-source census around the found patch: tainted share, plains share and the largest connected tainted patch. */
    static boolean deep(net.minecraft.server.level.ServerLevel level,BlockPos p){for(int x=-6;x<=6;x+=3)for(int z=-6;z<=6;z+=3)if(!level.getBiome(p.offset(x,0,z)).is(TaintBiomes.TAINTED_LAND))return false;return true;}
    static void stats(net.minecraft.server.level.ServerLevel level,BlockPos center){
        int step=16,n=512;var source=level.getChunkSource().getGenerator().getBiomeSource();var sampler=level.getChunkSource().randomState().sampler();
        boolean[][] taint=new boolean[n][n];int tainted=0,plains=0;
        for(int i=0;i<n;i++)for(int j=0;j<n;j++){
            int x=center.getX()+(i-n/2)*step,z=center.getZ()+(j-n/2)*step;
            var b=source.getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x),net.minecraft.core.QuartPos.fromBlock(70),net.minecraft.core.QuartPos.fromBlock(z),sampler);
            if(b.is(TaintBiomes.TAINTED_LAND)){taint[i][j]=true;tainted++;}
            if(b.is(net.minecraft.world.level.biome.Biomes.PLAINS))plains++;
        }
        int largest=0,patches=0;boolean[][] seen=new boolean[n][n];
        for(int i=0;i<n;i++)for(int j=0;j<n;j++){if(!taint[i][j]||seen[i][j])continue;patches++;int size=0;var q=new java.util.ArrayDeque<int[]>();q.add(new int[]{i,j});seen[i][j]=true;
            while(!q.isEmpty()){var c=q.poll();size++;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int a=c[0]+d[0],b=c[1]+d[1];if(a>=0&&b>=0&&a<n&&b<n&&taint[a][b]&&!seen[a][b]){seen[a][b]=true;q.add(new int[]{a,b});}}}
            largest=Math.max(largest,size);}
        System.out.printf("TC4_TAINTED_PREVIEW STATS area=%dx%d blocks tainted=%.2f%% plains=%.2f%% patches=%d largest=%d blocks^2 (~%dx%d)%n",n*step,n*step,100.0*tainted/(n*n),100.0*plains/(n*n),patches,largest*step*step,(int)Math.sqrt(largest)*step,(int)Math.sqrt(largest)*step);
    }
    static int ticks;static volatile boolean ready;static volatile String failure;
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("tc4modern.taintedLandShowOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        if(ticks==40&&!ready&&failure==null)return;
        ticks++;
        if(ticks==1)mc.options.renderDistance().set(3);
        if(ticks==40)mc.getSingleplayerServer().execute(()->{try {
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            var found=level.findClosestBiome3d(b->b.is(TaintBiomes.TAINTED_LAND),new BlockPos(40000,70,40000),6400,64,128);
            if(found==null)throw new IllegalStateException("No natural tainted land in preview climate");
            level.getServer().getPlayerList().setViewDistance(3);
            var center=found.getFirst();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.getInventory().clearContent();level.setDayTime(1000);level.setWeatherParameters(6000,0,false,false);
            int height=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center.getX(),center.getZ());
            player.teleportTo(level,center.getX()+.5,height+18,center.getZ()-22,0,30);
            ready=true;System.out.println("TC4_TAINTED_PREVIEW PASS natural biome at "+center);
            stats(level,center);
        }catch(Throwable ex){failure=ex.toString();ex.printStackTrace();}});
        if(ticks==240) {
            if(!ready||failure!=null)throw new IllegalStateException("Natural taint preview failed: "+failure);
            LiquidDeathAudit.shot(mc,"natural-tainted-land.png");System.out.println("TC4_TAINTED_PREVIEW ALL PASS fresh natural tainted land screenshot");
        }
        if(ticks==250)mc.getSingleplayerServer().execute(()->{
            // Close-up of wall-hugging fibres (the vine-like strands) on a stone wall deep in the biome, to check their tint.
            var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            BlockPos spot=null;var c=player.blockPosition();
            for(int r=0;r<64&&spot==null;r+=4)for(int dx=-r;dx<=r&&spot==null;dx+=4)for(int dz=-r;dz<=r&&spot==null;dz+=4){var p=c.offset(dx,0,dz);if(deep(level,p))spot=p;}
            if(spot==null)return;
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spot.getX(),spot.getZ());
            var base=new BlockPos(spot.getX(),y,spot.getZ());
            for(int dx=-3;dx<=3;dx++)for(int dy=0;dy<6;dy++)for(int dz=-4;dz<=1;dz++)level.setBlock(base.offset(dx,dy,dz),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            for(int dx=-2;dx<=2;dx++)for(int dy=0;dy<4;dy++)level.setBlock(base.offset(dx,dy,1),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var block=(TaintFibresBlock)TC4Modern.TAINT_FIBRES.get();
            for(int dx=-2;dx<=2;dx++)for(int dy=0;dy<4;dy++){var p=base.offset(dx,dy,0);level.setBlock(p,block.attach(block.defaultBlockState(),level,p),3);}
            player.teleportTo(level,base.getX()+.5,base.getY()+1,base.getZ()-3.5,0,10);
            System.out.println("TC4_TAINTED_PREVIEW fibre wall at "+base+" "+level.getBlockState(base));
        });
        if(ticks==290)LiquidDeathAudit.shot(mc,"natural-taint-fibres.png");
        if(ticks==300)mc.stop();
    }
}
