package dev.eugheist.tc4modern;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisDrainRules;
import thaumcraft.api.visnet.VisNearbyRules;

/** Per-world source registry/cache for original energized-vis endpoint selection and consumption. */
public final class VisNetwork {
    private static final Map<ServerLevel,VisNetwork> WORLDS=new WeakHashMap<>();
    private final Map<BlockPos,WeakReference<VisNearbyRules.Node>> sources=new HashMap<>();
    private final Map<BlockPos,java.util.List<WeakReference<VisNearbyRules.Node>>> nearby=new HashMap<>();
    public interface RelayNode extends thaumcraft.api.visnet.VisParentRules.Node {void disconnected();}
    private final Map<VisNearbyRules.Node,java.util.List<WeakReference<VisNearbyRules.Node>>> links=new WeakHashMap<>();
    public static java.util.List<VisNearbyRules.Node> children(ServerLevel level,VisNearbyRules.Node parent){
        var network=in(level);var references=network.links.get(parent);
        var result=new ArrayList<VisNearbyRules.Node>();if(references==null)return result;
        references.removeIf(ref->ref.get()==null||!ref.get().valid());
        for(var ref:references)result.add(ref.get());return result;
    }
    /** Caller stores the returned weak parent and delegates consumeVis to it, as TileVisNode did. */
    public static WeakReference<thaumcraft.api.visnet.VisParentRules.Node> attach(ServerLevel level,thaumcraft.api.visnet.VisParentRules.Node child){
        detach(level,child);var network=in(level);
        var roots=new ArrayList<thaumcraft.api.visnet.VisParentRules.Node>();
        for(var reference:network.sources.values())if(reference.get() instanceof thaumcraft.api.visnet.VisParentRules.Node root)roots.add(root);
        var parent=thaumcraft.api.visnet.VisParentRules.select(child,roots,(a,b)->visible(level,a,b));
        if(parent==null)return null;
        network.links.computeIfAbsent(parent,key->new ArrayList<>()).add(new WeakReference<>(child));invalidate();
        return new WeakReference<>(parent);
    }
    public static boolean visible(ServerLevel level,VisNearbyRules.Node a,VisNearbyRules.Node b){
            var from=net.minecraft.world.phys.Vec3.atCenterOf(new BlockPos(a.x(),a.y(),a.z()));
            var to=net.minecraft.world.phys.Vec3.atCenterOf(new BlockPos(b.x(),b.y(),b.z()));
            // Start just outside the source collider; accept the parent collider as the endpoint.
            from=outsideSource(from,to);
            var hit=level.clip(new net.minecraft.world.level.ClipContext(from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
            return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS||hit.getBlockPos().equals(new BlockPos(b.x(),b.y(),b.z()));
    }
    public static void detach(ServerLevel level,VisNearbyRules.Node child){
        var network=WORLDS.get(level);if(network==null)return;
        network.detachTree(child,java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
        invalidate();
    }
    private void detachTree(VisNearbyRules.Node node,java.util.Set<VisNearbyRules.Node> visited){
        if(!visited.add(node))return;
        var descendants=links.remove(node);
        if(descendants!=null)for(var ref:descendants){var child=ref.get();if(child!=null)detachTree(child,visited);}
        for(var refs:links.values())refs.removeIf(ref->ref.get()==null||ref.get()==node);
        if(node instanceof RelayNode relay)relay.disconnected();
    }
    static net.minecraft.world.phys.Vec3 outsideSource(net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to){
        var direction=to.subtract(from);
        double greatest=Math.max(Math.abs(direction.x),Math.max(Math.abs(direction.y),Math.abs(direction.z)));
        if(greatest==0)return from;
        // Leave the entire source cell, including diagonal rays, without skipping an adjacent collider.
        return from.add(direction.scale((.5+1e-7)/greatest));
    }
    private VisNetwork(){}
    private static VisNetwork in(ServerLevel level){return WORLDS.computeIfAbsent(level,key->new VisNetwork());}
    public static void invalidate(){for(var network:WORLDS.values())network.nearby.clear();}
    public static void register(ServerLevel level,VisNearbyRules.Node source){
        in(level).sources.put(new BlockPos(source.x(),source.y(),source.z()),new WeakReference<>(source));
        invalidate();
    }
    public static void unregister(ServerLevel level,VisNearbyRules.Node source){
        var network=WORLDS.get(level);if(network==null)return;
        var position=new BlockPos(source.x(),source.y(),source.z());
        var previous=network.sources.get(position);
        if(previous!=null&&previous.get()==source)network.sources.remove(position);
        detach(level,source);
        invalidate();
    }
    public static void unload(ServerLevel level){WORLDS.remove(level);invalidate();}
    public static int drain(ServerLevel level,BlockPos position,Aspect aspect,int amount,VisDrainRules.Effect effect){
        var network=in(level);
        var cached=network.nearby.get(position);
        if(cached==null){
            var roots=new ArrayList<VisNearbyRules.Node>();
            for(var reference:network.sources.values()){var root=reference.get();if(root!=null&&root.valid())roots.add(root);}
            cached=new ArrayList<>();
            for(var endpoint:VisNearbyRules.select(roots,position.getX(),position.getY(),position.getZ()))cached.add(new WeakReference<>(endpoint));
            network.nearby.put(position.immutable(),cached);
        }
        var endpoints=new ArrayList<VisNearbyRules.Node>();
        for(var reference:cached)endpoints.add(reference.get());
        return VisDrainRules.drain(endpoints,aspect,amount,effect);
    }
}
