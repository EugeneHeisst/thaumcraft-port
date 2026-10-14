package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Hidden screenshot sweep: every tc4modern entity type spawned without AI, five per page, to catch missing renderers or textures. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class EntityGalleryShow {
 static final BlockPos BASE=new BlockPos(720,121,-260);static final int PER=5;static int ticks,page=-1,fov=70;static List<EntityType<?>> types;
 static List<EntityType<?>> types(){if(types==null){types=new ArrayList<>();for(var t:BuiltInRegistries.ENTITY_TYPE)if(BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals(TC4Modern.MODID))types.add(t);}return types;}
 static void build(net.minecraft.server.level.ServerPlayer p,int pg){
  var l=p.serverLevel();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(24)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-12;x<=12;x++)for(int z=-4;z<=10;z++)for(int y=-1;y<8;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),2|16);
  var names=new StringBuilder();
  for(int i=0;i<PER&&pg*PER+i<types().size();i++){var t=types().get(pg*PER+i);names.append(i).append(':').append(BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()).append(' ');
   try{var e=t.create(l);if(e==null){names.append("(null) ");continue;}e.moveTo(BASE.getX()+.5+8-i*4,BASE.getY(),BASE.getZ()+5.5,180,0);e.setNoGravity(true);if(e instanceof Mob m){m.setNoAi(true);m.setPersistenceRequired();}e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);e.setInvulnerable(true);l.addFreshEntity(e);}
   catch(RuntimeException ex){names.append("(failed ").append(ex).append(") ");}}
  System.out.println("TC4_ENTITY_GALLERY page "+pg+" "+names);
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+2.5,BASE.getZ()-3.5,0,12);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.entityGalleryShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==60){mc.options.hideGui=true;fov=mc.options.fov().get();mc.options.fov().set(90);s.execute(()->{var p=s.getPlayerList().getPlayer(id);if(p.level()!=s.overworld())p.teleportTo(s.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});}
  if(ticks>=100&&(ticks-100)%50==0){
   if(page>=0)LiquidDeathAudit.shot(mc,"entities-"+page+".png");
   int next=page+1;if(next*PER>=types().size()){System.out.println("TC4_ENTITY_GALLERY DONE pages "+next+" types "+types().size());mc.options.fov().set(fov);mc.options.save();mc.stop();return;}
   page=next;s.execute(()->build(s.getPlayerList().getPlayer(id),next));}
 }
}
