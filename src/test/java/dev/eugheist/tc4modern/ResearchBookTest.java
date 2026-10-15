package dev.eugheist.tc4modern;

import java.util.*;
import thaumcraft.api.research.*;
import thaumcraft.common.config.ConfigResearchData;
import thaumcraft.client.lib.ResearchBookLayout;

public final class ResearchBookTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        ConfigResearchData.init();
        String[] keys={"ASPECTS","PECH","NODES","WARP","RESEARCH","KNOWFRAG","THAUMONOMICON","ORE","PLANTS","ENCHANT","NODETAPPER1","NODEPRESERVE","NODEJAR","NODETAPPER2","RESEARCHER1","DECONSTRUCTOR","RESEARCHER2","RESEARCHDUPE","CRIMSON"};
        int[] lengths={3,2,3,3,16,2,2,5,8,2,1,1,3,1,1,3,1,1,1};
        int[][] positions={{0,0},{-4,-4},{-2,0},{0,2},{2,0},{3,-2},{1,-2},{-2,-2},{-2,-4},{-4,-2},{-4,1},{-6,2},{-7,4},{-3,3},{4,1},{6,2},{3,3},{4,5},{0,4}};
        var category=ResearchCategories.getResearchList("BASICS");var originals=new ArrayList<>(category.research.keySet());require(category.research.size()==19,"All original BASICS entries");require(originals.equals(List.of(keys)),"Original registration order");
        Set<String> recipes=new HashSet<>();
        for(int i=0;i<keys.length;i++){var item=category.research.get(keys[i]);require(item.displayColumn==positions[i][0]&&item.displayRow==positions[i][1],"Original map position: "+item.key);require(item.getPages().length==lengths[i],"Original page boundaries: "+item.key);for(var page:item.getPages())if(page.recipeKey!=null){require(BasicsBookRecipes.KEYS.contains(page.recipeKey),"Complete BASICS recipe display: "+page.recipeKey);recipes.add(page.recipeKey);}else require(page.text!=null&&!page.text.isBlank(),"Text key present: "+item.key);}
        require(recipes.equals(BasicsBookRecipes.KEYS),"Every BASICS recipe page is reachable");
        require(ResearchCategories.getResearch("NODEJAR").getPages()[1].type==ResearchPage.PageType.COMPOUND_CRAFTING,"Jar is a construction, not a crafting recipe");
        require(ResearchCategories.getResearch("DECONSTRUCTOR").getPages()[1].type==ResearchPage.PageType.ARCANE_CRAFTING,"Deconstructor arcane recipe");
        byte[] metrics=new byte[65536];Arrays.fill(metrics,(byte)7);metrics['\n']=0;var layout=new ResearchBookLayout(metrics);
        var lines=layout.layout("alpha beta gamma",26);require(lines.size()==3,"Original wrapping at spaces");require(lines.get(1).value().equals("beta"),"Words are preserved");
        var parts=layout.layout("§lbold<BR>plain<LINE><IMG>thaumcraft:textures/gui/research4.png:0:0:128:128:0.6</IMG>end",139);
        require(parts.stream().filter(p->p.kind()==ResearchBookLayout.Kind.IMAGE).count()==1,"Image markup retained");require(parts.stream().filter(p->p.kind()==ResearchBookLayout.Kind.LINE).count()==1,"Rule markup retained");
        var image=parts.stream().filter(p->p.kind()==ResearchBookLayout.Kind.IMAGE).findFirst().orElseThrow();var last=parts.getLast();require(last.y()>=image.y()+76,"Image height advances original baseline");require(last.value().endsWith("end"),"Text after illustration preserved");
        require(layout.width("§lAB§rC")==17,"Formatting affects original glyph width");
        for(float tick:new float[]{0,7,100,10000})for(float[] edge:new float[][]{{0,0,120,48},{80,10,-20,10},{1,2,1,90},{0,0,0,0}}){
            var start=ResearchBrowserLines.point(edge[0],edge[1],edge[2],edge[3],0,true,tick);
            var end=ResearchBrowserLines.point(edge[0],edge[1],edge[2],edge[3],1,true,tick);
            require(Math.abs(start.x()-edge[0])<.001f&&Math.abs(start.y()-edge[1])<.001f,"Animated connection start is anchored");
            require(Math.abs(end.x()-edge[2])<.001f&&Math.abs(end.y()-edge[3])<.001f,"Animated connection reaches its parent");
            for(int i=0;i<=100;i++){var point=ResearchBrowserLines.point(edge[0],edge[1],edge[2],edge[3],i/100f,true,tick);require(Float.isFinite(point.x())&&Float.isFinite(point.y()),"Finite curve geometry");}
        }
        var note=new thaumcraft.common.lib.research.ResearchNoteData();
        var anchor=new thaumcraft.common.lib.utils.HexUtils.Hex(0,0);var neighbour=anchor.getNeighbour(0);var remote=new thaumcraft.common.lib.utils.HexUtils.Hex(4,4);
        for(var hex:List.of(anchor,neighbour,remote))note.hexes.put(hex.toString(),hex);
        note.hexEntries.put(anchor.toString(),new thaumcraft.common.lib.research.ResearchManager.HexEntry(thaumcraft.api.aspects.Aspect.AIR,1));
        note.hexEntries.put(neighbour.toString(),new thaumcraft.common.lib.research.ResearchManager.HexEntry(thaumcraft.api.aspects.Aspect.LIGHT,2));
        note.hexEntries.put(remote.toString(),new thaumcraft.common.lib.research.ResearchManager.HexEntry(thaumcraft.api.aspects.Aspect.LIGHT,2));
        var known=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,1).add(thaumcraft.api.aspects.Aspect.LIGHT,1);
        var graph=ResearchTableGraph.connected(note,known);require(graph.edges().size()==1&&graph.highlight().contains(neighbour.toString())&&!graph.highlight().contains(remote.toString()),"Only anchor-connected aspects highlighted");
        known.remove(thaumcraft.api.aspects.Aspect.LIGHT);require(ResearchTableGraph.connected(note,known).edges().isEmpty(),"Unknown aspects cannot expose connections");
        var copyData=new net.minecraft.nbt.CompoundTag();ResearchKnowledge.grantAll(copyData);
        var cost=new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.AIR,3).add(thaumcraft.api.aspects.Aspect.FIRE,3);
        require(ResearchKnowledge.payCopy(copyData,cost,2)&&ResearchKnowledge.points(copyData,"aer")==95&&ResearchKnowledge.points(copyData,"ignis")==95,"Copy cost adds accumulated copy count");
        require(!ResearchKnowledge.payCopy(copyData,cost,100)&&ResearchKnowledge.points(copyData,"aer")==95,"Failed copy spends no points");
        System.out.println("PASS: 19 original BASICS entries, 59 source pages, all 11 recipe keys, markup and legacy wrapping");
    }
}
