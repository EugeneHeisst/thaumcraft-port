package dev.eugheist.tc4modern;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.lib.world.dim.Cell;
public class OuterGenPortal
extends OuterGenCommon {
    static void generatePortal(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 1; a <= 15; ++a) {
            for (b = 1; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    OuterGenPortal.placeBlock(world, x + a, y + c, z + b, 1, cell);
                }
            }
        }
        for (a = 2; a <= 14; ++a) {
            for (b = 2; b <= 14; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14 || a == 2 && b > 3 && b < 12 && cell.west && c < 10 || a == 14 && b > 3 && b < 12 && cell.east && c < 10 || b == 2 && a > 3 && a < 12 && cell.north && c < 10 || b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    OuterGenPortal.placeBlock(world, x + a, y + c, z + b, 8, cell);
                }
            }
        }
        for (a = 3; a <= 13; ++a) {
            for (b = 3; b <= 13; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 3 && a != 13 && b != 3 && b != 13 || a <= 4 && b <= 4 || a <= 4 && b >= 12 || a >= 12 && b <= 4 || a >= 12 && b >= 12) continue;
                    OuterGenPortal.placeBlock(world, x + a, y + c, z + b, 2, cell);
                }
            }
        }
        for (a = 2; a <= 14; ++a) {
            for (b = 2; b <= 14; ++b) {
                int g;
                int q;
                OuterGenPortal.placeBlock(world, x + a, y - 1, z + b, 1, cell);
                OuterGenPortal.placeBlock(world, x + a, y, z + b, 8, cell);
                OuterGenPortal.placeBlock(world, x + a, y + 1, z + b, 19, cell);
                OuterGenPortal.placeBlock(world, x + a, y + 13, z + b, 1, cell);
                OuterGenPortal.placeBlock(world, x + a, y + 12, z + b, 8, cell);
                OuterGenPortal.placeBlock(world, x + a, y + 11, z + b, 2, cell);
                if (a > 1 && a < 15 && b > 1 && b < 15) {
                    q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                    for (g = 0; g < q - 1; ++g) {
                        OuterGenPortal.placeBlock(world, x + a, y + 1 + g, z + b, 19, cell);
                    }
                }
                if (a <= 3 || a >= 13 || b <= 3 || b >= 13) continue;
                q = Math.min(Math.abs(8 - a), Math.abs(8 - b));
                for (g = 0; g < q; ++g) {
                    OuterGenPortal.placeBlock(world, x + a, y + 11 - g, z + b, 19, cell);
                }
            }
        }
        for (int g = 0; g < 5; ++g) {
            OuterGenPortal.placeBlock(world, x + 6 + g, y + 2, z + 4, 10, Direction.NORTH, cell);
            OuterGenPortal.placeBlock(world, x + 6 + g, y + 2, z + 12, 10, Direction.SOUTH, cell);
            OuterGenPortal.placeBlock(world, x + 12, y + 2, z + 6 + g, 10, Direction.EAST, cell);
            OuterGenPortal.placeBlock(world, x + 4, y + 2, z + 6 + g, 10, Direction.WEST, cell);
        }
        OuterGenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);
        for (a = 3; a <= 13; ++a) {
            for (b = 3; b <= 13; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (!(a <= 4 && b <= 4 || a <= 4 && b >= 12 || a >= 12 && b <= 4) && (a < 12 || b < 12)) continue;
                    OuterGenPortal.placeBlock(world, x + a, y + c, z + b, 9, cell);
                    
                }
            }
        }
        OuterGenPortal.placeBlock(world, x + 5, y + 3, z + 5, 10, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 4, y + 3, z + 5, 10, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 3, z + 4, 10, Direction.WEST, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 8, z + 5, 11, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 4, y + 8, z + 5, 11, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 8, z + 4, 11, Direction.WEST, cell);
        OuterGenPortal.placeBlock(world, x + 12, y + 3, z + 5, 10, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 3, z + 5, 10, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 3, z + 4, 10, Direction.EAST, cell);
        OuterGenPortal.placeBlock(world, x + 12, y + 8, z + 5, 11, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 8, z + 5, 11, Direction.NORTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 8, z + 4, 11, Direction.EAST, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 3, z + 11, 10, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 4, y + 3, z + 11, 10, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 3, z + 12, 10, Direction.WEST, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 8, z + 11, 11, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 4, y + 8, z + 11, 11, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 5, y + 8, z + 12, 11, Direction.WEST, cell);
        OuterGenPortal.placeBlock(world, x + 12, y + 3, z + 11, 10, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 3, z + 11, 10, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 3, z + 12, 10, Direction.EAST, cell);
        OuterGenPortal.placeBlock(world, x + 12, y + 8, z + 11, 11, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 8, z + 11, 11, Direction.SOUTH, cell);
        OuterGenPortal.placeBlock(world, x + 11, y + 8, z + 12, 11, Direction.EAST, cell);
        world.setBlock(new BlockPos(x+8,y+2,z+8),CrimsonContent.CAP.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x+8,y+3,z+8),OuterPortal.BLOCK.get().defaultBlockState(),3);
        OuterGenPortal.genObelisk(world, x + 8, y + 4, z + 8);
    }
}
