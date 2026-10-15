package dev.eugheist.tc4modern;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.common.lib.events.CommandThaumcraft;

/** Brigadier transport for the locally adapted original command parser. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class LegacyCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        for (String alias : new String[]{"thaumcraft", "thaum", "tc"}) {
            dispatcher.register(Commands.literal(alias).requires(source -> source.hasPermission(2))
                .executes(ctx -> CommandThaumcraft.execute(ctx.getSource(), new String[0]))
                .then(Commands.literal("debug").then(Commands.literal("all")
                    .executes(ctx -> debugAll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(ctx -> debugAll(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx,"player")))))
                    .then(Commands.literal("scanitems")
                    .executes(ctx -> scanItems(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                        .executes(ctx -> scanItems(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx,"player"))))))
                .then(Commands.argument("arguments", StringArgumentType.greedyString())
                    .suggests(LegacyCommands::suggest)
                    .executes(ctx -> CommandThaumcraft.execute(ctx.getSource(),
                        StringArgumentType.getString(ctx, "arguments").trim().split("\\s+")))));
        }
    }
    static int debugAll(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer player) {
        thaumcraft.common.config.ConfigResearchData.init();
        // Debug convenience: unlock knowledge without awarding gameplay research warp.
        CommandThaumcraft.grantAllResearch(player.getPersistentData(), key -> {});
        ResearchKnowledge.grantAll(player.getPersistentData());
        // Forget earlier debug item marks so every item is scanned for real and pays its aspect points again.
        var knowledge = player.getPersistentData().getCompound(ResearchKnowledge.KEY); var scanned = knowledge.getCompound("scanned");
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) scanned.remove(ScanTarget.item(new net.minecraft.world.item.ItemStack(item), player.level()).knowledgeKey());
        knowledge.remove("debugAllItemScans"); knowledge.put("scanned", scanned); player.getPersistentData().put(ResearchKnowledge.KEY, knowledge);
        var gained = scanAllItems(player);
        ResearchKnowledge.grantAllItemScans(player.getPersistentData());
        ThaumometerItem.sync(player, "");
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Все исследования, аспекты и сканирования предметов открыты: " + player.getGameProfile().getName() + " (просканировано " + gained[0] + " предметов, +" + gained[1] + " очков аспектов)"), true);
        return 1;
    }
    static int scanItems(net.minecraft.commands.CommandSourceStack source, net.minecraft.server.level.ServerPlayer player) {
        var gained = scanAllItems(player);
        ThaumometerItem.sync(player, "");
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(player.getGameProfile().getName() + ": просканировано " + gained[0] + " предметов, +" + gained[1] + " очков аспектов"), true);
        return gained[0];
    }
    /**
     * A real thaumometer scan of every registered item: points into the aspect pool, newly discovered aspects and research clues,
     * exactly as ThaumometerItem does. Repeats until nothing new scans, since compound aspects need their parents discovered first.
     * Returns {items scanned, aspect points gained}.
     */
    static int[] scanAllItems(net.minecraft.server.level.ServerPlayer player) {
        var data = player.getPersistentData(); int items = 0, points = 0; boolean progress = true;
        while (progress) {
            progress = false;
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (item == net.minecraft.world.item.Items.AIR) continue;
                var target = ScanTarget.item(new net.minecraft.world.item.ItemStack(item), player.level());
                var awarded = new thaumcraft.api.aspects.AspectList();
                if (!ResearchKnowledge.scan(data, target.knowledgeKey(), target.aspects(), awarded::add).equals("scanned")) continue;
                items++; progress = true; points += awarded.visSize();
                if (awarded.size() > 0) ResearchClues.revealClue(player, null, target.knowledgeKey(), awarded);
            }
        }
        return new int[]{items, points};
    }
    static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(
            com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        thaumcraft.common.config.ConfigResearchData.init();
        String text=builder.getRemaining();
        String[] words=text.split("\\s+",-1);
        int index=words.length-1;
        var choices=new java.util.ArrayList<String>();
        if(index==0)choices.addAll(java.util.List.of("help","research","aspect","warp","debug"));
        else if(index==1) {
            if(words[0].equalsIgnoreCase("debug"))choices.add("all");
            else if(java.util.List.of("research","aspect","warp").contains(words[0].toLowerCase(java.util.Locale.ROOT))) {
                choices.addAll(ctx.getSource().getOnlinePlayerNames());choices.add("@s");choices.add("@p");
                if(words[0].equalsIgnoreCase("research"))choices.add("list");
            }
        } else if(index==2) {
            switch(words[0].toLowerCase(java.util.Locale.ROOT)) {
                case "research" -> {if(!words[1].equalsIgnoreCase("list")){choices.addAll(java.util.List.of("all","reset"));for(var category:thaumcraft.api.research.ResearchCategories.researchCategories.values())choices.addAll(category.research.keySet());}}
                case "aspect" -> {choices.add("all");choices.addAll(thaumcraft.api.aspects.Aspect.aspects.keySet());}
                case "warp" -> choices.addAll(java.util.List.of("add","set"));
                case "debug" -> {choices.addAll(ctx.getSource().getOnlinePlayerNames());choices.addAll(java.util.List.of("@s","@p"));}
            }
        } else if(index==3 && (words[0].equalsIgnoreCase("aspect")||words[0].equalsIgnoreCase("warp")))choices.addAll(java.util.List.of("1","10","100"));
        else if(index==4 && words[0].equalsIgnoreCase("warp"))choices.addAll(java.util.List.of("PERM","TEMP"));
        var offset=builder.createOffset(builder.getStart()+text.length()-words[index].length());
        String prefix=words[index].toLowerCase(java.util.Locale.ROOT);
        choices.stream().distinct().filter(v->v.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).sorted().forEach(offset::suggest);
        return offset.buildFuture();
    }

}
