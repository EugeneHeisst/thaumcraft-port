package dev.eugheist.tc4modern;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.aspects.IEssentiaTransport;

/**
 * ItemResonator: tapping a tube, jar or other essentia device tells the essentia it holds on the tapped side (every aspect of
 * an essentia buffer) and the suction strength and aspect there, with the alembic knock.
 */
public final class ResonatorItem extends Item {
    public ResonatorItem(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.UNCOMMON)); }

    @Override public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        var level = context.getLevel();var pos = context.getClickedPos();var player = context.getPlayer();
        if (!(level.getBlockEntity(pos) instanceof IEssentiaTransport transport) || player == null) return InteractionResult.PASS;
        if (level.isClientSide) { player.swing(context.getHand()); return InteractionResult.SUCCESS; }
        var face = context.getClickedFace();
        if (!(transport instanceof EssentiaBufferEntity) && transport.getEssentiaType(face) != null)
            player.sendSystemMessage(Component.translatable("tc.resonator1", "" + transport.getEssentiaAmount(face), transport.getEssentiaType(face).getName()));
        else if (transport instanceof EssentiaBufferEntity buffer && buffer.getAspects().size() > 0)
            for (var aspect : buffer.getAspects().getAspectsSorted())
                player.sendSystemMessage(Component.translatable("tc.resonator1", "" + buffer.getAspects().getAmount(aspect), aspect.getName()));
        String suction = transport.getSuctionType(face) != null ? transport.getSuctionType(face).getName() : Component.translatable("tc.resonator3").getString();
        player.sendSystemMessage(Component.translatable("tc.resonator2", "" + transport.getSuctionAmount(face), suction));
        level.playSound(null, pos, AlembicBlock.KNOCK, SoundSource.BLOCKS, .5f, 1.9f + level.random.nextFloat() * .1f);
        return InteractionResult.SUCCESS;
    }
}
