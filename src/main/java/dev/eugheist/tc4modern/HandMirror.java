package dev.eugheist.tc4modern;
import java.util.List;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class HandMirror {
 public static final DeferredHolder<MenuType<?>,MenuType<Menu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("hand_mirror"));
 public static Item item(){return BuiltInRegistries.ITEM.get(AlchemyContent.id("hand_mirror"));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->h.register(AlchemyContent.id("hand_mirror"),new MirrorItem()));e.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,b)->new Menu(id,inv,b.readBoolean()))));}
 static MagicMirrors.MirrorEntity target(ItemStack mirror,Player p){if(!NoteData.tag(mirror).contains("linkX"))return null;var dim=MagicMirrors.dimension(NoteData.tag(mirror));if(p.getServer().getLevel(dim)==null)return null;var t=MagicMirrors.destination(p.level(),NoteData.tag(mirror));if(t==null||t.essentia()){mirror.remove(DataComponents.CUSTOM_DATA);MagicMirrors.sound(p,"zap",1,.8f);p.displayClientMessage(Component.translatable("tc.handmirrorerror").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);return null;}return t;}
 static boolean transport(ItemStack mirror,ItemStack items,Player p){var t=target(mirror,p);if(t==null)return false;t.keepTicking();if(!t.spawn(items.copy()))return false;p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,.1f,1);t.event();return true;}
 public static final class MirrorItem extends Item {
  MirrorItem(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
  @Override public boolean isFoil(ItemStack s){return s.has(DataComponents.CUSTOM_DATA);}
  @Override public InteractionResult onItemUseFirst(ItemStack s,UseOnContext c){if(!(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof MagicMirrors.MirrorEntity t)||t.essentia())return InteractionResult.PASS;if(!c.getLevel().isClientSide){MagicMirrors.bind(s,t);MagicMirrors.sound(c.getPlayer(),"jar",1,2);c.getPlayer().displayClientMessage(Component.translatable("tc.handmirrorlinked").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);}return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
  @Override public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(p instanceof net.minecraft.server.level.ServerPlayer server&&target(stack,p)!=null)server.openMenu(new SimpleMenuProvider((id,inv,player)->new Menu(id,inv,hand==InteractionHand.OFF_HAND),Component.translatable("item.tc4modern.hand_mirror")),b->b.writeBoolean(hand==InteractionHand.OFF_HAND));return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){MagicMirrors.tooltip(s,lines,"tc.handmirrorlinkedto");}
 }
 public static final class Menu extends AbstractContainerMenu {
  final Inventory inventory;final ItemStack mirror;final boolean offhand;final int held;final SimpleContainer contents=new SimpleContainer(1);boolean transferring;
  Menu(int id,Inventory inv,boolean offhand){super(MENU.get(),id);inventory=inv;this.offhand=offhand;held=inv.selected;mirror=offhand?inv.player.getOffhandItem():inv.getItem(held);addSlot(new Slot(contents,0,80,24));for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,84+row*18));for(int col=0;col<9;col++){int inventoryIndex=col;addSlot(new Slot(inv,col,8+col*18,142){public boolean mayPickup(Player p){return offhand||inventoryIndex!=held;}public boolean mayPlace(ItemStack s){return offhand||inventoryIndex!=held;}});}contents.addListener(c->changed());}
  void changed(){var p=inventory.player;if(transferring||p.level().isClientSide||contents.getItem(0).isEmpty())return;var s=contents.getItem(0);if(ItemStack.isSameItemSameComponents(s,mirror)){p.closeContainer();return;}transferring=true;try{if(transport(mirror,s,p))contents.setItem(0,ItemStack.EMPTY);}finally{transferring=false;}}
  @Override public boolean stillValid(Player p){return (offhand?p.getOffhandItem():inventory.getItem(held))==mirror&&mirror.is(item());}
  @Override public void clicked(int slot,int button,ClickType type,Player p){if(type==ClickType.SWAP&&((!offhand&&button==held)||(offhand&&button==40)))return;super.clicked(slot,button,type,p);changed();}
  @Override public ItemStack quickMoveStack(Player p,int index){var slot=slots.get(index);if(!slot.mayPickup(p)||slot.getItem().is(item()))return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();if(index==0){if(!moveItemStackTo(s,1,37,true))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();changed();return copy;}
  @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide){var rest=contents.removeItemNoUpdate(0);if(!rest.isEmpty())p.drop(rest,false);}}
 }
}
