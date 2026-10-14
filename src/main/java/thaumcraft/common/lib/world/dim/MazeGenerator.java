/*
 * Decompiled with CFR 0.152.
 */
package thaumcraft.common.lib.world.dim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import thaumcraft.common.lib.world.dim.Cell;
import thaumcraft.common.lib.world.dim.CellLoc;

public class MazeGenerator {
    int width = 0;
    int height = 0;
    long seed = 0L;
    Random rand = null;
    public int[][] grid;
    public static final int N = 1;
    public static final int S = 2;
    public static final int E = 4;
    public static final int W = 8;
    public static final int A = 16;
    public static final int B = 32;

    public static int getOPP(int in) {
        switch (in) {
            case 1: {
                return 2;
            }
            case 2: {
                return 1;
            }
            case 4: {
                return 8;
            }
            case 8: {
                return 4;
            }
        }
        return -99;
    }

    public static int getDX(int in) {
        switch (in) {
            case 1: {
                return 0;
            }
            case 2: {
                return 0;
            }
            case 4: {
                return 1;
            }
            case 8: {
                return -1;
            }
        }
        return -99;
    }

    public static int getDY(int in) {
        switch (in) {
            case 1: {
                return -1;
            }
            case 2: {
                return 1;
            }
            case 4: {
                return 0;
            }
            case 8: {
                return 0;
            }
        }
        return -99;
    }

