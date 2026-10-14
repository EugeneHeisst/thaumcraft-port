package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** EventHandlerEntity.itemExpire: the entire dropped stack expires after200 ticks, making ONE source. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class BathSaltExpiry {
 @SubscribeEvent static void expire(net.neoforged.neoforge.event.entity.item.ItemExpireEvent event){var item=event.getEntity();var level=item.level();var pos=item.blockPosition();var state=level.getBlockState(pos);if(item.getItem().is(Hygiene.SALTS.get())&&state.is(net.minecraft.world.level.block.Blocks.WATER)&&state.getFluidState().isSource())level.setBlock(pos,PureWater.BLOCK.get().defaultBlockState(),3);}
}
