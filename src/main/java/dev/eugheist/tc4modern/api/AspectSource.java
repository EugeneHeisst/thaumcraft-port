package dev.eugheist.tc4modern.api;

import thaumcraft.api.aspects.Aspect;

/**
 * IAspectSource: a block entity the infusion matrix (and other EssentiaHandler drains) may pull essentia from, like warded
 * jars and reservoirs. Thaumic Energistics' infusion provider answers from its ME network.
 */
public interface AspectSource {
    /** takeFromContainer: all or nothing. */
    boolean takeFromContainer(Aspect aspect, int amount);
}
