package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Original Config.golemChestInteract: golem inventory tasks call openInventory on every transfer and closeInventory when
 * the task resets after its five-tick countChest, so chests show the lid animation and play the open/close sounds.
 * Player openers are kept: the lid only closes, and the close sound only plays, when no player still has it open.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class GolemChests {
 static final Map<ServerLevel,Map<BlockPos,Integer>> OPEN=new WeakHashMap<>();
 static int opened,closed;
 static void interact(Level level,BlockPos pos){
  if(!GolemConfig.chestInteract()||!(level instanceof ServerLevel server)||!(level.getBlockEntity(pos) instanceof ChestBlockEntity))return;
  if(OPEN.computeIfAbsent(server,k->new HashMap<>()).put(pos.immutable(),5)!=null)return;
  int players=ChestBlockEntity.getOpenCount(level,pos);level.blockEvent(pos,level.getBlockState(pos).getBlock(),1,players+1);if(players==0)sound(level,pos,SoundEvents.CHEST_OPEN);opened++;
 }
 static void sound(Level level,BlockPos pos,SoundEvent sound){
  var state=level.getBlockState(pos);double x=pos.getX()+.5,y=pos.getY()+.5,z=pos.getZ()+.5;
  if(state.hasProperty(ChestBlock.TYPE)&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){var d=ChestBlock.getConnectedDirection(state);x+=d.getStepX()*.5;z+=d.getStepZ()*.5;}
  level.playSound(null,x,y,z,sound,SoundSource.BLOCKS,.5f,level.random.nextFloat()*.1f+.9f);
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event){
  if(!(event.getLevel() instanceof ServerLevel server))return;var open=OPEN.get(server);if(open==null||open.isEmpty())return;
  for(var it=open.entrySet().iterator();it.hasNext();){var e=it.next();if(e.setValue(e.getValue()-1)>1)continue;it.remove();var pos=e.getKey();
   if(!(server.getBlockEntity(pos) instanceof ChestBlockEntity))continue;int players=ChestBlockEntity.getOpenCount(server,pos);server.blockEvent(pos,server.getBlockState(pos).getBlock(),1,players);if(players==0)sound(server,pos,SoundEvents.CHEST_CLOSE);closed++;}
 }
}
