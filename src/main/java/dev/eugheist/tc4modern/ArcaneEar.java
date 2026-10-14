package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** TileSensor: old five-instrument notes, inclusive64-block range and ten-tick redstone pulse. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ArcaneEar {
 public static final DeferredHolder<Block,EarBlock> BLOCK=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_ear"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<EarEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("arcane_ear"));
 @SubscribeEvent static void register(RegisterEvent event){event.register(Registries.BLOCK,h->h.register(BLOCK.getId(),new EarBlock(Block.Properties.of().strength(2.5f,TC4SoundTypes.res(10)).sound(SoundType.WOOD).noOcclusion())));event.register(Registries.ITEM,h->h.register(BLOCK.getId(),new BlockItem(BLOCK.get(),new Item.Properties())));event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(EarEntity::new,BLOCK.get()).build(null)));}
 record Note(BlockPos pos,int tone,int note){}
 static final Map<Level,List<Note>> NOTES=new WeakHashMap<>();
 static int legacyTone(NoteBlockInstrument instrument){return switch(instrument){case HARP->0;case BASEDRUM->1;case SNARE->2;case HAT->3;case BASS->4;default->-1;};}
 /** Legacy Material mapping, deliberately ignoring modern extra note-block instruments. */
 static int floorTone(BlockState state){if(state.getSoundType()==SoundType.WOOD||state.is(net.minecraft.tags.BlockTags.PLANKS)||state.is(net.minecraft.tags.BlockTags.LOGS))return 4;
  if(state.getSoundType()==SoundType.GLASS)return 3;
  if(state.getSoundType()==SoundType.SAND||state.is(Blocks.GRAVEL)||state.is(Blocks.SOUL_SAND))return 2;
  if(state.getSoundType()==SoundType.STONE)return 1;
  var instrument=state.instrument();int tone=legacyTone(instrument);return tone<0?0:tone;}
 @EventBusSubscriber(modid=TC4Modern.MODID)
 public static final class Events {
  @SubscribeEvent static void note(net.neoforged.neoforge.event.level.NoteBlockEvent.Play event){if(!event.getLevel().isClientSide()&&event.getLevel() instanceof Level l)NOTES.computeIfAbsent(l,k->new ArrayList<>()).add(new Note(event.getPos().immutable(),legacyTone(event.getInstrument()),event.getVanillaNoteId()));}
  @SubscribeEvent static void end(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event){var notes=NOTES.get(event.getLevel());if(notes!=null)notes.clear();}
  @SubscribeEvent static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event){NOTES.remove(event.getLevel());}
 }
 public static final class EarBlock extends BaseEntityBlock {
  static final MapCodec<EarBlock> CODEC=simpleCodec(EarBlock::new);static final BooleanProperty ACTIVE=BooleanProperty.create("active");EarBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(ACTIVE);}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EarEntity(p,s);}@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ENTITY.get(),EarEntity::tick);}
  @Override protected boolean isSignalSource(BlockState s){return true;}@Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return l.getBlockEntity(p) instanceof EarEntity ear&&ear.redstoneSignal>0?15:0;}@Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof EarEntity ear){ear.note=(byte)((ear.note+1)%25);ear.setChanged();ear.trigger(true);}return InteractionResult.sidedSuccess(l.isClientSide);}
  @Override protected void attack(BlockState s,Level l,BlockPos p,Player player){if(!l.isClientSide&&l.getBlockEntity(p) instanceof EarEntity ear)ear.trigger(true);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block block,BlockPos from,boolean moving){if(l.getBlockEntity(p) instanceof EarEntity ear)ear.updateTone();}
  @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(l.getBlockEntity(p) instanceof EarEntity ear)ear.updateTone();}
  @Override protected boolean triggerEvent(BlockState s,Level l,BlockPos p,int tone,int note){if(tone<=4){if(l.isClientSide){if(tone>=0){var sound=switch(tone){case 1->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM;case 2->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_SNARE;case 3->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT;case 4->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS;default->net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP;};l.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,sound.value(),net.minecraft.sounds.SoundSource.RECORDS,3,(float)Math.pow(2,(note-12)/12.0),false);}l.addParticle(net.minecraft.core.particles.ParticleTypes.NOTE,p.getX()+.5,p.getY()+1.2,p.getZ()+.5,note/24.0,0,0);}return true;}return super.triggerEvent(s,l,p,tone,note);}
 }
 public static final class EarEntity extends BlockEntity {
  byte note,tone;int redstoneSignal;EarEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}
  void updateTone(){if(level!=null)tone=(byte)floorTone(level.getBlockState(worldPosition.below()));}
  void trigger(boolean sound){if(level.getBlockState(worldPosition.above()).isAir())level.blockEvent(worldPosition,BLOCK.get(),sound?floorTone(level.getBlockState(worldPosition.below())):-1,note);}
  void notifySignal(){var active=redstoneSignal>0;if(getBlockState().getValue(EarBlock.ACTIVE)!=active)level.setBlock(worldPosition,getBlockState().setValue(EarBlock.ACTIVE,active),2);level.updateNeighborsAt(worldPosition,BLOCK.get());level.updateNeighborsAt(worldPosition.below(),BLOCK.get());}
  static void tick(Level l,BlockPos p,BlockState s,EarEntity ear){if(s.getValue(EarBlock.ACTIVE)!=(ear.redstoneSignal>0))ear.notifySignal();if(ear.redstoneSignal>0&&--ear.redstoneSignal==0)ear.notifySignal();var notes=NOTES.get(l);if(notes!=null)for(var n:notes){if(n.tone!=ear.tone||n.note!=ear.note||n.pos.distSqr(p)>4096)continue;ear.trigger(false);ear.redstoneSignal=10;ear.notifySignal();break;}}
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putByte("note",note);tag.putByte("tone",tone);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);note=(byte)net.minecraft.util.Mth.clamp(tag.getByte("note"),0,24);tone=tag.getByte("tone");}
 }
}
