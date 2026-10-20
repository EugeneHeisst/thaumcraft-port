package dev.eugheist.portupdates.tc4modern;

import com.google.gson.*;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.*;

/** GitHub is contacted only by the background client worker. No credentials or telemetry. */
public final class UpdateService {
    public static final Map<String, String> REPOS = Map.of(
        "tc4modern", "thaumcraft-port", "ic2", "industrialcraft-port",
        "ttinkerer", "thaumic-tinkerer-port", "thaumicenergistics", "thaumic-energistics-port",
        "advanced_solar_panels", "advanced-solar-panels-port", "emt", "electro-magic-tools-port",
        "gravisuite", "gravisuite-port");
    private static final long LIMIT = 128L * 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    public record Dependency(String id, String range, String type, String side) {}
    public record Installed(String id, String name, String version, Path file, List<Dependency> dependencies) {}
    public record Release(Installed installed, String version, String name, URI url, String sha256, long size) {}
    public record Check(List<Release> updates, List<String> errors) {}
    public record Metadata(String id, String version, List<Dependency> dependencies, String loaderRange) {}
    public record Prepared(Path directory, Path manifest, int count) {}

    public Check check(List<Installed> installed) {
        List<Release> updates = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Installed mod : installed) {
            if (!REPOS.containsKey(mod.id())) continue;
            try {
                URI api = URI.create("https://api.github.com/repos/EugeneHeisst/" + REPOS.get(mod.id()) + "/releases/latest");
                byte[] json = fetch(api, 2 * 1024 * 1024);
                Release r = release(mod, JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject());
                if (r != null) updates.add(r);
            } catch (Exception e) { errors.add(mod.name() + ": " + message(e)); }
        }
        return new Check(List.copyOf(updates), List.copyOf(errors));
    }

    public static Release release(Installed mod, JsonObject json) throws IOException {
        if (json.get("draft").getAsBoolean() || json.get("prerelease").getAsBoolean()) return null;
        String version = json.get("tag_name").getAsString().replaceFirst("^v", "");
        if (!version.matches("[0-9]+(?:\\.[0-9]+){1,3}")) throw new IOException("Unsupported release version");
        if (new DefaultArtifactVersion(version).compareTo(new DefaultArtifactVersion(mod.version())) <= 0) return null;
        List<JsonObject> jars = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("assets")) {
            JsonObject a = e.getAsJsonObject();
            String n = a.get("name").getAsString();
            if (n.endsWith(".jar") && !n.endsWith("-sources.jar") && !n.endsWith("-javadoc.jar")) jars.add(a);
        }
        if (jars.size() != 1) throw new IOException("Release must have exactly one runtime JAR");
        JsonObject a = jars.getFirst();
        String name = a.get("name").getAsString();
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9._+-]*\\.jar")) throw new IOException("Unsafe asset name");
        if (!a.has("digest") || a.get("digest").isJsonNull()) throw new IOException("Release has no SHA-256 digest");
        String digest = a.get("digest").getAsString();
        if (!digest.matches("sha256:[a-fA-F0-9]{64}")) throw new IOException("Missing SHA-256 digest");
        URI url = URI.create(a.get("browser_download_url").getAsString());
        String prefix = "/EugeneHeisst/" + REPOS.get(mod.id()) + "/releases/download/";
        if (!"https".equals(url.getScheme()) || !"github.com".equals(url.getHost()) || !url.getPath().startsWith(prefix)
                || url.getUserInfo() != null || url.getPort() != -1) throw new IOException("Unexpected release URL");
        long size = a.get("size").getAsLong();
        if (size <= 0 || size > LIMIT) throw new IOException("Invalid asset size");
        return new Release(mod, version, name, url, digest.substring(7).toLowerCase(Locale.ROOT), size);
    }

    /** Bounded reads and manually checked HTTPS redirects prevent giant/error downloads. */
    byte[] fetch(URI uri, long maximum) throws IOException, InterruptedException {
        for (int hop = 0; hop < 6; hop++) {
            String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getPort() != -1
                || !("api.github.com".equals(host) || "github.com".equals(host)
                     || "release-assets.githubusercontent.com".equals(host)
                     || "objects.githubusercontent.com".equals(host))) throw new IOException("Unexpected download host");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(90))
                .header("User-Agent", "EugeneHeisst-PortUpdates/1").header("Accept", "application/vnd.github+json").GET().build();
            // BodyHandlers.ofByteArray completes within the request timeout; the subscriber enforces the size bound.
            HttpResponse<byte[]> response = http.send(request, info -> new BoundedBody(maximum));
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                uri = uri.resolve(response.headers().firstValue("location").orElseThrow(() -> new IOException("Redirect without location")));
                continue;
            }
            if (response.statusCode() != 200) throw new IOException("GitHub HTTP " + response.statusCode());
            return response.body();
        }
        throw new IOException("Too many redirects");
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final long maximum;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final java.util.concurrent.CompletableFuture<byte[]> result = new java.util.concurrent.CompletableFuture<>();
        private java.util.concurrent.Flow.Subscription subscription;
        BoundedBody(long maximum) { this.maximum = maximum; }
        public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(java.util.concurrent.Flow.Subscription s) { subscription = s; s.request(1); }
        public void onNext(List<java.nio.ByteBuffer> buffers) {
            for (var b : buffers) {
                if ((long) bytes.size() + b.remaining() > maximum) {
                    subscription.cancel(); result.completeExceptionally(new IOException("Download exceeds size limit")); return;
                }
                byte[] chunk = new byte[b.remaining()]; b.get(chunk); bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable e) { result.completeExceptionally(e); }
        public void onComplete() { result.complete(bytes.toByteArray()); }
    }

    public Prepared prepare(Path game, List<Installed> installed, List<Release> releases, String loaderVersion,
                            Consumer<String> progress) throws Exception {
        if (releases.isEmpty()) throw new IOException("No updates selected");
        Path mods = game.resolve("mods").toRealPath();
        Path root = game.resolve("port-updates"); Files.createDirectories(root);
        Path dir = Files.createDirectory(root.resolve(UUID.randomUUID().toString()));
        Properties plan = new Properties();
        plan.setProperty("mods", mods.toString());
        plan.setProperty("count", Integer.toString(releases.size()));
        Map<String, Metadata> replacement = new HashMap<>();
        Set<Path> oldFiles = new HashSet<>(), newFiles = new HashSet<>();
        for (int i = 0; i < releases.size(); i++) {
            Release r = releases.get(i); progress.accept(r.installed().name());
            Path old = r.installed().file().toRealPath();
            Path target = mods.resolve(r.name());
            if (!old.getParent().equals(mods) || !Files.isRegularFile(old, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(r.installed().file()) || !oldFiles.add(old) || !newFiles.add(target))
                throw new IOException("Only separate JARs directly in mods/ can be updated");
            if (!old.equals(target) && Files.exists(target)) throw new IOException("Target JAR already exists: " + r.name());
            if (old.getFileName().toString().contains("client-dim")) throw new IOException("Custom client build requires manual update");
            byte[] bytes = fetch(r.url(), r.size());
            if (bytes.length != r.size()) throw new IOException("Incorrect download size: " + r.name());
            Path staged = dir.resolve(i + ".jar"); Files.write(staged, bytes, StandardOpenOption.CREATE_NEW);
            if (!UpdateInstaller.hash(staged).equals(r.sha256())) throw new IOException("SHA-256 mismatch: " + r.name());
            Metadata metadata = metadata(staged);
            if (!metadata.id().equals(r.installed().id()) || !metadata.version().equals(r.version()))
                throw new IOException("Release JAR mod ID/version mismatch: " + r.name());
            if (!matches(metadata.loaderRange(), loaderVersion)) throw new IOException("Update requires another NeoForge loader");
            replacement.put(metadata.id(), metadata);
            plan.setProperty(i + ".old", old.getFileName().toString());
            plan.setProperty(i + ".new", r.name());
            plan.setProperty(i + ".oldHash", UpdateInstaller.hash(old));
            plan.setProperty(i + ".newHash", r.sha256());
        }
        validateDependencies(installed, replacement);
        // A standalone JDK-only helper, not the mod JAR, survives replacement on Windows too.
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(dir.resolve("installer.jar")))) {
            String resource = UpdateInstaller.class.getName().replace('.', '/') + ".class";
            out.putNextEntry(new JarEntry(resource));
            try (InputStream in = UpdateInstaller.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) throw new IOException("Installer class is missing");
                in.transferTo(out);
            }
            out.closeEntry();
        }
        Path manifest = dir.resolve("plan.properties");
        try (OutputStream out = Files.newOutputStream(manifest, StandardOpenOption.CREATE_NEW)) { plan.store(out, "Port update transaction"); }
        return new Prepared(dir, manifest, releases.size());
    }

    public static Metadata metadata(Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            JarEntry entry = jar.getJarEntry("META-INF/neoforge.mods.toml");
            if (entry == null || entry.getSize() > 1024 * 1024) throw new IOException("Not a NeoForge mod JAR");
            Config c;
            try (Reader in = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) { c = new TomlParser().parse(in); }
            if (!"javafml".equals(c.get("modLoader"))) throw new IOException("Wrong mod loader");
            List<Config> mods = c.get("mods");
            if (mods == null || mods.size() != 1) throw new IOException("Expected a single-mod JAR");
            String id = mods.getFirst().get("modId"), version = mods.getFirst().get("version");
            if ("${file.jarVersion}".equals(version)) version = jar.getManifest() == null ? "" : jar.getManifest().getMainAttributes().getValue("Implementation-Version");
            List<Config> raw = c.getOrElse("dependencies." + id, List.of());
            List<Dependency> deps = new ArrayList<>();
            for (Config d : raw) deps.add(new Dependency(d.get("modId"), d.getOrElse("versionRange", ""),
                d.getOrElse("type", "required"), d.getOrElse("side", "BOTH")));
            if (deps.stream().noneMatch(d -> d.id().equals("minecraft") && d.type().equalsIgnoreCase("required")))
                throw new IOException("No Minecraft version requirement");
            return new Metadata(id, version, List.copyOf(deps), c.get("loaderVersion"));
        } catch (RuntimeException e) { throw new IOException("Invalid mod metadata", e); }
    }

    static boolean matches(String range, String version) throws Exception {
        if (range == null || range.isBlank()) return true;
        VersionRange r = VersionRange.createFromVersionSpec(range);
        if (r.getRestrictions().isEmpty()) return new DefaultArtifactVersion(range).compareTo(new DefaultArtifactVersion(version)) == 0;
        return r.containsVersion(new DefaultArtifactVersion(version));
    }
    public static void validateDependencies(List<Installed> installed, Map<String, Metadata> replacements) throws Exception {
        Map<String, String> versions = new HashMap<>();
        for (Installed m : installed) versions.put(m.id(), m.version());
        replacements.forEach((id, m) -> versions.put(id, m.version()));
        for (Installed m : installed) {
            List<Dependency> deps = replacements.containsKey(m.id()) ? replacements.get(m.id()).dependencies() : m.dependencies();
            for (Dependency d : deps) {
                // Preserve loader-approved, unrelated overrides; only changes to this dependency graph matter.
                if (!replacements.containsKey(m.id()) && !replacements.containsKey(d.id())) continue;
                if (d.side().equalsIgnoreCase("SERVER")) continue;
                String v = versions.get(d.id());
                boolean present = v != null, fits = present && matches(d.range(), v);
                boolean fail = switch (d.type().toLowerCase(Locale.ROOT)) {
                    case "required" -> !fits;
                    case "optional" -> present && !fits;
                    case "incompatible" -> fits;
                    case "discouraged" -> false;
                    default -> throw new IOException("Unknown dependency type: " + d.type());
                };
                if (fail) throw new IOException(m.name() + " requires compatible " + d.id() + " " + d.range());
            }
        }
    }

    public Process startInstaller(Prepared prepared) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Files.deleteIfExists(prepared.directory().resolve("ready"));
        Process process = new ProcessBuilder(java.toString(), "-cp", prepared.directory().resolve("installer.jar").toString(),
            UpdateInstaller.class.getName(), Long.toString(ProcessHandle.current().pid()), prepared.manifest().toString())
            .redirectErrorStream(true).redirectOutput(prepared.directory().resolve("install.log").toFile()).start();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) {
            if (Files.exists(prepared.directory().resolve("ready"))) return process;
            Thread.sleep(50);
        }
        process.destroy();
        throw new IOException("Installer could not start; game has not been closed");
    }
    public static String message(Throwable e) {
        String m = e.getMessage(); return m == null ? e.getClass().getSimpleName() : m;
    }
}
