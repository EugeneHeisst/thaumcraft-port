package dev.eugheist.tc4modern;

import dev.eugheist.tc4modern.api.AspectContainers;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Built-in AspectContainers for the tc4modern essentia blocks, with Thaumic Energistics' original capacities. */
final class AspectContainerHosts {
    private AspectContainerHosts() {}
    static void register() {
        AspectContainers.register(tile -> {
            if (tile instanceof WardedJarEntity jar) return new Base(64, true, true) {
                public AspectList getAspects() { var l = new AspectList(); if (jar.state.aspect != null && jar.state.amount > 0) l.add(jar.state.aspect, jar.state.amount); return l; }
                public boolean accepts(Aspect a) { return jar.state.amount > 0 && jar.state.aspect != null ? a == jar.state.aspect : jar.state.doesContainerAccept(a); }
                public boolean take(Aspect a, int n) { if (!jar.state.takeFromContainer(a, n)) return false; jar.changed(); return true; }
                public int add(Aspect a, int n) { int r = jar.state.addToContainer(a, n); if (r != n) jar.changed(); return r; }
                public boolean voids() { return jar.state instanceof thaumcraft.common.tiles.essentia.VoidJarState; }
            };
            if (tile instanceof EssentiaReservoir.ReservoirEntity r) return new Base(256, true, true) {
                public AspectList getAspects() { return r.essentia.copy(); }
                public boolean accepts(Aspect a) { return true; }
                public boolean take(Aspect a, int n) { return r.takeFromContainer(a, n); }
                public int add(Aspect a, int n) { return r.addToContainer(a, n); }
            };
            if (tile instanceof AlembicEntity al) return new Base(32, true, false) {
                public AspectList getAspects() { var l = new AspectList(); if (al.state.aspect != null && al.state.amount > 0) l.add(al.state.aspect, al.state.amount); return l; }
                public boolean take(Aspect a, int n) { if (!al.state.takeFromContainer(a, n)) return false; al.changed(); return true; }
            };
            if (tile instanceof EssentiaBufferEntity b) return new Base(8, true, false) {
                public AspectList getAspects() { return b.state.aspects.copy(); }
                public boolean take(Aspect a, int n) { if (b.state.aspects.getAmount(a) < n) return false; b.state.aspects.remove(a, n); b.sync(); return true; }
            };
            if (tile instanceof AlchemyMachineEntity m && !m.state.crystalizer) return new Base(0, true, false) {
                public AspectList getAspects() { var l = new AspectList(); if (m.state.output != null) l.add(m.state.output, 1); return l; }
                public boolean take(Aspect a, int n) { if (m.state.output == null || a != m.state.output || n > 1) return false; m.state.output = null; m.changed(); return true; }
            };
            if (tile instanceof AdvancedFurnace.NozzleEntity nozzle) return new Base(0, true, false) {
                public AspectList getAspects() { var f = nozzle.furnace(); return f == null ? new AspectList() : f.aspects.copy(); }
                public boolean take(Aspect a, int n) { return nozzle.take(a, n); }
            };
            return null;
        });
    }
    abstract static class Base implements AspectContainers.Container {
        final int capacity; final boolean extract, inject;
        Base(int capacity, boolean extract, boolean inject) { this.capacity = capacity; this.extract = extract; this.inject = inject; }
        public int capacity() { return capacity; }
        public boolean canExtract() { return extract; }
        public boolean canInject() { return inject; }
        public boolean accepts(Aspect a) { return false; }
        public int add(Aspect a, int n) { return n; }
    }
}
