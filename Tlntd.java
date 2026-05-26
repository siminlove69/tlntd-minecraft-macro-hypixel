package me.shimmy.tlntd;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Tlntd implements ClientModInitializer {

    // ── Toast notification (plain static class, not a record) ─────────────────
    public static final class Toast {
        private final String msg;
        private final int    color;
        private final long   expireMs;
        public Toast(String msg, int color, long expireMs) {
            this.msg = msg; this.color = color; this.expireMs = expireMs;
        }
        public String msg()      { return msg; }
        public int    color()    { return color; }
        public long   expireMs() { return expireMs; }
    }
    public static final java.util.Deque<Toast> toasts = new java.util.ArrayDeque<>();
    public static void toast(String msg, int col) {
        toasts.addLast(new Toast(msg, col, System.currentTimeMillis() + 2500));
        while (toasts.size() > 3) toasts.pollFirst();
    }

    // ── Structured event log ──────────────────────────────────────────────────
    // entries: [type, message, timestamp]  type = START | STOP | SAFETY | INFO
    public static final List<String[]> eventLog = new ArrayList<>();
    public static void addEvent(String type, String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        eventLog.add(0, new String[]{type, msg, ts});
        if (eventLog.size() > 60) eventLog.remove(eventLog.size() - 1);
    }

    // ── Session stats ─────────────────────────────────────────────────────────
    public static int  sessionCropsCollected = 0;
    public static long sessionStartTime      = 0;   // ms, public for footer
    public static long totalFarmingMs        = 0;   // accumulated run time this game session

    // ── Bot state ────────────────────────────────────────────────────────────
    public static boolean isRunning     = false;
    public static String  activeBotType = "none";
    public static int     currentStep   = 0;
    public static int     tickCounter   = 0;

    // ── Macro switch ─────────────────────────────────────────────────────────
    public static volatile boolean isSwitching = false;
    public static volatile String  switchingTo  = "";
    private static final ScheduledExecutorService switchExecutor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TLNTD-MacroSwitch"); t.setDaemon(true); return t;
        });

    // ── Logs ─────────────────────────────────────────────────────────────────
    public static final List<String> consoleLog = new ArrayList<>();

    // ── Keys ─────────────────────────────────────────────────────────────────
    private static net.minecraft.client.option.KeyBinding menuKey;
    private static net.minecraft.client.option.KeyBinding toggleKey;

    // ── Stats ─────────────────────────────────────────────────────────────────
    public static int   totalStepsDone  = 0;
    public static long  sessionStartMs  = 0;
    public static final java.util.Deque<long[]>  stepHistory  = new java.util.ArrayDeque<>();
    public static final List<String[]>            cropHistory  = new ArrayList<>();
    private static String  lastCropType    = "none";
    private static long    lastCropStartMs = 0;

    // ── Internals ─────────────────────────────────────────────────────────────
    private static int     fakeActivityTimer = 0;
    private static boolean originalPause     = true;
    private static int     safetyTimer       = 0;
    private static int     holdTicksLeft     = 0;
    private static final Random rng              = new Random();
    private static int     antiAFKTimer          = 0;
    private static int     antiAFKHoldLeft       = 0;
    private static boolean antiAFKHolding        = false;
    private static boolean wasRunningBeforeMenu  = false;
    private static boolean paused               = false;
    private static boolean tpsPaused            = false;

    public static boolean isPaused() { return paused; }

    @Override
    public void onInitializeClient() {
        menuKey = new KeyBinding("key.tlntd.menu",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, KeyBinding.Category.MISC);
        toggleKey = new KeyBinding("key.tlntd.toggle",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, KeyBinding.Category.MISC);
        KeyBindingHelper.registerKeyBinding(menuKey);
        KeyBindingHelper.registerKeyBinding(toggleKey);

        HudRenderCallback.EVENT.register(new TlntdHud());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // ── Suppress ESC/game-menu while bot is running or switching ────
            if ((isRunning || isSwitching) &&
                client.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen) {
                client.setScreen(null);
            }

            // ── Menu key ────────────────────────────────────────────────────
            while (menuKey.wasPressed()) {
                if (client.currentScreen instanceof TlntdMenu) {
                    client.setScreen(null);
                    if (wasRunningBeforeMenu && !isRunning) resumeBot(client);
                } else {
                    wasRunningBeforeMenu = isRunning;
                    if (isRunning) pauseBot(client);
                    client.setScreen(new TlntdMenu());
                }
            }

            boolean menuOpen = client.currentScreen instanceof TlntdMenu;
            if (menuOpen && isRunning) { pauseBot(client); wasRunningBeforeMenu = true; }

            if (client.player == null || client.world == null) return;

            // ── Toggle key ──────────────────────────────────────────────────
            while (toggleKey.wasPressed()) {
                if (isRunning) {
                    stopBot(client);
                    client.player.sendMessage(Text.literal("§7[TLNTD] Bot stopped."), true);
                } else {
                    TlntdConfig cfg0 = TlntdConfig.load();
                    startBot(cfg0.lastSelectedBot, client);
                    client.player.sendMessage(
                        Text.literal("§7[TLNTD] Started: §f" + cfg0.lastSelectedBot), true);
                }
            }

            TlntdConfig cfg = TlntdConfig.load();

            // ── Anti-AFK ────────────────────────────────────────────────────
            if (cfg.antiAFK && !isRunning) {
                if (antiAFKHolding) {
                    if (antiAFKHoldLeft > 0) {
                        if (client.options.jumpKey != null) client.options.jumpKey.setPressed(true);
                        antiAFKHoldLeft--;
                    } else {
                        if (client.options.jumpKey != null) client.options.jumpKey.setPressed(false);
                        antiAFKHolding = false; antiAFKTimer = 0;
                    }
                } else {
                    if (++antiAFKTimer >= cfg.antiAFKInterval) {
                        antiAFKHolding = true;
                        antiAFKHoldLeft = 2 + rng.nextInt(7);
                        antiAFKTimer = 0;
                        log("Anti-AFK jump (" + antiAFKHoldLeft + "t)");
                    }
                }
            } else { antiAFKTimer = 0; antiAFKHolding = false; }

            // ── Fake activity ────────────────────────────────────────────────
            if (cfg.fakeActivity && paused && client.player != null) {
                if (++fakeActivityTimer >= cfg.fakeActivityRate) {
                    fakeActivityTimer = 0;
                    float yawNudge = (rng.nextFloat() - 0.5f) * 0.6f;
                    client.player.setYaw(client.player.getYaw() + yawNudge);
                }
            }

            // ── Legacy session limit (hours) ─────────────────────────────────
            if (isRunning && cfg.sessionLimit && sessionStartTime > 0) {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                if (elapsed >= cfg.sessionLimitHours * 3_600_000L) {
                    stopBot(client);
                    log("Session limit reached (" + cfg.sessionLimitHours + "h)");
                    addEvent("INFO", "Session limit: " + cfg.sessionLimitHours + "h");
                    toast("⏱ Session limit reached", 0xFFFFDD44);
                    if (client.player != null)
                        client.player.sendMessage(Text.literal("§c[TLNTD] Session limit — stopped."), true);
                }
            }

            // ── Scheduled stop (minutes) ─────────────────────────────────────
            if (isRunning && cfg.scheduledStopEnabled && sessionStartTime > 0) {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                long limit   = (long)(cfg.scheduledStopMinutes * 60_000L);
                if (elapsed >= limit) {
                    int m = (int)cfg.scheduledStopMinutes;
                    stopBot(client);
                    log("Scheduled stop after " + m + "m");
                    addEvent("INFO", "Scheduled stop: " + m + "m");
                    toast("⏱ Scheduled stop: " + m + "m", 0xFFFFDD44);
                    if (client.player != null)
                        client.player.sendMessage(Text.literal("§e[TLNTD] Scheduled stop."), true);
                }
            }

            if (!isRunning) return;

            // ── Panic stop ───────────────────────────────────────────────────
            if (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_DELETE)) {
                stopBot(client);
                log("PANIC STOP");
                addEvent("SAFETY", "Panic stop (DELETE)");
                toast("🛑 PANIC STOP", 0xFFCC3A3A);
                client.player.sendMessage(Text.literal("§c[TLNTD] Panic stop!"), true);
                return;
            }

            // ── Safety check (every 5 ticks) ──────────────────────────────────
            if (++safetyTimer >= 5) {
                safetyTimer = 0;
                if (cfg.safetyAdminStop && client.world != null) {
                    for (PlayerEntity p : client.world.getPlayers()) {
                        if (p == null || p == client.player) continue;
                        if (p.distanceTo(client.player) >= cfg.safetyRangeLimit) continue;
                        String pName = p.getName().getString();
                        if (cfg.safetyWhitelist.contains(pName)) continue;
                        stopBot(client);
                        addEvent("SAFETY", "Player nearby: " + pName);
                        toast("⚠ Player nearby — stopped!", 0xFFFF8844);
                        client.player.sendMessage(Text.literal("§c[TLNTD] Player nearby — stopped!"), false);
                        return;
                    }
                    if (cfg.detectStaff && client.getNetworkHandler() != null) {
                        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                            if (isHypixelStaff(entry)) {
                                stopBot(client);
                                addEvent("SAFETY", "Staff in tab list");
                                toast("⚠ Staff detected — stopped!", 0xFFCC3A3A);
                                client.player.sendMessage(Text.literal("§4[TLNTD] §cStaff detected — stopped!"), false);
                                log("Staff detected in tab list.");
                                return;
                            }
                        }
                    }
                }
            }
            if (!isRunning) return;

            // ── TPS auto-pause ───────────────────────────────────────────────
            if (cfg.tpsAutoPause && client.world != null) {
                float tps = client.world.getTickManager().getTickRate();
                if (tps < cfg.tpsThreshold && !paused && !tpsPaused) {
                    tpsPaused = true;
                    pauseBot(client);
                    addEvent("SAFETY", String.format("TPS auto-pause: %.1f", tps));
                    toast(String.format("⚠ TPS %.1f — paused", tps), 0xFFFFDD44);
                } else if (tps >= cfg.tpsThreshold && tpsPaused) {
                    tpsPaused = false;
                    resumeBot(client);
                    addEvent("INFO", String.format("TPS recovered: %.1f", tps));
                    toast(String.format("✓ TPS %.1f — resumed", tps), 0xFF4AB868);
                }
            } else { tpsPaused = false; }

            if (!isRunning) return;

            // ── Bot tick ─────────────────────────────────────────────────────
            if (client.options.attackKey != null) client.options.attackKey.setPressed(true);

            if (!cfg.isEnabled(activeBotType, currentStep)) { advanceStep(client); return; }

            int maxTicks  = cfg.getTicks(activeBotType, currentStep);
            int holdTicks = Math.min(cfg.getHold(activeBotType, currentStep), maxTicks);

            if (holdTicksLeft > 0) {
                KeyBinding key = resolveKey(client, cfg.getKey(activeBotType, currentStep));
                if (key != null) key.setPressed(true);
                holdTicksLeft--;
            }

            if (++tickCounter >= maxTicks) advanceStep(client);
            if (tickCounter == 0) holdTicksLeft = holdTicks;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static boolean isHypixelStaff(PlayerListEntry entry) {
        try {
            net.minecraft.text.Text display = entry.getDisplayName();
            if (display == null) return false;
            String raw = display.getString().toUpperCase();
            return raw.contains("[ADMIN]")  || raw.contains("[MOD]")    ||
                   raw.contains("[HELPER]") || raw.contains("[OWNER]")  ||
                   raw.contains("[GM]")     || raw.contains("[STAFF]")  ||
                   raw.contains("[YMCA]")   || raw.contains("[PIG+++]") ||
                   raw.contains("[EVENTS]") || raw.contains("[BUILD]");
        } catch (Exception e) { return false; }
    }

    private static void pauseBot(MinecraftClient client) {
        if (!isRunning || paused) return;
        paused = true;
        resetKeys(client);
        if (client.options != null) {
            if (client.options.attackKey != null) client.options.attackKey.setPressed(false);
            if (client.options.useKey    != null) client.options.useKey.setPressed(false);
        }
        log("Bot paused.");
    }

    public static void resumeBot(MinecraftClient client) {
        if (!paused) return;
        paused = false;
        tickCounter   = 0;
        holdTicksLeft = 0;
        log("Bot resumed.");
    }

    private static void advanceStep(MinecraftClient client) {
        resetKeys(client);
        tickCounter = 0;
        currentStep++;
        TlntdConfig cfg = TlntdConfig.load();
        int maxStep = cfg.getSteps(activeBotType).size();
        if (currentStep >= maxStep) {
            currentStep = 0;
            sessionCropsCollected++;
            cfg.itemsCollected++;
        }
        holdTicksLeft = Math.min(cfg.getHold(activeBotType, currentStep),
                                 cfg.getTicks(activeBotType, currentStep));
        totalStepsDone++;
        long now = System.currentTimeMillis();
        stepHistory.addLast(new long[]{now, currentStep});
        while (!stepHistory.isEmpty() && now - stepHistory.peekFirst()[0] > 60_000)
            stepHistory.pollFirst();
    }

    private static KeyBinding resolveKey(MinecraftClient client, String key) {
        if (key == null || client.options == null) return null;
        return switch (key.toUpperCase()) {
            case "W" -> client.options.forwardKey;
            case "S" -> client.options.backKey;
            case "A" -> client.options.leftKey;
            case "D" -> client.options.rightKey;
            default  -> null;
        };
    }

    private static void resetKeys(MinecraftClient client) {
        if (client.options == null) return;
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }

    public static void log(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        consoleLog.add(0, "[" + ts + "] " + msg);
        if (consoleLog.size() > 80) consoleLog.remove(consoleLog.size() - 1);
    }

    public static void startBot(String type, MinecraftClient client) {
        if (client == null || client.options == null) return;
        originalPause = client.options.pauseOnLostFocus;
        client.options.pauseOnLostFocus = false;
        activeBotType = type;
        isRunning     = true;
        paused        = false;
        tpsPaused     = false;
        tickCounter   = 0;
        currentStep   = 0;
        sessionCropsCollected = 0;
        if (client.player != null) {
            TlntdConfig cfg = TlntdConfig.load();
            client.player.setYaw(cfg.getYaw(type));
            client.player.setPitch(cfg.getPitch(type));
            if (!cfg.getSteps(type).isEmpty())
                holdTicksLeft = Math.min(cfg.getHold(type, 0), cfg.getTicks(type, 0));
        }
        lastCropType     = type;
        lastCropStartMs  = System.currentTimeMillis();
        sessionStartTime = System.currentTimeMillis();
        sessionStartMs   = sessionStartTime;
        totalStepsDone   = 0;
        stepHistory.clear();
        String label = type.replace("_", " ").toUpperCase();
        log("Bot started: " + label);
        addEvent("START", "Started: " + label);
        toast("▶ " + label, 0xFF4AB868);
    }

    public static int stepsPerMinute() {
        long cutoff = System.currentTimeMillis() - 60_000;
        int count = 0;
        for (long[] e : stepHistory) if (e[0] >= cutoff) count++;
        return count;
    }

    public static float getAntiAFKProgress() {
        TlntdConfig cfg = TlntdConfig.load();
        int interval = Math.max(1, cfg.antiAFKInterval);
        return MathHelper.clamp((float) antiAFKTimer / interval, 0f, 1f);
    }

    /**
     * Soft-stop: releases keys and stops the bot WITHOUT restoring pauseOnLostFocus.
     * Used internally during crop switching so alt-tab doesn't open the ESC menu.
     */
    private static void softStop(MinecraftClient client) {
        isRunning     = false;
        paused        = false;
        tpsPaused     = false;
        holdTicksLeft = 0;
        // Accumulate farming time
        if (sessionStartTime > 0) {
            totalFarmingMs += System.currentTimeMillis() - sessionStartTime;
            sessionStartTime = 0;
        }
        // Keep pauseOnLostFocus = false so alt-tab doesn't pause during switch window
        if (client != null && client.options != null) {
            resetKeys(client);
            if (client.options.attackKey != null) client.options.attackKey.setPressed(false);
            if (client.options.useKey    != null) client.options.useKey.setPressed(false);
        }
        if (!lastCropType.equals("none") && lastCropStartMs > 0) {
            long endMs = System.currentTimeMillis();
            cropHistory.add(0, new String[]{lastCropType, String.valueOf(lastCropStartMs), String.valueOf(endMs)});
            if (cropHistory.size() > 5) cropHistory.remove(cropHistory.size() - 1);
            lastCropType    = "none";
            lastCropStartMs = 0;
        }
    }

    public static void switchBot(String newType, MinecraftClient client) {
        if (isSwitching) return;
        String prevType = activeBotType;

        // Soft stop: keeps pauseOnLostFocus = false during the 1-second switch window
        softStop(client);
        isSwitching = true;
        switchingTo = newType;
        wasRunningBeforeMenu = false;
        log("Switching: " + prevType.toUpperCase() + " → " + newType.toUpperCase() + " (1s cooldown)");
        addEvent("INFO", "Switching: " + prevType + " → " + newType);
        toast("⟳ " + newType.replace("_"," ").toUpperCase(), 0xFFFFDD44);

        if (client != null && client.player != null) {
            client.player.sendMessage(
                Text.literal("§7[TLNTD] §fSwitching to §e"
                    + newType.replace("_", " ").toUpperCase() + " §7in 1s…"), true);
        }

        switchExecutor.schedule(() -> {
            if (client != null) {
                client.execute(() -> {
                    isSwitching = false;
                    switchingTo = "";
                    TlntdConfig cfg2 = TlntdConfig.load();
                    cfg2.lastSelectedBot = newType;
                    cfg2.save();
                    startBot(newType, client);
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("§7[TLNTD] §aStarted: §f"
                                + newType.replace("_", " ").toUpperCase()), true);
                    }
                });
            }
        }, 1, TimeUnit.SECONDS);
    }

    public static void stopBot(MinecraftClient client) {
        isRunning     = false;
        paused        = false;
        tpsPaused     = false;
        activeBotType = "none";
        holdTicksLeft = 0;
        wasRunningBeforeMenu = false;
        // Accumulate farming time
        if (sessionStartTime > 0) {
            totalFarmingMs += System.currentTimeMillis() - sessionStartTime;
            sessionStartTime = 0;
        }
        if (client != null && client.options != null) {
            client.options.pauseOnLostFocus = originalPause;
            resetKeys(client);
            if (client.options.attackKey != null) client.options.attackKey.setPressed(false);
        }
        if (!lastCropType.equals("none") && lastCropStartMs > 0) {
            long endMs = System.currentTimeMillis();
            cropHistory.add(0, new String[]{lastCropType, String.valueOf(lastCropStartMs), String.valueOf(endMs)});
            if (cropHistory.size() > 5) cropHistory.remove(cropHistory.size() - 1);
            lastCropType    = "none";
            lastCropStartMs = 0;
        }
        log("Bot stopped.");
        addEvent("STOP", "Bot stopped.");
        toast("■ Stopped", 0xFFCC3A3A);
    }
}
