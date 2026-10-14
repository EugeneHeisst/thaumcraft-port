package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.ResearchTableBonus;
import thaumcraft.common.tiles.ResearchTableBonus.*;

public final class ResearchTableBonusTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final class Environment implements ResearchTableBonus.Environment {
        Sample first = new Sample(Block.OTHER, 0, Material.GROUND);
        int samples, randomCalls;
        public boolean daytime() { return true; }
        public int lightAbove() { return 15; }
        public boolean skyAbove() { return true; }
        public int y() { return 64; }
        public int minY() { return 0; }
        public int maxY() { return 256; }
        public int nextInt(int bound) { randomCalls++; return 0; }
        public Sample sample(int x, int y, int z) { samples++; return first; }
    }
    public static void run() {
        var table = new ResearchTableBonus(); var env = new Environment();
        for (int i=0; i<601; i++) require(!table.tick(env), "Original postincrement waits 601 ticks");
        require(table.tick(env) && env.samples==1 && table.bonusAspects.getAmount(Aspect.EARTH)==1, "602nd tick grants first ground bonus and returns");
        table.recalculateBonus(env);
        require(table.bonusAspects.getAmount(Aspect.EARTH)==1, "Bonuses cap at one rather than accumulating");
        var saved = new CompoundTag(); table.save(saved); var restored = new ResearchTableBonus(); restored.load(saved);
        require(restored.bonusAspects.getAmount(Aspect.EARTH)==1, "Table bonuses survive persistence");
        for (var material : new Material[]{Material.FIRE, Material.LAVA, Material.WATER, Material.CIRCUITS, Material.PISTON}) {
            var expected = material==Material.WATER ? Aspect.WATER : material==Material.FIRE || material==Material.LAVA ? Aspect.FIRE : Aspect.ORDER;
            var other = new ResearchTableBonus(); env.first = new Sample(Block.OTHER,0,material); other.recalculateBonus(env);
            require(other.bonusAspects.getAmount(expected)==1, "Original material bonus: "+material);
        }
        var data = new CompoundTag(); var bonus = new AspectList().add(Aspect.AIR,1);
        require(ResearchKnowledge.combine(data,"aer","aer",bonus).equals("insufficient") && bonus.getAmount(Aspect.AIR)==1, "One table bonus cannot pay twice; failed action is atomic");
        ResearchKnowledge.discover(data,"fixture","aer");
        require(ResearchKnowledge.combine(data,"aer","aer",bonus).equals("no_combination"), "Personal point plus bonus pays same-aspect experiment");
        require(bonus.getAmount(Aspect.AIR)==0 && ResearchKnowledge.points(data,"aer")==0, "Both sources consumed once");
        bonus.add(Aspect.AIR,1).add(Aspect.ORDER,1);
        require(ResearchKnowledge.combine(data,"aer","ordo",bonus).equals("combined:motus"), "Experiments work entirely from table reserves");
        require(bonus.size()==0, "Spent reserves are removed");
        var note=new thaumcraft.common.lib.research.ResearchNoteData();
        note.hexEntries.put("0:0",new thaumcraft.common.lib.research.ResearchManager.HexEntry(null,0));
        bonus.add(Aspect.EARTH,1);
        require(ResearchKnowledge.place(data,note,0,0,Aspect.EARTH,1f,bonus).equals("placed"), "A note can use a reserve with an empty personal pool");
        require(ResearchKnowledge.points(data,"terra")==0 && bonus.getAmount(Aspect.EARTH)==0, "Reserve placement never makes personal points negative");
        bonus.add(Aspect.EARTH,1);
        require(ResearchKnowledge.place(data,note,0,0,Aspect.EARTH,1f,bonus).equals("unknown") && bonus.getAmount(Aspect.EARTH)==1, "Occupied-cell rejection preserves reserves");
        require(ResearchKnowledge.place(data,note,0,0,null,1f,bonus).equals("placed") && bonus.getAmount(Aspect.EARTH)==1, "Ordinary erasure does not restore a spent reserve");
    }
}
