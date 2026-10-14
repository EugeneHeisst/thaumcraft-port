package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;import net.minecraft.world.item.*;import net.minecraft.world.inventory.MenuType;import net.minecraft.world.level.block.*;import net.neoforged.bus.api.SubscribeEvent;import net.neoforged.fml.common.EventBusSubscriber;import net.neoforged.neoforge.registries.*;
/** Real items required by the original Pech trade pools. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class PechRewards {
 public static final DeferredHolder<Item,Item> POUCH=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("focus_pouch"));
 public static final DeferredHolder<MenuType<?>,MenuType<FocusPouchMenu>> POUCH_MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("focus_pouch"));
 @SubscribeEvent static void register(RegisterEvent event){event.register(Registries.BLOCK,h->{for(int i=0;i<16;i++)h.register(AlchemyContent.id("candle_"+i),new ThaumicCandleBlock(i));});event.register(Registries.ITEM,h->{h.register(POUCH.getId(),new FocusPouchItem());h.register(AlchemyContent.id("vis_amulet_lesser"),new AccessoryItem(false));h.register(AlchemyContent.id("runic_ring_lesser"),new AccessoryItem(true));for(String name:java.util.List.of("iron","gold","cinnabar","copper"))h.register(AlchemyContent.id(name+"_cluster"),new Item(new Item.Properties()));h.register(AlchemyContent.id("coin"),new Item(new Item.Properties()));h.register(AlchemyContent.id("void_seed"),new Item(new Item.Properties()));for(int i=0;i<16;i++)h.register(AlchemyContent.id("candle_"+i),new BlockItem(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(AlchemyContent.id("candle_"+i)),new Item.Properties()));});event.register(Registries.MENU,h->{h.register(POUCH_MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new FocusPouchMenu(id,inv,buf.readBoolean())));});}
 /** ItemAmuletVis (lesser 25 vis, greater 250 vis gated by VISAMULET) and ItemRingRunic (lesser), worn as Baubles. */
 public static final class AccessoryItem extends Item implements baubles.api.IBauble,thaumcraft.api.IRunicArmor {
  public final boolean ring;
  public final int capacity;
  private final String research;
  public AccessoryItem(boolean ring){this(ring,2500,Rarity.UNCOMMON,null);}
  public AccessoryItem(boolean ring,int capacity,Rarity rarity,String research){super(new Properties().stacksTo(1).rarity(rarity));this.ring=ring;this.capacity=capacity;this.research=research;}
  @Override public int getRunicCharge(ItemStack stack){return ring?1:0;}
  @Override public baubles.api.BaubleType getBaubleType(ItemStack stack){return ring?baubles.api.BaubleType.RING:baubles.api.BaubleType.AMULET;}
  @Override public void onWornTick(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){if(!ring&&entity instanceof net.minecraft.world.entity.player.Player player)Accessories.visAmuletTick(stack,player);}
  @Override public boolean canEquip(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){
   return research==null||!(entity instanceof net.minecraft.world.entity.player.Player player)||ResearchProgress.completed(player.getPersistentData()).contains(research);
  }
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> list,net.minecraft.world.item.TooltipFlag flag){
   if(ring)return;
   if(research==null)list.add(net.minecraft.network.chat.Component.translatable("item.ItemAmuletVis.text").withStyle(net.minecraft.ChatFormatting.AQUA));
   list.add(net.minecraft.network.chat.Component.translatable("item.capacity.text").append(" "+capacity/100).withStyle(net.minecraft.ChatFormatting.GOLD));
   var tag=NoteData.tag(stack);var format=new java.text.DecimalFormat("#######.##");
   for(var aspect:thaumcraft.api.aspects.Aspect.getPrimalAspects())if(tag.contains(aspect.getTag()))
    list.add(net.minecraft.network.chat.Component.literal(" \u00a7"+aspect.getChatcolor()+aspect.getName()+"\u00a7r x "+format.format(tag.getInt(aspect.getTag())/100f)));
  }
 }


}
