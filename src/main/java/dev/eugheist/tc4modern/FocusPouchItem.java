package dev.eugheist.tc4modern;
import net.minecraft.world.item.*;import net.minecraft.world.item.component.ItemContainerContents;import net.minecraft.core.component.DataComponents;
/** ItemFocusPouchBauble: also worn in the Baubles belt slot, where wand focus switching still reaches it. */
public final class FocusPouchItem extends Item implements baubles.api.IBauble {
 @Override public baubles.api.BaubleType getBaubleType(ItemStack stack){return baubles.api.BaubleType.BELT;}
 public FocusPouchItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
 public static java.util.List<ItemStack> contents(ItemStack pouch){var result=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(18,ItemStack.EMPTY));var stored=pouch.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);for(int i=0;i<Math.min(18,stored.getSlots());i++)result.set(i,stored.getStackInSlot(i).copy());return result;}
 public static void store(ItemStack pouch,java.util.List<ItemStack> items){pouch.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));}
 @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){if(player instanceof net.minecraft.server.level.ServerPlayer server)server.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new FocusPouchMenu(id,inv,hand==net.minecraft.world.InteractionHand.OFF_HAND),getName(player.getItemInHand(hand))),buf->buf.writeBoolean(hand==net.minecraft.world.InteractionHand.OFF_HAND));return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
}
