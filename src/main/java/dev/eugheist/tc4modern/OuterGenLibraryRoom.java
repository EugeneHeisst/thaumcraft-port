package dev.eugheist.tc4modern;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import thaumcraft.common.lib.world.dim.Cell;
public class OuterGenLibraryRoom
extends OuterGenCommon {
    static void generateRoom(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int c;
        int b;
        int a;
        int x = cx * 16;
        int z = cz * 16;
        for (a = 1; a <= 15; ++a) {
            for (b = 1; b <= 15; ++b) {
                for (c = 0; c < 13; ++c) {
                    if (a != 1 && a != 15 && b != 1 && b != 15) continue;
                    OuterGenLibraryRoom.placeBlock(world, x + a, y + c, z + b, 1, cell);
                }
            }
        }
        for (a = 2; a <= 14; ++a) {
            for (b = 2; b <= 14; ++b) {
                for (c = 1; c < 12; ++c) {
                    if (a != 2 && a != 14 && b != 2 && b != 14 || a == 2 && b > 3 && b < 12 && cell.west && c < 10 || a == 14 && b > 3 && b < 12 && cell.east && c < 10 || b == 2 && a > 3 && a < 12 && cell.north && c < 10 || b == 14 && a > 3 && a < 12 && cell.south && c < 10) continue;
                    OuterGenLibraryRoom.placeBlock(world, x + a, y + c, z + b, 8, cell);
                }
            }
        }
        for (a = 3; a <= 13; ++a) {
            for (b = 3; b <= 13; ++b) {
                for (c = 2; c < 11; ++c) {
                    if (a != 3 && a != 13 && b != 3 && b != 13) continue;
                    OuterGenLibraryRoom.placeBlock(world, x + a, y + c, z + b, 2, cell);
                }
            }
        }
        for (a = 2; a <= 14; ++a) {
            for (b = 2; b <= 14; ++b) {
                OuterGenLibraryRoom.placeBlock(world, x + a, y - 1, z + b, 1, cell);
                OuterGenLibraryRoom.placeBlock(world, x + a, y, z + b, 8, cell);
                OuterGenLibraryRoom.placeBlock(world, x + a, y + 1, z + b, 2, cell);
                OuterGenLibraryRoom.placeBlock(world, x + a, y + 12, z + b, 1, cell);
                OuterGenLibraryRoom.placeBlock(world, x + a, y + 11, z + b, 8, cell);
                OuterGenLibraryRoom.placeBlock(world, x + a, y + 10, z + b, 2, cell);
                if (a <= 3 || a >= 13 || b <= 3 || b >= 13) continue;
                if (a <= 5 && b <= 5 || a <= 5 && b >= 11 || a >= 11 && b <= 5 || a >= 11 && b >= 11) {
                    OuterGenLibraryRoom.placeBlock(world, x + a, y + 2, z + b, 2, cell);
                    OuterGenLibraryRoom.placeBlock(world, x + a, y + 9, z + b, 2, cell);
                }
                if (!(a == 5 && b == 5 || a == 5 && b == 11 || a == 11 && b == 5) && (a != 11 || b != 11)) continue;
                world.setBlock(new BlockPos(x + a, y + 3, z + b),OuterContent.PEDESTAL.get().defaultBlockState(),3);
                world.setBlock(new BlockPos(x + a, y + 8, z + b),OuterContent.PEDESTAL.get().defaultBlockState(),3);
            }
        }
        for (int g = 0; g < 5; ++g) {
            OuterGenLibraryRoom.placeBlock(world, x + 6 + g, y + 2, z + 4, 10, Direction.NORTH, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 6 + g, y + 2, z + 12, 10, Direction.SOUTH, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 12, y + 2, z + 6 + g, 10, Direction.EAST, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 4, y + 2, z + 6 + g, 10, Direction.WEST, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 6 + g, y + 9, z + 4, 11, Direction.NORTH, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 6 + g, y + 9, z + 12, 11, Direction.SOUTH, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 12, y + 9, z + 6 + g, 11, Direction.EAST, cell);
            OuterGenLibraryRoom.placeBlock(world, x + 4, y + 9, z + 6 + g, 11, Direction.WEST, cell);
        }
        world.setBlock(new BlockPos(x + 5, y + 4, z + 5),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 5, y + 5, z + 5),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3);
        world.setBlock(new BlockPos(x + 5, y + 4, z + 11),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 5, y + 5, z + 11),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3);
        world.setBlock(new BlockPos(x + 11, y + 4, z + 5),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 11, y + 5, z + 5),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3);
        world.setBlock(new BlockPos(x + 11, y + 4, z + 11),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 11, y + 5, z + 11),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3);
        world.setBlock(new BlockPos(x + 5, y + 7, z + 5),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 5, y + 6, z + 5),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
        world.setBlock(new BlockPos(x + 5, y + 7, z + 11),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 5, y + 6, z + 11),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
        world.setBlock(new BlockPos(x + 11, y + 7, z + 5),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 11, y + 6, z + 5),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
        world.setBlock(new BlockPos(x + 11, y + 7, z + 11),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 11, y + 6, z + 11),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
        world.setBlock(new BlockPos(x + 8, y + 2, z + 8),OuterContent.PEDESTAL.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 8, y + 3, z + 8),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 8, y + 4, z + 8),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3);
        world.setBlock(new BlockPos(x + 8, y + 9, z + 8),OuterContent.PEDESTAL.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 8, y + 8, z + 8),OuterContent.GLYPHED.get().defaultBlockState(),3);
        world.setBlock(new BlockPos(x + 8, y + 7, z + 8),OuterContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
        OuterGenCommon.generateConnections(world, random, cx, cz, y, cell, 3, true);
    }
}
