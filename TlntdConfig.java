package me.shimmy.tlntd;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class TlntdConfig {

    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("tlntd.json");
    private static final Gson GSON =
        new GsonBuilder().setPrettyPrinting().create();

    // ── Color / theme ─────────────────────────────────────────────────────────
    public int     red = 212, green = 160, blue = 23;
    public boolean useCustomColor = false;
    public boolean rgbMode        = false;
    /** 0=Gold 1=Cyan 2=Purple 3=Green 4=Coral 5=Pink 6=Custom */
    public int     themePreset    = 0;

    // ── Interface ─────────────────────────────────────────────────────────────
    public int     heartCount    = 40;
    public float   guiScale      = 1.0f;
    public int     particleCount = 60;
    public boolean navCollapsed  = false;
    /** 0=Hearts 1=Stars 2=Matrix 3=None */
    public int     backgroundStyle = 0;

    // ── Safety ────────────────────────────────────────────────────────────────
    public boolean safetyAdminStop  = false;
    public boolean detectStaff      = true;
    public int     safetyRangeLimit = 20;
    public boolean lockMouse        = true;
    public boolean tpsAutoPause     = false;
    public float   tpsThreshold     = 15.0f;

    // ── HUD ───────────────────────────────────────────────────────────────────
    public boolean hudWatermark = true;
    public boolean hudFPS       = true;
    public boolean hudTPS       = true;
    public boolean hudCoords    = false;
    public boolean hudPing      = false;
    public boolean hudDirection = false;
    public boolean hudBotStatus = true;
    public int     hudX         = 8, hudY = 8;
    public int     hudTheme     = 1;
    public float   hudOpacity   = 0.85f;

    // ── Anti-AFK ──────────────────────────────────────────────────────────────
    public boolean antiAFK         = false;
    public int     antiAFKInterval = 600;

    // ── Session / stats ───────────────────────────────────────────────────────
    public int     itemValueCoins    = 1000;
    public int     itemsCollected    = 0;
    public boolean sessionLimit      = false;
    public int     sessionLimitHours = 2;
    public boolean scheduledStopEnabled = false;
    public float   scheduledStopMinutes = 120f;

    // ── Misc ──────────────────────────────────────────────────────────────────
    public List<String> safetyWhitelist  = new ArrayList<>();
    public boolean fakeActivity          = false;
    public int     fakeActivityRate      = 120;
    public boolean autoFishEnabled       = false;
    public int     autoFishRecastDelay   = 10;
    public int     autoFishThreshold     = 12;
    public String  activeProfile         = "default";
    public String  lastSelectedBot       = "cane";
    public boolean cropAutoStart         = false;
    public boolean cropAutoSwitch        = false;

    // ── Crop rotations ────────────────────────────────────────────────────────
    public float caneYaw=0, canePitch=0;
    public float pumpkinYaw=0, pumpkinPitch=0;
    public float melonYaw=0, melonPitch=0;
    public float wheatYaw=0, wheatPitch=0;
    public float wartYaw=0, wartPitch=0;
    public float potatoYaw=0, potatoPitch=0;
    public float carrotYaw=0, carrotPitch=0;
    public float mushroomRedYaw=0,   mushroomRedPitch=0;
    public float mushroomBrownYaw=0, mushroomBrownPitch=0;
    public float moonflowerYaw=0, moonflowerPitch=0;
    public float sunflowerYaw=0, sunflowerPitch=0;
    public float wildroseYaw=0, wildrosePitch=0;
    public float cocoaYaw=0, cocoaPitch=0;
    public float cactusYaw=0, cactusPitch=0;

    private transient Map<String, List<StepData>> cropSteps;
    public JsonObject cropStepsJson = null;

    public static final String[] PREFIXES = {
        "cane","pumpkin","melon","wheat","wart","potato","carrot",
        "mushroom_red","mushroom_brown","moonflower","sunflower","wildrose","cocoa","cactus"
    };

    // ── Theme preset helpers ──────────────────────────────────────────────────
    public static final String[] PRESET_NAMES  = {"Gold","Cyan","Purple","Green","Coral","Pink","Custom"};
    public static final int[]    PRESET_COLORS = {
        0xFFD4A824, 0xFF28C4D4, 0xFFAA66FF,
        0xFF4AB868, 0xFFFF6644, 0xFFFF5599, 0xFFFFFFFF
    };

    public int resolveAccent(float rgbPhase) {
        if (rgbMode) return java.awt.Color.HSBtoRGB(rgbPhase, 0.75f, 0.95f);
        if (themePreset == 6 || (themePreset == 0 && useCustomColor))
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        return PRESET_COLORS[Math.min(themePreset, PRESET_COLORS.length - 1)];
    }

    // ── Steps ─────────────────────────────────────────────────────────────────
    private static List<StepData> defaultSteps(String prefix) {
        boolean twoStep = prefix.equals("cane") || prefix.equals("cactus");
        if (twoStep) return List.of(
            new StepData(20, 1, "S", true),
            new StepData(5,  1, "D", true)
        );
        return List.of(
            new StepData(20, 1, "S", true),
            new StepData(5,  1, "D", true),
            new StepData(20, 1, "W", true),
            new StepData(5,  1, "A", true)
        );
    }

    public List<StepData> getSteps(String prefix) {
        ensureSteps();
        return cropSteps.computeIfAbsent(prefix, TlntdConfig::defaultSteps);
    }

    public void addStep(String prefix) {
        List<StepData> steps = new ArrayList<>(getSteps(prefix));
        steps.add(StepData.defaults());
        cropSteps.put(prefix, steps);
    }

    public void removeStep(String prefix, int index) {
        List<StepData> steps = new ArrayList<>(getSteps(prefix));
        if (steps.size() > 1 && index >= 0 && index < steps.size()) {
            steps.remove(index);
            cropSteps.put(prefix, steps);
        }
    }

    public float getYaw(String t) {
        return switch (t) {
            case "cane" -> caneYaw; case "pumpkin" -> pumpkinYaw;
            case "melon" -> melonYaw; case "wheat" -> wheatYaw;
            case "wart" -> wartYaw; case "potato" -> potatoYaw;
            case "mushroom_red" -> mushroomRedYaw; case "mushroom_brown" -> mushroomBrownYaw;
            case "moonflower" -> moonflowerYaw; case "sunflower" -> sunflowerYaw;
            case "wildrose" -> wildroseYaw; case "cocoa" -> cocoaYaw;
            case "cactus" -> cactusYaw; default -> carrotYaw;
        };
    }

    public float getPitch(String t) {
        return switch (t) {
            case "cane" -> canePitch; case "pumpkin" -> pumpkinPitch;
            case "melon" -> melonPitch; case "wheat" -> wheatPitch;
            case "wart" -> wartPitch; case "potato" -> potatoPitch;
            case "mushroom_red" -> mushroomRedPitch; case "mushroom_brown" -> mushroomBrownPitch;
            case "moonflower" -> moonflowerPitch; case "sunflower" -> sunflowerPitch;
            case "wildrose" -> wildrosePitch; case "cocoa" -> cocoaPitch;
            case "cactus" -> cactusPitch; default -> carrotPitch;
        };
    }

    public void setYaw(String t, float v) {
        switch (t) {
            case "cane" -> caneYaw=v; case "pumpkin" -> pumpkinYaw=v;
            case "melon" -> melonYaw=v; case "wheat" -> wheatYaw=v;
            case "wart" -> wartYaw=v; case "potato" -> potatoYaw=v;
            case "mushroom_red" -> mushroomRedYaw=v; case "mushroom_brown" -> mushroomBrownYaw=v;
            case "moonflower" -> moonflowerYaw=v; case "sunflower" -> sunflowerYaw=v;
            case "wildrose" -> wildroseYaw=v; case "cocoa" -> cocoaYaw=v;
            case "cactus" -> cactusYaw=v; default -> carrotYaw=v;
        }
    }

    public void setPitch(String t, float v) {
        switch (t) {
            case "cane" -> canePitch=v; case "pumpkin" -> pumpkinPitch=v;
            case "melon" -> melonPitch=v; case "wheat" -> wheatPitch=v;
            case "wart" -> wartPitch=v; case "potato" -> potatoPitch=v;
            case "mushroom_red" -> mushroomRedPitch=v; case "mushroom_brown" -> mushroomBrownPitch=v;
            case "moonflower" -> moonflowerPitch=v; case "sunflower" -> sunflowerPitch=v;
            case "wildrose" -> wildrosePitch=v; case "cocoa" -> cocoaPitch=v;
            case "cactus" -> cactusPitch=v; default -> carrotPitch=v;
        }
    }

    public int     getTicks  (String t, int s) { List<StepData> l=getSteps(t); return s<l.size()?l.get(s).ticks:20; }
    public void    setTicks  (String t, int s, int v) { List<StepData> l=getSteps(t); if(s<l.size()) l.get(s).ticks=v; }
    public String  getKey    (String t, int s) { List<StepData> l=getSteps(t); return s<l.size()?l.get(s).key:"S"; }
    public void    setKey    (String t, int s, String k) { List<StepData> l=getSteps(t); if(s<l.size()) l.get(s).key=k; }
    public boolean isEnabled (String t, int s) { List<StepData> l=getSteps(t); return s<l.size()&&l.get(s).enabled; }
    public void    setEnabled(String t, int s, boolean v) { List<StepData> l=getSteps(t); if(s<l.size()) l.get(s).enabled=v; }
    public int     getHold   (String t, int s) { List<StepData> l=getSteps(t); return s<l.size()?l.get(s).hold:1; }
    public void    setHold   (String t, int s, int v) { List<StepData> l=getSteps(t); if(s<l.size()) l.get(s).hold=v; }

    private void ensureSteps() {
        if (cropSteps != null) return;
        cropSteps = new HashMap<>();
        if (cropStepsJson != null) {
            for (String prefix : PREFIXES) {
                if (cropStepsJson.has(prefix)) {
                    JsonArray arr = cropStepsJson.getAsJsonArray(prefix);
                    List<StepData> list = new ArrayList<>();
                    for (JsonElement el : arr) list.add(GSON.fromJson(el, StepData.class));
                    if (!list.isEmpty()) cropSteps.put(prefix, list);
                }
            }
        }
        for (String prefix : PREFIXES)
            cropSteps.computeIfAbsent(prefix, TlntdConfig::defaultSteps);
    }

    private void packStepsToJson() {
        if (cropSteps == null) return;
        cropStepsJson = new JsonObject();
        for (Map.Entry<String, List<StepData>> e : cropSteps.entrySet())
            cropStepsJson.add(e.getKey(), GSON.toJsonTree(e.getValue()));
    }

    public static TlntdConfig load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) { TlntdConfig c = new TlntdConfig(); c.save(); return c; }
        try (FileReader r = new FileReader(file)) {
            TlntdConfig loaded = GSON.fromJson(r, TlntdConfig.class);
            if (loaded == null) return new TlntdConfig();
            loaded.ensureSteps();
            return loaded;
        } catch (IOException e) { return new TlntdConfig(); }
    }

    public void save() {
        packStepsToJson();
        try (FileWriter w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Profile system ────────────────────────────────────────────────────────
    private static Path profilePath(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve("tlntd_" + name + ".json");
    }

    public static List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        names.add("default");
        File dir = FabricLoader.getInstance().getConfigDir().toFile();
        File[] files = dir.listFiles((d, n) -> n.startsWith("tlntd_") && n.endsWith(".json"));
        if (files != null) { Arrays.sort(files); for (File f : files) names.add(f.getName().replace("tlntd_","").replace(".json","")); }
        return names;
    }

    public void saveAs(String name) {
        if (name.equals("default")) { save(); return; }
        packStepsToJson();
        try (FileWriter w = new FileWriter(profilePath(name).toFile())) { GSON.toJson(this, w); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public static TlntdConfig loadProfile(String name) {
        if (name.equals("default") || name.isEmpty()) return load();
        File f = profilePath(name).toFile();
        if (!f.exists()) return load();
        try (FileReader r = new FileReader(f)) {
            TlntdConfig loaded = GSON.fromJson(r, TlntdConfig.class);
            if (loaded == null) return new TlntdConfig();
            loaded.ensureSteps(); loaded.activeProfile = name; return loaded;
        } catch (IOException e) { return new TlntdConfig(); }
    }

    public static boolean deleteProfile(String name) {
        if (name.equals("default")) return false;
        return profilePath(name).toFile().delete();
    }

    public String toBase64() {
        packStepsToJson();
        byte[] bytes = GSON.toJson(this).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    public static TlntdConfig fromBase64(String b64) {
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(b64.trim());
            String json  = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            TlntdConfig c = GSON.fromJson(json, TlntdConfig.class);
            if (c != null) { c.ensureSteps(); return c; }
        } catch (Exception ignored) {}
        return null;
    }
}
