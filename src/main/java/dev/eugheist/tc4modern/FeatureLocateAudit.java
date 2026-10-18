package dev.eugheist.tc4modern;

import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Opt-in native dispatcher + normal fresh-world generation, never places fixtures. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class FeatureLocateAudit {
    static final List<String> messages=new ArrayList<>();
    static final CommandSource sink=new CommandSource(){
        public void sendSystemMessage(Component c){messages.add(c.getString());System.out.println("TC4_LOCATE MESSAGE "+c.getString());}
        public boolean acceptsSuccess(){return true;}public boolean acceptsFailure(){return true;}public boolean shouldInformAdmins(){return false;}
    };
    static String beforeState;static CommandSourceStack source;static int current,ticks;static boolean active;static final Properties results=new Properties();
    static void check(boolean value,String text){if(!value)throw new AssertionError("TC4_LOCATE FAIL "+text);System.out.println("TC4_LOCATE PASS "+text);}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e)throws Exception {
        if(!Boolean.getBoolean("tc4modern.featureLocateAudit"))return;
        var server=e.getServer();source=server.createCommandSourceStack().withSource(sink).withPosition(new net.minecraft.world.phys.Vec3(0,80,0));
        try {
            var d=server.getCommands().getDispatcher();
            for(String prefix:List.of("locate structure ","locate structure tc4modern:")) {
                var suggestions=d.getCompletionSuggestions(d.parse(prefix,source)).get().getList();
                for(String id:FeatureLocate.IDS)check(suggestions.stream().anyMatch(s->s.getText().equals("tc4modern:"+id)),"native completion "+prefix+id);
            }
            var biome=d.getCompletionSuggestions(d.parse("locate biome minecraft:pl",source)).get().getList();
            check(biome.stream().anyMatch(s->s.getText().equals("minecraft:plains")),"vanilla locate biome completion retained");
            boolean denied=false;try{d.execute("locate structure tc4modern:mound",source.withPermission(1));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            check(denied,"permission level 2 enforced");
            check(d.execute("locate structure tc4modern:mound",source.withLevel(server.getLevel(net.minecraft.world.level.Level.NETHER)))==0,"Nether rejected without search");
            check(d.execute("locate structure tc4modern:mound",source.withLevel(server.getLevel(net.minecraft.world.level.Level.END)))==0,"End rejected without search");
            WorldgenConfig.STRUCTURES.set(false);
            check(d.execute("locate structure tc4modern:mound",source)==0,"disabled structure config rejected");WorldgenConfig.STRUCTURES.set(true);
            check(server.getLevel(OuterDimension.KEY)!=null,"Outer Lands dimension available");
            check(d.execute("locate structure tc4modern:mound",source.withLevel(server.getLevel(OuterDimension.KEY)))==0,"Outer Lands rejected without search");
            if(source.getLevel().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource) {
                for(String id:FeatureLocate.IDS)check(d.execute("locate structure tc4modern:"+id,source)==0,"flat world rejected "+id);
                System.out.println("TC4_LOCATE FLAT ALL PASS");server.halt(false);return;
            }
            active=true;begin();
        }catch(Throwable ex){server.halt(false);throw ex;}
    }
    static BlockPos position(String coords){var v=coords.split(", ");return new BlockPos(Integer.parseInt(v[0]),Integer.parseInt(v[1]),Integer.parseInt(v[2]));}
    static String fingerprint(BlockPos p)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(int x=-10;x<=10;x++)for(int y=-8;y<=16;y++)for(int z=-10;z<=10;z++)
            digest.update(source.getLevel().getBlockState(p.offset(x,y,z)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    static void begin()throws Exception {
        messages.clear();ticks=0;beforeState=null;
        var path=Path.of(System.getProperty("tc4modern.featureLocateResults"));
        if(Files.exists(path)){var old=new Properties();try(var r=Files.newBufferedReader(path)){old.load(r);}beforeState=fingerprint(position(old.getProperty(FeatureLocate.IDS.get(current))));}
        check(source.getServer().getCommands().getDispatcher().execute("locate structure tc4modern:"+FeatureLocate.IDS.get(current),source)==1,"native command accepted "+FeatureLocate.IDS.get(current));}
    @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e)throws Exception {
        if(!active)return;
        try {
            if(++ticks>15000)throw new AssertionError("search timeout");
            String found=messages.stream().filter(s->s.startsWith("Found tc4modern:")).findFirst().orElse(null);
            if(found==null){checkNoFailure();return;}
            String id=FeatureLocate.IDS.get(current);String coords=found.substring(found.indexOf('[')+1,found.indexOf(']'));
            var p=position(coords);var l=source.getLevel();
            if(beforeState!=null)check(beforeState.equals(fingerprint(p)),id+" repeated native search leaves surrounding block states unchanged (no second placement)");
            check(!l.getBiome(p).is(net.minecraft.tags.BiomeTags.IS_OCEAN),id+" real location is not an ocean biome");
            check(!l.getBlockState(p).isAir(),id+" actual generated block "+l.getBlockState(p)+" at "+p);
            switch(id) {
                case "mound" -> {
                    check(l.getBlockEntity(p) instanceof RandomizableContainerBlockEntity chest&&chest.getLootTable()!=null,"mound retained loot table");
                    check(l.getBlockState(p.offset(-6,4,-5)).is(Blocks.SPAWNER)&&l.getBlockState(p.offset(-6,4,5)).is(Blocks.SPAWNER),"mound retained both original spawners");
                    check(l.getBlockEntity(p.offset(-1,7,0)) instanceof AuraNodeEntity,"mound retained node");
                    check(l.getBlockState(p.offset(-3,0,0)).is(Blocks.LADDER),"mound original blueprint ladder");
                }
                case "hilltop_stones" -> {
                    check(l.getBlockEntity(p.above(2)) instanceof RandomizableContainerBlockEntity chest&&chest.getLootTable()!=null,"hilltop retained loot table");
                    check(l.getBlockEntity(p.above(5)) instanceof AuraNodeEntity,"hilltop retained node");
                    for(int x:new int[]{-3,3})for(int z:new int[]{-1,1})check(l.getBlockState(p.offset(x,1,z)).is(ObsidianTotems.PLAIN.get()),"hilltop original pillar "+x+","+z);
                }
                case "eldritch_ring" -> {check(l.getBlockEntity(p) instanceof CrimsonAltarEntity,"ring retained altar entity");check(l.getBlockEntity(p.above()) instanceof AuraNodeEntity,"ring retained eerie node");for(int y=3;y<=6;y++)check(l.getBlockState(p.above(y)).is(CrimsonContent.OBELISK_BODY.get()),"ring original obelisk body "+y);}
                case "obsidian_totem" -> check(l.getBlockEntity(p) instanceof AuraNodeEntity,"totem retained charged node");
            }
            results.setProperty(id,coords);System.out.println("TC4_LOCATE RESULT "+id+"="+coords+" ticks="+ticks);
            if(++current<FeatureLocate.IDS.size()){begin();return;}
            var path=Path.of(System.getProperty("tc4modern.featureLocateResults"));
            if(Files.exists(path)){var old=new Properties();try(var r=Files.newBufferedReader(path)){old.load(r);}check(old.equals(results),"save/reload deterministic coordinates "+results);}
            else try(var w=Files.newBufferedWriter(path)){results.store(w,"Native locate results");}
            System.out.println("TC4_LOCATE ALL PASS");active=false;e.getServer().halt(false);
        }catch(Throwable ex){active=false;e.getServer().halt(false);throw ex;}
    }
    static void checkNoFailure(){if(messages.stream().anyMatch(s->s.startsWith("No intact")||s.startsWith("Thaumcraft search failed")))throw new AssertionError(messages.toString());}
}
