package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Opt-in visible inspection of a disposable world, never enabled in normal gameplay. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class ForestPreview {
    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("tc4modern.forestPreview") || !(event.getEntity() instanceof ServerPlayer player)) return;
        var level=player.serverLevel();
        var found=level.findClosestBiome3d(b -> b.is(MagicalForestContent.BIOME),new BlockPos(0,70,0),6400,64,128);
        if(found==null) throw new IllegalStateException("No magical forest found in preview world");
        var center=found.getFirst();
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(1000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false,level.getServer());
        level.setWeatherParameters(6000,0,false,false);
        player.teleportTo(center.getX()+.5,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,center.getX(),center.getZ())+3,center.getZ()+.5);
        player.getAbilities().flying=true;player.onUpdateAbilities();
        for(var item:java.util.List.of(TC4Modern.THAUMOMETER.get(),TC4Modern.AURA_NODE_ITEM.get(),BasicsContent.MANA_BEAN.get(),BasicsContent.DECONSTRUCTOR.get().asItem(),BasicsContent.NODE_JAR.get().asItem())) player.getInventory().add(new net.minecraft.world.item.ItemStack(item));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Тестовый магический лес. Творческий режим; деревья, стручки и их аспекты можно осмотреть самостоятельно."));
        System.out.println("TC4_FOREST_PREVIEW ready at "+center);
    }
}
