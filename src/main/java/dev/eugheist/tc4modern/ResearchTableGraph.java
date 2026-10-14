package dev.eugheist.tc4modern;
import java.util.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.utils.HexUtils;
/** Original graph highlighting starts at discovered anchors, not at disconnected placed aspects. */
final class ResearchTableGraph {
    record Edge(HexUtils.Hex first,HexUtils.Hex second) {}
    record Graph(List<Edge> edges,Set<String> highlight) {}
    static Graph connected(ResearchNoteData note,AspectList known){
        var edges=new ArrayList<Edge>();var visited=new HashSet<String>();var highlight=new HashSet<String>();
        for(var hex:note.hexes.values()){var entry=note.hexEntries.get(hex.toString());if(entry!=null&&entry.type==1&&known.aspects.containsKey(entry.aspect))walk(note,known,hex,visited,highlight,edges);}
        return new Graph(List.copyOf(edges),Set.copyOf(highlight));
    }
    private static void walk(ResearchNoteData note,AspectList known,HexUtils.Hex hex,Set<String> visited,Set<String> highlight,List<Edge> edges){
        visited.add(hex.toString());var first=note.hexEntries.get(hex.toString());
        for(int i=0;i<6;i++){var neighbour=hex.getNeighbour(i);var second=note.hexEntries.get(neighbour.toString());if(visited.contains(neighbour.toString())||second==null||second.type<1||!known.aspects.containsKey(second.aspect)||!linked(first.aspect,second.aspect))continue;
            edges.add(new Edge(hex,neighbour));highlight.add(neighbour.toString());walk(note,known,neighbour,visited,highlight,edges);
        }
    }
    static boolean linked(Aspect a,Aspect b){if(a==null||b==null)return false;return !a.isPrimal()&&(a.getComponents()[0]==b||a.getComponents()[1]==b)||!b.isPrimal()&&(b.getComponents()[0]==a||b.getComponents()[1]==a);}
}
