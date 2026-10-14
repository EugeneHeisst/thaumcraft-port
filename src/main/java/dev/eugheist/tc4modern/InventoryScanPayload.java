package dev.eugheist.tc4modern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
@net.neoforged.fml.common.EventBusSubscriber(modid=TC4Modern.MODID)
public record InventoryScanPayload(int menuId,int slot,boolean finish,net.minecraft.world.item.ItemStack creativeStack) implements CustomPacketPayload {
    public InventoryScanPayload(int menuId,int slot,boolean finish){this(menuId,slot,finish,net.minecraft.world.item.ItemStack.EMPTY);}
    public static final Type<InventoryScanPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"inventory_scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf,InventoryScanPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,InventoryScanPayload::menuId,ByteBufCodecs.VAR_INT,InventoryScanPayload::slot,ByteBufCodecs.BOOL,InventoryScanPayload::finish,net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC,InventoryScanPayload::creativeStack,InventoryScanPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(InventoryScanPayload payload,net.neoforged.neoforge.network.handling.IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        var data=player.getPersistentData();
        if(payload.slot<0){clear(player);return;}
        var target=target(player,payload.menuId,payload.slot,payload.creativeStack);
        if(target==null){clear(player);return;}
        if(!payload.finish){
            if(!payload.creativeStack.isEmpty())data.put("tc4_inventory_creative",payload.creativeStack.saveOptional(player.registryAccess()));
            else data.remove("tc4_inventory_creative");
            data.putInt("tc4_inventory_menu",payload.menuId);data.putInt("tc4_inventory_slot",payload.slot);
            data.putString("tc4_inventory_target",target.identity());data.putLong("tc4_inventory_start",player.level().getGameTime());
            data.remove("tc4_inventory_finish");return;
        }
        if(!data.contains("tc4_inventory_start") || data.getInt("tc4_inventory_menu")!=payload.menuId || data.getInt("tc4_inventory_slot")!=payload.slot || !data.getString("tc4_inventory_target").equals(target.identity()))return;
        // Client and server ticks are not synchronized. Queue an early finish instead of silently losing it.
        data.putBoolean("tc4_inventory_finish",true);
        complete(player);
    }
    private static ScanTarget target(ServerPlayer player,int id,int slot,net.minecraft.world.item.ItemStack creativeStack){
        // Creative's item picker and cursor are client-only. Creative players can already
        // create arbitrary stacks; accept the hovered snapshot only in this game mode.
        if(!creativeStack.isEmpty())return player.isCreative()&&id==0&&slot>=0?ScanTarget.item(creativeStack,player.level()):null;
        var menu=player.containerMenu;
        if(menu.containerId!=id || !menu.stillValid(player) || !menu.getCarried().is(TC4Modern.THAUMOMETER.get()) || slot<0 || slot>=menu.slots.size())return null;
        var stack=menu.getSlot(slot).getItem();return stack.isEmpty()?null:ScanTarget.item(stack,player.level());
    }
    static void clear(ServerPlayer player){var data=player.getPersistentData();data.remove("tc4_inventory_start");data.remove("tc4_inventory_finish");data.remove("tc4_inventory_creative");}
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer player)complete(player);
    }
    private static void complete(ServerPlayer player){
        var data=player.getPersistentData();if(!data.getBoolean("tc4_inventory_finish"))return;
        var target=target(player,data.getInt("tc4_inventory_menu"),data.getInt("tc4_inventory_slot"),net.minecraft.world.item.ItemStack.parseOptional(player.registryAccess(),data.getCompound("tc4_inventory_creative")));
        if(target==null || !data.getString("tc4_inventory_target").equals(target.identity())){clear(player);return;}
        if(player.level().getGameTime()-data.getLong("tc4_inventory_start")<ThaumometerItem.SCAN_TICKS)return;
        clear(player);
        ThaumometerItem.sync(player,ResearchKnowledge.scan(data,target.knowledgeKey(),target.aspects(),awarded -> { AspectGain.send(player, awarded); ResearchClues.revealClue(player,null,target.knowledgeKey(),awarded); }));
    }
}
