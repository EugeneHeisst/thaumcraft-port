package dev.eugheist.tc4modern;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
/** Original ItemLootBag, Config.initLoot and Utils.generateLoot; split metadata into three items. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class LootBags {
 public static final List<DeferredHolder<Item,Item>> ITEMS=java.util.stream.Stream.of("common","uncommon","rare").map(n->DeferredHolder.<Item,Item>create(Registries.ITEM,AlchemyContent.id("loot_bag_"+n))).toList();
 record Entry(int weight,Supplier<ItemStack> item){}
 static List<List<Entry>> pools;
 static ItemStack chestAmulet(){initialize();return pools.get(1).stream().filter(e->e.weight()==6).findFirst().orElseThrow().item().get().copy();}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{for(int i=0;i<3;i++)h.register(ITEMS.get(i).getId(),new Bag(i));for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())h.register(AlchemyContent.id("primal_ring_"+a.getTag()),new PrimalRing(a));});}
 public static final class Bag extends Item {
  final int rarity;
  Bag(int rarity){super(new Properties().stacksTo(16).rarity(rarity==0?Rarity.COMMON:rarity==1?Rarity.UNCOMMON:Rarity.RARE));this.rarity=rarity;}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);if(!l.isClientSide){int count=8+l.random.nextInt(5);for(int i=0;i<count;i++){var loot=generate(rarity,l);l.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(l,p.getX(),p.getY(),p.getZ(),loot));}p.playSound(FociContent.sound("coins"),.75f,1);s.shrink(1);}return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tc.lootbag"));}
 }
 public static final class PrimalRing extends Item implements baubles.api.IBauble,thaumcraft.api.IVisDiscountGear,thaumcraft.api.IRunicArmor {
  public final thaumcraft.api.aspects.Aspect aspect;
  PrimalRing(thaumcraft.api.aspects.Aspect a){super(new Properties().stacksTo(1));aspect=a;}
  public baubles.api.BaubleType getBaubleType(ItemStack s){return baubles.api.BaubleType.RING;}
  public int getVisDiscount(ItemStack s,Player p,thaumcraft.api.aspects.Aspect a){return a==aspect?1:0;}
  public int getRunicCharge(ItemStack s){return 0;}
  @Override public void appendHoverText(ItemStack s,TooltipContext c,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.literal(aspect.getName()+" ").append(net.minecraft.network.chat.Component.translatable("tc.discount")).append(": 1%").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
 }
 static void entry(List<Entry> list,int weight,Supplier<ItemStack> item){list.add(new Entry(weight,item));}
 static Supplier<ItemStack> mod(String name,int count){return ()->{var stack=PechTrades.mod(name);stack.setCount(count);return stack;};}
 static synchronized void initialize(){if(pools!=null)return;var amulet=PechTrades.mod("vis_amulet_lesser");var amuletTag=NoteData.tag(amulet).copy();var random=new Random();for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())amuletTag.putInt(a.getTag(),random.nextInt(5)*100);amulet.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(amuletTag));var out=new ArrayList<List<Entry>>();for(int rarity=0;rarity<3;rarity++){var p=new ArrayList<Entry>();entry(p,2500-rarity*250,mod("coin",rarity+1));if(rarity==2){entry(p,1,mod("primordial_pearl",1));entry(p,1,()->new ItemStack(Items.NETHER_STAR));}entry(p,rarity==0?10:50,()->new ItemStack(Items.DIAMOND));entry(p,rarity==0?15:75,()->new ItemStack(Items.EMERALD));entry(p,100,()->new ItemStack(Items.GOLD_INGOT));entry(p,100,()->new ItemStack(Items.ENDER_PEARL));entry(p,25,mod("knowledge_fragment",1));if(rarity==0){for(String name:List.of("mundane_amulet","mundane_ring","mundane_belt"))entry(p,10,mod(name,1));}else{for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())entry(p,rarity==1?5:7,mod("primal_ring_"+a.getTag(),1));entry(p,6,amulet::copy);entry(p,5,mod("runic_ring_lesser",1));}entry(p,rarity==0?5:rarity==1?10:20,()->new ItemStack(Items.EXPERIENCE_BOTTLE));entry(p,rarity+1,()->new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));entry(p,3*(rarity+1),()->new ItemStack(Items.GOLDEN_APPLE));entry(p,10,()->new ItemStack(Items.BOOK));potions(p);out.add(List.copyOf(p));}pools=List.copyOf(out);}
 static void potions(List<Entry> p){var base=List.of(Potions.REGENERATION,Potions.SWIFTNESS,Potions.FIRE_RESISTANCE,Potions.POISON,Potions.HEALING,Potions.NIGHT_VISION,Potions.WEAKNESS,Potions.STRENGTH,Potions.SLOWNESS,Potions.HARMING,Potions.WATER_BREATHING,Potions.INVISIBILITY);var extended=List.of(Potions.LONG_REGENERATION,Potions.LONG_SWIFTNESS,Potions.LONG_FIRE_RESISTANCE,Potions.LONG_POISON,Potions.HEALING,Potions.LONG_NIGHT_VISION,Potions.LONG_WEAKNESS,Potions.LONG_STRENGTH,Potions.LONG_SLOWNESS,Potions.HARMING,Potions.LONG_WATER_BREATHING,Potions.LONG_INVISIBILITY);var strong=List.of(Potions.STRONG_REGENERATION,Potions.STRONG_SWIFTNESS,Potions.FIRE_RESISTANCE,Potions.STRONG_POISON,Potions.STRONG_HEALING,Potions.NIGHT_VISION,Potions.WEAKNESS,Potions.STRONG_STRENGTH,Potions.SLOWNESS,Potions.STRONG_HARMING,Potions.WATER_BREATHING,Potions.INVISIBILITY);for(var item:List.of(Items.POTION,Items.SPLASH_POTION)){int weight=0;for(var variants:List.of(base,strong,extended)){int w=++weight;for(var potion:variants)entry(p,w,()->PotionContents.createItemStack(item,potion));}}}
 public static ItemStack generate(int rarity,Level l){initialize();var r=l.random;ItemStack stack;if(rarity>0&&r.nextFloat()<.025f*rarity){stack=gear(rarity,l);if(stack.isEmpty())return generate(rarity,l);}else{var list=pools.get(rarity);int roll=r.nextInt(list.stream().mapToInt(Entry::weight).sum());Entry selected=list.getLast();for(var e:list)if((roll-=e.weight)<0){selected=e;break;}stack=selected.item.get();}if(stack.is(Items.BOOK))stack=enchant(stack,rarity,l);return stack.copy();}
 static ItemStack enchant(ItemStack s,int rarity,Level l){return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(l.random,s,(int)(5+rarity*.75f*l.random.nextInt(18)),l.registryAccess(),Optional.empty());}
 static ItemStack gear(int rarity,Level l){var r=l.random;int quality=r.nextInt(2);for(float chance:new float[]{.2f,.15f,.1f,.095f,.095f})if(r.nextFloat()<chance)quality++;int slot=r.nextInt(5);String name;if(slot==0)name=new String[]{"minecraft:iron_axe","minecraft:iron_sword","minecraft:golden_axe","minecraft:golden_sword","tc4modern:thaumium_sword","minecraft:diamond_sword","tc4modern:void_sword"}[quality];else{String material=new String[]{"leather","golden","chainmail","iron","thaumium","diamond","void"}[quality];String piece=new String[]{"","boots","leggings","chestplate","helmet"}[slot];name=(quality==4||quality==6?"tc4modern:":"minecraft:")+material+"_"+piece;}var id=net.minecraft.resources.ResourceLocation.parse(name);if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))return ItemStack.EMPTY;var stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));stack.setDamageValue(r.nextInt(1+stack.getMaxDamage()/6));if(r.nextInt(4)<rarity)stack=enchant(stack,rarity,l);return stack;}
}
