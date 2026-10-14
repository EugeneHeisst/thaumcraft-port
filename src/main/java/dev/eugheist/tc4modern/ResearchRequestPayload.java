package dev.eugheist.tc4modern;

import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.research.ResearchManager;

/** Adapts original book research requests, ink/paper consumption and secondary research spending. */
public record ResearchRequestPayload(int menuId,String key) implements CustomPacketPayload {
    public static final Type<ResearchRequestPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"request_research"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ResearchRequestPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,ResearchRequestPayload::menuId,ByteBufCodecs.STRING_UTF8,ResearchRequestPayload::key,ResearchRequestPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ResearchRequestPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if(!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ThaumonomiconMenu menu) || menu.containerId!=payload.menuId || !menu.stillValid(player)) return;
        var completed=ResearchProgress.completed(player.getPersistentData());
        var research=ResearchCategories.getResearch(payload.key);
        if(research==null || completed.contains(payload.key) || research.isVirtual() || research.isStub() || (research.isLost() || research.isHidden()) && !completed.contains("@"+payload.key) || !ResearchProgress.requisites(completed,research)) { ThaumometerItem.sync(player,"research_locked");return; }
        if(ResearchConfig.purchaseMode(research,ResearchConfig.difficulty())) {
            if(!ResearchKnowledge.purchase(player.getPersistentData(),research)) { ThaumometerItem.sync(player,"insufficient");return; }
            ResearchProgress.completeAndNotify(player,payload.key);ThaumometerItem.sync(player,"research_complete");return;
        }
        int paper=-1,ink=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);
            var note=NoteData.read(stack);
            if(note!=null && note.key.equals(payload.key)) { ThaumometerItem.sync(player,"note_exists");return; }
            if(stack.is(Items.PAPER)) paper=i;
            if(ScribingToolsItem.hasInk(stack)) ink=i;
        }
        if(paper<0 || ink<0) { ThaumometerItem.sync(player,"need_paper_ink");return; }
        var tag=ResearchManager.createNote(new net.minecraft.nbt.CompoundTag(),payload.key,new Random(player.getRandom().nextLong()));
        if(tag==null) return;
        ItemStack note=new ItemStack(TC4Modern.RESEARCH_NOTES.get());
        note.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        player.getInventory().getItem(paper).shrink(1);
        ItemStack tools=player.getInventory().getItem(ink);tools.setDamageValue(tools.getDamageValue()+1);
        if(!player.getInventory().add(note)) player.drop(note,false);
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();ThaumometerItem.sync(player,"note_created");
        player.level().playSound(null,player.blockPosition(),TC4Modern.LEARN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.75f,1);
    }
}
