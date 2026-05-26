package me.shimmy.tlntd;

import net.minecraft.client.gui.DrawContext;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public class ConsoleSection implements IMenuSection {

    private int    scrollOffset   = 0;

    private String filterText     = "";
    private boolean filterFocused = false;

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta) {

        UI.textShadow(ctx, "CONSOLE", x, y, accent);
        UI.hline(ctx, x, y+10, 48, accent);

        boolean clrH = UI.hovered(mx, my, x+w-52, y-1, 52, 12);
        UI.fill(ctx, x+w-52, y-1, 52, 12, clrH ? UI.withAlpha(UI.RED,0x22) : UI.SURFACE);
        UI.textShadow(ctx, "Clear log", x+w-47, y+1, clrH ? UI.RED : UI.GRAY);

        int fY = y + 14;
        boolean ff = filterFocused;
        UI.fill(ctx, x, fY, w, 13, ff ? UI.SURFACE2 : UI.SURFACE);
        UI.hline(ctx, x, fY, w, ff ? accent : UI.withAlpha(UI.WHITE, 0x10));
        String fDisp = filterText.isEmpty() && !ff ? "Filter logs..." : filterText + (ff ? "|" : "");
        UI.text(ctx, fDisp, x+6, fY+3, filterText.isEmpty() && !ff ? UI.GRAY_D : UI.WHITE);
        if (!filterText.isEmpty()) {

            UI.textShadow(ctx, "×", x+w-10, fY+3, UI.GRAY);
        }

        int logY  = fY + 17;
        int lineH = 10;
        int maxLines = (y + 220 - logY) / lineH;

        List<String> log = Tlntd.consoleLog;

        List<String> filtered = filterText.isEmpty() ? log :
            log.stream().filter(l -> l.toLowerCase().contains(filterText.toLowerCase()))
               .collect(java.util.stream.Collectors.toList());

        int total = filtered.size();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, total - maxLines)));

        if (total == 0) {
            UI.text(ctx, filterText.isEmpty() ? "No log entries yet." : "No matches for: " + filterText,
                x, logY+4, UI.GRAY_D);
            return;
        }

        int start = Math.max(0, total - maxLines - scrollOffset);
        int end   = Math.min(total, start + maxLines);

        for (int i = start; i < end; i++) {
            String line = filtered.get(i);
            int ly = logY + (i - start) * lineH;
            if ((i - start) % 2 == 0) UI.fill(ctx, x, ly-1, w, lineH, UI.withAlpha(UI.WHITE, 0x03));

            int col = lineColor(line);

            String clean = line.replaceAll("§[0-9a-fk-or]", "");
            UI.text(ctx, clean, x+2, ly+1, col);
        }

        if (total > maxLines) UI.text(ctx, "▲ scroll ▼", x+w-52, logY+2, UI.GRAY_D);

        UI.hline(ctx, x, y+222, w, UI.withAlpha(accent, 0x22));
        UI.text(ctx, total + " entries" + (filterText.isEmpty() ? "" : " (filtered)"), x, y+224, UI.GRAY_D);
    }

    private int lineColor(String line) {
        String u = line.toUpperCase();
        if (u.contains("PANIC") || u.contains("STAFF") || u.contains("STOPPED"))  return UI.RED;
        if (u.contains("STARTED") || u.contains("RESUMED"))                        return UI.GREEN;
        if (u.contains("ANTI-AFK") || u.contains("PAUSED") || u.contains("LIMIT")) return 0xFFFFDD44;
        if (u.contains("SAFETY") || u.contains("DETECTED"))                        return 0xFFFF8844;
        return UI.GRAY;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w) {
        if (UI.hovered(mx, my, x+w-52, y-1, 52, 12)) {
            Tlntd.consoleLog.clear(); return true;
        }
        int fY = y + 14;
        if (UI.hovered(mx, my, x, fY, w, 13)) { filterFocused = true; return true; }

        if (!filterText.isEmpty() && UI.hovered(mx, my, x+w-12, fY+2, 10, 10)) {
            filterText = ""; return true;
        }
        filterFocused = false;
        return false;
    }

    @Override
    public boolean onKeyPressed(int key, int scan, int mods) {
        if (!filterFocused) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) { filterFocused = false; return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !filterText.isEmpty()) {
            filterText = filterText.substring(0, filterText.length()-1); return true;
        }
        return false;
    }

    @Override
    public boolean onCharTyped(char chr, int mods) {
        if (!filterFocused) return false;
        filterText += chr; scrollOffset = 0; return true;
    }

    public void scroll(int direction) { scrollOffset = Math.max(0, scrollOffset + direction); }

    @Override public void updateDragging(int mx, int startX) {}
}
