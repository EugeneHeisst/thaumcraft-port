package baubles.api;

/** Baubles 1.0.1.10 slot kinds: slot 0 amulet, slots 1-2 rings, slot 3 belt. */
public enum BaubleType {
    RING,
    AMULET,
    BELT,
    /** Later Baubles' TRINKET: fits any bauble slot. */
    TRINKET;
}
