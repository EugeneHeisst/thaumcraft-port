package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.ResearchManager;

/** Modern packet transport; never accepts new cells, anchor edits or client-side point balances. */
public record PlaceAspectPayload(int menuId,int q,int r,String aspect) implements CustomPacketPayload {
    public static final Type<PlaceAspectPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"place_aspect"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlaceAspectPayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,PlaceAspectPayload::menuId,ByteBufCodecs.VAR_INT,PlaceAspectPayload::q,
        ByteBufCodecs.VAR_INT,PlaceAspectPayload::r,ByteBufCodecs.STRING_UTF8,PlaceAspectPayload::aspect,PlaceAspectPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(PlaceAspectPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if(!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ResearchTableMenu menu) || menu.containerId!=payload.menuId || !menu.stillValid(player)) return;
        var ink=menu.table.getItem(0);var stack=menu.table.getItem(1);var note=NoteData.read(stack);
        if(note==null || note.complete || !ScribingToolsItem.hasInk(ink)) return;
        String cell=payload.q+":"+payload.r;
        var entry=note.hexEntries.get(cell);
        if(entry==null || entry.type==1) return;
        Aspect aspect=payload.aspect.isEmpty() ? null : Aspect.getAspect(payload.aspect);
        if(!payload.aspect.isEmpty() && aspect==null) return;
        if(aspect!=null && entry.type!=0) return;
        if(aspect==null && entry.type!=2) return;
        String result=ResearchKnowledge.place(player.getPersistentData(),note,payload.q,payload.r,aspect,player.getRandom().nextFloat(),menu.bonus);
        if(!result.equals("placed")) { ThaumometerItem.sync(player,result);return; }
        var knowledge=ResearchKnowledge.load(player.getPersistentData());
        ResearchManager.checkResearchCompletion(NoteData.tag(stack),note,"current",knowledge);
        NoteData.write(stack,note);
        ink.setDamageValue(ink.getDamageValue()+1);menu.table.setChanged();menu.broadcastChanges();
        ThaumometerItem.sync(player,note.complete ? "note_complete" : "");
    }
}
