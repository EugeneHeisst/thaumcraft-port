package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
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
import thaumcraft.api.aspects.*;

/** TileArcaneBore/Base: spiral sampling and the optional twenty-block entropy accelerator. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ArcaneBore {
 public static final DeferredHolder<Block,BoreBlock> BASE=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_bore_base")),BORE=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_bore"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BaseEntity>> BASE_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,BASE.getId());
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BoreEntity>> BORE_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,BORE.getId());
 public static final DeferredHolder<MenuType<?>,MenuType<Menu>> MENU=DeferredHolder.create(Registries.MENU,BORE.getId());
 static java.util.function.Consumer<BoreEntity> effects=t->{};
 @SubscribeEvent static void register(RegisterEvent e){
  e.register(Registries.BLOCK,h->{h.register(BASE.getId(),new BoreBlock(false,Block.Properties.of().strength(2.5f,TC4SoundTypes.res(10)).sound(SoundType.WOOD).noOcclusion()));h.register(BORE.getId(),new BoreBlock(true,Block.Properties.of().strength(2.5f,TC4SoundTypes.res(10)).sound(SoundType.WOOD).noOcclusion()));});
  e.register(Registries.ITEM,h->{h.register(BASE.getId(),new BlockItem(BASE.get(),new Item.Properties()));h.register(BORE.getId(),new BlockItem(BORE.get(),new Item.Properties()));});
  e.register(Registries.BLOCK_ENTITY_TYPE,h->{h.register(BASE_ENTITY.getId(),BlockEntityType.Builder.of(BaseEntity::new,BASE.get()).build(null));h.register(BORE_ENTITY.getId(),BlockEntityType.Builder.of(BoreEntity::new,BORE.get()).build(null));});
  e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new Menu(id,inv,(BoreEntity)inv.player.level().getBlockEntity(buf.readBlockPos())))));
 }
 static final DirectionProperty FACE=BlockStateProperties.FACING;static final EnumProperty<AttachFace> MOUNT=BlockStateProperties.ATTACH_FACE;
 public static final class BoreBlock extends BaseEntityBlock {
  final boolean bore;static final MapCodec<BoreBlock> CODEC=simpleCodec(p->new BoreBlock(true,p));
  BoreBlock(boolean b,Properties p){super(p);bore=b;registerDefaultState(stateDefinition.any().setValue(FACE,Direction.UP).setValue(MOUNT,AttachFace.FLOOR));}
  @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACE,MOUNT);}
  @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){if(bore&&c.getClickedFace().getAxis()!=Direction.Axis.Y)return null;return defaultBlockState().setValue(FACE,bore?c.getNearestLookingDirection().getOpposite():c.getHorizontalDirection().getOpposite()).setValue(MOUNT,c.getClickedFace()==Direction.DOWN?AttachFace.CEILING:AttachFace.FLOOR);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){if(!bore)return Shapes.block();var d=s.getValue(FACE);return Shapes.box(d.getStepX()<0?-1:0,d.getStepY()<0?-1:0,d.getStepZ()<0?-1:0,1+(d.getStepX()>0?1:0),1+(d.getStepY()>0?1:0),1+(d.getStepZ()>0?1:0));}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return bore?new BoreEntity(p,s):new BaseEntity(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return bore?createTickerHelper(t,BORE_ENTITY.get(),BoreEntity::tick):null;}
  @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(WandData.isWand(stack)){if(!l.isClientSide)l.setBlock(p,s.setValue(FACE,hit.getDirection()),3);ElementalTools.sound(l,p,"tool",.3f,1.9f+l.random.nextFloat()*.2f);player.swing(hand);return ItemInteractionResult.sidedSuccess(l.isClientSide);}return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(bore){if(player instanceof net.minecraft.server.level.ServerPlayer sp&&l.getBlockEntity(p) instanceof BoreEntity t)sp.openMenu(t,p);return InteractionResult.sidedSuccess(l.isClientSide);}return InteractionResult.PASS;}
  @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof BoreEntity t)Containers.dropContents(l,p,t);super.onRemove(s,l,p,next,moving);}
  @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos from,boolean moving){if(bore&&!l.isClientSide){var support=p.relative(s.getValue(MOUNT)==AttachFace.CEILING?Direction.UP:Direction.DOWN);if(!(l.getBlockState(support).getBlock() instanceof BoreBlock bb&&!bb.bore)&&!l.getBlockState(support).is(AlchemyContent.GREATWOOD_PLANKS.get())&&!l.getBlockState(support).is(AlchemyContent.SILVERWOOD_PLANKS.get()))l.destroyBlock(p,true);}}
 }
 public static final class BaseEntity extends BlockEntity implements IEssentiaTransport {
  BaseEntity(BlockPos p,BlockState s){super(BASE_ENTITY.get(),p,s);}Direction facing(){return getBlockState().getValue(FACE);}
  boolean drawEssentia(){for(var face:Direction.values()){var t=EssentiaTransport.connectable(level,worldPosition,face);if(t==null)continue;var opposite=face.getOpposite();if(!t.canOutputTo(opposite))return false; // Original exits at the first non-output neighbour.
   if(t.getSuctionAmount(opposite)<getSuctionAmount(face)&&t.takeEssentia(Aspect.ENTROPY,1,opposite)==1)return true;}return false;}
  @Override public boolean isConnectable(Direction f){return true;}@Override public boolean canOutputTo(Direction f){return false;}@Override public boolean canInputFrom(Direction f){return true;}
  @Override public void setSuction(Aspect a,int n){}@Override public Aspect getSuctionType(Direction f){return Aspect.ENTROPY;}@Override public int getSuctionAmount(Direction f){return f==facing()?0:128;}
  @Override public int getMinimumSuction(){return 0;}@Override public boolean renderExtendedTube(){return true;}@Override public Aspect getEssentiaType(Direction f){return null;}@Override public int getEssentiaAmount(Direction f){return 0;}
  @Override public int takeEssentia(Aspect a,int n,Direction f){return 0;}@Override public int addEssentia(Aspect a,int n,Direction f){return 0;}
 }
 public static final class BoreEntity extends BlockEntity implements Container,MenuProvider {
  final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);final AspectList repairCost=new AspectList(),repairVis=new AspectList();
  int spiral,count,fortune,area,speed,repairCounter,rotX,rotZ,tarX,tarZ,speedX,speedZ,topRotation,paused=100,maxPause=100,beamLength;
  long soundDelay;float currentRadius,radInc,speedyTime,vRadX,vRadZ,tRadX,tRadZ,mRadX,mRadZ;BlockPos last=BlockPos.ZERO,dig=BlockPos.ZERO;boolean toDig,first=true;BlockState digState=Blocks.AIR.defaultBlockState();
  net.neoforged.neoforge.common.util.FakePlayer fake;
  BoreEntity(BlockPos p,BlockState s){super(BORE_ENTITY.get(),p,s);}Direction facing(){return getBlockState().getValue(FACE);}Direction mount(){return getBlockState().getValue(MOUNT)==AttachFace.CEILING?Direction.DOWN:Direction.UP;}
  BaseEntity base(){return level.getBlockEntity(worldPosition.relative(mount().getOpposite())) instanceof BaseEntity b?b:null;}
  boolean gettingPower(){return level.hasNeighborSignal(worldPosition)||level.hasNeighborSignal(worldPosition.relative(mount().getOpposite()));}
  boolean validItems(){var p=items.get(1);var electric=dev.eugheist.tc4modern.api.TC4Addons.boreTool(p);return items.get(0).getItem() instanceof FociWorld.ExcavationFocus&&(electric!=null?electric.canUse(p):p.getItem() instanceof PickaxeItem&&p.isDamageableItem()&&p.getDamageValue()+1<p.getMaxDamage());}
  int upgrade(int id){return items.get(0).getItem() instanceof FociContent.Focus f?f.getUpgradeLevel(items.get(0),id):0;}
  int enchant(ItemStack s,net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key){return s.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key));}
  void properties(){fortune=Math.max(upgrade(FociWorld.TREASURE),level==null?0:enchant(items.get(1),Enchantments.FORTUNE));area=upgrade(FociWorld.ENLARGE);speed=upgrade(FociContent.POTENCY)+(level==null?0:enchant(items.get(1),Enchantments.EFFICIENCY));}
  boolean silk(){return enchant(items.get(1),Enchantments.SILK_TOUCH)>0||upgrade(FociWorld.SILKTOUCH)>0;}boolean clusters(){return items.get(1).getItem() instanceof ElementalTools.Pick||upgrade(FociWorld.DOWSING)>0;}
  void sync(){if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
  @Override public void setChanged(){super.setChanged();properties();sync();}
  void orientation(boolean initial){switch(facing()){case DOWN->{tarX=0;tarZ=180;}case UP->{tarX=0;tarZ=0;}case NORTH->{tarX=270;tarZ=90;}case SOUTH->{tarX=90;tarZ=90;}case WEST->{tarX=0;tarZ=90;}case EAST->{tarX=180;tarZ=90;}}if(initial){rotX=tarX;rotZ=tarZ;}toDig=false;radInc=0;last=BlockPos.ZERO;paused=100;vRadX=vRadZ=tRadX=tRadZ=mRadX=mRadZ=0;dig=BlockPos.ZERO;}
  static void tick(Level l,BlockPos p,BlockState s,BoreEntity t){
   if(t.first){t.first=false;t.orientation(true);t.properties();if(l instanceof ServerLevel server)t.fake=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftBore".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"FakeThaumcraftBore"));}
   if(t.tarX!=switch(t.facing()){case NORTH->270;case SOUTH->90;case EAST->180;default->0;}||t.tarZ!=(t.facing()==Direction.UP?0:t.facing()==Direction.DOWN?180:90))t.orientation(false);
   if(l instanceof ServerLevel server&&t.speedyTime<20){t.speedyTime+=VisNetwork.drain(server,p,Aspect.ENTROPY,100,(node,color)->{})/5f;var base=t.base();if(t.speedyTime<20&&base!=null&&base.drawEssentia())t.speedyTime+=20;}
   t.rotate();if(t.gettingPower()&&t.validItems()&&t.rotX==t.tarX&&t.rotZ==t.tarZ){if(l.isClientSide){effects.accept(t);}else if(--t.count<=0){boolean dug=t.toDig;t.toDig=false;if(dug)t.mine();t.findNext();if(dug&&t.speedyTime>0)t.speedyTime--;}}
   else if(l.isClientSide){if(t.topRotation%90!=0)t.topRotation+=Math.min(10,90-t.topRotation%90);t.vRadX*=.9f;t.vRadZ*=.9f;}
   if(l instanceof ServerLevel&&t.items.get(1).getItem() instanceof PickaxeItem){if(t.repairCounter++%40==0&&t.items.get(1).isDamaged())t.repair();if(t.repairCounter%5==0)for(var a:t.repairCost.getAspects())if(t.repairVis.getAmount(a)<t.repairCost.getAmount(a))t.repairVis.add(a,VisNetwork.drain((ServerLevel)l,p,a,t.repairCost.getAmount(a),(node,color)->{}));t.fake.tickCount=t.repairCounter;try{t.items.get(1).inventoryTick(l,t.fake,0,true);}catch(Exception ignored){}}
  }
  void rotate(){if(rotX<tarX){rotX+=speedX;speedX=rotX<tarX?speedX+1:(int)(speedX/3f);}else if(rotX>tarX){rotX+=speedX;speedX=rotX>tarX?speedX-1:(int)(speedX/3f);}else speedX=0;if(rotZ<tarZ){rotZ+=speedZ;speedZ=rotZ<tarZ?speedZ+1:(int)(speedZ/3f);}else if(rotZ>tarZ){rotZ+=speedZ;speedZ=rotZ>tarZ?speedZ-1:(int)(speedZ/3f);}else speedZ=0;}
  void repair(){var stack=items.get(1);int n=Math.min(2,enchant(stack,BasicsEnchantments.REPAIR));if(n<=0)return;if(!stack.is(net.minecraft.tags.TagKey.create(Registries.ITEM,AlchemyContent.id("repairable")))){for(var a:repairCost.getAspects())repairCost.remove(a);return;}var tags=DeconstructorEntity.tags(stack,level);if(tags.size()==0)return;var primals=thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(tags);for(var a:primals.getAspects())repairCost.merge(a,(int)Math.sqrt(primals.getAmount(a)*2)*n);for(var a:repairCost.getAspects())if(repairVis.getAmount(a)<repairCost.getAmount(a))return;for(var a:repairCost.getAspects())repairVis.reduce(a,repairCost.getAmount(a));stack.setDamageValue(Math.max(0,stack.getDamageValue()-n));setChanged();}
  void findNext(){
   if(radInc==0)radInc=(2+area)/360f;BlockPos sample=last;var f=facing();while(sample.equals(last)){spiral=(spiral+2)%360;currentRadius+=radInc;if(currentRadius>2+area||currentRadius<-(2+area))radInc*=-1;
    // TCVec3 rotateAroundZ, then Y and X, with the original clockwise signs.
    double angle=spiral/180f*Math.PI,x=currentRadius*Math.sin(angle),y=currentRadius*Math.cos(angle),z=0;
    double ry=Math.PI/2*f.getStepX(),rx=Math.PI/2*f.getStepY(),xx=x*Math.cos(ry)+z*Math.sin(ry),zz=z*Math.cos(ry)-x*Math.sin(ry);x=xx;z=zz;double yy=y*Math.cos(rx)+z*Math.sin(rx);z=z*Math.cos(rx)-y*Math.sin(rx);y=yy;
    sample=BlockPos.containing(worldPosition.getX()+f.getStepX()+.5+x,worldPosition.getY()+f.getStepY()+.5+y,worldPosition.getZ()+f.getStepZ()+.5+z);
   }last=sample;var at=sample.relative(f);for(int depth=0;depth<64;depth++){at=at.relative(f);if(!level.hasChunkAt(at))break;var state=level.getBlockState(at);if(state.getDestroySpeed(level,at)<0)break;if(state.isAir()||!state.getFluidState().isEmpty()||state.getCollisionShape(level,at).isEmpty())continue;
    dig=at;var start=Vec3.atCenterOf(worldPosition.relative(f));var hit=level.clip(new ClipContext(start,Vec3.atCenterOf(at),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,fake));if(hit.getType()==HitResult.Type.BLOCK){var hs=level.getBlockState(hit.getBlockPos());if(hs.getDestroySpeed(level,hit.getBlockPos())>=0&&!hs.getCollisionShape(level,hit.getBlockPos()).isEmpty())dig=hit.getBlockPos();}
    digState=level.getBlockState(dig);count=Math.max(10-speed,(int)(digState.getDestroySpeed(level,dig)*2)-speed*2);if(speedyTime<1)count*=4;toDig=true;sync();break;
   }
  }
  void mine(){var l=(ServerLevel)level;var state=l.getBlockState(dig);var electric=dev.eugheist.tc4modern.api.TC4Addons.boreTool(items.get(1));if(!state.isAir()){
    var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(l,dig,state,fake);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(event.isCanceled())return;
    // Snapshot before paying: gameplay enchantments can depend on the tool still holding the last operation's EU.
    var tool=items.get(1).copy();boolean silk=silk();
    if(electric!=null&&!electric.consume(items.get(1),l,fake))return;
    tool.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);var ench=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);if(silk)tool.enchant(ench.getOrThrow(Enchantments.SILK_TOUCH),1);else if(fortune>0)tool.enchant(ench.getOrThrow(Enchantments.FORTUNE),fortune);
    var drops=new ArrayList<>(Block.getDrops(state,l,dig,l.getBlockEntity(dig),fake,tool));for(var old:l.getEntitiesOfClass(ItemEntity.class,new AABB(dig).inflate(1))){drops.add(old.getItem().copy());old.discard();}
    l.levelEvent(2001,dig,Block.getId(state));for(var stack:drops){if(!silk&&clusters()){var special=FociWorld.Dowsing.resolve(stack);if(special!=null&&l.random.nextFloat()<(.2f+fortune*.075f)*special.chance())stack=new ItemStack(FociWorld.Dowsing.clusterItem(special),special.perBlock()?1:stack.getCount());}output(stack);}
   }if(electric==null)items.get(1).hurtAndBreak(1,l,fake,item->{});l.removeBlock(dig,false);placeLight();setChanged();
  }
  void output(ItemStack stack){var base=base();if(base==null)return;var f=base.facing();var target=base.getBlockPos().offset(f.getStepX(),0,f.getStepZ());var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,target,f.getOpposite());if(handler!=null)stack=net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler,stack,false);if(stack.isEmpty())return;var item=new ItemEntity(level,worldPosition.getX()+.5+f.getStepX()*.66,worldPosition.getY()+.4+mount().getOpposite().getStepY(),worldPosition.getZ()+.5+f.getStepZ()*.66,stack.copy());item.setDeltaMovement(.075f*f.getStepX(),.025f,.075f*f.getStepZ());level.addFreshEntity(item);}
  void placeLight(){var base=base();if(base==null)return;for(var f:Direction.Plane.HORIZONTAL)if(level.getBlockState(base.getBlockPos().relative(f)).is(ArcaneLamps.LAMP.get())){int d=level.random.nextInt(32)*2,p=d/2%4;var forward=facing();var at=worldPosition.relative(forward,1+d);if(forward.getStepX()!=0)at=at.offset(0,0,p==0?3:p==2?-3:0);else at=at.offset(p==0?3:p==2?-3:0,0,0);if(p==3&&forward.getStepY()==0)at=at.below(2);if(level.isEmptyBlock(at)&&level.getMaxLocalRawBrightness(at)<15)level.setBlock(at,ArcaneLamps.LIGHT.get().defaultBlockState(),3);break;}}
  @Override public int getContainerSize(){return 2;}@Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}@Override public ItemStack getItem(int i){return items.get(i);}@Override public ItemStack removeItem(int i,int n){var s=ContainerHelper.removeItem(items,i,n);setChanged();return s;}@Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}@Override public void setItem(int i,ItemStack s){items.set(i,s);setChanged();}@Override public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p);}@Override public void clearContent(){items.clear();setChanged();}@Override public boolean canPlaceItem(int i,ItemStack s){return i==0?s.getItem() instanceof FociWorld.ExcavationFocus:(s.getItem() instanceof PickaxeItem||dev.eugheist.tc4modern.api.TC4Addons.boreTool(s)!=null);}@Override public int getMaxStackSize(){return 1;}
  @Override public Component getDisplayName(){return Component.translatable("block.tc4modern.arcane_bore");}@Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,this);}
  @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);ContainerHelper.saveAllItems(tag,items,r);tag.putShort("SpeedyTime",(short)speedyTime);tag.putLong("dig",dig.asLong());tag.putBoolean("toDig",toDig);tag.putInt("count",count);}
  @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){super.loadAdditional(tag,r);items.clear();ContainerHelper.loadAllItems(tag,items,r);speedyTime=tag.getShort("SpeedyTime");dig=BlockPos.of(tag.getLong("dig"));toDig=tag.getBoolean("toDig");count=tag.getInt("count");properties();}
  @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 }
 public static final class Menu extends AbstractContainerMenu {
  final BoreEntity bore;Menu(int id,Inventory inv,BoreEntity t){super(MENU.get(),id);bore=t;addSlot(new Slot(t,0,26,18){@Override public boolean mayPlace(ItemStack s){return t.canPlaceItem(0,s);}});addSlot(new Slot(t,1,74,18){@Override public boolean mayPlace(ItemStack s){return t.canPlaceItem(1,s);}});for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,59+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,117));}
  @Override public boolean stillValid(Player p){return bore.stillValid(p);}@Override public ItemStack quickMoveStack(Player p,int i){var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();boolean moved=i<2?moveItemStackTo(stack,2,38,true):bore.canPlaceItem(0,stack)?moveItemStackTo(stack,0,1,false):bore.canPlaceItem(1,stack)&&moveItemStackTo(stack,1,2,false);if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;}
 }
}
