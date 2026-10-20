package dev.eugheist.portupdates.tc4modern;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Deliberately JDK-only: executed in a separate JVM after Minecraft exits. */
public final class UpdateInstaller {
    public static String hash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) != -1;) digest.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static Properties read(Path path) throws IOException {
        Properties p = new Properties(); try (InputStream in = Files.newInputStream(path)) { p.load(in); } return p;
    }
    private static void write(Path path, Properties p) throws IOException {
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Port updater"); }
        try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
    }
    private static Path name(Path root, String name) throws IOException {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9._+-]*\\.jar")) throw new IOException("Unsafe JAR path");
        return root.resolve(name);
    }
    public static void validate(Path manifest) throws Exception {
        Path dir = manifest.toRealPath().getParent();
        Properties p = read(manifest);
        Path mods = Path.of(p.getProperty("mods")).toRealPath();
        int count = Integer.parseInt(p.getProperty("count"));
        if (count < 1 || count > 7 || !mods.getFileName().toString().equals("mods")) throw new IOException("Invalid transaction");
        Set<Path> old = new HashSet<>(), targets = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Path from = name(mods, p.getProperty(i + ".old")), to = name(mods, p.getProperty(i + ".new"));
            Path staged = dir.resolve(i + ".jar");
            if (!old.add(from) || !targets.add(to) || Files.isSymbolicLink(from) || Files.isSymbolicLink(staged)
                || !Files.isRegularFile(from) || !Files.isRegularFile(staged)) throw new IOException("Invalid source file");
            if (!from.equals(to) && Files.exists(to)) throw new IOException("Target already exists: " + to);
            if (Files.exists(dir.resolve(i + ".backup.jar"))) throw new IOException("Transaction already used");
            if (!hash(from).equals(p.getProperty(i + ".oldHash")) || !hash(staged).equals(p.getProperty(i + ".newHash")))
                throw new IOException("JAR changed since download; nothing installed");
        }
        for (Path path : targets) if (old.contains(path)) {
            // The same old/new name is OK; swapping two mods' names is not.
            for (int i = 0; i < count; i++) if (name(mods, p.getProperty(i + ".new")).equals(path)
                && !p.getProperty(i + ".old").equals(p.getProperty(i + ".new"))) throw new IOException("Overlapping target names");
        }
    }
    public static void install(Path manifest) throws Exception {
        Path dir = manifest.toRealPath().getParent();
        Path root = dir.getParent();
        try (FileChannel lockChannel = FileChannel.open(root.resolve("install.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = lockChannel.tryLock()) {
            if (lock == null) throw new IOException("Another update is being installed");
            validate(manifest);
            Properties p = read(manifest), journal = new Properties();
            Path mods = Path.of(p.getProperty("mods")).toRealPath();
            int count = Integer.parseInt(p.getProperty("count")), backed = 0, placed = 0;
            journal.setProperty("transaction", dir.toString()); journal.setProperty("state", "INSTALLING");
            write(dir.resolve("journal.properties"), journal);
            try {
                for (int i = 0; i < count; i++) {
                    Files.move(name(mods, p.getProperty(i + ".old")), dir.resolve(i + ".backup.jar"));
                    backed++;
                    journal.setProperty("backedUp", Integer.toString(backed)); write(dir.resolve("journal.properties"), journal);
                }
                for (int i = 0; i < count; i++) {
                    Files.move(dir.resolve(i + ".jar"), name(mods, p.getProperty(i + ".new")));
                    placed++;
                    journal.setProperty("installed", Integer.toString(placed)); write(dir.resolve("journal.properties"), journal);
                }
                journal.setProperty("state", "SUCCESS");
                write(dir.resolve("journal.properties"), journal); write(root.resolve("last-result.properties"), journal);
                System.out.println("SUCCESS: installed " + count + " updates; backups: " + dir);
            } catch (Exception failure) {
                boolean restored = true;
                for (int i = placed - 1; i >= 0; i--) {
                    try { Files.move(name(mods, p.getProperty(i + ".new")), dir.resolve(i + ".jar")); }
                    catch (Exception e) { restored = false; failure.addSuppressed(e); }
                }
                for (int i = backed - 1; i >= 0; i--) {
                    try { Files.move(dir.resolve(i + ".backup.jar"), name(mods, p.getProperty(i + ".old"))); }
                    catch (Exception e) { restored = false; failure.addSuppressed(e); }
                }
                journal.setProperty("state", restored ? "ROLLED_BACK" : "RECOVERY_NEEDED");
                journal.setProperty("error", failure.toString());
                write(dir.resolve("journal.properties"), journal); write(root.resolve("last-result.properties"), journal);
                throw failure;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected parent PID and manifest path");
        long pid = Long.parseLong(args[0]);
        if (pid <= 0 || pid == ProcessHandle.current().pid()) throw new IllegalArgumentException("Invalid parent PID");
        Path manifest = Path.of(args[1]).toRealPath();
        validate(manifest);
        ProcessHandle parent = ProcessHandle.of(pid).orElse(null);
        Files.writeString(manifest.getParent().resolve("ready"), "ready", StandardOpenOption.CREATE_NEW);
        if (parent != null) parent.onExit().get(24, TimeUnit.HOURS);
        try { install(manifest); }
        catch (Exception e) {
            Properties result = new Properties();
            Path last = manifest.getParent().getParent().resolve("last-result.properties");
            // Preserve the more specific rollback/recovery result when available.
            if (!Files.exists(manifest.getParent().resolve("journal.properties"))) {
                result.setProperty("state", "FAILED"); result.setProperty("error", e.toString());
                result.setProperty("transaction", manifest.getParent().toString()); write(last, result);
            }
            throw e;
        }
    }
}
