
package dev.eugheist.tc4modern;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.lib.world.dim.Cell;


public class OuterGen2x2
extends OuterGenCommon {
    static void generateUpperLeft(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int g;
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 1; a <= 15; ++a) {
            for (b = 1; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 1 && b != 1) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)1, (Cell)cell);
                }
            }
        }
        for (a = 2; a <= 15; ++a) {
            for (b = 2; b <= 15; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 2 && b != 2 || a == 2 && b > 4 && b < 12 && cell.west && c < 10 || b == 2 && a > 4 && a < 12 && cell.north && c < 10) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)8, (Cell)cell);
                }
            }
        }
        for (a = 3; a <= 15; ++a) {
            for (b = 3; b <= 15; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 3 && b != 3) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)18, (Cell)cell);
                }
            }
        }
        for (a = 2; a <= 15; ++a) {
            for (b = 2; b <= 15; ++b) {
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y - 1), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)y, (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 1), (int)(z + b), (int)19, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 13), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 12), (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 11), (int)(z + b), (int)2, (Cell)cell);
            }
        }
        for (g = 4; g <= 15; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 2), (int)(z + 4), (int)10, (Direction)Direction.NORTH, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 10), (int)(z + 4), (int)11, (Direction)Direction.NORTH, (Cell)cell);
        }
        for (g = 4; g <= 15; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 4), (int)(y + 2), (int)(z + g), (int)10, (Direction)Direction.WEST, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 4), (int)(y + 10), (int)(z + g), (int)11, (Direction)Direction.WEST, (Cell)cell);
        }
        OuterGenCommon.generateConnections((WorldGenLevel)world, (Random)random, (int)cx, (int)cz, (int)y, (Cell)cell, (int)3, (boolean)true);
    }

    static void generateUpperRight(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int g;
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 0; a <= 15; ++a) {
            for (b = 1; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 15 && b != 1) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)1, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 14; ++a) {
            for (b = 2; b <= 15; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 14 && b != 2 || a == 14 && b > 4 && b < 12 && cell.east && c < 10 || b == 2 && a > 4 && a < 12 && cell.north && c < 10) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)8, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 13; ++a) {
            for (b = 3; b <= 15; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 13 && b != 3) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)18, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 14; ++a) {
            for (b = 2; b <= 15; ++b) {
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y - 1), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)y, (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 1), (int)(z + b), (int)19, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 13), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 12), (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 11), (int)(z + b), (int)2, (Cell)cell);
            }
        }
        for (g = 0; g <= 11; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 2), (int)(z + 4), (int)10, (Direction)Direction.NORTH, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 10), (int)(z + 4), (int)11, (Direction)Direction.NORTH, (Cell)cell);
        }
        for (g = 4; g <= 15; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 12), (int)(y + 2), (int)(z + g), (int)10, (Direction)Direction.EAST, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 12), (int)(y + 10), (int)(z + g), (int)11, (Direction)Direction.EAST, (Cell)cell);
        }
        OuterGenCommon.generateConnections((WorldGenLevel)world, (Random)random, (int)cx, (int)cz, (int)y, (Cell)cell, (int)3, (boolean)true);
    }

    static void generateLowerLeft(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int g;
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 1; a <= 15; ++a) {
            for (b = 0; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 1 && b != 15) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)1, (Cell)cell);
                }
            }
        }
        for (a = 2; a <= 15; ++a) {
            for (b = 0; b <= 14; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 2 && b != 14 || a == 2 && b > 4 && b < 12 && cell.west && c < 10 || b == 14 && a > 4 && a < 12 && cell.south && c < 10) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)8, (Cell)cell);
                }
            }
        }
        for (a = 3; a <= 15; ++a) {
            for (b = 0; b <= 13; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 3 && b != 13) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)18, (Cell)cell);
                }
            }
        }
        for (a = 2; a <= 15; ++a) {
            for (b = 0; b <= 14; ++b) {
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y - 1), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)y, (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 1), (int)(z + b), (int)19, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 13), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 12), (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 11), (int)(z + b), (int)2, (Cell)cell);
            }
        }
        for (g = 4; g <= 15; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 2), (int)(z + 12), (int)10, (Direction)Direction.SOUTH, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 10), (int)(z + 12), (int)11, (Direction)Direction.SOUTH, (Cell)cell);
        }
        for (g = 0; g <= 11; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 4), (int)(y + 2), (int)(z + g), (int)10, (Direction)Direction.WEST, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 4), (int)(y + 10), (int)(z + g), (int)11, (Direction)Direction.WEST, (Cell)cell);
        }
        OuterGenCommon.generateConnections((WorldGenLevel)world, (Random)random, (int)cx, (int)cz, (int)y, (Cell)cell, (int)3, (boolean)true);
    }

    static void generateLowerRight(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int g;
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 0; a <= 15; ++a) {
            for (b = 0; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 15 && b != 15) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)1, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 14; ++a) {
            for (b = 0; b <= 14; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 14 && b != 14 || a == 14 && b > 4 && b < 12 && cell.east && c < 10 || b == 14 && a > 4 && a < 12 && cell.south && c < 10) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)8, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 13; ++a) {
            for (b = 0; b <= 13; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 13 && b != 13) continue;
                    OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + c), (int)(z + b), (int)18, (Cell)cell);
                }
            }
        }
        for (a = 0; a <= 14; ++a) {
            for (b = 0; b <= 14; ++b) {
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y - 1), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)y, (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 1), (int)(z + b), (int)19, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 13), (int)(z + b), (int)1, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 12), (int)(z + b), (int)8, (Cell)cell);
                OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + a), (int)(y + 11), (int)(z + b), (int)2, (Cell)cell);
            }
        }
        for (g = 0; g <= 11; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 2), (int)(z + 12), (int)10, (Direction)Direction.SOUTH, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + g), (int)(y + 10), (int)(z + 12), (int)11, (Direction)Direction.SOUTH, (Cell)cell);
        }
        for (g = 0; g <= 12; ++g) {
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 12), (int)(y + 2), (int)(z + g), (int)10, (Direction)Direction.EAST, (Cell)cell);
            OuterGen2x2.placeBlock((WorldGenLevel)world, (int)(x + 12), (int)(y + 10), (int)(z + g), (int)11, (Direction)Direction.EAST, (Cell)cell);
        }
        OuterGenCommon.generateConnections((WorldGenLevel)world, (Random)random, (int)cx, (int)cz, (int)y, (Cell)cell, (int)3, (boolean)true);
    }
}
