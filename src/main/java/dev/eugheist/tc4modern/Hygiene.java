package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** ItemBathSalts, ItemSanitySoap and BlockStoneDevice metadata12 registration. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class Hygiene {
 public static java.util.function.BiConsumer<Player,Boolean> soapEffects=(p,finished)->{};
 public static java.util.function.BiConsumer<Level,BlockPos> pureBubbles=(l,p)->{};
 public static final DeferredHolder<Item,Item> SALTS=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("bath_salts"));
 public static final DeferredHolder<Item,Item> SOAP=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("sanity_soap"));
 public static final DeferredHolder<Block,Block> TALLOW=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("tallow_block"));
 public static final DeferredHolder<Block,SpaBlock> SPA=DeferredHolder.create(Registries.BLOCK,AlchemyContent.id("arcane_spa"));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SpaEntity>> SPA_ENTITY=DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE,AlchemyContent.id("arcane_spa"));
 public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,net.minecraft.world.inventory.MenuType<SpaMenu>> SPA_MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("arcane_spa"));
 @SubscribeEvent static void register(RegisterEvent event){
  event.register(Registries.BLOCK,h->{h.register(SPA.getId(),new SpaBlock(Block.Properties.of().strength(3,TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops()));h.register(TALLOW.getId(),new Block(Block.Properties.of().strength(2,TC4SoundTypes.res(10)).sound(SoundType.STONE).requiresCorrectToolForDrops()));});
  event.register(Registries.ITEM,h->{h.register(SALTS.getId(),new Item(new Item.Properties()){@Override public int getEntityLifespan(ItemStack s,Level l){return 200;}});h.register(SOAP.getId(),new Soap());h.register(SPA.getId(),new BlockItem(SPA.get(),new Item.Properties()));h.register(TALLOW.getId(),new BlockItem(TALLOW.get(),new Item.Properties()));});
  event.register(Registries.BLOCK_ENTITY_TYPE,h->h.register(SPA_ENTITY.getId(),BlockEntityType.Builder.of(SpaEntity::new,SPA.get()).build(null)));
  event.register(Registries.MENU,h->h.register(SPA_MENU.getId(),new net.minecraft.world.inventory.MenuType<>(SpaMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET)));
 }
 @SubscribeEvent static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){
  for(var bucket:java.util.List.of(PureWater.BUCKET.get(),LiquidDeath.BUCKET.get()))event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM,(stack,context)->new net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper(stack){@Override public net.neoforged.neoforge.fluids.FluidStack getFluid(){return container.is(bucket)?new net.neoforged.neoforge.fluids.FluidStack(bucket==PureWater.BUCKET.get()?PureWater.FLUID.get():LiquidDeath.FLUID.get(),1000):net.neoforged.neoforge.fluids.FluidStack.EMPTY;}},bucket);
  // Original canFill/canDrain exclude UP; fill/drain themselves do not check the direction.
  event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,SPA_ENTITY.get(),(tile,side)->side==Direction.UP?null:tile.tank);
  event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,SPA_ENTITY.get(),(tile,side)->new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(tile,side==null?Direction.DOWN:side));
 }
 public static final class Soap extends Item {
  Soap(){super(new Properties());}
  @Override public int getUseDuration(ItemStack s,LivingEntity e){return 200;}
  @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.BLOCK;}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
  @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){if(!(entity instanceof Player player))return;if(200-remaining>195)player.releaseUsingItem();if(level.isClientSide)soapEffects.accept(player,false);}
  @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){
   if(!(entity instanceof Player player)||200-remaining<=195)return;
   stack.shrink(1); // Original consumes soap in creative mode too.
   if(level.isClientSide){soapEffects.accept(player,true);return;}
   float chance=.33f;if(player.hasEffect(WarpEffects.WARP_WARD))chance+=.25f;if(level.getBlockState(player.blockPosition()).is(PureWater.BLOCK.get()))chance+=.25f;
   if(level.random.nextFloat()<chance&&PlayerWarp.load(player).get(WarpState.Kind.STICKY)>0)PlayerWarp.add(player,WarpState.Kind.STICKY,-1);
   int temporary=PlayerWarp.load(player).get(WarpState.Kind.TEMPORARY);if(temporary>0)PlayerWarp.add(player,WarpState.Kind.TEMPORARY,-temporary);
  }
 }
}
