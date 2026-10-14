package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * ThaumcraftApi.portableHoleBlackList: the fixed ConfigBlocks entries (bed, pistons, wooden and iron doors, every
 * BlockEldritch metadata, the eldritch portal, flux goo/gas and the infernal furnace) are the block tag
 * tc4modern:portable_hole_blacklist; general.portablehole_blacklist adds comma-separated block names, default iron_door.
 */
public final class PortableHoleConfig {
    private PortableHoleConfig(){}
    public static final TagKey<Block> BLACKLIST=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("tc4modern","portable_hole_blacklist"));
    static ModConfigSpec.ConfigValue<String> NAMES;
    private static String parsed;private static Set<Block> configured=Set.of();
    static void define(ModConfigSpec.Builder b){b.push("general");NAMES=b.comment("This is a comma-delimited list of any block names the portable hole is not allowed to pass through.").define("portablehole_blacklist","iron_door");b.pop();}
    static synchronized Set<Block> configured(){
        String names=WarpConfig.SPEC.isLoaded()?NAMES.get():"iron_door";
        if(!names.equals(parsed)){var out=new HashSet<Block>();for(var name:names.split(",")){var id=ResourceLocation.tryParse(name.trim());if(id!=null)BuiltInRegistries.BLOCK.getOptional(id).ifPresent(out::add);}configured=out;parsed=names;}
        return configured;
    }
    public static boolean blocked(BlockState state){return state.is(BLACKLIST)||configured().contains(state.getBlock());}
}
