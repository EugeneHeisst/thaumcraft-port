package dev.eugheist.tc4modern;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.common.lib.world.dim.Cell;
public class OuterGenBossRoom
extends OuterGenCommon {
    static final int[][] PAT_DOORWAY = new int[][]{{0, 2, 2, 2, 2, 2, 0}, {2, 2, 9, 9, 9, 2, 2}, {2, 9, 9, 9, 9, 9, 2}, {2, 9, 9, 1, 9, 9, 2}, {2, 9, 9, 9, 9, 9, 2}, {2, 2, 9, 9, 9, 2, 2}, {0, 2, 2, 2, 2, 2, 0}};

    static void generateRoom(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int x = cx * 16;
        int z = cz * 16;
        switch (cell.feature) {
            case 2: {
                OuterGen2x2.generateUpperLeft(world, random, cx, cz, 50, cell);
                break;
            }
            case 3: {
                OuterGen2x2.generateUpperRight(world, random, cx, cz, 50, cell);
                break;
            }
            case 4: {
                OuterGen2x2.generateLowerLeft(world, random, cx, cz, 50, cell);
                break;
            }
            case 5: {
                OuterGen2x2.generateLowerRight(world, random, cx, cz, 50, cell);
            }
        }
        // Only the quadrant with an external corridor has a doorway.
        // Legacy UNKNOWN otherwise wrote the doorway at world origin.
        if(!cell.north&&!cell.south&&!cell.east&&!cell.west)return;
        for (int a = 0; a < 7; ++a) {
            block12: for (int b = 0; b < 7; ++b) {
                int xx = 0;
                int zz = 0;
                Direction dir = Direction.NORTH;
                if (cell.north) {
                    xx = x + 5 + a;
                    zz = z + 3;
                    dir = Direction.NORTH;
                }
                if (cell.south) {
                    xx = x + 5 + a;
                    zz = z + 13;
                    dir = Direction.SOUTH;
                }
                if (cell.east) {
                    xx = x + 13;
                    zz = z + 5 + a;
                    dir = Direction.EAST;
                }
                if (cell.west) {
                    xx = x + 3;
                    zz = z + 5 + a;
                    dir = Direction.WEST;
                }
                switch (PAT_DOORWAY[a][b]) {
                    case 1: {
                        OuterGenBossRoom.placeBlock(world, (int)xx, (int)(y + 2 + b), (int)zz, (int)16, (Cell)cell);
                        var pos=new BlockPos(xx,y+2+b,zz);world.setBlock(pos,OuterGate.LOCK.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,dir),3);
                        continue block12;
                    }
                    case 2: {
                        OuterGenBossRoom.placeBlock(world, (int)xx, (int)(y + 2 + b), (int)zz, (int)15, (Cell)cell);
                        continue block12;
                    }
                    case 9: {
                        OuterGenBossRoom.placeBlock(world, (int)xx, (int)(y + 2 + b), (int)zz, (int)17, (Cell)cell);
                    }
                }
            }
        }
    }
}
