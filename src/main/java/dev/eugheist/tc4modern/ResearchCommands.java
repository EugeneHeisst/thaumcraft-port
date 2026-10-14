package dev.eugheist.tc4modern;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = TC4Modern.MODID)
public final class ResearchCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tc4")
            .then(Commands.literal("give_aspects").requires(source->source.hasPermission(2)).executes(ctx -> {
                var player=ctx.getSource().getPlayerOrException();
                ResearchKnowledge.grantAll(player.getPersistentData());
                ThaumometerItem.sync(player, "");
                ctx.getSource().sendSuccess(()->Component.translatable("commands.tc4modern.give_aspects"),false);
                return 1;
            }))
            .then(Commands.literal("discover_aspects").requires(source->source.hasPermission(2)).executes(ctx -> {
                var player=ctx.getSource().getPlayerOrException();
                int added=ResearchKnowledge.discoverAll(player.getPersistentData());
                ThaumometerItem.sync(player, "");
                ctx.getSource().sendSuccess(()->Component.translatable("commands.tc4modern.discover_aspects",added),false);
                return 1;
            }))
            .then(Commands.literal("combine").then(Commands.argument("first", StringArgumentType.word())
                .then(Commands.argument("second", StringArgumentType.word()).executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    String result = ResearchKnowledge.combine(player.getPersistentData(), StringArgumentType.getString(ctx, "first"), StringArgumentType.getString(ctx, "second"));
                    ThaumometerItem.sync(player, result);
                    return result.startsWith("combined:") ? 1 : 0;
                }))))
            .then(Commands.literal("aspects").executes(ctx -> {
                var player = ctx.getSource().getPlayerOrException();
                ResearchKnowledge.initialize(player.getPersistentData());
                StringBuilder text = new StringBuilder();
                for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values()) {
                    int points = ResearchKnowledge.points(player.getPersistentData(), aspect.getTag());
                    if (points > 0) text.append(aspect.getTag()).append(": ").append(points).append("  ");
                }
                ctx.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
                return 1;
            })));
    }
}
