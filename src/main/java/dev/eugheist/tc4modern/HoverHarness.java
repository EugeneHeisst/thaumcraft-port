package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.*;
import thaumcraft.api.aspects.*;
/** Original ItemHoverHarness, ItemGirdleHover and Hover; fuel remains a complete nested jar stack. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class HoverHarness {
 public static final DeferredHolder<MenuType<?>,MenuType<Menu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("hover_harness"));
 static final Map<Player,Boolean> STATES=new WeakHashMap<>();
 public static Item harness(){return BuiltInRegistries.ITEM.get(AlchemyContent.id("hover_harness"));}
 public static Item girdle(){return BuiltInRegistries.ITEM.get(AlchemyContent.id("hover_girdle"));}
 static ItemStack chest(Player p){return p.getItemBySlot(EquipmentSlot.CHEST);}
 public static boolean isPaidHover(Player p){return !p.getAbilities().instabuild&&active(p)&&chest(p).is(harness())&&fuel(jar(chest(p),p.registryAccess()))>0;}
 static boolean active(Player p){return STATES.getOrDefault(p,false);}
 static boolean belted(Player p){return BaublesInventory.of(p).getItem(3).is(girdle());}
 static ItemStack jar(ItemStack armor,HolderLookup.Provider registries){return ItemStack.parseOptional(registries,NoteData.tag(armor).getCompound("jar"));}
 static void store(ItemStack armor,ItemStack jar,HolderLookup.Provider registries){var tag=NoteData.tag(armor);tag.put("jar",jar.saveOptional(registries));armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
 static int fuel(ItemStack jar){if(!(jar.getItem() instanceof WardedJarBlock.JarItem))return 0;var aspects=EssentiaItems.getAspects(jar);return aspects==null?0:aspects.getAmount(Aspect.ENERGY);}
 static boolean expend(Player p,ItemStack armor){var jar=jar(armor,p.registryAccess());int fuel=fuel(jar);if(fuel<=0)return false;var tag=NoteData.tag(armor);short charge=tag.getShort("charge");
  // Original girdle reduces the threshold: paradoxically it consumes fuel faster (289 rather than361 ticks).
  if(charge<(belted(p)?288:360)){tag.putShort("charge",(short)(charge+1));armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return true;}
  tag.putShort("charge",(short)0);armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));EssentiaItems.setAspects(jar,fuel>1?new AspectList().add(Aspect.ENERGY,fuel-1):new AspectList());store(armor,jar,p.registryAccess());return fuel>1;
 }
 static boolean set(Player p,boolean requested){var armor=chest(p);boolean value=requested&&armor.is(harness())&&fuel(jar(armor,p.registryAccess()))>0;STATES.put(p,value);p.getPersistentData().putBoolean("TC4Hover",value);if(armor.is(harness())){var tag=NoteData.tag(armor);tag.putByte("hover",(byte)(value?1:0));armor.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}if(!p.getAbilities().instabuild){p.getAbilities().flying=value;if(!p.level().isClientSide)p.onUpdateAbilities();}if(p instanceof net.minecraft.server.level.ServerPlayer server)PacketDistributor.sendToPlayer(server,new State(value));return value;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){var p=e.getEntity();var armor=chest(p);if(!armor.is(harness())){if(active(p))set(p,false);STATES.remove(p);p.getPersistentData().remove("TC4Hover");return;}if(p.getAbilities().instabuild)return;
  STATES.putIfAbsent(p,NoteData.tag(armor).getByte("hover")==1);boolean hover=active(p);p.getPersistentData().putBoolean("TC4Hover",hover);p.getAbilities().flying=hover;
  if(hover&&expend(p,armor)){if(p.level().isClientSide){float mod=Math.min(1,.7f+.075f*BasicsEnchantments.level(p,armor,BasicsEnchantments.HASTE)+(belted(p)?.21f:0));var movement=p.getDeltaMovement();p.setDeltaMovement(movement.x*mod,movement.y,movement.z*mod);HoverHarnessClient.hum(p);}else{p.fallDistance=0;if(p instanceof net.minecraft.server.level.ServerPlayer server)((dev.eugheist.tc4modern.mixin.HoverFloatCounterAccessor)server.connection).tc4modern$floatCounter(0);}}
  else {if(hover)set(p,false);if(!p.level().isClientSide)p.fallDistance*=.75f;}
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)static final class Registration {
  @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{h.register(AlchemyContent.id("hover_harness"),new Harness());h.register(AlchemyContent.id("hover_girdle"),new Girdle());});e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new Menu(id,inv,buf.readBoolean()))));}
  @SubscribeEvent static void network(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){var reg=e.registrar("tc4-hover-1");reg.playToServer(Toggle.TYPE,Toggle.CODEC,(packet,context)->context.enqueueWork(()->set(context.player(),packet.enabled)));reg.playToClient(State.TYPE,State.CODEC,(packet,context)->context.enqueueWork(()->HoverHarnessClient.receive(packet.enabled)));}
 }
 public record Toggle(boolean enabled) implements CustomPacketPayload {static final Type<Toggle> TYPE=new Type<>(AlchemyContent.id("hover_toggle"));static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,Toggle> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,Toggle::enabled,Toggle::new);public Type<? extends CustomPacketPayload> type(){return TYPE;}}
 public record State(boolean enabled) implements CustomPacketPayload {static final Type<State> TYPE=new Type<>(AlchemyContent.id("hover_state"));static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,State> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,State::enabled,State::new);public Type<? extends CustomPacketPayload> type(){return TYPE;}}
 public static final class Harness extends ArmorItem implements IRunicArmor,IRepairable,IVisDiscountGear {
  Harness(){super(TC4Modern.SPECIAL_MATERIAL,Type.CHESTPLATE,new Properties().durability(400).rarity(Rarity.EPIC));}
  public int getRunicCharge(ItemStack s){return 0;}public int getVisDiscount(ItemStack s,Player p,Aspect a){return a==Aspect.AIR?5:2;}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack material){return material.is(Items.GOLD_INGOT);}
  @Override public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return AlchemyContent.id("textures/models/hoverharness.png");}
  @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(p instanceof net.minecraft.server.level.ServerPlayer server)server.openMenu(new SimpleMenuProvider((id,inv,player)->new Menu(id,inv,hand==InteractionHand.OFF_HAND),Component.translatable("container.hoverharness")),buf->buf.writeBoolean(hand==InteractionHand.OFF_HAND));return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,TooltipContext context,List<Component> lines,TooltipFlag flag){if(context.registries()!=null)EssentiaItems.tooltip(jar(s,context.registries()),lines);lines.add(Component.translatable("tc.visdiscount").append(": 2%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));lines.add(Component.translatable("tc.visdiscount").append(" (Aer): 5%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
 }
 public static final class Girdle extends Item implements baubles.api.IBauble,IRunicArmor {
  Girdle(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}public int getRunicCharge(ItemStack s){return 0;}public baubles.api.BaubleType getBaubleType(ItemStack s){return baubles.api.BaubleType.BELT;}public void onWornTick(ItemStack s,LivingEntity p){if(p.fallDistance>0)p.fallDistance-=.33f;}
 }
 public static final class Menu extends AbstractContainerMenu {
  final Inventory inventory;final ItemStack armor;final boolean offhand;final int held;final SimpleContainer contents=new SimpleContainer(1);
  Menu(int id,Inventory inventory,boolean offhand){super(MENU.get(),id);this.inventory=inventory;this.offhand=offhand;held=inventory.selected;armor=offhand?inventory.player.getOffhandItem():inventory.getItem(held);contents.setItem(0,jar(armor,inventory.player.registryAccess()));addSlot(new Slot(contents,0,80,32){public boolean mayPlace(ItemStack s){return fuel(s)>0;}public int getMaxStackSize(){return 1;}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));for(int col=0;col<9;col++){int inventoryIndex=col;addSlot(new Slot(inventory,col,8+col*18,142){public boolean mayPickup(Player p){return offhand||inventoryIndex!=held;}public boolean mayPlace(ItemStack s){return offhand||inventoryIndex!=held;}});}}
  @Override public boolean stillValid(Player p){return armor.is(harness())&&(offhand?p.getOffhandItem():inventory.getItem(held))==armor;}
  @Override public void clicked(int slot,int button,ClickType type,Player p){if(!offhand&&type==ClickType.SWAP&&button==held)return;super.clicked(slot,button,type,p);}
  @Override public ItemStack quickMoveStack(Player p,int index){var slot=slots.get(index);if(!slot.mayPickup(p))return ItemStack.EMPTY;var stack=slot.getItem();var old=stack.copy();if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}else if(fuel(stack)>0){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}else return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return old;}
  @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide){store(armor,contents.getItem(0),p.registryAccess());inventory.setChanged();}}
 }
}
