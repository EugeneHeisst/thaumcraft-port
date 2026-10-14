package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterItems {
 public static final DeferredHolder<Item,Item> TABLET=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("runed_tablet"));
 public static final DeferredHolder<EntityType<?>,EntityType<PermanentItem>> PERMANENT=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("permanent_item"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->{h.register(AlchemyContent.id("obelisk_placer"),new Placer());h.register(TABLET.getId(),new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)){@Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("item.ItemEldritchObject.text.4").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}});});e.register(Registries.ENTITY_TYPE,h->h.register(PERMANENT.getId(),EntityType.Builder.<PermanentItem>of(PermanentItem::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(20).build(PERMANENT.getId().toString())));}
 public static final class Placer extends Item {
  Placer(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
  @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("item.tc4modern.obelisk_placer.creative").withStyle(net.minecraft.ChatFormatting.ITALIC));}
  @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){if(context.getClickedFace()!=net.minecraft.core.Direction.UP)return net.minecraft.world.InteractionResult.PASS;var level=context.getLevel();var pos=context.getClickedPos();for(int i=1;i<=6;i++)if(!level.isEmptyBlock(pos.above(i)))return net.minecraft.world.InteractionResult.FAIL;if(!level.isClientSide){level.setBlockAndUpdate(pos.above(),CrimsonContent.ALTAR.get().defaultBlockState());level.setBlockAndUpdate(pos.above(3),CrimsonContent.OBELISK.get().defaultBlockState());for(int i=4;i<=7;i++)level.setBlockAndUpdate(pos.above(i),CrimsonContent.OBELISK_BODY.get().defaultBlockState());}return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);}
 }
 public static final class PermanentItem extends ItemEntity {
  public PermanentItem(EntityType<? extends ItemEntity> t,Level l){super(t,l);setUnlimitedLifetime();}
  @Override public void tick(){var v=getDeltaMovement();setDeltaMovement(v.x,v.y>0?v.y*.9+.04:v.y+.04,v.z);super.tick();}
  @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return !source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)&&super.hurt(source,amount);}
  @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setUnlimitedLifetime();}
 }
 @EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
 public static final class Client {
  @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(PERMANENT.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);}
 }
}
