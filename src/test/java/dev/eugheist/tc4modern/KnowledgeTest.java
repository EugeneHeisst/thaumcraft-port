package dev.eugheist.tc4modern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;

public final class KnowledgeTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        ResearchClueScanTest.run();
        NotificationQueueTest.run();
        ResearchTableBonusTest.run();
        CompoundTag alice = new CompoundTag();
        require(ResearchKnowledge.discover(alice, "minecraft:stone", "terra"), "First scan must succeed");
        require(!ResearchKnowledge.discover(alice, "minecraft:stone", "terra"), "Duplicate must fail");
        require(ResearchKnowledge.points(alice, "terra") == 1, "Duplicate must not award points");
        require(ResearchKnowledge.discover(alice, "minecraft:dirt", "terra"), "Different block must succeed");
        require(ResearchKnowledge.points(alice, "terra") == 2, "Two distinct blocks award two points");
        require(ResearchKnowledge.discover(new CompoundTag(), "minecraft:stone", "terra"), "Players must be independent");
        var bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(alice, bytes);
        CompoundTag loaded = NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()), NbtAccounter.unlimitedHeap());
        require(ResearchKnowledge.points(loaded, "terra") == 2, "Points must survive NBT serialization");
        require(!ResearchKnowledge.discover(loaded, "minecraft:stone", "terra"), "Scanned IDs must survive serialization");
        CompoundTag clone = new CompoundTag();
        ResearchKnowledge.copy(loaded, clone);
        require(!ResearchKnowledge.discover(clone, "minecraft:stone", "terra"), "Knowledge must survive player replacement");
        ResearchKnowledge.discover(clone, "minecraft:torch", "ignis");
        require(ResearchKnowledge.points(loaded, "ignis") == 0, "Clone must not share mutable NBT");
        CompoundTag compound = new CompoundTag();
        require(ResearchKnowledge.discover(compound, "minecraft:sand", AspectTable.get("minecraft:sand")), "Compound scan must succeed");
        require(ResearchKnowledge.points(compound, "terra") == 1 && ResearchKnowledge.points(compound, "perditio") == 1, "All aspects must be awarded together");
        require(!ResearchKnowledge.discover(compound, "minecraft:sand", AspectTable.get("minecraft:sand")), "Compound duplicates must fail");
        require(ResearchKnowledge.points(compound, "terra") == 1, "Compound duplicate must not increase points");
        require(!ResearchKnowledge.discover(compound, "minecraft:unknown", AspectTable.get("minecraft:unknown")), "Unknown block must not become studied");
        int discovered=ResearchKnowledge.discoverAll(alice);
        require(discovered>0,"Discover-all must unlock missing compound aspects");
        require(ResearchKnowledge.points(alice,"terra")==2,"Discover-all must retain earned points");
        require(ResearchKnowledge.discoverAll(alice)==0,"Discover-all must be idempotent");
        var all=alice.getCompound(ResearchKnowledge.KEY).getList("Aspects",10);
        require(all.size()==thaumcraft.api.aspects.Aspect.aspects.size(),"All aspects must persist including zero-point discoveries");
        require(!ResearchKnowledge.discover(alice,"minecraft:stone","terra"),"Discover-all must retain scanned IDs");
        ResearchKnowledge.grantAll(alice);
        for(var aspect:thaumcraft.api.aspects.Aspect.aspects.values())require(ResearchKnowledge.points(alice,aspect.getTag())==100,"Grant-all must award every aspect pool");
        ResearchKnowledge.grantAll(alice);
        require(ResearchKnowledge.points(alice,"terra")==100,"Grant-all must refill without duplicating points");
        require(!ResearchKnowledge.discover(alice,"minecraft:stone","terra"),"Grant-all must retain scanned IDs");
        System.out.println("Knowledge tests passed: duplicate prevention, independent players, NBT round-trip, player replacement.");
    }
}
