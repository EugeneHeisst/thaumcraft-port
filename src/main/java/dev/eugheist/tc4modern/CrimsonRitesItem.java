package dev.eugheist.tc4modern;
public final class CrimsonRitesItem extends net.minecraft.world.item.Item {
 public CrimsonRitesItem(Properties p){super(p);}
 @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level l,net.minecraft.world.entity.player.Player p,net.minecraft.world.InteractionHand h){if(p instanceof net.minecraft.server.level.ServerPlayer server&&ResearchProgress.complete(p,"CRIMSON")){ThaumometerItem.sync(server,"research_complete");net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new ResearchCompletedPayload("CRIMSON"));l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("learn")),net.minecraft.sounds.SoundSource.PLAYERS,1,1);}return net.minecraft.world.InteractionResultHolder.sidedSuccess(p.getItemInHand(h),l.isClientSide);}
}
