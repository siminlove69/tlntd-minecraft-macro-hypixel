package me.shimmy.tlntd;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class UI {

    // ── Accent / status ────────────────────────────────────────────────────
    public static final int GOLD     = 0xFFD4A824;   // warm gold, slightly brighter
    public static final int GOLD_D   = 0xFF7A6010;
    public static final int GOLD_DIM = 0x33D4A824;
    public static final int GREEN    = 0xFF4AB868;   // cleaner green
    public static final int RED      = 0xFFCC3A3A;   // vivid enough to read
    public static final int BLUE     = 0xFF4A8ED4;

    // ── Surface hierarchy ──────────────────────────────────────────────────
    //   BG      = window body — the darkest level
    //   SURFACE = groups, panels — enough contrast to read as "raised"
    //   SURFACE2 = hover, inputs, elevated chips
    public static final int BG       = 0xFF07070F;   // near-black, cool tint
    public static final int SURFACE  = 0xFF0F0F1E;   // +8 offset — clearly distinct
    public static final int SURFACE2 = 0xFF181830;   // +17 offset — elevated
    // Compat aliases — sections use these names
    public static final int BG0      = 0xCC07070F;
    public static final int BG1      = 0xDD0A0A18;
    public static final int BG2      = 0xFF0F0F1E;
    public static final int BG2B     = 0xFF0C0C1A;
    public static final int BG3      = 0xFF181830;
    public static final int BG_HOVER = 0xFF181830;

    // ── Borders — now actually visible ────────────────────────────────────
    public static final int BORDER   = 0x1AFFFFFF;   // 10% — clear separator
    public static final int BORDER_A = 0x2EFFFFFF;   // 18% — emphasized

    // ── Text — 3 tiers, all readable on BG ────────────────────────────────
    //   Contrast ratios on BG=0xFF07070F (lum ~0.001):
    //   WHITE  ~#EAEAF6 → 13:1   — primary, always readable
    //   GRAY   ~#9090C4 →  5:1   — secondary, clearly readable
    //   GRAY_D ~#525278 →  2.5:1 — muted labels, intentionally quiet
    public static final int WHITE    = 0xFFEAEAF6;   // lifted +30 units vs old
    public static final int GRAY     = 0xFF9090C4;   // lifted +72 units vs old
    public static final int GRAY_D   = 0xFF525278;   // lifted +48 units vs old

    private UI() {}

    // ── Primitives ──────────────────────────────────────────────────────────
    public static void fill(DrawContext ctx, int x, int y, int w, int h, int col) { ctx.fill(x, y, x+w, y+h, col); }
    public static void hline(DrawContext ctx, int x, int y, int w, int col) { ctx.fill(x, y, x+w, y+1, col); }
    public static void vline(DrawContext ctx, int x, int y, int h, int col) { ctx.fill(x, y, x+1, y+h, col); }
    public static void grad(DrawContext ctx, int x, int y, int w, int h, int c1, int c2) { ctx.fillGradient(x, y, x+w, y+h, c1, c2); }
    public static void gradH(DrawContext ctx, int x, int y, int w, int h, int left, int right) {
        ctx.fillGradient(x, y, x+w, y+h, left, right);
    }

    // ── Panels ──────────────────────────────────────────────────────────────
    public static void box(DrawContext ctx, int x, int y, int w, int h, int bg, int border) {
        fill(ctx, x, y, w, h, bg);
        hline(ctx, x, y, w, border);
    }
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        fill(ctx, x, y, w, h, SURFACE);
        hline(ctx, x, y, w, BORDER);
    }
    public static void inlinePanel(DrawContext ctx, int x, int y, int w, int h) {
        fill(ctx, x, y, w, h, SURFACE);
        hline(ctx, x, y, w, BORDER);
    }

    // ── Group header ────────────────────────────────────────────────────────
    //   12px SURFACE band, accent left-bar, gold label
    public static void groupHeader(DrawContext ctx, int x, int y, int w, String title, int accent) {
        // Background — very subtle accent tint + surface
        fill(ctx, x, y, w, 12, SURFACE);
        ctx.fillGradient(x, y, x+w/2, y+12, withAlpha(accent, 0x10), 0x00000000);
        // Left bar: 3px solid, 1px fade, 1px ghost
        fill(ctx, x,   y, 3, 12, withAlpha(accent, 0xCC));
        fill(ctx, x+3, y, 1, 12, withAlpha(accent, 0x33));
        fill(ctx, x+4, y, 1, 12, withAlpha(accent, 0x0C));
        // Top micro-line: bright
        hline(ctx, x, y, w, withAlpha(accent, 0x22));
        // Bottom separator
        hline(ctx, x, y+11, w, withAlpha(accent, 0x33));
        hline(ctx, x, y+12, w, withAlpha(WHITE, 0x06));
        // Label
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.65f, 0.65f);
        // Glow shadow
        text(ctx, title, (int)((x+9) / 0.65f), (int)((y+3) / 0.65f), withAlpha(accent, 0x33));
        text(ctx, title, (int)((x+8) / 0.65f), (int)((y+2) / 0.65f), withAlpha(accent, 0xCC));
        ctx.getMatrices().popMatrix();
    }
    public static void sectionHeader(DrawContext ctx, int x, int y, int w, String title, int accent) {
        groupHeader(ctx, x, y, w, title, accent);
    }

    // ── Checkbox ────────────────────────────────────────────────────────────
    public static void checkbox(DrawContext ctx, int x, int y, boolean val, int accent) {
        if (val) {
            fill(ctx, x, y, 9, 9, withAlpha(accent, 0x22));
            hline(ctx, x, y, 9, withAlpha(accent, 0x88));
            fill(ctx, x+2, y+2, 5, 5, accent);
            // Micro highlight on top of the fill dot
            fill(ctx, x+2, y+2, 5, 1, withAlpha(WHITE, 0x44));
        } else {
            fill(ctx, x, y, 9, 9, withAlpha(WHITE, 0x06));
            hline(ctx, x, y, 9, withAlpha(WHITE, 0x18));
        }
    }

    // ── Arrow button ────────────────────────────────────────────────────────
    public static void arrowBtn(DrawContext ctx, int x, int y, int w, int h,
                                String label, boolean hover, int accent) {
        if (hover) fill(ctx, x, y, w, h, withAlpha(accent, 0x20));
        textCentered(ctx, label, x + w/2, y + (h-8)/2, hover ? accent : GRAY_D);
    }

    // ── Key badge ────────────────────────────────────────────────────────────
    public static void keyBadge(DrawContext ctx, int x, int y, String key,
                                boolean hover, int accent) {
        fill(ctx, x, y, 18, 14, hover ? withAlpha(accent, 0x20) : SURFACE2);
        hline(ctx, x, y, 18, hover ? withAlpha(accent, 0x55) : BORDER_A);
        textCentered(ctx, key, x+9, y+3, hover ? accent : WHITE);
    }

    // ── Slider — 1px track, 3px knob ─────────────────────────────────────
    public static void slider(DrawContext ctx, int x, int y, int w, float progress, int accent) {
        progress = MathHelper.clamp(progress, 0f, 1f);
        // Track: dim full width
        hline(ctx, x, y, w, withAlpha(WHITE, 0x18));
        // Filled portion: brighter accent
        int filled = (int)(w * progress);
        if (filled > 0) hline(ctx, x, y, filled, withAlpha(accent, 0xBB));
        // Knob: solid accent dot
        int kx = MathHelper.clamp(x + filled - 1, x, x + w - 3);
        fill(ctx, kx, y-1, 3, 3, accent);
        fill(ctx, kx, y-1, 3, 1, withAlpha(WHITE, 0x55)); // top highlight
    }

    // ── Status dot ──────────────────────────────────────────────────────────
    public static void statusDot(DrawContext ctx, int cx, int cy, int col) {
        fill(ctx, cx-3, cy-3, 6, 6, withAlpha(col, 0x28));
        fill(ctx, cx-2, cy-2, 4, 4, col);
        fill(ctx, cx-1, cy-2, 2, 1, withAlpha(WHITE, 0x66));
    }

    // ── Sparkline ────────────────────────────────────────────────────────────
    public static void sparkline(DrawContext ctx, int x, int y, int w, int h,
                                  float[] samples, int col) {
        if (samples == null || samples.length == 0) return;
        float max = 0.001f;
        for (float s : samples) if (s > max) max = s;
        int barW = Math.max(1, w / samples.length);
        for (int i = 0; i < samples.length; i++) {
            int bh = (int)((samples[i] / max) * h);
            if (bh < 1) bh = 1;
            fill(ctx, x + i*barW, y + h - bh, barW-1, bh, withAlpha(col, 0x55));
        }
    }

    // ── Glow / shadow ────────────────────────────────────────────────────────
    public static void outerGlow(DrawContext ctx, int x, int y, int w, int h,
                                  int col, int layers) {
        for (int i = layers; i >= 1; i--) {
            int alpha = (int)(12.0 / i * layers / 4);
            ctx.fill(x-i, y-i, x+w+i, y+h+i, withAlpha(col, alpha));
        }
    }
    public static void dropShadow(DrawContext ctx, int x, int y, int w, int h, int depth) {
        for (int i = 1; i <= depth; i++) {
            int alpha = (int)(40.0 / i);
            ctx.fill(x+i, y+i, x+w+i, y+h+i, withAlpha(0, alpha));
        }
    }

    // ── Tooltip — elevated surface with accent border ────────────────────────
    public static void tooltip(DrawContext ctx, int mx, int my, String text) {
        int tw = textWidth(text) + 12;
        int tx = mx + 8, ty = my - 16;
        fill(ctx, tx, ty, tw, 14, SURFACE2);
        hline(ctx, tx, ty, tw, BORDER_A);
        hline(ctx, tx, ty+13, tw, withAlpha(0, 0x88));  // bottom shadow
        textShadow(ctx, text, tx + 6, ty + 3, GRAY);
    }

    public static void titleBar(DrawContext ctx, int x, int y, int w, int h, int accent) {
        fill(ctx, x, y, w, h, BG);
        hline(ctx, x, y+h-1, w, BORDER);
    }

    // ── Text primitives ──────────────────────────────────────────────────────
    public static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }
    public static void text(DrawContext ctx, String s, int x, int y, int col) { ctx.drawText(tr(), s, x, y, col, false); }
    public static void textShadow(DrawContext ctx, String s, int x, int y, int col) { ctx.drawText(tr(), s, x, y, col, true); }
    public static void textBold(DrawContext ctx, String s, int x, int y, int col) { ctx.drawText(tr(), s, x, y, col, false); ctx.drawText(tr(), s, x+1, y, col, false); }
    public static void textBoldShadow(DrawContext ctx, String s, int x, int y, int col) { ctx.drawText(tr(), s, x, y, col, true); ctx.drawText(tr(), s, x+1, y, col, true); }
    public static void textCentered(DrawContext ctx, String s, int cx, int y, int col) { ctx.drawCenteredTextWithShadow(tr(), s, cx, y, col); }
    public static int textWidth(String s) { return tr().getWidth(s); }

    public static void dataValue(DrawContext ctx, int x, int y, String number, String unit, int numCol) {
        int nw = textWidth(number);
        textShadow(ctx, number, x + numCol - nw, y, WHITE);
        textShadow(ctx, unit, x + numCol + 3, y, GRAY);
    }

    // ── Color math ────────────────────────────────────────────────────────────
    public static int darken(int col, float f) {
        return 0xFF000000
            | ((int)(((col>>16)&0xFF)*f) << 16)
            | ((int)(((col>>8)&0xFF)*f)  << 8)
            |  (int)((col&0xFF)*f);
    }
    public static int withAlpha(int col, int a) { return (a<<24)|(col&0x00FFFFFF); }
    public static int lerp(int c1, int c2, float t) {
        int r=(int)(((c1>>16)&0xFF)*(1-t)+((c2>>16)&0xFF)*t);
        int g=(int)(((c1>>8)&0xFF)*(1-t)+((c2>>8)&0xFF)*t);
        int b=(int)((c1&0xFF)*(1-t)+(c2&0xFF)*t);
        int a=(int)(((c1>>24)&0xFF)*(1-t)+((c2>>24)&0xFF)*t);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
    public static boolean hovered(double mx, double my, int x, int y, int w, int h) {
        return mx>=x && mx<x+w && my>=y && my<y+h;
    }
}
