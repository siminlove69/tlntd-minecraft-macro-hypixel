package me.shimmy.tlntd;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class TlntdHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen instanceof TlntdMenu) return;

        TlntdConfig cfg = TlntdConfig.load();
        if (!cfg.hudWatermark) return;

        int accent = cfg.rgbMode
            ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 6000) / 6000f, 0.75f, 0.95f)
            : UI.GOLD;

        int x = cfg.hudX, y = cfg.hudY, panelW = 130;
        int cy = y;

        ctx.fill(x, cy, x + panelW, cy + 22, UI.withAlpha(UI.BG0, 0xCC));
        UI.hline(ctx, x, cy, panelW, accent);
        ctx.fill(x, cy, x + 2, cy + 22, accent);
        UI.textShadow(ctx, "TLNTD", x + 8, cy + 3, accent);
        String status = Tlntd.isRunning ? "ACTIVE" : "IDLE";
        int sc = Tlntd.isRunning ? accent : UI.GRAY;
        UI.textShadow(ctx, status, x + panelW - UI.textWidth(status) - 6, cy + 3, sc);
        cy += 24;

        if (cfg.hudFPS || cfg.hudTPS) {
            ctx.fill(x, cy, x + panelW, cy + 12, UI.withAlpha(UI.BG1, 0xCC));
            ctx.fill(x, cy, x + 2, cy + 12, UI.GOLD_D);
            if (cfg.hudFPS)
                UI.textBoldShadow(ctx, String.valueOf(mc.getCurrentFps()), x + 6, cy + 2, UI.WHITE);
            UI.text(ctx, " fps", x + 6 + me.shimmy.tlntd.UI.textWidth(String.valueOf(mc.getCurrentFps())) + 1, cy + 2, UI.GRAY);
            if (cfg.hudTPS && mc.world != null) {
                String tpsVal = String.format("%.0f", mc.world.getTickManager().getTickRate());
                String tpsUnit = " tps";
                int tpsW = UI.textWidth(tpsVal) + 1 + UI.textWidth(tpsUnit);
                UI.textBoldShadow(ctx, tpsVal, x + panelW - tpsW - 6, cy + 2, UI.WHITE);
                UI.text(ctx, tpsUnit, x + panelW - UI.textWidth(tpsUnit) - 6, cy + 2, UI.GRAY);
            }
            cy += 14;
        }

        if (cfg.hudCoords && mc.player != null) {
            ctx.fill(x, cy, x + panelW, cy + 12, UI.withAlpha(UI.BG1, 0xCC));
            ctx.fill(x, cy, x + 2, cy + 12, UI.GOLD_D);
            String coord = String.format("%.0f / %.0f / %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
            UI.textBoldShadow(ctx, coord, x + 6, cy + 2, UI.WHITE);
            cy += 14;
        }

        if (cfg.hudPing && mc.player != null && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            int ping = entry != null ? entry.getLatency() : 0;
            ctx.fill(x, cy, x + panelW, cy + 12, UI.withAlpha(UI.BG1, 0xCC));
            ctx.fill(x, cy, x + 2, cy + 12, UI.GOLD_D);
            UI.textShadow(ctx, "Ping: " + ping + "ms", x + 6, cy + 2, UI.GRAY);
            cy += 14;
        }

        if (cfg.hudDirection && mc.player != null) {
            Direction dir = mc.player.getHorizontalFacing();
            ctx.fill(x, cy, x + panelW, cy + 12, UI.withAlpha(UI.BG1, 0xCC));
            ctx.fill(x, cy, x + 2, cy + 12, UI.GOLD_D);
            UI.textShadow(ctx, "Facing: " + dir.asString().toUpperCase(), x + 6, cy + 2, UI.GRAY);
            cy += 14;
        }

        if (cfg.hudBotStatus && Tlntd.isRunning) {
            int maxT = cfg.getTicks(Tlntd.activeBotType, Tlntd.currentStep);
            float prog = MathHelper.clamp((float) Tlntd.tickCounter / Math.max(maxT, 1), 0f, 1f);
            String key = cfg.getKey(Tlntd.activeBotType, Tlntd.currentStep);

            ctx.fill(x, cy, x + panelW, cy + 28, UI.withAlpha(UI.BG0, 0xCC));
            UI.hline(ctx, x, cy, panelW, UI.GOLD_D);
            ctx.fill(x, cy, x + 2, cy + 28, accent);

            UI.textShadow(ctx, "Step " + (Tlntd.currentStep + 1) + "  Key: " + key, x + 6, cy + 3, UI.WHITE);
            UI.textShadow(ctx, (int)(prog * 100) + "%", x + panelW - 22, cy + 3, accent);

            ctx.fill(x + 4, cy + 18, x + panelW - 4, cy + 22, UI.BG2);
            ctx.fill(x + 4, cy + 18, x + 4 + (int)((panelW - 8) * prog), cy + 22, accent);
        }
    }
}
