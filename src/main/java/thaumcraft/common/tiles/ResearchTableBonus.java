package thaumcraft.common.tiles;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Adapted TileResearchTable.recalculateBonus; platform block/material lookups supplied by the bridge. */
public final class ResearchTableBonus {
    public enum Block { OTHER, ORE, CRYSTAL, BOOKSHELF, BRAIN_JAR }
    public enum Material { OTHER, FIRE, LAVA, GROUND, WATER, CIRCUITS, PISTON }
    public record Sample(Block block, int metadata, Material material) {}
    public interface Environment {
        boolean daytime(); int lightAbove(); boolean skyAbove();
        int y(); int minY(); int maxY(); int nextInt(int bound);
        Sample sample(int x, int y, int z);
    }
    public final AspectList bonusAspects = new AspectList();
    private int nextRecalc;
    public boolean tick(Environment env) {
        if (nextRecalc++ > 600) { nextRecalc = 0; recalculateBonus(env); return true; }
        return false;
    }
    public void save(CompoundTag tag) {
        tag.putInt("nextRecalc", nextRecalc);
        ListTag list = new ListTag();
        for (Aspect aspect : bonusAspects.getAspects()) {
            if (aspect == null || bonusAspects.getAmount(aspect) <= 0) continue;
            CompoundTag entry = new CompoundTag(); entry.putString("tag", aspect.getTag()); list.add(entry);
        }
        tag.put("bonusAspects", list);
    }
    public void load(CompoundTag tag) {
        nextRecalc = tag.getInt("nextRecalc"); bonusAspects.aspects.clear();
        ListTag list = tag.getList("bonusAspects", 10);
        for (int i=0; i<list.size(); i++) {
            Aspect aspect = Aspect.getAspect(list.getCompound(i).getString("tag"));
            if (aspect != null) bonusAspects.merge(aspect, 1);
        }
    }
    public void recalculateBonus(Environment env) {
        if (!env.daytime() && env.lightAbove() < 4 && !env.skyAbove() && env.nextInt(20) == 0) {
            this.bonusAspects.merge(Aspect.ENTROPY, 1);
        }
        if ((float)(env.y() - env.minY()) > (float)(env.maxY() - env.minY()) * 0.5f && env.nextInt(20) == 0) {
            this.bonusAspects.merge(Aspect.AIR, 1);
        }
        if ((float)(env.y() - env.minY()) > (float)(env.maxY() - env.minY()) * 0.66f && env.nextInt(20) == 0) {
            this.bonusAspects.merge(Aspect.AIR, 1);
        }
        if ((float)(env.y() - env.minY()) > (float)(env.maxY() - env.minY()) * 0.75f && env.nextInt(20) == 0) {
            this.bonusAspects.merge(Aspect.AIR, 1);
        }
        for (int x = -8; x <= 8; ++x) {
            for (int z = -8; z <= 8; ++z) {
                for (int y = -8; y <= 8; ++y) {
                    if (y + env.y() <= env.minY() || y + env.y() >= env.maxY()) continue;
                    Sample sample = env.sample(x, y, z);
                    Block bi = sample.block();
                    int md = sample.metadata();
                    Material bm = sample.material();
                    if (bi == Block.ORE && md == 1) {
                        if (this.bonusAspects.getAmount(Aspect.AIR) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.AIR, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 0) {
                        if (this.bonusAspects.getAmount(Aspect.AIR) < 1 && env.nextInt(10) == 0) {
                            this.bonusAspects.merge(Aspect.AIR, 1);
                            return;
                        }
                    } else if (bm == Material.FIRE || bm == Material.LAVA || bi == Block.ORE && md == 2) {
                        if (this.bonusAspects.getAmount(Aspect.FIRE) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.FIRE, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 1) {
                        if (this.bonusAspects.getAmount(Aspect.FIRE) < 1 && env.nextInt(10) == 0) {
                            this.bonusAspects.merge(Aspect.FIRE, 1);
                            return;
                        }
                    } else if (bm == Material.GROUND) {
                        if (this.bonusAspects.getAmount(Aspect.EARTH) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.EARTH, 1);
                            return;
                        }
                    } else if (bi == Block.ORE && md == 4) {
                        if (this.bonusAspects.getAmount(Aspect.EARTH) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.EARTH, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 3) {
                        if (this.bonusAspects.getAmount(Aspect.EARTH) < 1 && env.nextInt(10) == 0) {
                            this.bonusAspects.merge(Aspect.EARTH, 1);
                            return;
                        }
                    } else if (bm == Material.WATER) {
                        if (this.bonusAspects.getAmount(Aspect.WATER) < 1 && env.nextInt(15) == 0) {
                            this.bonusAspects.merge(Aspect.WATER, 1);
                            return;
                        }
                    } else if (bi == Block.ORE && md == 3) {
                        if (this.bonusAspects.getAmount(Aspect.WATER) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.WATER, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 2) {
                        if (this.bonusAspects.getAmount(Aspect.WATER) < 1 && env.nextInt(10) == 0) {
                            this.bonusAspects.merge(Aspect.WATER, 1);
                            return;
                        }
                    } else if (bm == Material.CIRCUITS || bm == Material.PISTON) {
                        if (this.bonusAspects.getAmount(Aspect.ORDER) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.ORDER, 1);
                            return;
                        }
                    } else if (bi == Block.ORE && md == 5) {
                        if (this.bonusAspects.getAmount(Aspect.ORDER) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.ORDER, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 4) {
                        if (this.bonusAspects.getAmount(Aspect.ORDER) < 1 && env.nextInt(10) == 0) {
                            this.bonusAspects.merge(Aspect.ORDER, 1);
                            return;
                        }
                    } else if (bi == Block.ORE && md == 6) {
                        if (this.bonusAspects.getAmount(Aspect.ENTROPY) < 1 && env.nextInt(20) == 0) {
                            this.bonusAspects.merge(Aspect.ENTROPY, 1);
                            return;
                        }
                    } else if (bi == Block.CRYSTAL && md == 5 && this.bonusAspects.getAmount(Aspect.ENTROPY) < 1 && env.nextInt(10) == 0) {
                        this.bonusAspects.merge(Aspect.ENTROPY, 1);
                        return;
                    }
                    if ((bi != Block.BOOKSHELF || env.nextInt(300) != 0) && (bi != Block.BRAIN_JAR || md != 1 || env.nextInt(200) != 0)) continue;
                    Aspect[] aspects = new Aspect[]{};
                    aspects = Aspect.aspects.values().toArray(aspects);
                    this.bonusAspects.merge(aspects[env.nextInt(aspects.length)], 1);
                    return;
                }
            }
        }
    }

}
