package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
/** Original TileOwned access strings, ItemKey and the three pressure-plate modes. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class WardedArcana {
 public static final DeferredHolder<Block,ArcaneDoor> DOOR=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_door"));
 public static final DeferredHolder<Block,ArcanePlate> PLATE=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_pressure_plate"));
 public static final DeferredHolder<Block,WardedGlass> GLASS=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("warded_glass"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<Owned>> ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("owned"));
 static Item key(boolean gold){return BuiltInRegistries.ITEM.get(AlchemyContent.id(gold?"gold_key":"iron_key"));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.BLOCK,h->{h.register(DOOR.getId(),new ArcaneDoor(Block.Properties.of().strength(-1,999).sound(SoundType.METAL).noOcclusion()));h.register(PLATE.getId(),new ArcanePlate(Block.Properties.of().strength(-1,999).sound(SoundType.WOOD).noOcclusion()));h.register(GLASS.getId(),new WardedGlass(Block.Properties.of().strength(-1,999).sound(SoundType.STONE).noOcclusion()));});e.register(Registries.ITEM,h->{h.register(DOOR.getId(),new DoorItem());h.register(PLATE.getId(),new BlockItem(PLATE.get(),new Item.Properties()));h.register(GLASS.getId(),new BlockItem(GLASS.get(),new Item.Properties()));h.register(AlchemyContent.id("iron_key"),new Key(false));h.register(AlchemyContent.id("gold_key"),new Key(true));});e.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(ENTITY.getId(),BlockEntityType.Builder.of(Owned::new,DOOR.get(),PLATE.get(),GLASS.get()).build(null)));}
 /** Blocks whose original hardness is -1 only while Config.wardedStone; otherwise BlockArcaneDoor 15, plate 2 and warded glass 5. */
 public interface Hardened { float cosmeticHardness(); }
 static boolean warded(){return WarpConfig.wardedStone();}
 /** Cosmetic mode: BlockArcaneDoor drops its item from the lower half, the plate and glass drop themselves. */
 static List<ItemStack> cosmeticDrops(BlockState s){if(warded()||s.is(DOOR.get())&&s.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER)return List.of();return List.of(new ItemStack(s.getBlock()));}
 static Owned owned(BlockGetter l,BlockPos p){return l.getBlockEntity(p) instanceof Owned t?t:null;}
 static boolean allowed(Owned t,String name){return t!=null&&(t.owner.equals(name)||t.access.contains("0"+name)||t.access.contains("1"+name));}
 static boolean manager(Owned t,String name){return t!=null&&(t.owner.equals(name)||t.access.contains("1"+name));}
 static void assign(Level l,BlockPos p,LivingEntity player){if(player instanceof Player pl&&owned(l,p) instanceof Owned t){t.owner=pl.getGameProfile().getName();t.changed();}}
 static BlockPos lower(BlockState s,BlockPos p){return s.is(DOOR.get())&&s.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?p.below():p;}
 public static final class Owned extends BlockEntity {
  String owner="";final List<String> access=new ArrayList<>();byte setting;
  Owned(BlockPos p,BlockState s){super(ENTITY.get(),p,s);}void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putString("owner",owner);var list=new ListTag();for(var name:access){var item=new CompoundTag();item.putString("name",name);list.add(item);}t.put("access",list);t.putByte("setting",setting);}
  @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);owner=t.getString("owner");access.clear();for(var item:t.getList("access",10))access.add(((CompoundTag)item).getString("name"));setting=t.getByte("setting");}
  @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();t.putString("owner",owner);t.putByte("setting",setting);return t;}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 }
 static boolean removeWithWand(ItemStack stack,UseOnContext c){var l=c.getLevel();var pos=c.getClickedPos();var s=l.getBlockState(pos);var p=c.getPlayer();if(p==null||!WandData.isWand(stack)||!(s.is(DOOR.get())||s.is(GLASS.get())||s.is(PLATE.get())&&!s.getValue(ArcanePlate.POWERED)))return false;var t=owned(l,pos);if(warded()&&(t==null||!t.owner.equals(p.getGameProfile().getName())))return false;
  if(!l.isClientSide){var base=lower(s,pos);l.levelEvent(2001,pos,Block.getId(s));
   /* Not warded: only the clicked half is removed and only a clicked lower half drops; a removed top lets the lower half drop itself. */
   if(s.is(DOOR.get())&&!warded()){if(base.equals(pos))Block.popResource(l,pos,new ItemStack(s.getBlock()));l.removeBlock(pos,false);}
   else{Block.popResource(l,pos,new ItemStack(s.getBlock()));if(s.is(DOOR.get())){l.removeBlock(base.above(),false);l.removeBlock(base,false);}else l.removeBlock(pos,false);}}else p.swing(c.getHand());return true;
 }
 public static final class DoorItem extends BlockItem {
  DoorItem(){super(DOOR.get(),new Item.Properties().stacksTo(1));}
  @Override public InteractionResult useOn(UseOnContext c){if(c.getClickedFace()!=Direction.UP)return InteractionResult.PASS;return super.useOn(c);}
 }
 public static final class ArcaneDoor extends DoorBlock implements EntityBlock,Hardened {
  @Override public float cosmeticHardness(){return 15;}@Override protected List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return cosmeticDrops(s);}
  static final MapCodec<ArcaneDoor> CODEC=simpleCodec(ArcaneDoor::new);ArcaneDoor(Properties p){super(BlockSetType.IRON,p);}@Override public MapCodec<? extends DoorBlock> codec(){return CODEC;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Owned(p,s);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=super.getStateForPlacement(c);return s==null?null:s.setValue(OPEN,false).setValue(POWERED,false);}
  @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity player,ItemStack stack){super.setPlacedBy(l,p,s,player,stack);assign(l,p,player);assign(l,p.above(),player);}
  @Override protected BlockState updateShape(BlockState s,Direction face,BlockState other,LevelAccessor l,BlockPos p,BlockPos from){if(face.getAxis()==Direction.Axis.Y&&((s.getValue(HALF)==DoubleBlockHalf.LOWER)==(face==Direction.UP)))return other.is(this)?s.setValue(FACING,other.getValue(FACING)).setValue(OPEN,other.getValue(OPEN)).setValue(HINGE,other.getValue(HINGE)):Blocks.AIR.defaultBlockState();return s;}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide){if(allowed(owned(l,p),player.getGameProfile().getName()))toggle(l,lower(s,p),!s.getValue(OPEN));else{player.displayClientMessage(Component.literal("The door refuses to budge."),false);ElementalTools.sound(l,p,"doorfail",.66f,1);}}return InteractionResult.sidedSuccess(l.isClientSide);}
  static void toggle(Level l,BlockPos p,boolean open){var s=l.getBlockState(p);if(!s.is(DOOR.get())||s.getValue(OPEN)==open)return;l.setBlock(p,s.setValue(OPEN,open),2);if(l.getBlockState(p.above()).is(DOOR.get()))l.setBlock(p.above(),l.getBlockState(p.above()).setValue(OPEN,open),2);l.playSound(null,p,l.random.nextBoolean()?net.minecraft.sounds.SoundEvents.WOODEN_DOOR_OPEN:net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,1,.9f+l.random.nextFloat()*.1f);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(l.isClientSide)return;if(b==PLATE.get()){var t=owned(l,p);if(t==null)return;var users=new ArrayList<String>();users.add(t.owner);for(var a:t.access)users.add(a.substring(1));int open=0;outer:for(var f:Direction.Plane.HORIZONTAL){var at=p.relative(f);var plate=l.getBlockState(at);if(!plate.is(PLATE.get()))continue;var pt=owned(l,at);if(pt==null)continue;for(var user:users)if(pt.owner.equals(user)||pt.access.contains(user)){if(plate.getValue(ArcanePlate.POWERED)){open=1;break outer;}open=-1;break;}}if(open!=0)toggle(l,lower(s,p),open==1);
   }else if(s.getValue(HALF)==DoubleBlockHalf.UPPER){if(!l.getBlockState(p.below()).is(this))l.removeBlock(p,false);else if(b!=Blocks.AIR&&b!=this)neighborChanged(l.getBlockState(p.below()),l,p.below(),b,from,moving);}else if(!l.getBlockState(p.above()).is(this)){/* BlockDoor: a lower half losing its top drops the door when not warded */if(!warded())Block.dropResources(s,l,p);l.removeBlock(p,false);}
  }
  @Override public boolean canEntityDestroy(BlockState s,BlockGetter l,BlockPos p,Entity e){return false;}@Override public void onBlockExploded(BlockState s,Level l,BlockPos p,Explosion e){}@Override public boolean canDropFromExplosion(BlockState s,BlockGetter l,BlockPos p,Explosion e){return false;}
 }
 public static final class ArcanePlate extends BaseEntityBlock implements Hardened {
  @Override public float cosmeticHardness(){return 2;}@Override protected List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return cosmeticDrops(s);}
  static final IntegerProperty MODE=IntegerProperty.create("mode",0,2);static final BooleanProperty POWERED=BooleanProperty.create("powered");static final MapCodec<ArcanePlate> CODEC=simpleCodec(ArcanePlate::new);ArcanePlate(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(POWERED,false).setValue(MODE,0));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(POWERED,MODE);}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Owned(p,s);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,s.getValue(POWERED)?.5:1,15);}@Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity player,ItemStack stack){assign(l,p,player);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){var t=owned(l,p);if(!l.isClientSide&&manager(t,player.getGameProfile().getName())){t.setting=(byte)((t.setting+1)%3);l.setBlock(p,s.setValue(MODE,(int)t.setting),2);player.displayClientMessage(Component.literal(switch(t.setting){case 1->"It will now trigger on everything except you.";case 2->"It will now trigger on just you.";default->"It will now trigger on everything.";}),false);l.playSound(null,p,net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f);t.changed();}return InteractionResult.sidedSuccess(l.isClientSide);}
  @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){if(!l.isClientSide&&!s.getValue(POWERED))evaluate(l,p);}
  @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(s.getValue(POWERED))evaluate(l,p);}
  static void evaluate(Level l,BlockPos p){var t=owned(l,p);if(t==null)return;var s=l.getBlockState(p);boolean previous=s.getValue(POWERED),active=false;for(var e:l.getEntities(null,new AABB(p.getX()+.125,p.getY(),p.getZ()+.125,p.getX()+.875,p.getY()+.25,p.getZ()+.875))){if(e.isIgnoringBlockTriggers())continue;boolean authorized=e instanceof Player player&&allowed(t,player.getGameProfile().getName());if(t.setting==1&&authorized||t.setting==2&&(!(e instanceof Player)||!authorized))continue;active=true;break;}if(active!=previous){l.setBlock(p,s.setValue(POWERED,active),2);l.updateNeighborsAt(p,PLATE.get());l.updateNeighborsAt(p.below(),PLATE.get());l.playSound(null,p,active?net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON:net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_OFF,net.minecraft.sounds.SoundSource.BLOCKS,.2f,active?.6f:.5f);}if(active)l.scheduleTick(p,PLATE.get(),20);}
  @Override protected boolean isSignalSource(BlockState s){return true;}@Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction f){return s.getValue(POWERED)?15:0;}@Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction f){return s.getValue(POWERED)&&f==Direction.UP?15:0;}
  @Override public boolean canEntityDestroy(BlockState s,BlockGetter l,BlockPos p,Entity e){return false;}@Override public void onBlockExploded(BlockState s,Level l,BlockPos p,Explosion e){}@Override public boolean canDropFromExplosion(BlockState s,BlockGetter l,BlockPos p,Explosion e){return false;}
 }
 public static final class WardedGlass extends BaseEntityBlock implements Hardened {
  @Override public float cosmeticHardness(){return 5;}@Override protected List<ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder b){return cosmeticDrops(s);}
  static final MapCodec<WardedGlass> CODEC=simpleCodec(WardedGlass::new);WardedGlass(Properties p){super(p);}@Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}@Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}@Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Owned(p,s);}
  @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity player,ItemStack stack){assign(l,p,player);}@Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
  @Override public boolean canEntityDestroy(BlockState s,BlockGetter l,BlockPos p,Entity e){return false;}@Override public void onBlockExploded(BlockState s,Level l,BlockPos p,Explosion e){}@Override public boolean canDropFromExplosion(BlockState s,BlockGetter l,BlockPos p,Explosion e){return false;}
 }
 public static final class Key extends Item {
  final boolean gold;Key(boolean g){super(new Item.Properties().rarity(Rarity.UNCOMMON));gold=g;}@Override public boolean isFoil(ItemStack s){return s.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);}
  @Override public InteractionResult onItemUseFirst(ItemStack s,UseOnContext c){var l=c.getLevel();var p=c.getPlayer();var at=c.getClickedPos();var state=l.getBlockState(at);if(p==null||!(state.is(DOOR.get())||state.is(PLATE.get())))return InteractionResult.SUCCESS;if(l.isClientSide)return InteractionResult.PASS;var pos=lower(state,at);var t=owned(l,pos);if(t==null)return InteractionResult.SUCCESS;String name=p.getGameProfile().getName(),location=pos.getX()+","+pos.getY()+","+pos.getZ();var tag=NoteData.tag(s);boolean bound=s.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);int type=gold?1:0;
   if(!bound){if(t.owner.equals(name)||!gold&&t.access.contains("1"+name)){var key=new ItemStack(key(gold));var nbt=new CompoundTag();nbt.putString("location",location);nbt.putByte("type",(byte)(state.is(DOOR.get())?0:1));key.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(nbt));if(!p.addItem(key))p.drop(key,false);if(!p.getAbilities().instabuild)s.shrink(1);message(p,state.is(DOOR.get())?"tc.key1":"tc.key2",null);ElementalTools.sound(l,at,"key",1,.9f);p.swing(c.getHand());}}
   else if(!t.owner.equals(name)&&!t.access.contains(type+name)&&!t.access.contains("1"+name)&&location.equals(tag.getString("location"))){t.access.add(type+name);t.changed();if(state.is(DOOR.get())){var upper=owned(l,pos.above());if(upper!=null){upper.access.add(type+name);upper.changed();}}message(p,state.is(DOOR.get())?"tc.key3":"tc.key5",gold?(state.is(DOOR.get())?"tc.key4":"tc.key6"):null);ElementalTools.sound(l,at,"key",1,1.1f);if(!p.getAbilities().instabuild)s.shrink(1);p.swing(c.getHand());}
   else message(p,location.equals(tag.getString("location"))?"tc.key8":"tc.key7",null);return InteractionResult.SUCCESS;
  }
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){var t=NoteData.tag(s);if(t.contains("location")){lines.add(Component.translatable("tc.key9").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC));lines.add(Component.translatable(t.getByte("type")==0?"tc.key10":"tc.key11").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));var parts=t.getString("location").split(",");lines.add(Component.literal(parts.length==3?"x "+parts[0]+", z "+parts[2]+", y "+parts[1]:t.getString("location")));}}
  static void message(Player p,String key,String extra){var text=Component.translatable(key);if(extra!=null)text.append(Component.translatable(extra));p.displayClientMessage(text.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);}
 }
 @EventBusSubscriber(modid=TC4Modern.MODID)static final class Protection {
  @SubscribeEvent static void broken(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){if(warded()&&e.getLevel().getBlockEntity(e.getPos()) instanceof Owned)e.setCanceled(true);}
 }
}
