package thaumcraft.common.lib.events;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.eugheist.tc4modern.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.*;
import thaumcraft.common.config.ConfigResearchData;

/** Local adaptation of TC4 4.2.3.5 CommandThaumcraft; modern command/player/NBT transport. */
public final class CommandThaumcraft {
    public static final String RESEARCH_USAGE="/thaumcraft research <list|player> <all|reset|<research>>";
    public static final String ASPECT_USAGE="/thaumcraft aspect <player> <aspect|all> <amount>";
    public static final String WARP_USAGE="/thaumcraft warp <player> <add|set> <amount> <PERM|TEMP>";

    public static int execute(CommandSourceStack source,String[] args) throws CommandSyntaxException {
        ConfigResearchData.init();
        if(args.length==0)return invalid(source,"/thaumcraft help to get help");
        if(args[0].equalsIgnoreCase("help")) {
            message(source,"§3You can also use /thaum or /tc instead of /thaumcraft.");
            message(source,"§3Use this to give research to a player.");message(source,"  "+RESEARCH_USAGE);
            message(source,"§3Use this to give aspect research points to a player.");message(source,"  "+ASPECT_USAGE);
            message(source,"§3Use this to give set a players warp level.");message(source,"  "+WARP_USAGE);
            message(source,"  not specifying perm or temp will just add normal warp");return 1;
        }
        if(args.length<2)return invalid(source,"/thaumcraft help to get help");
        if(args[0].equalsIgnoreCase("research")&&args[1].equalsIgnoreCase("list")) {
            int count=0;for(var category:ResearchCategories.researchCategories.values())for(var research:category.research.values()) {message(source,"§5"+research.key);count++;}return count;
        }
        ServerPlayer player=EntityArgument.player().parse(new StringReader(args[1])).findSinglePlayer(source);
        if(args[0].equalsIgnoreCase("research")) {
            if(args.length!=3)return invalid(source,RESEARCH_USAGE);
            if(args[2].equalsIgnoreCase("reset")) {
                resetResearch(player.getPersistentData());notify(source,player,"has reset you research.");
            } else if(args[2].equalsIgnoreCase("all")) {
                grantAllResearch(player.getPersistentData(),key->ResearchWarp.award(player,key));
                notify(source,player,"has given you all research.");
            } else {
                if(ResearchCategories.getResearch(args[2])==null)return error(source,"Research does not exist.");
                grantResearch(player.getPersistentData(),args[2],key->ResearchWarp.award(player,key));
                notify(source,player,"gave you "+args[2]+" research and its requisites.");
            }
            ThaumometerItem.sync(player,"");return 1;
        }
        if(args[0].equalsIgnoreCase("aspect")) {
            if(args.length!=4)return invalid(source,ASPECT_USAGE);
            int amount=number(args[3],1,Integer.MAX_VALUE);
            Aspect aspect=resolveAspect(args[2]);boolean all=args[2].equalsIgnoreCase("all");
            if(!all&&aspect==null)return error(source,"Aspect does not exist.");
            giveAspects(player.getPersistentData(),args[2],amount);ThaumometerItem.sync(player,"");
            notify(source,player,"gave you "+amount+(all?" of all the aspects.":" "+aspect.getName()));return 1;
        }
        if(args[0].equalsIgnoreCase("warp")) {
            if(args.length<4||!args[2].equalsIgnoreCase("set")&&!args[2].equalsIgnoreCase("add"))return invalid(source,WARP_USAGE);
            boolean add=args[2].equalsIgnoreCase("add");int amount=number(args[3],add?-100:0,add?100:Integer.MAX_VALUE);
            WarpState.Kind kind=warpKind(args.length==5?args[4]:"");
            var state=PlayerWarp.load(player);changeWarp(state,kind,amount,add);PlayerWarp.save(player,state);
            PacketDistributor.sendToPlayer(player,new WarpSyncPayload(state.get(kind),kind.ordinal()));
            if(add)PacketDistributor.sendToPlayer(player,new WarpMessagePayload(amount,kind.ordinal()));
            notify(source,player,add?"added "+amount+" warp to your total.":"set your warp to "+amount);return 1;
        }
        return invalid(source,"/thaumcraft help to get help");
    }
    private static int number(String text,int min,int max) throws CommandSyntaxException {
        int value;try {value=Integer.parseInt(text);}catch(NumberFormatException ex){throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().create(text);}
        if(value<min)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(value,min);
        if(value>max)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(value,max);return value;
    }
    private static void message(CommandSourceStack source,String value){source.sendSuccess(()->Component.literal(value),false);}
    private static int error(CommandSourceStack source,String value){source.sendFailure(Component.literal("§c"+value));return 0;}
    private static int invalid(CommandSourceStack source,String usage){error(source,"Invalid arguments");return error(source,"Use "+usage);}
    private static void notify(CommandSourceStack source,ServerPlayer player,String value){player.sendSystemMessage(Component.literal("§5"+source.getTextName()+" "+value));message(source,"§5Success!");}

