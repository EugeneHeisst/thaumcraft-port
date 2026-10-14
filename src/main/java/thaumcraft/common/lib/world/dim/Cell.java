/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  thaumcraft.common.lib.utils.Utils
 */
package thaumcraft.common.lib.world.dim;



public class Cell {
    public boolean north;
    public boolean south;
    public boolean east;
    public boolean west;
    public boolean above;
    public boolean below = false;
    public byte feature = 0;

    public Cell() {
    }

    public Cell(short data) {
        this.unpack(data);
    }

    private void unpack(short data) {
        this.north = ((data & (1 << 0)) != 0);
        this.south = ((data & (1 << 1)) != 0);
        this.east = ((data & (1 << 2)) != 0);
        this.west = ((data & (1 << 3)) != 0);
        this.above = ((data & (1 << 4)) != 0);
        this.below = ((data & (1 << 5)) != 0);
        this.feature = (byte)(data >> 8);
    }

    public short pack() {
        int out = 0;
        if (this.north) {
            out = (out | (1 << 0));
        }
        if (this.south) {
            out = (out | (1 << 1));
        }
        if (this.east) {
            out = (out | (1 << 2));
        }
        if (this.west) {
            out = (out | (1 << 3));
        }
        if (this.above) {
            out = (out | (1 << 4));
        }
        if (this.below) {
            out = (out | (1 << 5));
        }
        return (short)(out |= this.feature << 8);
    }
}
