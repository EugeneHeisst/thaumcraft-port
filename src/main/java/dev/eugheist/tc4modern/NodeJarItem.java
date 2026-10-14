package dev.eugheist.tc4modern;
public final class NodeJarItem extends net.minecraft.world.item.BlockItem {
 public NodeJarItem(net.minecraft.world.level.block.Block b,Properties p){super(b,p);}
 @Override public void appendHoverText(net.minecraft.world.item.ItemStack s,TooltipContext c,java.util.List<net.minecraft.network.chat.Component> t,net.minecraft.world.item.TooltipFlag f){var aspects=new thaumcraft.api.aspects.AspectList();aspects.readFromNBT(NoteData.tag(s));for(var a:aspects.getAspectsSorted())t.add(net.minecraft.network.chat.Component.literal(a.getName()+" x "+aspects.getAmount(a)));}
}
