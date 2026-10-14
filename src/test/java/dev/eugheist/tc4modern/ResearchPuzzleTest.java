package dev.eugheist.tc4modern;

import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ConfigResearchData;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.utils.HexUtils;

public final class ResearchPuzzleTest {
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static void cell(ResearchNoteData note,int q,int r,Aspect aspect,int type) {
        var hex=new HexUtils.Hex(q,r);note.hexes.put(hex.toString(),hex);note.hexEntries.put(hex.toString(),new ResearchManager.HexEntry(aspect,type));
    }
    public static void main(String[] args) {
        ConfigResearchData.init();
        require(ResearchCategories.researchCategories.size()==6,"Six original categories");
        var bellows=ResearchCategories.getResearch("BELLOWS");
        require(bellows.isSecondary()&&bellows.isConcealed(),"Original bellows is concealed secondary study, not a scroll");
        require(!ResearchProgress.requisites(java.util.Set.of("NITOR","ALUMENTUM"),bellows),"Alchemy alone does not unlock bellows");
        require(ResearchProgress.requisites(java.util.Set.of("INFERNALFURNACE"),bellows),"Infernal furnace research unlocks bellows");
        CompoundTag bellowsData=new CompoundTag();var bellowsKnowledge=ResearchKnowledge.load(bellowsData);
        for(var aspect:new Aspect[]{Aspect.AIR,Aspect.MECHANISM,Aspect.MOTION}){bellowsKnowledge.addDiscoveredAspect("current",aspect);bellowsKnowledge.setAspectPool("current",aspect,(short)10);}
        bellowsKnowledge.setAspectPool("current",Aspect.MOTION,(short)2);ResearchKnowledge.save(bellowsData,bellowsKnowledge);
        require(!ResearchKnowledge.purchase(bellowsData,bellows),"Insufficient motion blocks bellows study");
        require(ResearchKnowledge.points(bellowsData,"aer")==10&&ResearchKnowledge.points(bellowsData,"machina")==10,"Failed bellows purchase preserves other pools");
        bellowsKnowledge=ResearchKnowledge.load(bellowsData);bellowsKnowledge.setAspectPool("current",Aspect.MOTION,(short)10);ResearchKnowledge.save(bellowsData,bellowsKnowledge);
        require(ResearchKnowledge.purchase(bellowsData,bellows),"Discovered and funded bellows study succeeds");
        require(ResearchKnowledge.points(bellowsData,"aer")==4&&ResearchKnowledge.points(bellowsData,"machina")==7&&ResearchKnowledge.points(bellowsData,"motus")==7,"Bellows study spends original six air, three mechanism and three motion");
        var basic=ResearchCategories.getResearch("NODETAPPER1");
        require(basic!=null && basic.getComplexity()==2 && basic.tags.getAmount(Aspect.AURA)==3,"Original research cost and complexity");
        CompoundTag encoded=ResearchManager.createNote(new CompoundTag(),"NODETAPPER1",new Random(123));
        var generated=ResearchManager.getData(encoded);
        require(generated!=null && generated.key.equals("NODETAPPER1") && !generated.complete,"Original note creation and decoding");
        require(generated.hexes.size()==33,"Radius three 37 cells minus four holes");
        require(generated.hexEntries.values().stream().filter(e -> e.type==1).count()==4,"Four original fixed aspect anchors");
        CompoundTag roundTrip=new CompoundTag();ResearchManager.updateData(roundTrip,generated);
        require(ResearchManager.getData(roundTrip).hexEntries.size()==generated.hexEntries.size(),"Original grid NBT round-trip");
        for(int q=-4;q<=4;q++) for(int r=-4;r<=4;r++) {
            var hex=new HexUtils.Hex(q,r);require(hex.toPixel(9).toHex(9).equals(hex),"Original hex coordinate inverse");
        }
        CompoundTag data=new CompoundTag();
        var knowledge=ResearchKnowledge.load(data);knowledge.setAspectPool("current",Aspect.LIFE,(short)5);ResearchKnowledge.save(data,knowledge);
        var note=new ResearchNoteData();note.key="NODETAPPER1";note.color=Aspect.AURA.getColor();
        cell(note,-1,0,Aspect.EARTH,1);cell(note,0,0,null,0);cell(note,1,0,Aspect.WATER,1);cell(note,0,1,null,0);
        require(!ResearchManager.checkResearchCompletion(new CompoundTag(),note,"current",ResearchKnowledge.load(data)),"Disconnected fixed anchors are incomplete");
        int before=ResearchKnowledge.points(data,"victus");
        require(ResearchKnowledge.place(data,note,-1,0,Aspect.LIFE,1).equals("unknown"),"Anchor edit rejected");
        require(ResearchKnowledge.place(data,note,10,10,Aspect.LIFE,1).equals("unknown"),"Cells outside note rejected");
        require(ResearchKnowledge.points(data,"victus")==before,"Rejected actions do not spend points");
        require(ResearchKnowledge.place(data,note,0,0,Aspect.LIFE,1).equals("placed"),"Known aspect placement");
        require(ResearchKnowledge.points(data,"victus")==before-1,"One point spent on placement");
        require(ResearchManager.checkResearchCompletion(new CompoundTag(),note,"current",ResearchKnowledge.load(data)),"Terra -> Victus -> Aqua completes original puzzle");
        require(note.complete && !note.hexes.containsKey("0:1"),"Completion removes irrelevant cells");
        require(ResearchKnowledge.place(data,note,0,0,null,1).equals("unknown"),"Completed notes cannot be edited");
        require(!ResearchProgress.complete(data,"NODEPRESERVE"),"Research prerequisite checked");
        require(ResearchProgress.complete(data,"NODETAPPER1"),"Auto-unlocked NODES permits node tapping research");
        require(ResearchProgress.complete(data,"NODEPRESERVE"),"Completed parent permits next research");
        require(!ResearchProgress.complete(data,"NODEPRESERVE"),"Duplicate research learning rejected");
        var learned=data.copy();require(ResearchProgress.completed(learned).contains("NODEPRESERVE"),"Learned research persists in player NBT");
        System.out.println("Research puzzle tests passed: original metadata, generated grid, NBT, hex coordinates, connection rules, point spending, edit validation and prerequisites.");
    }
}
