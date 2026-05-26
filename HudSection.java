package me.shimmy.tlntd;

import net.minecraft.client.gui.DrawContext;

public class HudSection implements IMenuSection {
    private final TlntdMenu parent;
    public HudSection(TlntdMenu p) { parent = p; }

    private static final int ROW = 14;
    // Store row y positions during render, use them in click
    private final int[] rowY = new int[7];

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta) {
        int cy = y;
        int ri = 0;

        UI.groupHeader(ctx, x, cy, w, "WATERMARK", accent); cy += 13;
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show Watermark", parent.config.hudWatermark, accent);
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show FPS",       parent.config.hudFPS,       accent);
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show TPS",       parent.config.hudTPS,       accent);
        cy += 5;

        UI.groupHeader(ctx, x, cy, w, "PLAYER INFO", accent); cy += 13;
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show Coordinates", parent.config.hudCoords,    accent);
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show Ping",        parent.config.hudPing,      accent);
        rowY[ri++] = cy; cy = row(ctx, x, cy, w, mx, my, "Show Direction",   parent.config.hudDirection, accent);
        cy += 5;

        UI.groupHeader(ctx, x, cy, w, "BOT", accent); cy += 13;
        rowY[ri++] = cy; row(ctx, x, cy, w, mx, my, "Show Bot Status", parent.config.hudBotStatus, accent);
    }

    private int row(DrawContext ctx, int x, int y, int w, int mx, int my,
                    String label, boolean val, int accent) {
        boolean h = UI.hovered(mx, my, x, y, w, ROW);
        if (h) UI.fill(ctx, x, y, w, ROW, UI.withAlpha(UI.WHITE, 0x06));
        if (val) UI.fill(ctx, x, y, 2, ROW, UI.withAlpha(accent, 0x77));
        UI.checkbox(ctx, x+8, y+3, val, accent);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.75f, 0.75f);
        UI.textShadow(ctx, label, (int)((x+21)/0.75f), (int)((y+4)/0.75f), UI.WHITE);
        ctx.getMatrices().popMatrix();
        return y + ROW;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        TlntdConfig c = parent.config;
        // Full-row hit zones using stored positions
        if (UI.hovered(mx,my, x, rowY[0], w, ROW)) { c.hudWatermark = !c.hudWatermark; c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[1], w, ROW)) { c.hudFPS       = !c.hudFPS;       c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[2], w, ROW)) { c.hudTPS       = !c.hudTPS;       c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[3], w, ROW)) { c.hudCoords    = !c.hudCoords;    c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[4], w, ROW)) { c.hudPing      = !c.hudPing;      c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[5], w, ROW)) { c.hudDirection = !c.hudDirection; c.save(); parent.playClick(); return true; }
        if (UI.hovered(mx,my, x, rowY[6], w, ROW)) { c.hudBotStatus = !c.hudBotStatus; c.save(); parent.playClick(); return true; }
        return false;
    }

    @Override public void updateDragging(int mx, int startX) {}
}
