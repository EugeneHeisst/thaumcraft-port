package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Recipe transfer into the arcane workbench (NEI overlay handler of the original): the client picks one template stack per
 * grid slot, the server moves the old grid contents back to the player and fills each slot with one matching item from the
 * player's inventory. {@link #grid} is shared with addons that fill their own 3x3 grids (Thaumic Energistics terminal).
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArcaneGridFill {
    private ArcaneGridFill() {}

    /** 3x3 slot ingredients of an arcane entry or a vanilla crafting recipe (shaped recipes keep their layout). */
    public static NonNullList<Ingredient> grid(Object recipe) {
        var out = NonNullList.withSize(9, Ingredient.EMPTY);
        if (recipe instanceof ArcaneRecipes.Entry e) {
            for (int i = 0; i < e.ingredients().size(); i++) { int row = i / e.width(), col = i % e.width(); if (row < 3 && col < 3) out.set(row * 3 + col, e.ingredients().get(i)); }
        } else if (recipe instanceof ShapedRecipe s) {
            for (int i = 0; i < s.getIngredients().size(); i++) { int row = i / s.getWidth(), col = i % s.getWidth(); if (row < 3 && col < 3) out.set(row * 3 + col, s.getIngredients().get(i)); }
        } else if (recipe instanceof Recipe<?> r) {
            var list = r.getIngredients();
            for (int i = 0; i < list.size() && i < 9; i++) out.set(i, list.get(i));
        }
        return out;
    }

    public record Fill(List<ItemStack> templates) implements CustomPacketPayload {
        public static final Type<Fill> TYPE = new Type<>(AlchemyContent.id("arcane_grid_fill"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Fill> CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)), Fill::templates, Fill::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("tc4-arcane-fill-1").playToServer(Fill.TYPE, Fill.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.containerMenu instanceof ArcaneWorkbenchMenu menu && p.templates().size() == 9) fill(player, menu, p.templates());
        }));
    }

    static void fill(ServerPlayer player, ArcaneWorkbenchMenu menu, List<ItemStack> templates) {
        var table = menu.table;var inv = player.getInventory();
        // Everything not matching its slot goes back to the player first, so it can be reused for other slots.
        for (int i = 0; i < 9; i++) {
            var current = table.getItem(i);
            if (current.isEmpty() || !templates.get(i).isEmpty() && current.is(templates.get(i).getItem()) && current.getCount() == 1) continue;
            table.setItem(i, ItemStack.EMPTY);
            if (!inv.add(current)) player.drop(current, false);
        }
        for (int i = 0; i < 9; i++) {
            var want = templates.get(i);
            if (want.isEmpty() || !table.getItem(i).isEmpty()) continue;
            for (int s = 0; s < inv.items.size(); s++) {
                var stack = inv.items.get(s);
                if (stack.is(want.getItem()) && (want.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(stack, want))) { table.setItem(i, stack.split(1));break; }
            }
        }
        table.setChanged();menu.broadcastChanges();
    }
}
