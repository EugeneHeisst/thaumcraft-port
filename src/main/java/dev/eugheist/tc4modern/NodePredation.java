package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/** TileNode.handleDischarge and PacketFXBlockZap, including the original asymmetry of stabilizer locks. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class NodePredation {
    private NodePredation() {}
    static boolean discharge(AuraNodeEntity node,RandomSource random) {
        var level=node.getLevel();var source=node.reservoir;
        if(!node.getBlockState().is(TC4Modern.AURA_NODE.get())||source.lock==1||source.modifier==NodeModifier.FADING)return false;
        boolean shiny=node.nodeType==NodeType.HUNGRY||source.modifier==NodeModifier.BRIGHT;
        // Hungry nodes without a modifier still use the original normal-node interval.
        int interval=source.modifier==null?2:shiny?1:source.modifier==NodeModifier.PALE?3:2;
        if(source.count%interval!=0)return false;
        int x=random.nextInt(5)-random.nextInt(5),y=random.nextInt(5)-random.nextInt(5),z=random.nextInt(5)-random.nextInt(5);
        if(source.modifier==NodeModifier.PALE&&random.nextBoolean())return false;
        if(x==0&&y==0&&z==0)return false;
        var pos=node.getBlockPos().offset(x,y,z);
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).is(TC4Modern.AURA_NODE.get())||!(level.getBlockEntity(pos) instanceof AuraNodeEntity victim))return false;
        var target=victim.reservoir;
        if(target.lock>0)return false;
        int targetAverage=(target.aspects.visSize()+target.aspectsBase.visSize())/2;
        int sourceAverage=(source.aspects.visSize()+source.aspectsBase.visSize())/2;
        if(targetAverage>=sourceAverage||target.aspects.size()==0)return false;
        var aspect=target.aspects.getAspects()[random.nextInt(target.aspects.size())];
        if(source.aspects.getAmount(aspect)<source.getNodeVisBase(aspect)&&target.takeFromContainer(aspect,1)) {
            source.addToContainer(aspect,1);
        } else if(target.takeFromContainer(aspect,1)) {
            if(random.nextInt(1+(int)(source.getNodeVisBase(aspect)/(shiny?1.5:1.0)))==0) {
                source.aspectsBase.add(aspect,1);
                if(source.modifier==NodeModifier.PALE&&random.nextInt(100)==0){source.modifier=null;source.regeneration=-1;}
                if(random.nextInt(3)==0)target.setNodeVisBase(aspect,(short)(target.getNodeVisBase(aspect)-1));
            }
        } else return false;
        target.wait=target.regeneration/2;
        victim.changed();
        PacketDistributor.sendToPlayersNear((ServerLevel)level,null,node.getBlockPos().getX(),node.getBlockPos().getY(),node.getBlockPos().getZ(),32,new Zap(pos,node.getBlockPos()));
        return true;
    }
    public record Zap(BlockPos from,BlockPos to) implements CustomPacketPayload {
        static final Type<Zap> TYPE=new Type<>(AlchemyContent.id("node_predation_zap"));
        static final StreamCodec<FriendlyByteBuf,Zap> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Zap::from,BlockPos.STREAM_CODEC,Zap::to,Zap::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @SubscribeEvent static void payloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
        event.registrar("tc4-node-predation-1").playToClient(Zap.TYPE,Zap.CODEC,(p,ctx)->ctx.enqueueWork(()->NodeClientEffects.predation(p.from(),p.to())));
    }
}
