package dev.eugheist.tc4modern;

import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Opt-in actual server/dispatcher check using ephemeral players in a separate audit world. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class LegacyCommandAudit {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("tc4modern.commandAudit"))return;
        var server=event.getServer();
        java.util.List<net.minecraft.server.level.ServerPlayer> players;
        try {// PlayerList exposes a read-only view; add ephemeral audit players to its backing list.
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);players=(java.util.List<net.minecraft.server.level.ServerPlayer>)field.get(server.getPlayerList());}
        catch(ReflectiveOperationException ex){server.halt(false);throw new IllegalStateException("Audit player fixture unavailable",ex);}
        var first=FakePlayerFactory.get(server.overworld(),new GameProfile(UUID.fromString("12a37f2c-d20c-45b6-8a9c-de8047281c73"),"TC4Audit"));
        var second=FakePlayerFactory.get(server.overworld(),new GameProfile(UUID.fromString("ecda8841-2b8b-4d3f-b7f1-43e04d5651ef"),"TC4Other"));
        players.add(first);players.add(second);
        try {
            var dispatcher=server.getCommands().getDispatcher();var source=server.createCommandSourceStack();
            for(String alias:new String[]{"thaumcraft","thaum","tc"})require(dispatcher.execute(alias+" help",source)==1,"Help alias "+alias);
            require(dispatcher.execute("tc research list",source)>0,"Research listing");
            require(dispatcher.execute("thaumcraft aspect TC4Audit Aer 125",source)==1&&ResearchKnowledge.points(first.getPersistentData(),"aer")==125,"Named-player aspect grant");
            require(ResearchKnowledge.points(second.getPersistentData(),"aer")==0,"Other player's knowledge independent");
            require(dispatcher.execute("tc AsPeCt @s all 3",first.createCommandSourceStack().withPermission(2))==1,"Original action case and selector");
            require(dispatcher.execute("tc research TC4Audit RESEARCHDUPE",source)==1&&ResearchProgress.completed(first.getPersistentData()).contains("RESEARCHER2"),"Recursive grant over dispatcher");
            var beforeWarp=PlayerWarp.load(first).total();require(beforeWarp>0,"Research grants original warp");
            require(dispatcher.execute("thaum research TC4Audit all",source)==1,"All research");
            int allWarp=PlayerWarp.load(first).total();dispatcher.execute("thaum research TC4Audit all",source);require(PlayerWarp.load(first).total()==allWarp,"Repeated all doesn't award warp twice");
            dispatcher.execute("tc research TC4Audit reset",source);require(!ResearchProgress.completed(first.getPersistentData()).contains("RESEARCHDUPE")&&PlayerWarp.load(first).total()==allWarp,"Reset leaves warp");
            var state=PlayerWarp.load(first);state.setCounter(19);PlayerWarp.save(first,state);
            dispatcher.execute("tc warp TC4Audit set 20 PERM",source);dispatcher.execute("tc warp TC4Audit add -7 perm",source);
            require(PlayerWarp.load(first).get(WarpState.Kind.PERMANENT)==13&&PlayerWarp.load(first).counter()==19,"Low-level permanent decrement and counter");
            dispatcher.execute("tc warp TC4Audit set +6 TEMP",source);dispatcher.execute("tc warp TC4Audit set 4",source);
            require(PlayerWarp.load(first).get(WarpState.Kind.TEMPORARY)==6&&PlayerWarp.load(first).get(WarpState.Kind.STICKY)==4,"Bucket selection");
            require(dispatcher.execute("tc aspect TC4Audit unknown 1",source)==0&&dispatcher.execute("tc research TC4Audit unknown",source)==0,"Unknown identifiers fail");
            for(String command:new String[]{"tc aspect TC4Audit aer 0","tc warp TC4Audit add 101","tc warp TC4Audit set -1","tc warp TC4Audit set 1x"}) {
                boolean rejected=false;try{dispatcher.execute(command,source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}require(rejected,"Original numeric bound: "+command);
            }
            boolean denied=false;try{dispatcher.execute("tc help",source.withPermission(1));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}require(denied,"Operator level2 required");
            require(dispatcher.execute("tc debug all TC4Audit",source)==1,"Debug grants research and aspects");
            for(var category:thaumcraft.api.research.ResearchCategories.researchCategories.values())
                for(var research:category.research.values())require(ResearchProgress.completed(first.getPersistentData()).contains(research.key),"Debug research "+research.key);
            for(var aspect:thaumcraft.api.aspects.Aspect.aspects.values())require(ResearchKnowledge.points(first.getPersistentData(),aspect.getTag())>=100,"Debug aspect "+aspect.getTag());
            for(var input:java.util.Map.of("tc ","research","tc research TC4Audit RES","RESEARCHER1","tc aspect TC4Audit a","aer","tc warp TC4Audit set 1 ","PERM","tc research ","TC4Audit").entrySet()) {
                var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse(input.getKey(),source)).get();
                require(suggestions.getList().stream().anyMatch(v->v.getText().equals(input.getValue())),"Argument completion "+input.getKey()+" -> "+suggestions);
            }
            String result="PASS: actual server dispatcher, all aliases/actions, named player and selector, research warp and reset, numeric bounds, independent players, permissions\n";
            Files.writeString(Path.of("command-audit.txt"),result);System.out.print("TC4_COMMAND_AUDIT "+result);
        } catch(Exception ex){throw new IllegalStateException("Legacy command audit failed",ex);}
        finally {players.remove(first);players.remove(second);server.halt(false);}
    }
}
