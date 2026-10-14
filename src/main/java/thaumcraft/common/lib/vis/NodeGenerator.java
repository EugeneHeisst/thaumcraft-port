package thaumcraft.common.lib.vis;
import java.util.Random;
import java.util.ArrayList;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.nodes.NodeModifier;
/** Original random node content generation; biome/environment samples supplied by world adapter. */
public final class NodeGenerator {
    public record Generated(NodeType type,NodeModifier modifier,AspectList aspects){}
    public static Generated generate(Random random,int baura,java.util.function.Supplier<Aspect> biomeTag,int water,int lava,int stone,int foliage,boolean silverwood,boolean eerie,boolean small){
        return generate(random,baura,biomeTag,water,lava,stone,foliage,silverwood,eerie,small,18);
    }
    public static Generated generate(Random random,int baura,java.util.function.Supplier<Aspect> biomeTag,int water,int lava,int stone,int foliage,boolean silverwood,boolean eerie,boolean small,int specialRarity){
        specialRarity=Math.max(3,specialRarity);
        int a;
        var basicAspects=Aspect.getPrimalAspects();var complexAspects=Aspect.getCompoundAspects();
        NodeType type = NodeType.NORMAL;
        if (silverwood) {
            type = NodeType.PURE;
        } else if (eerie) {
            type = NodeType.DARK;
        } else if (random.nextInt(specialRarity) == 0) {
            switch (random.nextInt(10)) {
                case 0: 
                case 1: 
                case 2: {
                    type = NodeType.DARK;
                    break;
                }
                case 3: 
                case 4: 
                case 5: {
                    type = NodeType.UNSTABLE;
                    break;
                }
                case 6: 
                case 7: 
                case 8: {
                    type = NodeType.PURE;
                    break;
                }
                case 9: {
                    type = NodeType.HUNGRY;
                }
            }
        }
        NodeModifier modifier = null;
        if (random.nextInt(specialRarity / 2) == 0) {
            switch (random.nextInt(3)) {
                case 0: {
                    modifier = NodeModifier.BRIGHT;
                    break;
                }
                case 1: {
                    modifier = NodeModifier.PALE;
                    break;
                }
                case 2: {
                    modifier = NodeModifier.FADING;
                }
            }
        }
        if (silverwood || small) {
            baura /= 4;
        }
        int value = random.nextInt(baura / 2) + baura / 2;
        Aspect ra=biomeTag.get();
        AspectList al = new AspectList();
        if (ra != null) {
            al.add(ra, 2);
        } else {
            Aspect aa = complexAspects.get(random.nextInt(complexAspects.size()));
            al.add(aa, 1);
            aa = basicAspects.get(random.nextInt(basicAspects.size()));
            al.add(aa, 1);
        }
        for (int a2 = 0; a2 < 3; ++a2) {
            Aspect aa;
            if (!random.nextBoolean()) continue;
            if (random.nextInt(specialRarity) == 0) {
                aa = complexAspects.get(random.nextInt(complexAspects.size()));
                al.merge(aa, 1);
                continue;
            }
            aa = basicAspects.get(random.nextInt(basicAspects.size()));
            al.merge(aa, 1);
        }
        if (type == NodeType.HUNGRY) {
            al.merge(Aspect.HUNGER, 2);
            if (random.nextBoolean()) {
                al.merge(Aspect.GREED, 1);
            }
        } else if (type == NodeType.PURE) {
            if (random.nextBoolean()) {
                al.merge(Aspect.LIFE, 2);
            } else {
                al.merge(Aspect.ORDER, 2);
            }
        } else if (type == NodeType.DARK) {
            if (random.nextBoolean()) {
                al.merge(Aspect.DEATH, 1);
            }
            if (random.nextBoolean()) {
                al.merge(Aspect.UNDEAD, 1);
            }
            if (random.nextBoolean()) {
                al.merge(Aspect.ENTROPY, 1);
            }
            if (random.nextBoolean()) {
                al.merge(Aspect.DARKNESS, 1);
            }
        }
        if (water > 100) {
            al.merge(Aspect.WATER, 1);
        }
        if (lava > 100) {
            al.merge(Aspect.FIRE, 1);
            al.merge(Aspect.EARTH, 1);
        }
        if (stone > 500) {
            al.merge(Aspect.EARTH, 1);
        }
        if (foliage > 100) {
            al.merge(Aspect.PLANT, 1);
        }
        int[] spread = new int[al.size()];
        float total = 0.0f;
        for (a = 0; a < spread.length; ++a) {
            spread[a] = al.getAmount(al.getAspectsSorted()[a]) == 2 ? 50 + random.nextInt(25) : 25 + random.nextInt(50);
            total += (float)spread[a];
        }
        for (a = 0; a < spread.length; ++a) {
            al.merge(al.getAspectsSorted()[a], (int)((float)spread[a] / total * (float)value));
        }
        return new Generated(type,modifier,al);
    }
}
