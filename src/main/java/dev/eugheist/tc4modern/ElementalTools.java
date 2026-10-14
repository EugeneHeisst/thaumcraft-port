package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** ItemElementalAxe/Pickaxe and BlockUtils.findBlocks. Deliberately removes ONE furthest log per swing. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class ElementalTools {
 public static final Tier TIER=new net.neoforged.neoforge.common.SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,1500,10,3,18,()->Ingredient.of(TC4Modern.THAUMIUM.get()));
 public static final DeferredHolder<EntityType<?>,EntityType<FollowingItem>> FOLLOWING=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("following_item"));
 public static Item item(String name){return BuiltInRegistries.ITEM.get(AlchemyContent.id("elemental_"+name));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{h.register(AlchemyContent.id("elemental_hoe"),new ElementalHoe());h.register(AlchemyContent.id("elemental_shovel"),new ElementalShovel());h.register(AlchemyContent.id("elemental_sword"),new ElementalSword());h.register(AlchemyContent.id("elemental_axe"),new Axe());h.register(AlchemyContent.id("elemental_pickaxe"),new Pick());});e.register(Registries.ENTITY_TYPE,h->h.register(FOLLOWING.getId(),EntityType.Builder.<FollowingItem>of(FollowingItem::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(8).updateInterval(1).build("tc4modern:following_item")));}
 public record BlockBubble(BlockPos pos) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
  static final Type<BlockBubble> TYPE=new Type<>(AlchemyContent.id("block_bubble"));
  static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.FriendlyByteBuf,BlockBubble> CODEC=net.minecraft.network.codec.StreamCodec.composite(BlockPos.STREAM_CODEC,BlockBubble::pos,BlockBubble::new);
  public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}
 }
 @SubscribeEvent static void payloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(BlockBubble.TYPE,BlockBubble.CODEC,(data,ctx)->ctx.enqueueWork(()->ElementalToolsClient.blockBubble(data.pos())));}
 static Item.Properties properties(float damage,float speed){return new Item.Properties().rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(TIER,damage,speed));}
 static void sound(Level l,BlockPos p,String name,float volume,float pitch){l.playSound(null,p,SoundEvent.createVariableRangeEvent(AlchemyContent.id(name)),SoundSource.PLAYERS,volume,pitch);}
 public static final class Pick extends PickaxeItem {
  Pick(){super(TIER,properties(2,-2.8f));}
  @Override public boolean onLeftClickEntity(ItemStack s,Player p,Entity victim){if(!p.level().isClientSide&&(!(victim instanceof Player)||p.getServer().isPvpAllowed()))victim.igniteForSeconds(2);return false;}
  @Override public InteractionResult useOn(UseOnContext c){var p=c.getPlayer();if(p==null)return InteractionResult.PASS;var l=c.getLevel();c.getItemInHand().hurtAndBreak(5,p,LivingEntity.getSlotForHand(c.getHand()));if(l.isClientSide){ElementalToolsClient.startScan(c.getClickedPos());p.swing(c.getHand());}else sound(l,c.getClickedPos(),"wandfail",.2f,.2f+l.random.nextFloat()*.2f);return InteractionResult.PASS;}
 }
 public static final class Axe extends AxeItem {
  Axe(){super(TIER,properties(3,-3));}
  @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.BOW;}
  @Override public int getUseDuration(ItemStack s,LivingEntity e){return 72000;}
  @Override public InteractionResult useOn(UseOnContext c){return InteractionResult.PASS;}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){p.startUsingItem(h);return InteractionResultHolder.consume(p.getItemInHand(h));}
  @Override public void onUseTick(Level l,LivingEntity user,ItemStack s,int remaining){if(!(user instanceof Player p))return;for(var item:l.getEntitiesOfClass(ItemEntity.class,new AABB(p.position(),p.position()).inflate(10))){if(!item.isAlive()||item instanceof FollowingItem f&&f.target()!=null)continue;double dx=item.getX()-p.getX(),dy=item.getY()-p.getY()+p.getBbHeight()/2,dz=item.getZ()-p.getZ();double length=Math.sqrt(dx*dx+dy*dy+dz*dz);if(length==0)continue;var v=item.getDeltaMovement();item.setDeltaMovement(Mth.clamp(v.x-dx/length*.3,-.35,.35),Mth.clamp(v.y-dy/length*.3,-.35,.35),Mth.clamp(v.z-dz/length*.3,-.35,.35));if(l.isClientSide)ElementalToolsClient.bubble(item);}}
  public boolean onBlockStartBreak(ItemStack s,BlockPos pos,Player p){var l=p.level();if(p.isShiftKeyDown()||!l.getBlockState(pos).is(BlockTags.LOGS))return false;if(p instanceof ServerPlayer server){var target=furthest(l,pos,l.getBlockState(pos).getBlock());harvest(server,target);net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(server.serverLevel(),null,pos.getX(),pos.getY(),pos.getZ(),32,new BlockBubble(pos));server.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(l,pos));l.levelEvent(2001,pos,Block.getId(l.getBlockState(pos)));sound(l,pos,"bubble",.15f,1);}s.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);return true;}
 }
 /** Exact original greedy search order, bounds and distance comparison, including gaps up to two blocks. */
 static BlockPos furthest(Level l,BlockPos origin,Block block){BlockPos last=origin;double distance=0;search:while(true){for(int x=-2;x<=2;x++)for(int y=2;y>=-2;y--)for(int z=-2;z<=2;z++){var next=last.offset(x,y,z);int dx=next.getX()-origin.getX(),dy=next.getY()-origin.getY(),dz=next.getZ()-origin.getZ();if(Math.abs(dx)>24||Math.abs(dy)>48||Math.abs(dz)>24)return last;var s=l.getBlockState(next);double d=(double)dx*dx+(double)dy*dy+(double)dz*dz;if(s.is(block)&&s.is(BlockTags.LOGS)&&s.getDestroySpeed(l,next)>=0&&d>distance){distance=d;last=next;continue search;}}return last;}}
 static boolean harvest(ServerPlayer p,BlockPos pos){return harvest(p,pos,10);}
 static boolean harvest(ServerPlayer p,BlockPos pos,int type){var l=p.serverLevel();var state=l.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(l,pos)<0||!l.mayInteract(p,pos)||p.blockActionRestricted(l,pos,p.gameMode.getGameModeForPlayer()))return false;var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(l,pos,state,p);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(event.isCanceled())return false;var be=l.getBlockEntity(pos);var stack=p.getMainHandItem();boolean drop=!p.getAbilities().instabuild&&state.canHarvestBlock(l,pos,p);l.levelEvent(2001,pos,Block.getId(state));if(!state.onDestroyedByPlayer(l,pos,p,drop,l.getFluidState(pos)))return false;state.getBlock().destroy(l,pos,state);if(drop){state.getBlock().playerDestroy(l,p,pos,state,be,stack);for(var old:l.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)))if(old.tickCount==0&&!(old instanceof FollowingItem)){var flying=new FollowingItem(FOLLOWING.get(),l);flying.setItem(old.getItem().copy());flying.setPos(old.position());flying.setDeltaMovement(old.getDeltaMovement());flying.target(p);flying.getEntityData().set(FollowingItem.PARTICLE,type);l.addFreshEntity(flying);old.discard();}}for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){var at=pos.offset(x,y,z);l.scheduleTick(at,l.getBlockState(at).getBlock(),150+l.random.nextInt(150));}return true;}
 public static final class FollowingItem extends ItemEntity {
  static final EntityDataAccessor<Integer> PARTICLE=SynchedEntityData.defineId(FollowingItem.class,EntityDataSerializers.INT);
  static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(FollowingItem.class,EntityDataSerializers.INT);
  FollowingItem(EntityType<? extends FollowingItem> type,Level l){super(type,l);setPickUpDelay(10);}
  @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TARGET,-1);b.define(PARTICLE,10);}
  Entity target(){return level().getEntity(entityData.get(TARGET));}void target(Entity e){entityData.set(TARGET,e.getId());}
  @Override public void tick(){var target=target();if(target!=null&&target.isAlive()){noPhysics=true;var delta=target.position().add(0,target.getBbHeight()/2,0).subtract(position());double d=delta.length();if(d>.5){int delay=Math.max(1,10-tickCount);setDeltaMovement(delta.scale(1/(d*delay)));}else{setDeltaMovement(getDeltaMovement().scale(.1));entityData.set(TARGET,-1);noPhysics=false;}if(level().isClientSide){if(entityData.get(PARTICLE)==10)ElementalToolsClient.bubble(this);else FociFx.sparkle(getX()+(random.nextFloat()-random.nextFloat())*.125,getY()+getBbHeight()/2+(random.nextFloat()-random.nextFloat())*.125,getZ()+(random.nextFloat()-random.nextFloat())*.125,1.5f,entityData.get(PARTICLE),0);}setNoGravity(true);}else{noPhysics=false;setNoGravity(false);}super.tick();}
  @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putShort("type",(short)(int)entityData.get(PARTICLE));}
  @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(PARTICLE,(int)tag.getShort("type"));}
 }
}
