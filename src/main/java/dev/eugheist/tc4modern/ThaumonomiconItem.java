package dev.eugheist.tc4modern;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

/**
 * ItemThaumonomicon: opening the book completes missing siblings of completed research. The metadata-42 cheat sheet (here a
 * separate item) instead grants every research and 50 points of each undiscovered aspect while general.allow_cheat_sheet is on.
 */
public class ThaumonomiconItem extends Item {
    final boolean cheat;
    public ThaumonomiconItem(Properties properties) { this(properties,false); }
    public ThaumonomiconItem(Properties properties,boolean cheat) { super(properties); this.cheat=cheat; }
    @Override public Component getName(ItemStack stack) { return Component.translatable("item.tc4modern.thaumonomicon"); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> tooltip,TooltipFlag flag) { if(cheat)tooltip.add(Component.literal("Cheat Sheet")); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server) {
            var data=player.getPersistentData();
            if(cheat&&ResearchConfig.cheatSheet()){
                thaumcraft.common.lib.events.CommandThaumcraft.grantAllResearch(data,key->ResearchWarp.award(player,key));
                var knowledge=ResearchKnowledge.load(data);for(Aspect aspect:Aspect.aspects.values())if(!knowledge.hasDiscoveredAspect("current",aspect))knowledge.addAspectPool("current",aspect,(short)50);ResearchKnowledge.save(data,knowledge);
            } else for(var category:ResearchCategories.researchCategories.values())for(var item:category.research.values()){
                var completed=ResearchProgress.completed(data);if(!completed.contains(item.key)||item.siblings==null)continue;
                for(String sibling:item.siblings)if(!ResearchProgress.completed(data).contains(sibling)&&ResearchProgress.add(data,sibling))ResearchWarp.award(player,sibling);
            }
            ResearchProgress.completed(data);
            ThaumometerItem.sync(server,"");
            server.openMenu(new SimpleMenuProvider(ThaumonomiconMenu::new,Component.translatable("item.tc4modern.thaumonomicon")));
        } else level.playLocalSound(player.getX(),player.getY(),player.getZ(),InfusionContent.MatrixEntity.sound("page"),net.minecraft.sounds.SoundSource.PLAYERS,1,1,false);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
