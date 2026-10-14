package dev.eugheist.tc4modern;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.api.research.ResearchCategories;

/**
 * Operator-only testing helper (not an original TC4 feature): /tc4dev research &lt;KEY&gt; completes a research
 * entry together with its missing parents, so recipe gates can be exercised in isolated test worlds.
 */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class DevCommands {
    private DevCommands() {}
    @SubscribeEvent static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tc4dev").requires(s -> s.hasPermission(2))
            .then(Commands.literal("research").then(Commands.argument("key", StringArgumentType.word()).executes(ctx -> {
                var player = ctx.getSource().getPlayerOrException();
                String key = StringArgumentType.getString(ctx, "key");
                thaumcraft.common.config.ConfigResearchData.init();
                if (ResearchCategories.getResearch(key) == null) { ctx.getSource().sendFailure(Component.literal("Unknown research " + key)); return 0; }
                int added = grant(player.getPersistentData(), key, new java.util.HashSet<>());
                ThaumometerItem.sync(player, "");
                ctx.getSource().sendSuccess(() -> Component.literal("Completed " + key + " (+" + added + ")"), false);
                return added;
            }))));
    }
    static int grant(net.minecraft.nbt.CompoundTag data, String key, java.util.Set<String> seen) {
        if (!seen.add(key)) return 0;
        var item = ResearchCategories.getResearch(key);
        if (item == null || ResearchProgress.completed(data).contains(key)) return 0;
        int n = 0;
        if (item.parents != null) for (String p : item.parents) n += grant(data, p, seen);
        if (item.parentsHidden != null) for (String p : item.parentsHidden) n += grant(data, p, seen);
        return n + (ResearchProgress.complete(data, key) ? 1 : 0);
    }
}
