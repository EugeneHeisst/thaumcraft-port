package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.Aspect;

/** Original TileMirror and TileMirrorEssentia. Dimension names bridge legacy integer IDs. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class MagicMirrors {
 public static final DeferredHolder<Block,MirrorBlock> ITEM=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("mirror")),ESSENTIA=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("mirror_essentia"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MirrorEntity>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("mirror"));
 static final TicketType<BlockPos> DELIVERY=TicketType.create("tc4_mirror_delivery",BlockPos::compareTo,100);
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{h.register(ITEM.getId(),new MirrorBlock(Block.Properties.of().strength(1,TC4SoundTypes.res(10)).sound(TC4SoundTypes.MIRROR).noOcclusion().noCollission()));h.register(ESSENTIA.getId(),new MirrorBlock(Block.Properties.of().strength(1,TC4SoundTypes.res(10)).sound(TC4SoundTypes.MIRROR).noOcclusion().noCollission()));});e.register(Registries.ITEM,h->{h.register(ITEM.getId(),new MirrorItem(ITEM.get()));h.register(ESSENTIA.getId(),new MirrorItem(ESSENTIA.get()));});e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(MirrorEntity::new,ITEM.get(),ESSENTIA.get()).build(null)));}
 @SubscribeEvent static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ENTITY.get(),(t,d)->t.essentia()?null:new net.neoforged.neoforge.items.wrapper.InvWrapper(t));}
 static ResourceKey<Level> dimension(CompoundTag tag){var id=ResourceLocation.tryParse(tag.getString("linkDimension"));return id!=null?ResourceKey.create(Registries.DIMENSION,id):switch(tag.getInt("linkDim")){case -1->Level.NETHER;case 1->Level.END;default->Level.OVERWORLD;};}
 static BlockPos position(CompoundTag tag){return new BlockPos(tag.getInt("linkX"),tag.getInt("linkY"),tag.getInt("linkZ"));}
 static void coordinates(CompoundTag tag,ResourceKey<Level> dim,BlockPos p){tag.putInt("linkX",p.getX());tag.putInt("linkY",p.getY());tag.putInt("linkZ",p.getZ());tag.putString("linkDimension",dim.location().toString());tag.putInt("linkDim",dim==Level.NETHER?-1:dim==Level.END?1:0);tag.putString("dimname",dim.location().toString());}
 static void bind(ItemStack s,MirrorEntity t){var tag=NoteData.tag(s);coordinates(tag,t.getLevel().dimension(),t.getBlockPos());s.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
 static MirrorEntity destination(Level origin,CompoundTag tag){if(origin.getServer()==null)return null;var target=origin.getServer().getLevel(dimension(tag));return target!=null&&target.getBlockEntity(position(tag)) instanceof MirrorEntity t?t:null;}
 static void sound(Player p,String id,float volume,float pitch){p.level().playSound(null,p.getX(),p.getY(),p.getZ(),InfusionContent.MatrixEntity.sound(id),net.minecraft.sounds.SoundSource.PLAYERS,volume,pitch);}
 static void tooltip(ItemStack s,List<Component> lines,String key){var tag=NoteData.tag(s);if(tag.contains("linkX"))lines.add((key.equals("Linked to")?Component.literal(key):Component.translatable(key)).append(" "+tag.getInt("linkX")+", "+tag.getInt("linkY")+", "+tag.getInt("linkZ")+" in "+tag.getString("dimname")).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
 public static final class MirrorItem extends BlockItem {
  MirrorItem(Block b){super(b,new Properties().rarity(Rarity.UNCOMMON));}
  @Override public InteractionResult onItemUseFirst(ItemStack s,UseOnContext c){if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof MirrorEntity t)||t.essentia()!=(getBlock()==ESSENTIA.get()))return InteractionResult.PASS;if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;var p=c.getPlayer();if(t.valid()){p.displayClientMessage(Component.literal("That mirror is already linked to a valid destination.").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);return InteractionResult.CONSUME;}var copy=new ItemStack(this);bind(copy,t);if(!p.addItem(copy))p.drop(copy,false);if(!p.getAbilities().instabuild)s.shrink(1);sound(p,"jar",1,2);return InteractionResult.CONSUME;}
  @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level l,Player p,ItemStack s,BlockState state){if(l.getBlockEntity(pos) instanceof MirrorEntity t&&NoteData.tag(s).contains("linkX")){t.readLink(NoteData.tag(s));if(!l.isClientSide)t.restore();}return super.updateCustomBlockEntityTag(pos,l,p,s,state);}
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){tooltip(s,lines,"Linked to");}
 }
 public static final class MirrorBlock extends BaseEntityBlock {
  static final MapCodec<MirrorBlock> CODEC=simpleCodec(MirrorBlock::new);MirrorBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.UP));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(BlockStateProperties.FACING);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(BlockStateProperties.FACING,c.getClickedFace());return canSurvive(s,c.getLevel(),c.getClickedPos())?s:null;}
  @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var d=s.getValue(BlockStateProperties.FACING);return l.getBlockState(p.relative(d.getOpposite())).isFaceSturdy(l,p.relative(d.getOpposite()),d);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(!l.isClientSide&&!canSurvive(s,l,p)){Block.dropResources(s,l,p,l.getBlockEntity(p));l.removeBlock(p,false);}}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(BlockStateProperties.FACING)){case DOWN->Block.box(0,15,0,16,16,16);case UP->Block.box(0,0,0,16,1,16);case NORTH->Block.box(0,0,15,16,16,16);case SOUTH->Block.box(0,0,0,16,16,1);case WEST->Block.box(15,0,0,16,16,16);case EAST->Block.box(0,0,0,1,16,16);};}
  @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new MirrorEntity(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ENTITY.get(),MirrorEntity::tick);}
  @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){if(!l.isClientSide&&entity instanceof ItemEntity item&&!item.isRemoved()&&!item.hasPickUpDelay()&&l.getBlockEntity(p) instanceof MirrorEntity t&&!t.essentia())t.transport(item);}
  @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){var drop=new ItemStack(this);if(b.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MirrorEntity t&&t.linked){var tag=new CompoundTag();coordinates(tag,t.linkDimension,t.link);drop.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}return List.of(drop);}
  @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof MirrorEntity t)t.invalidate();super.onRemove(s,l,p,next,moving);}
  @Override protected boolean triggerEvent(BlockState s,Level l,BlockPos p,int id,int data){if(id==1){if(l.isClientSide)MagicMirrorsClient.particles(l,p,s.getValue(BlockStateProperties.FACING));return true;}return super.triggerEvent(s,l,p,id,data);}
 }
 public static final class MirrorEntity extends BlockEntity implements Container {
  boolean linked;BlockPos link=BlockPos.ZERO;ResourceKey<Level> linkDimension=Level.OVERWORLD;int instability,count,inc=40;final List<ItemStack> output=new ArrayList<>();
  MirrorEntity(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}boolean essentia(){return getBlockState().is(ESSENTIA.get());}Direction facing(){return getBlockState().getValue(BlockStateProperties.FACING);}
  MirrorEntity target(){if(level==null||level.getServer()==null)return null;var remote=level.getServer().getLevel(linkDimension);return remote!=null&&remote.getBlockEntity(link) instanceof MirrorEntity t&&t.essentia()==essentia()?t:null;}
  boolean simple(){var t=linked?target():null;return t!=null&&t.linked&&t.link.equals(worldPosition)&&t.linkDimension==level.dimension();}
  boolean valid(){if(!linked)return false;if(level.getServer().getLevel(linkDimension)==null)return false;if(simple())return true;linked=false;changed();return false;}
  void restore(){var t=target();if(t!=null&&!t.valid()){t.link=worldPosition;t.linkDimension=level.dimension();t.linked=linked=true;changed();t.changed();}}
  void invalidate(){if(level==null||level.isClientSide)return;var remote=level.getServer().getLevel(linkDimension);if(remote!=null&&remote.hasChunkAt(link)&&remote.getBlockEntity(link) instanceof MirrorEntity t&&t.essentia()==essentia()){t.linked=false;t.changed();}}
  void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  void event(){level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);}
  void add(ItemStack s){output.add(s.copy());changed();keepTicking();}
  void keepTicking(){if(level instanceof ServerLevel server)server.getChunkSource().addRegionTicket(DELIVERY,new ChunkPos(worldPosition),2,worldPosition);}
  boolean transport(ItemEntity entity){if(entity.getPersistentData().contains("tc4_mirror_exit")&&entity.getPersistentData().getLong("tc4_mirror_exit")==worldPosition.asLong())return false;if(essentia()||!valid())return false;var t=target();if(t==null)return false;t.add(entity.getItem());instability+=entity.getItem().getCount();entity.discard();changed();event();return true;}
  boolean spawn(ItemStack s){var d=facing();var item=new ItemEntity(level,worldPosition.getX()+.5-d.getStepX()*.3,worldPosition.getY()+.5-d.getStepY()*.3,worldPosition.getZ()+.5-d.getStepZ()*.3,s);item.setDeltaMovement(d.getStepX()*.15,d.getStepY()*.15,d.getStepZ()*.15);item.setPickUpDelay(20);item.getPersistentData().putLong("tc4_mirror_exit",worldPosition.asLong());return level.addFreshEntity(item);}
  boolean take(Aspect a,int amount){if(!essentia()||amount>1||!valid())return false;var t=target();return t!=null&&EssentiaSources.drain(t,a,t.facing(),8,true);}
  static void tick(Level l,BlockPos p,BlockState s,MirrorEntity t){if(!t.essentia()){
   if(!t.output.isEmpty()){t.keepTicking();int rate=t.instability/50;if(t.count>20&&(rate==0||t.count%(rate*rate)==0)){int index=l.random.nextInt(t.output.size());var stack=t.output.get(index);if(t.spawn(stack.copyWithCount(1))){stack.shrink(1);if(stack.isEmpty())t.output.remove(index);t.instability++;t.changed();t.event();}}}
   if(t.instability>0&&t.count%20==0){t.instability--;t.changed();}if(t.instability>0){int drained=VisNetwork.drain((ServerLevel)l,p,Aspect.ORDER,1,(node,color)->{});if(drained>0){t.instability-=drained;var target=t.target();if(target!=null){target.instability=Math.max(0,target.instability-drained);target.changed();}t.changed();}}
  }if(t.count++%t.inc==0){if(!t.simple()){if(t.inc<600)t.inc+=20;t.restore();}else t.inc=40;}}
  @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide&&!output.isEmpty())keepTicking();}
  void readLink(CompoundTag tag){link=position(tag);linkDimension=dimension(tag);}
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);coordinates(tag,linkDimension,link);tag.putBoolean("linked",linked);tag.putInt("instability",instability);var items=new ListTag();for(var s:output)if(!s.isEmpty())items.add(s.save(r));tag.put("Items",items);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);readLink(tag);linked=tag.getBoolean("linked");instability=tag.getInt("instability");output.clear();var list=tag.getList("Items",10);for(int i=0;i<list.size();i++)ItemStack.parse(r,list.getCompound(i)).ifPresent(output::add);}
  @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var tag=new CompoundTag();coordinates(tag,linkDimension,link);tag.putBoolean("linked",linked);tag.putInt("instability",instability);return tag;}
  @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
  public int getContainerSize(){return essentia()?0:1;}public boolean isEmpty(){return true;}public ItemStack getItem(int i){return ItemStack.EMPTY;}public ItemStack removeItem(int i,int n){return ItemStack.EMPTY;}public ItemStack removeItemNoUpdate(int i){return ItemStack.EMPTY;}public void clearContent(){}public boolean stillValid(Player p){return false;}
  public boolean canPlaceItem(int i,ItemStack s){return !essentia()&&valid();}public void setItem(int i,ItemStack s){if(level==null||level.isClientSide||s.isEmpty()||essentia())return;var t=valid()?target():null;if(t!=null){t.add(s);instability+=s.getCount();changed();event();}else spawn(s.copy());}
 }
 @EventBusSubscriber(modid=TC4Modern.MODID)
 public static final class OutputExit {
  @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){if(e.getEntity() instanceof ItemEntity item&&!item.level().isClientSide){var n=item.getPersistentData();if(n.contains("tc4_mirror_exit")&&!item.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(BlockPos.of(n.getLong("tc4_mirror_exit")))))n.remove("tc4_mirror_exit");}}
 }

}
