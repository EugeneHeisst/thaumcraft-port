package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class NodeStabilizerEntity extends BlockEntity {
    public int count;
    public int lock(){return getBlockState().is(TC4Modern.NODE_STABILIZER_ADV.get())?2:1;}
    public NodeStabilizerEntity(BlockPos pos,BlockState state){super(TC4Modern.NODE_STABILIZER_ENTITY.get(),pos,state);}
    /** TileNode.handleNodeStability: stabilizers can normalize unstable nodes and rescue fading ones. */
    static boolean stabilize(AuraNodeEntity node,net.minecraft.util.RandomSource random){
        int lock=node.reservoir.lock;if(lock==0)return false;boolean changed=false;
        if(node.nodeType==thaumcraft.api.nodes.NodeType.UNSTABLE&&random.nextBoolean()&&random.nextInt(10000/lock)==42){node.nodeType=thaumcraft.api.nodes.NodeType.NORMAL;changed=true;}
        if(node.reservoir.modifier==thaumcraft.api.nodes.NodeModifier.FADING&&random.nextInt(12500/lock)==69){node.reservoir.modifier=thaumcraft.api.nodes.NodeModifier.PALE;changed=true;}
        // Original setNodeModifier does not reset regeneration here; retain that delayed recalculation.
        return changed;
    }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,NodeStabilizerEntity tile){
        if(level.isClientSide&&pos.getY()<level.getMaxBuildHeight()-1){
            var above=level.getBlockState(pos.above());
            tile.count=NodeDeviceVisuals.stabilizerCount(tile.count,above.is(TC4Modern.AURA_NODE.get())||above.is(TC4Modern.ENERGIZED_NODE.get()),level.hasNeighborSignal(pos));
        }
    }
}
