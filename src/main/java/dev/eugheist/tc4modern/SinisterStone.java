package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
/** Original ItemCompassStone, including one shared active icon for all stacks. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class SinisterStone extends Item implements thaumcraft.api.IWarpingGear {
 public static final DeferredHolder<Item,SinisterStone> ITEM=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("sinister_stone"));
 static java.util.function.BiConsumer<Level,BlockPos> nodeSeen=(l,p)->{};
 static java.util.function.Consumer<Entity> update=e->{};
 SinisterStone(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ITEM,h->h.register(ITEM.getId(),new SinisterStone()));}
 @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(l.isClientSide)update.accept(e);}
 @Override public int getWarp(ItemStack s,Player p){return 1;}
}
