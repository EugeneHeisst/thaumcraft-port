package dev.eugheist.portupdates.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Each port embeds a copy; the first installed copy alone owns the shared menu. */
@EventBusSubscriber(modid = "tc4modern", value = Dist.CLIENT)
public final class PortUpdates {
    private static final String SELF = "tc4modern";
    private static final UpdateService SERVICE = new UpdateService();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Port update worker"); t.setDaemon(true); return t;
    });
    private static volatile UpdateService.Check check = new UpdateService.Check(List.of(), List.of());
    private static volatile boolean checking, downloading;
    private static volatile String progress = "";
    private static volatile UpdateService.Prepared prepared;
    private static boolean started;
    private static Button menuButton;
    private static Boolean autoEnabled;
    private static String menuLabel = "";
    private static Path game() { return Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize(); }
    private static boolean ru() { return Minecraft.getInstance().options.languageCode.startsWith("ru"); }
    private static net.minecraft.network.chat.MutableComponent text(String ru, String en) { return Component.literal(ru() ? ru : en); }
    private static String words(String ru, String en) { return ru() ? ru : en; }
    static boolean ownsMenu() {
        for (String id : UpdateService.REPOS.keySet().stream().sorted().toList()) {
            var file = ModList.get().getModFileById(id);
            if (file != null && Files.exists(file.getFile().findResource("dev/eugheist/portupdates/" + id + "/PortUpdates.class")))
                return SELF.equals(id);
        }
        return false;
    }
    private static List<UpdateService.Installed> installed() {
        return ModList.get().getMods().stream().map(m -> new UpdateService.Installed(
            m.getModId(), m.getDisplayName().replace(" UNOFFICIAL PORT", ""), m.getVersion().toString(),
            m.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize(), m.getDependencies().stream().map(d ->
                new UpdateService.Dependency(d.getModId(), d.getVersionRange().toString(), d.getType().name(), d.getSide().name())).toList())).toList();
    }
    private static Path settings() { return game().resolve("config/port-updates.properties"); }
    private static boolean automatic() {
        if (autoEnabled != null) return autoEnabled;
        Properties p = new Properties();
        if (Files.exists(settings())) try (InputStream in = Files.newInputStream(settings())) { p.load(in); } catch (IOException ignored) {}
        autoEnabled = Boolean.parseBoolean(p.getProperty("checkOnStartup", "true"));
        return autoEnabled;
    }
    private static void automatic(boolean enabled) throws IOException {
        Properties p = new Properties(); p.setProperty("checkOnStartup", Boolean.toString(enabled));
        Files.createDirectories(settings().getParent());
        try (OutputStream out = Files.newOutputStream(settings())) { p.store(out, "Only checks are automatic. Installation always requires a click."); }
        autoEnabled = enabled;
    }
    private static void refresh() {
        if (checking || downloading || prepared != null) return;
        checking = true;
        List<UpdateService.Installed> snapshot = installed();
        WORKER.submit(() -> {
            try { check = SERVICE.check(snapshot); }
            finally { checking = false; }
        });
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !ownsMenu()) return;
        menuButton = new UpdateIconButton(b -> Minecraft.getInstance().setScreen(new UpdatesScreen(event.getScreen())));
        positionMenuButton(event.getScreen().children(), event.getScreen());
        updateMenuLabel();
        event.addListener(menuButton);
        if (!started) { started = true; if (automatic()) refresh(); }
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen && menuButton != null && ownsMenu()) {
            positionMenuButton(event.getScreen().children(), event.getScreen());
            updateMenuLabel();
        }
    }
    private static void positionMenuButton(java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> widgets, Screen screen) {
        String language = Component.translatable("options.language").getString();
        for (var listener : widgets) {
            if (listener instanceof net.minecraft.client.gui.components.AbstractWidget widget
                    && widget != menuButton && widget.getMessage().getString().equals(language)) {
                menuButton.setSize(widget.getWidth(), widget.getHeight());
                menuButton.setPosition(widget.getX(), widget.getY() - widget.getHeight() - 4);
                return;
            }
        }
        // Vanilla/NeoForge position when a menu mod replaces the language widget.
        menuButton.setPosition(screen.width / 2 - 124, screen.height / 4 + (Minecraft.getInstance().isDemo() ? 92 : 114));
    }
    private static void updateMenuLabel() {
        Component description = label();
        if (!menuLabel.equals(description.getString()) || menuButton.getTooltip() == null) {
            menuLabel = description.getString();
            menuButton.setMessage(description);
            menuButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(description));
        }
    }
    /** Native button background and focus/narration; compact download icon instead of clipped text. */
    private static final class UpdateIconButton extends Button {
        UpdateIconButton(OnPress press) { super(0, 0, 20, 20, label(), press, DEFAULT_NARRATION); }
        @Override public void renderString(GuiGraphics graphics, net.minecraft.client.gui.Font font, int color) {
            int x = getX() + getWidth() / 2, y = getY() + getHeight() / 2;
            graphics.fill(x - 1, y - 6, x + 1, y, color);
            graphics.fill(x - 4, y - 2, x + 4, y, color);
            graphics.fill(x - 3, y, x + 3, y + 1, color);
            graphics.fill(x - 2, y + 1, x + 2, y + 2, color);
            graphics.fill(x - 1, y + 2, x + 1, y + 3, color);
            graphics.fill(x - 5, y + 3, x - 3, y + 6, color);
            graphics.fill(x + 3, y + 3, x + 5, y + 6, color);
            graphics.fill(x - 5, y + 5, x + 5, y + 6, color);
            if (!check.updates().isEmpty()) graphics.fill(getX() + getWidth() - 5, getY() + 2,
                getX() + getWidth() - 2, getY() + 5, (color & 0xFF000000) | 0x70FF70);
        }
    }
    private static Component label() {
        return text("Обновления портов", "Port updates").append(checking ? " …" : check.updates().isEmpty() ? "" : " (" + check.updates().size() + ")");
    }
    private static List<Component> previousResult() {
        Path result = game().resolve("port-updates/last-result.properties");
        if (!Files.exists(result)) return List.of();
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(result)) { p.load(in); }
        catch (IOException e) { return List.of(); }
        return switch (p.getProperty("state", "")) {
            case "SUCCESS" -> List.of(text("Предыдущее обновление установлено.", "Previous update installed successfully.").withStyle(net.minecraft.ChatFormatting.GREEN));
            case "ROLLED_BACK" -> List.of(text("Сбой установки. Предыдущие версии восстановлены.", "Installation failed. Previous versions restored.").withStyle(net.minecraft.ChatFormatting.RED));
            case "FAILED", "RECOVERY_NEEDED" -> List.of(
                text("Не удалось завершить установку. Файлы и журнал:", "Installation did not finish. Files and log:").withStyle(net.minecraft.ChatFormatting.RED),
                Component.literal(p.getProperty("transaction", "port-updates")));
            default -> List.of();
        };
    }
    private static final class UpdatesScreen extends Screen {
        private final Screen parent;
        private Button install, refresh, auto;
        private String error = "";
        private boolean launching;
        private int scroll;
        private final List<Component> lastResult = previousResult();
        UpdatesScreen(Screen parent) { super(text("Обновления портов", "Port updates")); this.parent = parent; }
        @Override protected void init() {
            int w = Math.min(310, width - 16), x = (width - w) / 2;
            install = addRenderableWidget(Button.builder(Component.empty(), b -> install()).bounds(x, height - 94, w, 20).build());
            refresh = addRenderableWidget(Button.builder(text("Проверить ещё раз", "Check again"), b -> { error = ""; PortUpdates.refresh(); })
                .bounds(x, height - 70, w, 20).build());
            auto = addRenderableWidget(Button.builder(Component.empty(), b -> {
                try { automatic(!automatic()); } catch (IOException e) { error = UpdateService.message(e); }
            }).bounds(x, height - 46, w, 20).build());
            addRenderableWidget(Button.builder(text("Назад", "Back"), b -> onClose()).bounds(x, height - 22, w, 20).build());
            tick();
        }
        @Override public void tick() {
            install.setMessage(launching ? text("Подготовка выхода…", "Preparing to exit…") : downloading ? text("Скачивание…", "Downloading…") : prepared != null
                ? text("Закрыть игру и установить", "Exit game and install") : text("Скачать и установить", "Download and install"));
            install.active = !launching && !checking && !downloading && (prepared != null || !check.updates().isEmpty());
            refresh.active = !launching && !checking && !downloading && prepared == null;
            auto.active = !launching;
            auto.setMessage(text("Проверка при запуске: ", "Check on startup: ").append(automatic() ? words("вкл.", "on") : words("выкл.", "off")));
        }
        private void install() {
            if (launching || checking || downloading) return;
            error = "";
            if (prepared != null) {
                launching = true;
                WORKER.submit(() -> {
                    try {
                        SERVICE.startInstaller(prepared);
                        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
                    } catch (Exception e) {
                        Minecraft.getInstance().execute(() -> { error = UpdateService.message(e); launching = false; });
                    }
                });
                return;
            }
            downloading = true;
            List<UpdateService.Installed> snapshot = installed();
            List<UpdateService.Release> releases = check.updates();
            Path game = game();
            String loader = FMLLoader.versionInfo().fmlVersion();
            WORKER.submit(() -> {
                try { prepared = SERVICE.prepare(game, snapshot, releases, loader, p -> progress = p); }
                catch (Exception e) { Minecraft.getInstance().execute(() -> error = UpdateService.message(e)); }
                finally { progress = ""; downloading = false; }
            });
        }
        @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
            scroll = Math.max(0, scroll - (int) Math.signum(vertical) * 18); return true;
        }
        @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
            int top = 29, bottom = height - 133;
            g.enableScissor(6, top, width - 6, Math.max(top + 1, bottom));
            List<Component> lines = new ArrayList<>(lastResult);
            if (checking) lines.add(text("Проверяем GitHub Releases…", "Checking GitHub Releases…"));
            else if (check.updates().isEmpty()) lines.add(check.errors().isEmpty()
                ? text(started && automatic() ? "Доступных обновлений нет." : "Нажмите «Проверить ещё раз».", "No updates listed. Use Check again.")
                : text("Не удалось проверить часть модов.", "Some mods could not be checked."));
            for (var r : check.updates()) {
                lines.add(Component.literal(r.installed().name()));
                lines.add(Component.literal("  " + r.installed().version() + " → " + r.version()));
            }
            for (String e : check.errors()) lines.add(Component.literal(e).withStyle(net.minecraft.ChatFormatting.RED));
            if (!error.isEmpty()) lines.add(Component.literal(error).withStyle(net.minecraft.ChatFormatting.RED));
            int actualLines = 0;
            for (Component c : lines) actualLines += font.split(c, width - 24).size();
            scroll = Math.min(scroll, Math.max(0, actualLines * 11 - Math.max(1, bottom - top)));
            int y = top - scroll;
            for (Component line : lines) for (var part : font.split(line, width - 24)) { g.drawString(font, part, 12, y, 0xDDDDDD); y += 11; }
            g.disableScissor();
            g.drawCenteredString(font, text("После установки откройте игру снова.", "Open the game again after installation."), width / 2, height - 126, 0xBBBBBB);
            g.drawCenteredString(font, text("Версии модов должны совпадать с сервером.", "Mod versions must match your server."), width / 2, height - 115, 0xBBBBBB);
            if (downloading) g.drawCenteredString(font, Component.literal(progress), width / 2, height - 104, 0xFFFF88);
            else if (prepared != null) g.drawCenteredString(font, text("Файлы проверены. Можно устанавливать.", "Files verified. Ready to install."), width / 2, height - 104, 0x88FF88);
        }
        @Override public void onClose() { if (!launching) minecraft.setScreen(parent); }
    }
}
