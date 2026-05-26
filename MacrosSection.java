package me.shimmy.tlntd;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class MacrosSection implements IMenuSection {

    private final TlntdMenu parent;

    private int   scrollY    = 0;
    private float scrollAnim = 0f;

    private int   detailScrollY    = 0;
    private float detailScrollAnim = 0f;

    private String selPrefix = null;
    private String selTitle  = null;

    private String activeField = "";
    private String inputBuffer = "";
    private int draggingStep   = -1;

    private static final int SCROLL_SPEED = 14;
    private static final int CARD_GAP = 3;
    private static final int COLS     = 5;
    private static final int CARD_H   = 40;
    // step row constants — every hit zone is derived from these
    private static final int STEP_H   = 21;
    private static final int STEP_GAP = 2;
    private static final int GEN_HEADER_H = 12;  // groupHeader height
    private static final int GEN_ROW_H    = 13;  // each general toggle row

    private record Crop(String name, String prefix, ItemStack icon) {}
    private static final Crop[] CROPS = {
        new Crop("Cane",       "cane",          new ItemStack(Items.SUGAR_CANE)),
        new Crop("Pumpkin",    "pumpkin",        new ItemStack(Items.PUMPKIN)),
        new Crop("Melon",      "melon",          new ItemStack(Items.MELON_SLICE)),
        new Crop("Wart",       "wart",           new ItemStack(Items.NETHER_WART)),
        new Crop("Wheat",      "wheat",          new ItemStack(Items.WHEAT)),
        new Crop("Potato",     "potato",         new ItemStack(Items.POTATO)),
        new Crop("Carrot",     "carrot",         new ItemStack(Items.CARROT)),
        new Crop("Red Shroom", "mushroom_red",   new ItemStack(Items.RED_MUSHROOM)),
        new Crop("Brn Shroom", "mushroom_brown", new ItemStack(Items.BROWN_MUSHROOM)),
        new Crop("Moonflower", "moonflower",     new ItemStack(Items.LILY_OF_THE_VALLEY)),
        new Crop("Sunflower",  "sunflower",      new ItemStack(Items.SUNFLOWER)),
        new Crop("Wild Rose",  "wildrose",       new ItemStack(Items.ROSE_BUSH)),
        new Crop("Cocoa",      "cocoa",          new ItemStack(Items.COCOA_BEANS)),
        new Crop("Cactus",     "cactus",         new ItemStack(Items.CACTUS)),
    };

    private static final java.util.Map<String,Integer> CROP_COLORS = new java.util.HashMap<>();
    static {
        CROP_COLORS.put("cane",          0xFF44BB66);
        CROP_COLORS.put("pumpkin",       0xFFFF8822);
        CROP_COLORS.put("melon",         0xFFDD4444);
        CROP_COLORS.put("wart",          0xFF882222);
        CROP_COLORS.put("wheat",         0xFFDDAA33);
        CROP_COLORS.put("potato",        0xFFCC9944);
        CROP_COLORS.put("carrot",        0xFFFF6622);
        CROP_COLORS.put("mushroom_red",  0xFFCC4422);
        CROP_COLORS.put("mushroom_brown",0xFF886644);
        CROP_COLORS.put("moonflower",    0xFF8888FF);
        CROP_COLORS.put("sunflower",     0xFFFFDD00);
        CROP_COLORS.put("wildrose",      0xFFFF4488);
        CROP_COLORS.put("cocoa",         0xFF884422);
        CROP_COLORS.put("cactus",        0xFF22AA44);
    }
    private int cropColor(String p) { return CROP_COLORS.getOrDefault(p, UI.GOLD); }

    public MacrosSection(TlntdMenu p) { parent = p; }

    // ── grid top offset — shared between render and click ─────────────────
    // header(12) + row1(13) + row2(13) + gap(3) = 41
    private static final int GRID_TOP_OFFSET = GEN_HEADER_H + GEN_ROW_H + GEN_ROW_H + 3;

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta) {
        scrollAnim       = MathHelper.lerp(delta * 0.25f, scrollAnim,       scrollY);
        detailScrollAnim = MathHelper.lerp(delta * 0.25f, detailScrollAnim, detailScrollY);
        if (selPrefix == null) renderHub(ctx, x, y, w, mx, my, accent);
        else                   renderDetail(ctx, x, y, w, mx, my, accent);
    }

    // ── HUB ───────────────────────────────────────────────────────────────
    private void renderHub(DrawContext ctx, int x, int y, int w, int mx, int my, int accent) {
        // General toggles — genTop == y exactly
        int genTop = y;
        UI.groupHeader(ctx, x, genTop, w, "GENERAL", accent);

        // Row 1: auto-start  (y + 12)
        int r1y = genTop + GEN_HEADER_H;
        genRow(ctx, x, r1y, w, mx, my, "Auto-start bot when breaking a crop block",
               parent.config.cropAutoStart, accent);

        // Row 2: auto-switch  (y + 25)
        int r2y = r1y + GEN_ROW_H;
        genRow(ctx, x, r2y, w, mx, my, "Auto-switch macro on different crop break",
               parent.config.cropAutoSwitch, accent);

        // Crop grid  — starts at y + GRID_TOP_OFFSET = y+41
        int gridTop    = genTop + GRID_TOP_OFFSET;
        int gridBottom = y + parent.ch() - 18;
        int cardW      = (w - CARD_GAP * (COLS - 1)) / COLS;
        int totalRows  = (CROPS.length + COLS - 1) / COLS;
        int totalH     = totalRows * (CARD_H + CARD_GAP);
        int maxScroll  = Math.max(0, totalH - (gridBottom - gridTop));
        scrollY = MathHelper.clamp(scrollY, 0, maxScroll);
        int sy  = MathHelper.clamp((int) scrollAnim, 0, maxScroll);

        if (sy > 0)         UI.textCentered(ctx,"▲",x+w/2,gridTop-7,   UI.withAlpha(accent,0x88));
        if (sy < maxScroll) UI.textCentered(ctx,"▼",x+w/2,gridBottom+1,UI.withAlpha(accent,0x88));

        for (int i = 0; i < CROPS.length; i++) {
            int col  = i % COLS;
            int row  = i / COLS;
            int cx   = x + col * (cardW + CARD_GAP);
            int rawY = gridTop + row * (CARD_H + CARD_GAP) - sy;
            if (rawY + CARD_H <= gridTop || rawY >= gridBottom) continue;

            boolean hov     = UI.hovered(mx, my, cx, rawY, cardW, CARD_H);
            boolean running = Tlntd.isRunning && Tlntd.activeBotType.equals(CROPS[i].prefix());
            int cropC = cropColor(CROPS[i].prefix());

            // Base
            UI.fill(ctx, cx, rawY, cardW, CARD_H, UI.SURFACE);
            // Crop-tinted gradient top-band
            ctx.fillGradient(cx, rawY, cx+cardW, rawY+CARD_H/2,
                UI.withAlpha(cropC, running ? 0x30 : hov ? 0x1A : 0x0C), 0x00000000);
            // Top line: accent when running, hover-tint otherwise, dim when idle
            UI.hline(ctx, cx, rawY, cardW,
                running ? UI.withAlpha(cropC, 0xCC) :
                hov     ? UI.withAlpha(cropC, 0x66) : UI.BORDER);
            // Running dot
            if (running) UI.fill(ctx, cx+cardW-5, rawY+3, 4, 4, cropC);

            ctx.drawItem(CROPS[i].icon(), cx + cardW/2 - 8, rawY + 6);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(0.75f, 0.75f);
            int nc = running ? cropC : hov ? UI.WHITE : UI.GRAY;
            UI.textCentered(ctx, CROPS[i].name(),
                (int)((cx + cardW/2) / 0.75f), (int)((rawY + CARD_H - 9) / 0.75f), nc);
            ctx.getMatrices().popMatrix();
        }
    }

    private void genRow(DrawContext ctx, int x, int y, int w, int mx, int my,
                        String label, boolean val, int accent) {
        boolean h = UI.hovered(mx, my, x, y, w, GEN_ROW_H);
        if (h) UI.fill(ctx, x, y, w, GEN_ROW_H, UI.withAlpha(UI.WHITE, 0x06));
        if (val) {
            UI.fill(ctx, x, y, 2, GEN_ROW_H, UI.withAlpha(accent, 0x77));
        }
        UI.checkbox(ctx, x+8, y+2, val, accent);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.75f, 0.75f);
        UI.textShadow(ctx, label, (int)((x+21)/0.75f), (int)((y+3)/0.75f), UI.WHITE);
        ctx.getMatrices().popMatrix();
    }

    // ── DETAIL ────────────────────────────────────────────────────────────
    private void renderDetail(DrawContext ctx, int x, int y, int w, int mx, int my, int accent) {
        TlntdConfig cfg  = parent.config;
        List<StepData> steps = cfg.getSteps(selPrefix);
        int cropC = cropColor(selPrefix);

        // Back + title bar — y
        boolean backH = UI.hovered(mx, my, x, y, 38, 10);
        UI.fill(ctx, x, y, w, 10, UI.SURFACE);
        if (backH) UI.fill(ctx, x, y, 38, 10, UI.withAlpha(accent, 0x14));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.75f, 0.75f);
        UI.textShadow(ctx, "← BACK", (int)((x+6)/0.75f), (int)((y+1)/0.75f), backH ? accent : UI.GRAY);
        UI.textShadow(ctx, selTitle.toUpperCase(),
            (int)((x+46)/0.75f), (int)((y+1)/0.75f), cropC);
        ctx.getMatrices().popMatrix();

        // Rotation row — y+11
        int rotY = y + 11;
        UI.fill(ctx, x, rotY, w, 16, UI.SURFACE);
        UI.hline(ctx, x, rotY, w, UI.BORDER);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.65f, 0.65f);
        UI.text(ctx, "ROTATION", (int)((x+5)/0.65f), (int)((rotY+4)/0.65f),
            UI.withAlpha(accent, 0x99));
        ctx.getMatrices().popMatrix();
        int fldX = x + w - 112;
        boolean yawA   = activeField.equals("yaw");
        boolean pitchA = activeField.equals("pitch");
        inputField(ctx, fldX,    rotY+1, 50, yawA   ? inputBuffer+"▌" : (int)cfg.getYaw(selPrefix)+"°Y",   yawA,   accent, mx, my);
        inputField(ctx, fldX+54, rotY+1, 50, pitchA ? inputBuffer+"▌" : (int)cfg.getPitch(selPrefix)+"°P", pitchA, accent, mx, my);

        // Add Step — y+28
        int addY = rotY + 18;
        boolean addH = UI.hovered(mx, my, x, addY, w, 10);
        UI.fill(ctx, x, addY, w, 10,
            addH ? UI.withAlpha(UI.GREEN, 0x14) : UI.SURFACE);
        UI.hline(ctx, x, addY, w, addH ? UI.withAlpha(UI.GREEN, 0x55) : UI.BORDER);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.70f, 0.70f);
        UI.text(ctx, "+ Add Step", (int)((x+8)/0.70f), (int)((addY+2)/0.70f),
            addH ? UI.GREEN : UI.GRAY_D);
        ctx.getMatrices().popMatrix();

        // Steps — y+39
        int stepsTop    = addY + 11;
        int stepsBottom = y + parent.ch() - 4;
        int stepsViewH  = stepsBottom - stepsTop;
        int totalStepsH = steps.size() * (STEP_H + STEP_GAP);
        int maxDScroll  = Math.max(0, totalStepsH - stepsViewH);
        detailScrollY   = MathHelper.clamp(detailScrollY, 0, maxDScroll);
        int dsy         = MathHelper.clamp((int) detailScrollAnim, 0, maxDScroll);

        if (dsy > 0)          UI.textCentered(ctx,"▲",x+w/2,stepsTop-6,   UI.withAlpha(accent,0x88));
        if (dsy < maxDScroll) UI.textCentered(ctx,"▼",x+w/2,stepsBottom+1,UI.withAlpha(accent,0x88));

        for (int i = 0; i < steps.size(); i++) {
            int rawY = stepsTop + i * (STEP_H + STEP_GAP) - dsy;
            if (rawY + STEP_H <= stepsTop || rawY >= stepsBottom) continue;
            renderStep(ctx, x, rawY, w, mx, my, accent, i, steps);
        }
    }

    // ── STEP ROW  (21px) ──────────────────────────────────────────────────
    // Layout (all offsets from row y):
    //   [2]  index          0.65f text at y+7
    //   [16..36]  ON/OFF chip, h=13
    //   [39..53]  Key chip, h=13
    //   [56]  "T" label, then [-][val][+] at 63,71,93,101
    //   [106+]  slider fills gap
    //   right anchor (w-38):  H label, [-][val][+], gap, [↓][↑][×]
    // ─────────────────────────────────────────────────────────────────────
    private static final int CHIP_Y  = 4;   // chips start 4px from row top
    private static final int CHIP_H  = 13;  // chip height

    private void renderStep(DrawContext ctx, int x, int y, int w, int mx, int my, int accent,
                             int idx, List<StepData> steps) {
        StepData step = steps.get(idx);
        boolean rowH  = UI.hovered(mx, my, x, y, w, STEP_H);

        // Row background — alternate very subtle stripe
        UI.fill(ctx, x, y, w, STEP_H, idx%2==0 ? UI.SURFACE : UI.withAlpha(UI.WHITE, 0x03));
        if (rowH) UI.fill(ctx, x, y, w, STEP_H, UI.withAlpha(UI.WHITE, 0x04));
        UI.hline(ctx, x, y, w, UI.BORDER);

        // Active left bar
        if (step.enabled) {
            UI.fill(ctx, x,   y, 2, STEP_H, accent);
            UI.fill(ctx, x+2, y, 1, STEP_H, UI.withAlpha(accent, 0x22));
        }

        // Step index
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.65f, 0.65f);
        UI.text(ctx, String.valueOf(idx+1),
            (int)((x+5)/0.65f), (int)((y+7)/0.65f),
            step.enabled ? UI.GRAY : UI.GRAY_D);
        ctx.getMatrices().popMatrix();

        // ── Right cluster: [×][↑][↓] ──────────────────────────────────
        // Each is 12px wide, 4px gap from each other, starting from right
        int rBase = x + w - 12;
        boolean rmH = UI.hovered(mx, my, rBase,    y+CHIP_Y, 12, CHIP_H);
        boolean upH = UI.hovered(mx, my, rBase-14, y+CHIP_Y, 12, CHIP_H);
        boolean dnH = UI.hovered(mx, my, rBase-28, y+CHIP_Y, 12, CHIP_H);

        if (rmH) UI.fill(ctx, rBase,    y+CHIP_Y, 12, CHIP_H, UI.withAlpha(UI.RED, 0x2A));
        if (upH) UI.fill(ctx, rBase-14, y+CHIP_Y, 12, CHIP_H, UI.withAlpha(accent, 0x1A));
        if (dnH) UI.fill(ctx, rBase-28, y+CHIP_Y, 12, CHIP_H, UI.withAlpha(accent, 0x1A));

        UI.textCentered(ctx, "×", rBase+5,    y+CHIP_Y+2, rmH ? UI.RED   : UI.GRAY_D);
        UI.textCentered(ctx, "↑", rBase-14+5, y+CHIP_Y+2, upH ? accent   : UI.GRAY_D);
        UI.textCentered(ctx, "↓", rBase-28+5, y+CHIP_Y+2, dnH ? accent   : UI.GRAY_D);

        // ── ON/OFF chip  [16..36] ──────────────────────────────────────
        boolean togH = UI.hovered(mx, my, x+16, y+CHIP_Y, 20, CHIP_H);
        UI.fill(ctx, x+16, y+CHIP_Y, 20, CHIP_H,
            step.enabled ? UI.withAlpha(accent, 0x22) : UI.SURFACE2);
        UI.hline(ctx, x+16, y+CHIP_Y, 20,
            step.enabled ? UI.withAlpha(accent, 0x66) : UI.withAlpha(UI.WHITE, 0x12));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.6f, 0.6f);
        UI.textCentered(ctx, step.enabled ? "ON" : "OFF",
            (int)((x+26)/0.6f), (int)((y+CHIP_Y+3)/0.6f),
            step.enabled ? accent : UI.GRAY_D);
        ctx.getMatrices().popMatrix();

        if (!step.enabled) return;

        // ── Key chip  [39..53] ────────────────────────────────────────
        boolean keyH = UI.hovered(mx, my, x+39, y+CHIP_Y, 15, CHIP_H);
        UI.fill(ctx, x+39, y+CHIP_Y, 15, CHIP_H,
            keyH ? UI.withAlpha(accent, 0x22) : UI.SURFACE2);
        UI.hline(ctx, x+39, y+CHIP_Y, 15, UI.withAlpha(UI.WHITE, 0x14));
        UI.textCentered(ctx, step.key, x+46, y+CHIP_Y+2,
            keyH ? accent : UI.WHITE);

        // ── Ticks: T  [-][val][+]  ───────────────────────────────────
        // "T" at x+57, "-" at x+63, val at x+71, "+" at x+93
        String tf = "ticks" + idx;
        boolean tA  = activeField.equals(tf);
        boolean tmH = UI.hovered(mx, my, x+56, y+CHIP_Y, 10, CHIP_H);
        boolean tpH = UI.hovered(mx, my, x+88, y+CHIP_Y, 10, CHIP_H);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.6f, 0.6f);
        UI.text(ctx, "T", (int)((x+57)/0.6f), (int)((y+CHIP_Y+3)/0.6f), UI.GRAY_D);
        ctx.getMatrices().popMatrix();

        UI.arrowBtn(ctx, x+65, y+CHIP_Y, 10, CHIP_H, "-", tmH, accent);

        UI.fill(ctx, x+76, y+CHIP_Y, 20, CHIP_H, tA ? UI.BG3 : UI.SURFACE2);
        if (tA) UI.hline(ctx, x+76, y+CHIP_Y, 20, accent);
        UI.textCentered(ctx, tA ? inputBuffer+"▌" : String.valueOf(step.ticks),
            x+86, y+CHIP_Y+2, tA ? accent : UI.WHITE);

        UI.arrowBtn(ctx, x+97, y+CHIP_Y, 10, CHIP_H, "+", tpH, accent);

        // ── Hold: right-anchored H [-][val][+] ───────────────────────
        // anchor = rBase-28-4 = rBase-32 (right of ↓↑× block)
        int ha = rBase - 32;
        String hf = "hold" + idx;
        boolean hA  = activeField.equals(hf);
        boolean hmH = UI.hovered(mx, my, ha-40, y+CHIP_Y, 10, CHIP_H);
        boolean hpH = UI.hovered(mx, my, ha-8,  y+CHIP_Y, 10, CHIP_H);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.6f, 0.6f);
        UI.text(ctx, "H", (int)((ha-52)/0.6f), (int)((y+CHIP_Y+3)/0.6f), UI.GRAY_D);
        ctx.getMatrices().popMatrix();

        UI.arrowBtn(ctx, ha-40, y+CHIP_Y, 10, CHIP_H, "-", hmH, accent);

        UI.fill(ctx, ha-29, y+CHIP_Y, 20, CHIP_H, hA ? UI.BG3 : UI.SURFACE2);
        if (hA) UI.hline(ctx, ha-29, y+CHIP_Y, 20, accent);
        UI.textCentered(ctx, hA ? inputBuffer+"▌" : String.valueOf(step.hold),
            ha-19, y+CHIP_Y+2, hA ? accent : UI.WHITE);

        UI.arrowBtn(ctx, ha-8, y+CHIP_Y, 10, CHIP_H, "+", hpH, accent);

        // ── Slider: fills gap between ticks[+] and hold H ────────────
        int slStart = x + 108;
        int slEnd   = ha - 54;
        int slW     = slEnd - slStart;
        if (slW > 10) {
            UI.slider(ctx, slStart, y + (STEP_H/2), slW,
                MathHelper.clamp(step.ticks / 2000f, 0, 1), accent);
        }
    }

    private void inputField(DrawContext ctx, int x, int y, int w, String text,
                            boolean active, int accent, int mx, int my) {
        boolean h = UI.hovered(mx, my, x, y, w, 14);
        UI.fill(ctx, x, y, w, 14, active ? UI.BG3 : h ? UI.SURFACE2 : UI.SURFACE);
        UI.hline(ctx, x, y, w, active ? accent : UI.withAlpha(UI.WHITE, 0x14));
        UI.textCentered(ctx, text, x + w/2, y + 3, active ? accent : UI.GRAY);
    }

    // ── MOUSE CLICK ───────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        activeField = ""; inputBuffer = ""; draggingStep = -1;

        if (selPrefix == null) {
            // ── Hub ───────────────────────────────────────────────────────
            // genTop == y  (matches render exactly)
            int genTop = y;

            // Row 1 checkbox: x+8, genTop+GEN_HEADER_H+2
            int r1y = genTop + GEN_HEADER_H;
            if (UI.hovered(mx, my, x, r1y, w, GEN_ROW_H)) {
                parent.config.cropAutoStart = !parent.config.cropAutoStart;
                parent.config.save(); parent.playClick(); return true;
            }
            // Row 2 checkbox
            int r2y = r1y + GEN_ROW_H;
            if (UI.hovered(mx, my, x, r2y, w, GEN_ROW_H)) {
                parent.config.cropAutoSwitch = !parent.config.cropAutoSwitch;
                parent.config.save(); parent.playClick(); return true;
            }

            // Crop cards  — gridTop == y + GRID_TOP_OFFSET = y+41
            int gridTop = genTop + GRID_TOP_OFFSET;
            int cardW   = (w - CARD_GAP * (COLS-1)) / COLS;
            int sy      = MathHelper.clamp((int) scrollAnim, 0, 99999);
            for (int i = 0; i < CROPS.length; i++) {
                int col  = i % COLS;
                int row  = i / COLS;
                int cx   = x + col * (cardW + CARD_GAP);
                int rawY = gridTop + row * (CARD_H + CARD_GAP) - sy;
                if (UI.hovered(mx, my, cx, rawY, cardW, CARD_H)) {
                    selPrefix = CROPS[i].prefix(); selTitle = CROPS[i].name();
                    detailScrollY = 0; detailScrollAnim = 0;
                    parent.config.lastSelectedBot = selPrefix;
                    parent.config.save(); parent.playClick(); return true;
                }
            }
            return false;
        }

        // ── Detail ────────────────────────────────────────────────────────
        TlntdConfig cfg = parent.config;

        // Back  — y, full width 38px, h=10
        if (UI.hovered(mx, my, x, y, 38, 10)) { selPrefix = null; parent.playClick(); return true; }

        // Rotation fields — rotY = y+11
        int rotY = y + 11;
        int fldX = x + w - 112;
        if (UI.hovered(mx, my, fldX,    rotY+1, 50, 14)) { activeField="yaw";   inputBuffer=""; return true; }
        if (UI.hovered(mx, my, fldX+54, rotY+1, 50, 14)) { activeField="pitch"; inputBuffer=""; return true; }

        // Add step — addY = y+29, full width, h=10
        int addY = rotY + 18;
        if (UI.hovered(mx, my, x, addY, w, 10)) {
            cfg.addStep(selPrefix); cfg.save(); parent.playClick(); return true;
        }

        // Steps — stepsTop = y+40
        int stepsTop = addY + 11;
        int dsy = MathHelper.clamp((int) detailScrollAnim, 0, 99999);
        List<StepData> steps = cfg.getSteps(selPrefix);
        int rBase = x + w - 12;

        for (int i = 0; i < steps.size(); i++) {
            int ry = stepsTop + i * (STEP_H + STEP_GAP) - dsy;

            // ─ right cluster ─
            if (UI.hovered(mx, my, rBase,    ry+CHIP_Y, 12, CHIP_H)) {
                cfg.removeStep(selPrefix, i); cfg.save(); parent.playClick(); return true;
            }
            if (i > 0 && UI.hovered(mx, my, rBase-14, ry+CHIP_Y, 12, CHIP_H)) {
                var st = cfg.getSteps(selPrefix);
                StepData t = st.get(i-1); st.set(i-1, st.get(i)); st.set(i, t);
                cfg.save(); parent.playClick(); return true;
            }
            if (i < steps.size()-1 && UI.hovered(mx, my, rBase-28, ry+CHIP_Y, 12, CHIP_H)) {
                var st = cfg.getSteps(selPrefix);
                StepData t = st.get(i+1); st.set(i+1, st.get(i)); st.set(i, t);
                cfg.save(); parent.playClick(); return true;
            }
            // ON/OFF
            if (UI.hovered(mx, my, x+16, ry+CHIP_Y, 20, CHIP_H)) {
                steps.get(i).enabled = !steps.get(i).enabled; cfg.save(); parent.playClick(); return true;
            }
            if (!steps.get(i).enabled) continue;

            // Key
            if (UI.hovered(mx, my, x+39, ry+CHIP_Y, 15, CHIP_H)) {
                steps.get(i).key = cycleKey(steps.get(i).key); cfg.save(); parent.playClick(); return true;
            }
            // Ticks -
            if (UI.hovered(mx, my, x+65, ry+CHIP_Y, 10, CHIP_H)) {
                steps.get(i).ticks = Math.max(1, steps.get(i).ticks-1); cfg.save(); return true;
            }
            // Ticks val
            if (UI.hovered(mx, my, x+76, ry+CHIP_Y, 20, CHIP_H)) {
                activeField = "ticks"+i; inputBuffer = String.valueOf(steps.get(i).ticks); return true;
            }
            // Ticks +
            if (UI.hovered(mx, my, x+97, ry+CHIP_Y, 10, CHIP_H)) {
                steps.get(i).ticks = Math.min(9999, steps.get(i).ticks+1); cfg.save(); return true;
            }
            // Slider — full height, between ticks+ and hold cluster
            int ha = rBase - 32;
            if (UI.hovered(mx, my, x+108, ry, ha-54-(x+108-x), STEP_H)) { draggingStep = i; return true; }

            // Hold -
            if (UI.hovered(mx, my, ha-40, ry+CHIP_Y, 10, CHIP_H)) {
                steps.get(i).hold = Math.max(1, steps.get(i).hold-1); cfg.save(); return true;
            }
            // Hold val
            if (UI.hovered(mx, my, ha-29, ry+CHIP_Y, 20, CHIP_H)) {
                activeField = "hold"+i; inputBuffer = String.valueOf(steps.get(i).hold); return true;
            }
            // Hold +
            if (UI.hovered(mx, my, ha-8, ry+CHIP_Y, 10, CHIP_H)) {
                steps.get(i).hold = Math.min(999, steps.get(i).hold+1); cfg.save(); return true;
            }
        }
        return false;
    }

    @Override
    public void updateDragging(int mx, int startX) {
        if (draggingStep >= 0 && selPrefix != null) {
            // section x = startX+10, slider starts at x+108, ends at ha-54
            int secX = startX + 10;
            int slX  = secX + 108;
            int ha   = secX + (parent.cw()-20) - 12 - 32;
            int slW  = Math.max(1, ha - 54 - slX);
            float pct = MathHelper.clamp((float)(mx - slX) / slW, 0, 1);
            List<StepData> steps = parent.config.getSteps(selPrefix);
            if (draggingStep < steps.size()) {
                steps.get(draggingStep).ticks = Math.max(1, (int)(pct * 2000));
                parent.config.save();
            }
        }
    }

    @Override public int preferredHeight(int w) { return 480; }
    @Override public void mouseReleased() { draggingStep = -1; }

    public void scroll(int direction) {
        if (selPrefix == null) scrollY       = Math.max(0, scrollY + direction * 14);
        else                   detailScrollY = Math.max(0, detailScrollY + direction * 14);
    }

    @Override
    public boolean onKeyPressed(int key, int scan, int mods) {
        if (activeField.isEmpty()) return false;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitInput(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE) { activeField=""; inputBuffer=""; return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !inputBuffer.isEmpty()) {
            inputBuffer = inputBuffer.substring(0, inputBuffer.length()-1); return true;
        }
        return false;
    }

    @Override
    public boolean onCharTyped(char chr, int mods) {
        if (activeField.isEmpty()) return false;
        if (Character.isDigit(chr) || chr=='-' || chr=='.') { inputBuffer += chr; return true; }
        return false;
    }

    private void commitInput() {
        if (activeField.isEmpty() || selPrefix == null) return;
        try {
            TlntdConfig cfg = parent.config;
            List<StepData> steps = cfg.getSteps(selPrefix);
            if (activeField.equals("yaw")) {
                float v = Float.parseFloat(inputBuffer);
                while (v<=-180) v+=360; while (v>180) v-=360;
                cfg.setYaw(selPrefix, v);
            } else if (activeField.equals("pitch")) {
                cfg.setPitch(selPrefix, MathHelper.clamp(Float.parseFloat(inputBuffer), -90, 90));
            } else if (activeField.startsWith("ticks")) {
                int idx = Integer.parseInt(activeField.substring(5));
                if (idx < steps.size()) steps.get(idx).ticks = Math.max(1, Integer.parseInt(inputBuffer));
            } else if (activeField.startsWith("hold")) {
                int idx = Integer.parseInt(activeField.substring(4));
                if (idx < steps.size()) steps.get(idx).hold = Math.max(1, Integer.parseInt(inputBuffer));
            }
            cfg.save();
        } catch (NumberFormatException ignored) {}
        activeField=""; inputBuffer="";
    }

    private String cycleKey(String k) {
        return switch(k.toUpperCase()) { case "W"->"S"; case "S"->"A"; case "A"->"D"; default->"W"; };
    }
}