    public MazeGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.rand = new Random(seed);
        this.grid = new int[height][width];
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                this.grid[y][x] = 0;
            }
        }
    }

    public boolean generate() {
        int aa;
        int bx = 0;
        int by = 0;
        switch (this.rand.nextInt(4)) {
            case 0: {
                bx = 0;
                by = 0;
                break;
            }
            case 1: {
                bx = this.width - 2;
                by = this.height - 2;
                break;
            }
            case 2: {
                bx = this.width - 2;
                by = 0;
                break;
            }
            case 3: {
                bx = 0;
                by = this.height - 2;
            }
        }
        this.grid[by][bx] = 512;
        this.grid[by][bx + 1] = 768;
        this.grid[by + 1][bx] = 1024;
        this.grid[by + 1][bx + 1] = 1280;
        int px = 1 + this.width / 2;
        int py = 1 + this.height / 2;
        this.grid[py][px] = 256;
        ArrayList<Loc> cells = new ArrayList<Loc>();
        int l = (this.width + this.height) / 4;
        for (int z = 0; z < l; ++z) {
            int w = 1 + this.rand.nextInt(3);
            if (w > 2) {
                --l;
            }
            int qq = this.rand.nextInt(this.width - w);
            int ww = this.rand.nextInt(this.height - w);
            for (int a = qq; a < qq + w; ++a) {
                for (int b = ww; b < ww + w; ++b) {
                    if (this.grid[b][a] != 0) continue;
                    this.grid[b][a] = -1;
                }
            }
        }
        List<Integer> directions = Arrays.asList(1, 2, 4, 8);
        Collections.shuffle(directions, this.rand);
        int xx = px + MazeGenerator.getDX(directions.get(0));
        int yy = py + MazeGenerator.getDY(directions.get(0));
        int[] nArray = this.grid[py];
        int n = px;
        nArray[n] = nArray[n] | directions.get(0);
        if (this.grid[yy][xx] < 0) {
            this.grid[yy][xx] = 0;
        }
        int[] nArray2 = this.grid[yy];
        int n2 = xx;
        nArray2[n2] = nArray2[n2] | MazeGenerator.getOPP(directions.get(0));
        cells.add(new Loc(xx, yy));
        boolean success = false;
        while (!cells.isEmpty()) {
            int index = this.getNextIndex(cells.size());
            int x = ((Loc)cells.get((int)index)).x;
            int y = ((Loc)cells.get((int)index)).y;
            Collections.shuffle(directions, this.rand);
            boolean carved = false;
            for (int dir : directions) {
                int nx = x + MazeGenerator.getDX(dir);
                int ny = y + MazeGenerator.getDY(dir);
                if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                if (this.grid[ny][nx] == 0) {
                    int[] nArray3 = this.grid[y];
                    int n3 = x;
                    nArray3[n3] = nArray3[n3] | dir;
                    int[] nArray4 = this.grid[ny];
                    int n4 = nx;
                    nArray4[n4] = nArray4[n4] | MazeGenerator.getOPP(dir);
                    cells.add(new Loc(nx, ny));
                    carved = true;
                }
                if (!carved) continue;
                success = true;
                break;
            }
            if (carved) continue;
            cells.remove(index);
        }
        if (!success) {
            return false;
        }
        for (int aa2 = 0; aa2 < this.height; ++aa2) {
            for (int bb = 0; bb < this.width; ++bb) {
                if (this.grid[aa2][bb] >= 0) continue;
                this.grid[aa2][bb] = 0;
            }
        }
        Collections.shuffle(directions, this.rand);
        for (int dir : directions) {
            int nx = px + MazeGenerator.getDX(dir);
            int ny = py + MazeGenerator.getDY(dir);
            if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1 || this.grid[ny][nx] <= 0 || !this.rand.nextBoolean()) continue;
            int[] nArray5 = this.grid[ny];
            int n5 = nx;
            nArray5[n5] = nArray5[n5] | MazeGenerator.getOPP(dir);
            int[] nArray6 = this.grid[py];
            int n6 = px;
            nArray6[n6] = nArray6[n6] | dir;
        }
        Collections.shuffle(directions, this.rand);
        boolean connected = false;
        block22: for (int ax = 0; ax < 2; ++ax) {
            for (int ay = 0; ay < 2; ++ay) {
                for (int dir : directions) {
                    int nx = bx + ax + MazeGenerator.getDX(dir);
                    int ny = by + ay + MazeGenerator.getDY(dir);
                    if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1 || this.grid[ny][nx] <= 0 || new Cell((short)((short)this.grid[ny][nx])).feature != 0) continue;
                    int[] nArray7 = this.grid[ny];
                    int n7 = nx;
                    nArray7[n7] = nArray7[n7] | MazeGenerator.getOPP(dir);
                    int[] nArray8 = this.grid[by + ay];
                    int n8 = bx + ax;
                    nArray8[n8] = nArray8[n8] | dir;
                    connected = true;
                    break block22;
                }
            }
        }
        if (!connected) {
            List<Integer> directions2 = Arrays.asList(1, 2, 4, 8);
            Collections.shuffle(directions2, this.rand);
            success = false;
            block25: for (int ax = 0; ax < 2; ++ax) {
                for (int ay = 0; ay < 2; ++ay) {
                    for (int dir2 : directions2) {
                        int qx = bx + ax + MazeGenerator.getDX(dir2);
                        int qy = by + ay + MazeGenerator.getDY(dir2);
                        if (0 >= qx || qx >= this.width - 1 || 0 >= qy || qy >= this.height - 1 || this.grid[qy][qx] != 0) continue;
                        cells.add(new Loc(qx, qy));
                        while (!cells.isEmpty()) {
                            int index = this.getNextIndex(cells.size());
                            int x = ((Loc)cells.get((int)index)).x;
                            int y = ((Loc)cells.get((int)index)).y;
                            Collections.shuffle(directions, this.rand);
                            boolean carved = false;
                            for (int dir : directions) {
                                int nx = x + MazeGenerator.getDX(dir);
                                int ny = y + MazeGenerator.getDY(dir);
                                if (0 >= nx || nx >= this.width - 1 || 0 >= ny || ny >= this.height - 1) continue;
                                if (this.grid[ny][nx] == 0) {
                                    int[] nArray9 = this.grid[y];
                                    int n9 = x;
                                    nArray9[n9] = nArray9[n9] | dir;
                                    int[] nArray10 = this.grid[y];
                                    int n10 = x;
                                    nArray10[n10] = nArray10[n10] | 0x6300;
                                    int[] nArray11 = this.grid[ny];
                                    int n11 = nx;
                                    nArray11[n11] = nArray11[n11] | MazeGenerator.getOPP(dir);
                                    int[] nArray12 = this.grid[ny];
                                    int n12 = nx;
                                    nArray12[n12] = nArray12[n12] | 0x6300;
                                    cells.add(new Loc(nx, ny));
                                    carved = true;
                                } else if (new Cell((short)((short)this.grid[ny][nx])).feature == 0) {
                                    int[] nArray13 = this.grid[y];
                                    int n13 = x;
                                    nArray13[n13] = nArray13[n13] | dir;
                                    int[] nArray14 = this.grid[ny];
                                    int n14 = nx;
                                    nArray14[n14] = nArray14[n14] | MazeGenerator.getOPP(dir);
                                    int[] nArray15 = this.grid[qy];
                                    int n15 = qx;
                                    nArray15[n15] = nArray15[n15] | MazeGenerator.getOPP(dir2);
                                    int[] nArray16 = this.grid[by + ay];
                                    int n16 = bx + ax;
                                    nArray16[n16] = nArray16[n16] | dir2;
                                    success = true;
                                    break block25;
                                }
                                if (!carved) continue;
                                break;
                            }
                            if (carved) continue;
                            cells.remove(index);
                        }
                    }
                }
            }
            if (!success) {
                return false;
            }
        }
        for (int aa3 = 0; aa3 < this.height; ++aa3) {
            for (int bb = 0; bb < this.width; ++bb) {
                Cell c = new Cell((short)this.grid[aa3][bb]);
                if (c.feature != 99) continue;
                c.feature = 0;
                this.grid[aa3][bb] = c.pack();
            }
        }
        ArrayList<CellLoc> deadEndsloc = new ArrayList<CellLoc>();
        for (aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                Cell c = new Cell((short)this.grid[aa][bb]);
                int exits = (c.north ? 1 : 0) + (c.south ? 1 : 0) + (c.east ? 1 : 0) + (c.west ? 1 : 0);
                if (exits != 1 || c.feature != 0) continue;
                deadEndsloc.add(new CellLoc(aa, bb));
            }
        }
        if (deadEndsloc.size() == 0) {
            return false;
        }
        int r = this.rand.nextInt(deadEndsloc.size());
        CellLoc ll = (CellLoc)deadEndsloc.get(r);
        Cell c = new Cell((short)this.grid[ll.x][ll.z]);
        c.feature = (byte)6;
        this.grid[ll.x][ll.z] = c.pack();
        deadEndsloc.remove(r);
        if (deadEndsloc.size() > 0) {
            int count = 0;
            while (count < deadEndsloc.size() / 2) {
                int r2 = this.rand.nextInt(deadEndsloc.size());
                CellLoc ll2 = (CellLoc)deadEndsloc.get(r2);
                Cell c2 = new Cell((short)this.grid[ll2.x][ll2.z]);
                if (c2.feature != 0) continue;
                c2.feature = (byte)(7 + this.rand.nextInt(3));
                this.grid[ll2.x][ll2.z] = c2.pack();
                deadEndsloc.remove(r2);
                ++count;
            }
        }
        for (aa = 0; aa < this.height; ++aa) {
            for (int bb = 0; bb < this.width; ++bb) {
                c = new Cell((short)this.grid[aa][bb]);
                if (c.feature != 0 || !c.north && !c.south && !c.west && !c.east || this.rand.nextInt(25) != 0) continue;
                switch (this.rand.nextInt(8)) {
                    case 0: {
                        c.feature = (byte)8;
                        break;
                    }
                    case 1: {
                        c.feature = (byte)10;
                        break;
                    }
                    case 2: 
                    case 3: {
                        c.feature = (byte)11;
                        break;
                    }
                    case 4: 
                    case 5: {
                        c.feature = (byte)12;
                        break;
                    }
                    case 6: {
                        c.feature = (byte)13;
                        break;
                    }
                    case 7: {
                        c.feature = (byte)14;
                    }
                }
                this.grid[aa][bb] = c.pack();
            }
        }
        return true;
    }

    private int getNextIndex(int ceil) {
        float r = this.rand.nextFloat();
        if (r <= 0.45f) {
            return ceil - 1;
        }
        if (r <= 0.9f) {
            return this.rand.nextInt(ceil);
        }
        return 0;
    }

    public void print() {
        HashMap<Integer, String[]> tiles = new HashMap<Integer, String[]>();
        tiles.put(0, new String[]{"...", "...", "..."});
        tiles.put(1, new String[]{"# #", "# #", "###"});
        tiles.put(2, new String[]{"###", "# #", "# #"});
        tiles.put(4, new String[]{"###", "#  ", "###"});
        tiles.put(8, new String[]{"###", "  #", "###"});
        tiles.put(3, new String[]{"# #", "# #", "# #"});
        tiles.put(9, new String[]{"# #", "  #", "###"});
        tiles.put(5, new String[]{"# #", "#  ", "###"});
        tiles.put(10, new String[]{"###", "  #", "# #"});
        tiles.put(6, new String[]{"###", "#  ", "# #"});
        tiles.put(12, new String[]{"###", "   ", "###"});
        tiles.put(7, new String[]{"# #", "#  ", "# #"});
        tiles.put(11, new String[]{"# #", "  #", "# #"});
        tiles.put(13, new String[]{"# #", "   ", "###"});
        tiles.put(14, new String[]{"###", "   ", "# #"});
        tiles.put(15, new String[]{"# #", "   ", "# #"});
        tiles.put(19, new String[]{"#-#", " A ", "#-#"});
        tiles.put(28, new String[]{"# #", "|A|", "# #"});
        tiles.put(35, new String[]{"#-#", " B ", "#-#"});
        tiles.put(44, new String[]{"# #", "|B|", "# #"});
        for (int y = 0; y < this.height; ++y) {
            for (int q = 0; q < 3; ++q) {
                for (int x = 0; x < this.width; ++x) {
                    Cell c = new Cell((short)this.grid[y][x]);
                    if (tiles.containsKey(this.grid[y][x])) {
                        System.out.print(((String[])tiles.get(this.grid[y][x]))[q]);
                        continue;
                    }
                    if (c.feature == 1) {
                        if (q == 0 && c.north || q == 2 && c.south) {
                            System.out.print("P|P");
                            continue;
                        }
                        if (q == 1 && c.west && !c.east) {
                            System.out.print("-PP");
                            continue;
                        }
                        if (q == 1 && c.east && !c.west) {
                            System.out.print("PP-");
                            continue;
                        }
                        if (q == 1 && c.east && c.west) {
                            System.out.print("-P-");
                            continue;
                        }
                        System.out.print("PPP");
                        continue;
                    }
                    if (c.feature <= 1) continue;
                    System.out.print("FFF");
                }
                System.out.println();
            }
        }
    }

    private class Loc {
        int x;
        int y;

        public Loc(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
