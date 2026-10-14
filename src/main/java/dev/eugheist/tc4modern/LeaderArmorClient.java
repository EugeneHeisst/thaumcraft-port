package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class LeaderArmorClient {
 @SubscribeEvent static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){final LeaderArmorModel chest=new LeaderArmorModel(true),other=new LeaderArmorModel(false);
  @Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){var model=slot==EquipmentSlot.CHEST?chest:other;model.animate(entity.walkAnimation.position(),entity.walkAnimation.speed());return model;}
 },LeaderGear.item("cultist_leader_helmet"),LeaderGear.item("cultist_leader_chestplate"),LeaderGear.item("cultist_leader_leggings"));}
}
