package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real client biome colours for the original blue_magical_forest option, on chunk-rendered grass and greatwood leaves. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class BlueForestAudit {
    static final BlockPos BASE=new BlockPos(96,200,96);
    static int ticks;static boolean oldBlue;static long[] green,blue;static volatile boolean ready,restored;
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_BLUE_FOREST FAIL "+text);System.out.println("TC4_BLUE_FOREST PASS "+text);}
    /** Average red, green and blue over the central screen area. */
    static long[] average(Minecraft mc){try(var im=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){long r=0,g=0,b=0,n=0;int w=im.getWidth(),h=im.getHeight();for(int x=w/2-150;x<w/2+150;x++)for(int y=h/2-150;y<h/2+150;y++){int c=im.getPixelRGBA(x,y);r+=c&255;g+=c>>8&255;b+=c>>16&255;n++;}return new long[]{r/n,g/n,b/n};}}
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.blueForestAuditOnly"))return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
        if(ticks==10){if(mc.player.isDeadOrDying())mc.player.respawn();mc.options.renderDistance().set(3);server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(true);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY()+6,BASE.getZ()+.5,0,90);});}
        if(ticks==60)server.execute(()->{
            var p=server.getPlayerList().getPlayer(uuid);var l=p.serverLevel();l.setDayTime(6000);
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){l.setBlock(BASE.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);for(int y=0;y<=8;y++)l.setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
            for(int x=-2;x<=2;x++)for(int z=1;z<=3;z++)l.setBlock(BASE.offset(x,0,z),TC4Modern.GREATWOOD_LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),3);
            var source=server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            server.getCommands().performPrefixedCommand(source,"fillbiome "+(BASE.getX()-16)+" "+(BASE.getY()-8)+" "+(BASE.getZ()-16)+" "+(BASE.getX()+16)+" "+(BASE.getY()+16)+" "+(BASE.getZ()+16)+" tc4modern:magical_forest");
            p.connection.teleport(BASE.getX()+.5,BASE.getY()+6,BASE.getZ()+.5,0,90);ready=true;
        });
        if(ticks==140){
            check(ready&&WarpClientConfig.SPEC.isLoaded()&&!WarpClientConfig.blueMagicalForest(),"real client configuration loaded with original default green forest");
            var registry=mc.level.registryAccess().registryOrThrow(Registries.BIOME);var forest=registry.getOrThrow(MagicalForestContent.BIOME);var plains=registry.getOrThrow(Biomes.PLAINS);
            check(mc.level.getBiome(BASE).is(MagicalForestContent.BIOME),"client sees the filled magical forest biome");
            check(forest.getGrassColor(BASE.getX(),BASE.getZ())==5635969&&forest.getFoliageColor()==6750149,"default keeps original green grass 5635969 and foliage 6750149");
            oldBlue=WarpClientConfig.blueMagicalForest();int plainsGrass=plains.getGrassColor(0,0),plainsFoliage=plains.getFoliageColor();
            WarpClientConfig.BLUE_MAGICAL_FOREST.set(true);
            check(forest.getGrassColor(BASE.getX(),BASE.getZ())==0x66AACC&&forest.getFoliageColor()==0x77CCEE,"option restores original blue grass 0x66AACC and foliage 0x77CCEE");
            check(plains.getGrassColor(0,0)==plainsGrass&&plains.getFoliageColor()==plainsFoliage,"other biomes keep their colours");
            check(GreatwoodColors.foliage(mc.level,BASE.offset(0,0,2))==0x77CCEE,"greatwood leaves average the blue foliage like original BlockMagicalLeaves");
            WarpClientConfig.BLUE_MAGICAL_FOREST.set(false);mc.options.hideGui=true;mc.levelRenderer.allChanged();
        }
        if(ticks==190){green=average(mc);LiquidDeathAudit.shot(mc,"magical-forest-green.png");WarpClientConfig.BLUE_MAGICAL_FOREST.set(true);mc.levelRenderer.allChanged();}
        if(ticks==240){blue=average(mc);LiquidDeathAudit.shot(mc,"magical-forest-blue.png");
            check(green[1]>green[2]&&blue[2]>blue[1]&&blue[2]>green[2]+20,"rendered grass and leaves turn from green to blue (rgb "+green[0]+","+green[1]+","+green[2]+" -> "+blue[0]+","+blue[1]+","+blue[2]+")");
            WarpClientConfig.BLUE_MAGICAL_FOREST.set(oldBlue);mc.options.hideGui=false;mc.levelRenderer.allChanged();
            server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setInvulnerable(false);p.getAbilities().flying=false;p.onUpdateAbilities();restored=true;});}
        if(ticks==260){check(restored&&WarpClientConfig.blueMagicalForest()==oldBlue,"fixture flags and original preference restored");System.out.println("TC4_BLUE_FOREST ALL PASS original blue magical forest grass and foliage colours");mc.stop();}
    }
}
