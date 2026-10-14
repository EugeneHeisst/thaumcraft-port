package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import thaumcraft.api.nodes.NodeType;
/** Original hungry-node block fragments, using the shared FXBoreParticles implementation. */
final class NodeWorldClient {
 static void tick(AuraNodeEntity node){if(node.nodeType!=NodeType.HUNGRY)return;var l=(ClientLevel)node.getLevel();if(l==null)return;var p=node.getBlockPos();var target=NodeWorldEffects.sample(node,15,true);int top=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,target.getX(),target.getZ());if(target.getY()>top)target=target.atY(top);var hit=l.clip(new net.minecraft.world.level.ClipContext(Vec3.atCenterOf(p),Vec3.atCenterOf(target),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));var at=hit.getBlockPos();if(hit.getType()==HitResult.Type.BLOCK&&!at.equals(p)&&at.distSqr(p)<256){var start=new Vec3(at.getX()+l.random.nextFloat(),at.getY()+l.random.nextFloat(),at.getZ()+l.random.nextFloat());Minecraft.getInstance().particleEngine.add(new ArcaneBoreClient.BoreDebris(l,start,Vec3.atCenterOf(p),l.getBlockState(at),false));}}
}
