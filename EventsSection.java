package me.shimmy.tlntd;

import net.minecraft.client.gui.DrawContext;

public class EventsSection implements IMenuSection {

    private int scrollOffset = 0;
    private static final int LINE_H = 14;

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta) {
        // ── Header ──────────────────────────────────────────────────────────
        UI.textShadow(ctx, "EVENTS", x, y, accent);
        UI.hline(ctx, x, y + 10, 44, accent);

        boolean clrH = UI.hovered(mx, my, x + w - 52, y - 1, 52, 12);
        UI.fill(ctx, x + w - 52, y - 1, 52, 12, clrH ? UI.withAlpha(UI.RED, 0x22) : UI.SURFACE);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.75f, 0.75f);
        UI.textShadow(ctx, "Clear", (int) ((x + w - 46) / 0.75f), (int) ((y + 1) / 0.75f),
            clrH ? UI.RED : UI.GRAY);
        ctx.getMatrices().popMatrix();

        // ── Legend ──────────────────────────────────────────────────────────
        int lgY = y + 14;
        drawBadge(ctx, x,       lgY, "START",  0xFF4AB868);
        drawBadge(ctx, x + 38,  lgY, "STOP",   0xFFFF8844);
        drawBadge(ctx, x + 76,  lgY, "SAFETY", UI.RED);
        drawBadge(ctx, x + 122, lgY, "INFO",   0xFF88DDFF);

        // ── List ─────────────────────────────────────────────────────────────
        java.util.List<String[]> log = Tlntd.eventLog;
        int logY     = lgY + 14;
        int maxH     = y + 205 - logY;
        int maxLines = maxH / LINE_H;

        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, log.size() - maxLines)));

        if (log.isEmpty()) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(0.75f, 0.75f);
            UI.text(ctx, "No events recorded yet.",
                (int) ((x + 4) / 0.75f), (int) ((logY + 4) / 0.75f), UI.GRAY_D);
            ctx.getMatrices().popMatrix();
            return;
        }

        int end = Math.min(log.size(), scrollOffset + maxLines);
        for (int i = scrollOffset; i < end; i++) {
            String[] e   = log.get(i);
            String type  = e[0], msg = e[1], ts = e[2];
            int ly = logY + (i - scrollOffset) * LINE_H;

            if ((i - scrollOffset) % 2 == 0)
                UI.fill(ctx, x, ly, w, LINE_H, UI.withAlpha(UI.WHITE, 0x04));

            int typeCol = typeColor(type);
            UI.fill(ctx, x, ly, 2, LINE_H - 1, UI.withAlpha(typeCol, 0x99));

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(0.65f, 0.65f);
            UI.text(ctx, ts, (int) ((x + 6) / 0.65f), (int) ((ly + 3) / 0.65f), UI.GRAY_D);
            int tsW = (int)(UI.textWidth(ts) * 0.65f);
            String tag = "[" + type + "]";
            UI.text(ctx, tag, (int) ((x + 6 + tsW + 4) / 0.65f), (int) ((ly + 3) / 0.65f),
                UI.withAlpha(typeCol, 0xBB));
            int tagW = (int)(UI.textWidth(tag) * 0.65f);
            UI.textShadow(ctx, msg, (int) ((x + 6 + tsW + 4 + tagW + 4) / 0.65f),
                (int) ((ly + 3) / 0.65f), UI.WHITE);
            ctx.getMatrices().popMatrix();
        }

        // Scroll hint
        if (log.size() > maxLines) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(0.65f, 0.65f);
            UI.text(ctx, "▲ scroll ▼",
                (int) ((x + w - 48) / 0.65f), (int) ((logY + 2) / 0.65f), UI.GRAY_D);
            ctx.getMatrices().popMatrix();
        }

        // Footer
        UI.hline(ctx, x, logY + maxH + 2, w, UI.withAlpha(accent, 0x22));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.65f, 0.65f);
        int safetyCount = 0;
        for (String[] e : log) if ("SAFETY".equals(e[0])) safetyCount++;
        String summary = log.size() + " events" + (safetyCount > 0 ? " · " + safetyCount + " safety" : "");
        UI.text(ctx, summary, (int) ((x) / 0.65f), (int) ((logY + maxH + 5) / 0.65f), UI.GRAY_D);
        ctx.getMatrices().popMatrix();
    }

    private void drawBadge(DrawContext ctx, int x, int y, String label, int col) {
        int tw = (int)(UI.textWidth(label) * 0.6f) + 8;
        UI.fill(ctx, x, y, tw, 10, UI.withAlpha(col, 0x18));
        UI.hline(ctx, x, y, tw, UI.withAlpha(col, 0x55));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(0.6f, 0.6f);
        UI.textShadow(ctx, label, (int) ((x + 4) / 0.6f), (int) ((y + 2) / 0.6f), col);
        ctx.getMatrices().popMatrix();
    }

    private int typeColor(String type) {
        return switch (type) {
            case "SAFETY" -> UI.RED;
            case "START"  -> UI.GREEN;
            case "STOP"   -> 0xFFFF8844;
            case "INFO"   -> 0xFF88DDFF;
            default       -> UI.GRAY;
        };
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        if (UI.hovered(mx, my, x + w - 52, y - 1, 52, 12)) {
            Tlntd.eventLog.clear();
            return true;
        }
        return false;
    }

    public void scroll(int dir) {
        scrollOffset = Math.max(0, scrollOffset + dir);
    }

    @Override public void updateDragging(int mx, int startX) {}
}
