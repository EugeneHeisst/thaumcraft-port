package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden screenshot sweep: every tc4modern block placed in the world, ten per page, to catch render-type and model mistakes. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class BlockGalleryShow {
 static final BlockPos BASE=new BlockPos(720,121,-200);static final int PER=10;static int ticks,page=-1,fov=70;static List<Block> blocks;
 static List<Block> blocks(){if(blocks==null){blocks=new ArrayList<>();for(var b:BuiltInRegistries.BLOCK)if(BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(TC4Modern.MODID)&&!(b instanceof net.minecraft.world.level.block.LiquidBlock))blocks.add(b);}return blocks;}
 /* x grows to the left when looking south, so index 0 sits on the left of the picture */
 static BlockPos at(int i){return BASE.offset(6-i%5*3,0,2+i/5*3);}
 static void build(net.minecraft.server.level.ServerPlayer p,int pg){
  var l=p.serverLevel();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(16)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-11;x<=11;x++)for(int z=-3;z<=8;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),2|16);
  var names=new StringBuilder();
  for(int i=0;i<PER&&pg*PER+i<blocks().size();i++){var b=blocks().get(pg*PER+i);BlockState st=b.defaultBlockState();var pos=at(i);names.append(i).append(':').append(BuiltInRegistries.BLOCK.getKey(b).getPath()).append(' ');
   if(st.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){l.setBlock(pos,st,2|16);l.setBlock(pos.above(),st.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),2|16);}
   else{try{l.setBlock(pos,st,2|16);}catch(RuntimeException e){System.out.println("TC4_BLOCK_GALLERY place failed "+b+" "+e);}}}
  System.out.println("TC4_BLOCK_GALLERY page "+pg+" "+names);
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+3.6,BASE.getZ()-3.8,0,30);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.blockGalleryShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==60){mc.options.hideGui=true;fov=mc.options.fov().get();mc.options.fov().set(90);s.execute(()->{var p=s.getPlayerList().getPlayer(id);if(p.level()!=s.overworld())p.teleportTo(s.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});}
  if(ticks>=100&&(ticks-100)%60==0){
   if(page>=0)LiquidDeathAudit.shot(mc,"blocks-"+page+".png");
   int next=page+1;if(next*PER>=blocks().size()){System.out.println("TC4_BLOCK_GALLERY DONE pages "+next+" blocks "+blocks().size());mc.options.fov().set(fov);mc.options.save();mc.stop();return;}
   page=next;s.execute(()->build(s.getPlayerList().getPlayer(id),next));}
 }
}
