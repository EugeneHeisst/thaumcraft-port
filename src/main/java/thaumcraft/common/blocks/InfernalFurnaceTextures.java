package thaumcraft.common.blocks;
/** Original BlockArcaneFurnace texture index selection, including adjacency-dependent layers. */
public final class InfernalFurnaceTextures {
    public interface World {
        int metadata(int x,int y,int z);
        boolean furnace(int x,int y,int z);
        boolean outletOnSide(int x,int y,int z,int side);
    }
    public int calculateTexture(World world, int x, int y, int z, int side) {
        int meta = world.metadata(x, y, z);
        int level = this.calculateLevel(world, x, y, z);
        int add = 0;
        if (world.outletOnSide(x,y,z,side)) {
            add = 3;
        }
        switch (side) {
            case 0: 
            case 1: {
                if (side == 1 && level == 18) {
                    switch (meta) {
                        case 2: {
                            return 16;
                        }
                        case 4: {
                            return 17;
                        }
                        case 6: {
                            return 26;
                        }
                        case 8: {
                            return 25;
                        }
                    }
                }
                if (add == 3) break;
                if (meta == 5) {
                    return 10;
                }
                if ((meta - 1) % 3 + (meta - 1) / 3 * 9 < 0) {
                    return -1;
                }
                return (meta - 1) % 3 + (meta - 1) / 3 * 9;
            }
            case 2: {
                switch (meta) {
                    default: {
                        if (level != 9) {
                            return 7;
                        }
                        return 6;
                    }
                    case 1: {
                        return 2 + level + add;
                    }
                    case 2: {
                        return 1 + level + add;
                    }
                    case 3: 
                }
                return 0 + level + add;
            }
            case 3: {
                switch (meta) {
                    default: {
                        if (level != 9) {
                            return 7;
                        }
                        return 6;
                    }
                    case 7: {
                        return 0 + level + add;
                    }
                    case 8: {
                        return 1 + level + add;
                    }
                    case 9: 
                }
                return 2 + level + add;
            }
            case 4: {
                switch (meta) {
                    default: {
                        if (level != 9) {
                            return 7;
                        }
                        return 6;
                    }
                    case 1: {
                        return 0 + level + add;
                    }
                    case 4: {
                        return 1 + level + add;
                    }
                    case 7: 
                }
                return 2 + level + add;
            }
            case 5: {
                switch (meta) {
                    default: {
                        if (level != 9) {
                            return 7;
                        }
                        return 6;
                    }
                    case 3: {
                        return 2 + level + add;
                    }
                    case 6: {
                        return 1 + level + add;
                    }
                    case 9: 
                }
                return 0 + level + add;
            }
        }
        return add == 0 ? 7 : 6;
    }

    public int calculateLevel(World world, int x, int y, int z) {
        int metaB;
        int meta = world.metadata(x, y, z);
        int metaA = world.metadata(x, y + 1, z);
        if (metaA == 10 || metaA == 0) {
            metaA = meta;
        }
        if ((metaB = world.metadata(x, y - 1, z)) == 10 || metaB == 0) {
            metaB = meta;
        }
        boolean blockA = world.furnace(x, y + 1, z);
        boolean blockB = world.furnace(x, y - 1, z);
        if (meta == metaA && meta == metaB && blockA && blockB) {
            return 9;
        }
        if (meta == metaA && blockA && (meta != metaB || !blockB)) {
            return 18;
        }
        return 0;
    }

}
