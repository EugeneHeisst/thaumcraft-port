
package dev.eugheist.tc4modern;
import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.lib.world.dim.Cell;


public class OuterGenPassage
extends OuterGenCommon {
    static void generateDefaultPassage(WorldGenLevel world, Random random, int cx, int cz, int y, Cell cell) {
        int j;
        int h;
        int w;
        int mod;
        int z;
        int x;
        block55: {
            block54: {
                block53: {
                    block52: {
                        block51: {
                            block50: {
                                block49: {
                                    block48: {
                                        x = cx * 16;
                                        z = cz * 16;
                                        OuterGenPassage.generateConnections(world, random, cx, cz, y, cell, 4, false);
                                        mod = 0;
                                        if (cell.north && cell.south && cell.west && cell.east && random.nextBoolean()) {
                                            mod = 1;
                                        }
                                        for (w = 1; w < 8; ++w) {
                                            for (h = 1; h < 8; ++h) {
                                                if (w == 4 && h == 4 && mod == 1) {
                                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 2, z + 4 + h, 7, cell);
                                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 8, z + 4 + h, 7, cell);
                                                } else {
                                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 2, z + 4 + h, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 8, z + 4 + h, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                                                }
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y, z + 4 + h, 1, cell);
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 10, z + 4 + h, 1, cell);
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 1, z + 4 + h, 8, cell);
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 9, z + 4 + h, 8, cell);
                                            }
                                        }
                                        if (!cell.north) break block48;
                                        for (w = 2 + mod; w < 9 - mod; ++w) {
                                            for (h = 2 + mod; h < 9 - mod; ++h) {
                                                OuterGenPassage.placeBlock(world, x + 3 + w, y + 10 - h, z + 5, PAT_CONNECT[h][w], Direction.NORTH, cell);
                                            }
                                        }
                                        if (mod != 0) break block49;
                                        if (cell.west) {
                                            OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 6, 3, Direction.EAST, cell);
                                            OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 6, 5, Direction.EAST, cell);
                                        }
                                        if (!cell.east) break block49;
                                        OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 6, 3, Direction.EAST, cell);
                                        OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 6, 5, Direction.EAST, cell);
                                        break block49;
                                    }
                                    for (w = 1; w < 8; ++w) {
                                        for (h = 1; h < 8; ++h) {
                                            OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 5, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                                            OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 4, 8, cell);
                                            OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 3, 1, cell);
                                            if (h == 7) {
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 1, z + 4, 1, cell);
                                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 9, z + 4, 1, cell);
                                            }
                                            if (w != 7) continue;
                                            OuterGenPassage.placeBlock(world, x + 4, y + 9 - h, z + 4, 1, cell);
                                            OuterGenPassage.placeBlock(world, x + 12, y + 9 - h, z + 4, 1, cell);
                                        }
                                    }
                                    for (w = 2; w < 7; ++w) {
                                        OuterGenPassage.placeBlock(world, x + 4 + w, y + 3, z + 6, 3, Direction.EAST, cell);
                                        OuterGenPassage.placeBlock(world, x + 4 + w, y + 7, z + 6, 5, Direction.EAST, cell);
                                    }
                                }
                                if (!cell.south) break block50;
                                for (w = 2 + mod; w < 9 - mod; ++w) {
                                    for (h = 2 + mod; h < 9 - mod; ++h) {
                                        OuterGenPassage.placeBlock(world, x + 3 + w, y + 10 - h, z + 11, PAT_CONNECT[h][w], Direction.SOUTH, cell);
                                    }
                                }
                                if (mod != 0) break block51;
                                if (cell.west) {
                                    OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 10, 4, Direction.EAST, cell);
                                    OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 10, 6, Direction.EAST, cell);
                                }
                                if (!cell.east) break block51;
                                OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 10, 4, Direction.EAST, cell);
                                OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 10, 6, Direction.EAST, cell);
                                break block51;
                            }
                            for (w = 1; w < 8; ++w) {
                                for (h = 1; h < 8; ++h) {
                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 11, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 12, 8, cell);
                                    OuterGenPassage.placeBlock(world, x + 4 + w, y + 9 - h, z + 13, 1, cell);
                                    if (h == 7) {
                                        OuterGenPassage.placeBlock(world, x + 4 + w, y + 1, z + 12, 1, cell);
                                        OuterGenPassage.placeBlock(world, x + 4 + w, y + 9, z + 12, 1, cell);
                                    }
                                    if (w != 7) continue;
                                    OuterGenPassage.placeBlock(world, x + 4, y + 9 - h, z + 12, 1, cell);
                                    OuterGenPassage.placeBlock(world, x + 12, y + 9 - h, z + 12, 1, cell);
                                }
                            }
                            for (w = 2; w < 7; ++w) {
                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 3, z + 10, 4, Direction.EAST, cell);
                                OuterGenPassage.placeBlock(world, x + 4 + w, y + 7, z + 10, 6, Direction.EAST, cell);
                            }
                        }
                        if (!cell.east) break block52;
                        for (w = 2 + mod; w < 9 - mod; ++w) {
                            for (h = 2 + mod; h < 9 - mod; ++h) {
                                OuterGenPassage.placeBlock(world, x + 11, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Direction.EAST, cell);
                            }
                        }
                        if (mod != 0) break block53;
                        if (cell.north) {
                            OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 6, 4, Direction.NORTH, cell);
                            OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 6, 6, Direction.NORTH, cell);
                        }
                        if (!cell.south) break block53;
                        OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 10, 4, Direction.NORTH, cell);
                        OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 10, 6, Direction.NORTH, cell);
                        break block53;
                    }
                    for (w = 1; w < 8; ++w) {
                        for (h = 1; h < 8; ++h) {
                            OuterGenPassage.placeBlock(world, x + 11, y + 9 - h, z + 4 + w, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                            OuterGenPassage.placeBlock(world, x + 12, y + 9 - h, z + 4 + w, 8, cell);
                            OuterGenPassage.placeBlock(world, x + 13, y + 9 - h, z + 4 + w, 1, cell);
                            if (h == 7) {
                                OuterGenPassage.placeBlock(world, x + 12, y + 1, z + 4 + w, 1, cell);
                                OuterGenPassage.placeBlock(world, x + 12, y + 9, z + 4 + w, 1, cell);
                            }
                            if (w != 7) continue;
                            OuterGenPassage.placeBlock(world, x + 12, y + 9 - h, z + 4, 1, cell);
                            OuterGenPassage.placeBlock(world, x + 12, y + 9 - h, z + 12, 1, cell);
                        }
                    }
                    for (w = 2; w < 7; ++w) {
                        OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 4 + w, 4, Direction.NORTH, cell);
                        OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 4 + w, 6, Direction.NORTH, cell);
                    }
                }
                if (!cell.west) break block54;
                for (w = 2 + mod; w < 9 - mod; ++w) {
                    for (h = 2 + mod; h < 9 - mod; ++h) {
                        OuterGenPassage.placeBlock(world, x + 5, y + 10 - h, z + 3 + w, PAT_CONNECT[h][w], Direction.WEST, cell);
                    }
                }
                if (mod != 0) break block55;
                if (cell.north) {
                    OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 6, 3, Direction.NORTH, cell);
                    OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 6, 5, Direction.NORTH, cell);
                }
                if (!cell.south) break block55;
                OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 10, 3, Direction.NORTH, cell);
                OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 10, 5, Direction.NORTH, cell);
                break block55;
            }
            for (w = 1; w < 8; ++w) {
                for (h = 1; h < 8; ++h) {
                    OuterGenPassage.placeBlock(world, x + 5, y + 9 - h, z + 4 + w, cell.feature == 11 && random.nextInt(3) == 0 ? 20 : 2, cell);
                    OuterGenPassage.placeBlock(world, x + 4, y + 9 - h, z + 4 + w, 8, cell);
                    OuterGenPassage.placeBlock(world, x + 3, y + 9 - h, z + 4 + w, 1, cell);
                    if (h == 7) {
                        OuterGenPassage.placeBlock(world, x + 4, y + 1, z + 4 + w, 1, cell);
                        OuterGenPassage.placeBlock(world, x + 4, y + 9, z + 4 + w, 1, cell);
                    }
                    if (w != 7) continue;
                    OuterGenPassage.placeBlock(world, x + 4, y + 9 - h, z + 4, 1, cell);
                    OuterGenPassage.placeBlock(world, x + 4, y + 9 - h, z + 12, 1, cell);
                }
            }
            for (w = 2; w < 7; ++w) {
                OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 4 + w, 3, Direction.NORTH, cell);
                OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 4 + w, 5, Direction.NORTH, cell);
            }
        }
        if (mod == 1) {
            OuterGenPassage.placeBlock(world, x + 5, y + 3, z + 5, 3, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 5, y + 7, z + 5, 5, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 5, y + 3, z + 6, 3, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 5, y + 7, z + 6, 5, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 3, z + 5, 3, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 7, z + 5, 5, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 3, z + 6, 4, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 7, z + 6, 6, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 5, y + 3, z + 11, 3, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 5, y + 7, z + 11, 5, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 6, y + 3, z + 11, 4, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 6, y + 7, z + 11, 6, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 3, z + 11, 4, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 11, y + 7, z + 11, 6, Direction.NORTH, cell);
            OuterGenPassage.placeBlock(world, x + 10, y + 3, z + 11, 4, Direction.EAST, cell);
            OuterGenPassage.placeBlock(world, x + 10, y + 7, z + 11, 6, Direction.EAST, cell);
        }

        if(cell.feature==12)for(w=-4;w<=4;w++)for(h=-4;h<5;h++)for(j=-4;j<=4;j++){
            var pos=new BlockPos(x+8+w,y+4+h,z+8+j);var state=world.getBlockState(pos);
            if(!state.isAir()&&!cosmetic(state.getBlock())&&!state.is(OuterContent.STAIRS.get())||!random.nextBoolean())continue;
            placeBlock(world,pos.getX(),pos.getY(),pos.getZ(),21,cell);
        }
        if(cell.feature==13)for(w=-4;w<=4;w++)for(h=-3;h<=3;h++)for(j=-4;j<=4;j++){
            var pos=new BlockPos(x+8+w,y+4+h,z+8+j);if(!world.isEmptyBlock(pos))continue;
            boolean adjacent=false;for(var d:Direction.values()){var neighbour=pos.relative(d);if(world.getBlockState(neighbour).isFaceSturdy(world,neighbour,d.getOpposite())){adjacent=true;break;}}
            if(!adjacent)continue;
            if(random.nextInt(3)!=0)world.setBlock(pos,TC4Modern.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibresBlock.VARIANT,random.nextInt(4)==0?1:0),3);
            TaintBiomes.setColumn(world,pos,world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(TaintBiomes.TAINTED_LAND));
        }
        if(cell.feature==14){for(w=-3;w<=3;w++)for(h=-3;h<=3;h++)for(j=-3;j<=3;j++){
            var pos=new BlockPos(x+8+w,y+4+h,z+8+j);if(world.isEmptyBlock(pos)&&random.nextFloat()<.35f)world.setBlock(pos,Blocks.COBWEB.defaultBlockState(),3);
        }
            var pos=new BlockPos(x+8,y+4,z+8);world.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);
            if(world.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)spawner.setEntityId(WarpEntities.MIND_SPIDER.get(),world.getRandom());
        }
    }
}