    public static Aspect resolveAspect(String name) {
        Aspect result=Aspect.getAspect(name);
        if(result==null)for(Aspect aspect:Aspect.aspects.values())if(name.equalsIgnoreCase(aspect.getName()))return aspect;
        return result;
    }
    public static boolean giveAspects(CompoundTag data,String name,int amount) {
        boolean all=name.equalsIgnoreCase("all");Aspect aspect=resolveAspect(name);if(!all&&aspect==null)return false;
        var knowledge=ResearchKnowledge.load(data);
        if(all)for(Aspect entry:Aspect.aspects.values())knowledge.addAspectPool("current",entry,(short)amount);
        else knowledge.addAspectPool("current",aspect,(short)amount);
        ResearchKnowledge.save(data,knowledge);return true;
    }
    public static WarpState.Kind warpKind(String type) {
        return type.equalsIgnoreCase("PERM")?WarpState.Kind.PERMANENT:type.equalsIgnoreCase("TEMP")?WarpState.Kind.TEMPORARY:WarpState.Kind.STICKY;
    }
    /** Commands use the low-level PlayerKnowledge mutators: negative permanent additions are allowed; counter is preserved. */
    public static void changeWarp(WarpState state,WarpState.Kind kind,int amount,boolean add) {
        state.set(kind,add?state.get(kind)+amount:amount);
    }
    public static Set<String> grantResearch(CompoundTag data,String key,Consumer<String> onComplete) {
        var completed=ResearchProgress.completed(data);var added=new LinkedHashSet<String>();
        grant(key,completed,added);writeResearch(data,completed);added.forEach(onComplete);return added;
    }
    private static void grant(String key,Set<String> completed,Set<String> added) {
        var item=ResearchCategories.getResearch(key);if(item==null||!completed.add(key))return;
        added.add(key);
        if(item.parents!=null)for(String parent:item.parents)grant(parent,completed,added);
        if(item.parentsHidden!=null)for(String parent:item.parentsHidden)grant(parent,completed,added);
        if(item.siblings!=null)for(String sibling:item.siblings)grant(sibling,completed,added);
    }
    public static Set<String> grantAllResearch(CompoundTag data,Consumer<String> onComplete) {
        var completed=ResearchProgress.completed(data);var added=new LinkedHashSet<String>();
        for(var category:ResearchCategories.researchCategories.values())for(var item:category.research.values())if(completed.add(item.key))added.add(item.key);
        writeResearch(data,completed);added.forEach(onComplete);return added;
    }
    public static void resetResearch(CompoundTag data) {
        ConfigResearchData.init();var completed=new LinkedHashSet<String>();
        for(var category:ResearchCategories.researchCategories.values())for(var item:category.research.values())if(item.isAutoUnlock())completed.add(item.key);
        writeResearch(data,completed);
    }
    private static void writeResearch(CompoundTag data,Set<String> completed) {
        var list=new ListTag();completed.stream().sorted().forEach(key->list.add(StringTag.valueOf(key)));
        var knowledge=data.getCompound(ResearchKnowledge.KEY);knowledge.put("Research",list);data.put(ResearchKnowledge.KEY,knowledge);
    }
}
