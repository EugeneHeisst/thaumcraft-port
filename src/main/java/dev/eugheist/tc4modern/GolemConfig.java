package dev.eugheist.tc4modern;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original general golem polling in ticks and failed-item timeout in wall-clock milliseconds. */
final class GolemConfig {
 static ModConfigSpec.IntValue DELAY, IGNORE_DELAY;static ModConfigSpec.BooleanValue CHEST_INTERACT;
 static void define(ModConfigSpec.Builder b){b.push("general");DELAY=b.comment("Original interval in ticks for golem searches and home item transfers.").defineInRange("golem_delay",5,1,1000000);IGNORE_DELAY=b.comment("Original milliseconds for ignoring an item with no valid destination.").defineInRange("golem_ignore_delay",10000,1000,Integer.MAX_VALUE);CHEST_INTERACT=b.comment("If set to true golems will attempt to play the chest opening animations and sounds whenever they interact with them.").define("golem_chest_interact",true);b.pop();}
 static int delay(){return WarpConfig.SPEC.isLoaded()?DELAY.get():5;}
 static boolean chestInteract(){return !WarpConfig.SPEC.isLoaded()||CHEST_INTERACT.get();}
 static int ignoreDelay(){return WarpConfig.SPEC.isLoaded()?IGNORE_DELAY.get():10000;}
}
