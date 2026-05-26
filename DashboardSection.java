package me.shimmy.tlntd;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class DashboardSection implements IMenuSection {

    private final float[] fpsSamples = new float[20];
    private final float[] tpsSamples = new float[20];
    private int sampleIdx=0, sampleTimer=0;
    private static final int LABEL_COL=96, ROW_H=13;
    private static final float SC=0.8f;

    @Override
    public int preferredHeight(int w) { return 300; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, int accent, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if(++sampleTimer>=6){
            sampleTimer=0;
            fpsSamples[sampleIdx%20]=mc.getCurrentFps();
            tpsSamples[sampleIdx%20]=mc.world!=null?mc.world.getTickManager().getTickRate():20f;
            sampleIdx++;
        }
        int cy=y;
        boolean running=Tlntd.isRunning;

        // ── Status strip ──────────────────────────────────────────────────────
        float pulse=(float)(Math.sin(System.currentTimeMillis()/350.0)*0.35+0.65);
        UI.fill(ctx,x,cy,w,16,running?UI.withAlpha(UI.GREEN,(int)(pulse*0x12)):UI.SURFACE);
        UI.hline(ctx,x,cy,w,running?UI.withAlpha(UI.GREEN,(int)(pulse*0x55)):UI.BORDER);
        if(running)UI.fill(ctx,x,cy,2,16,UI.withAlpha(UI.GREEN,0x99));
        UI.statusDot(ctx,x+10,cy+8,UI.withAlpha(running?UI.GREEN:UI.GRAY,running?(int)(pulse*0xFF):0xBB));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);
        UI.textShadow(ctx,running?Tlntd.activeBotType.replace("_"," ").toUpperCase():"IDLE",
            (int)((x+19)/SC),(int)((cy+4)/SC),running?UI.withAlpha(UI.GREEN,(int)(pulse*0xFF)):UI.GRAY);
        ctx.getMatrices().popMatrix();
        cy+=20;

        // ── System stats ──────────────────────────────────────────────────────
        int sysH=4*ROW_H+4;
        UI.fill(ctx,x,cy,w,sysH,UI.SURFACE);UI.hline(ctx,x,cy,w,UI.BORDER);
        int ry=cy+2;

        // Uptime
        dataRow(ctx,x,ry,w,false);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"Uptime",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        drawUptimeDimmed(ctx,x+LABEL_COL,ry+3);ry+=ROW_H;

        // FPS
        dataRow(ctx,x,ry,w,true);
        int fps=mc.getCurrentFps();int fpsCol=fps>=60?UI.GREEN:fps>=30?accent:UI.RED;
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"FPS",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        UI.textBoldShadow(ctx,String.valueOf(fps),x+LABEL_COL,ry+3,fpsCol);
        UI.sparkline(ctx,x+w-50,ry+2,42,ROW_H-4,fpsSamples,UI.withAlpha(fpsCol,0x55));ry+=ROW_H;

        // TPS
        dataRow(ctx,x,ry,w,false);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"TPS",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        if(mc.world!=null){float tps=mc.world.getTickManager().getTickRate();int tc=tps>=19.5f?UI.GREEN:tps>=15f?accent:UI.RED;UI.textBoldShadow(ctx,String.format("%.1f",tps),x+LABEL_COL,ry+3,tc);UI.sparkline(ctx,x+w-50,ry+2,42,ROW_H-4,tpsSamples,UI.withAlpha(tc,0x55));}ry+=ROW_H;

        // XYZ
        dataRow(ctx,x,ry,w,true);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);
        UI.text(ctx,"XYZ",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);
        if(mc.player!=null){
            int xv=(int)mc.player.getX(),yv=(int)mc.player.getY(),zv=(int)mc.player.getZ();
            int lx=x+LABEL_COL;
            UI.text(ctx,"X",(int)(lx/SC),(int)((ry+3)/SC),UI.GRAY);UI.textBoldShadow(ctx,String.valueOf(xv),(int)((lx+8)/SC),(int)((ry+3)/SC),UI.WHITE);
            UI.text(ctx,"Y",(int)((lx+44)/SC),(int)((ry+3)/SC),UI.GRAY);UI.textBoldShadow(ctx,String.valueOf(yv),(int)((lx+52)/SC),(int)((ry+3)/SC),UI.WHITE);
            UI.text(ctx,"Z",(int)((lx+90)/SC),(int)((ry+3)/SC),UI.GRAY);UI.textBoldShadow(ctx,String.valueOf(zv),(int)((lx+98)/SC),(int)((ry+3)/SC),UI.WHITE);
        }
        ctx.getMatrices().popMatrix();
        cy+=sysH+4;

        // ── SESSION STATS ─────────────────────────────────────────────────────
        UI.groupHeader(ctx,x,cy,w,"SESSION STATS",accent);cy+=13;
        int statsH=4*ROW_H+4;
        UI.fill(ctx,x,cy,w,statsH,UI.SURFACE);UI.hline(ctx,x,cy,w,UI.BORDER);
        ry=cy+2;

        // Current session
        dataRow(ctx,x,ry,w,false);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"Session",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        // sessionStartTime is 0 when stopped (we clear it on stop now), so compute live elapsed
        long liveMs = running && Tlntd.sessionStartTime > 0 ? System.currentTimeMillis() - Tlntd.sessionStartTime : 0;
        UI.textBoldShadow(ctx, running ? fmtElapsed(liveMs) : "--:--", x+LABEL_COL, ry+3, running?UI.WHITE:UI.GRAY_D);
        ry+=ROW_H;

        // Total farmed time (accumulated + current if running)
        dataRow(ctx,x,ry,w,true);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"Farmed",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        long farmedMs = Tlntd.totalFarmingMs + liveMs;
        String farmedStr = farmedMs > 0 ? fmtElapsed(farmedMs) : "--:--";
        UI.textBoldShadow(ctx, farmedStr, x+LABEL_COL, ry+3, farmedMs>0?accent:UI.GRAY_D);
        ry+=ROW_H;

        // Crops/hr
        dataRow(ctx,x,ry,w,false);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"Crops/hr",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        if(running&&liveMs>0){
            float hr=liveMs/3_600_000f;
            UI.textBoldShadow(ctx,String.valueOf(hr>0?(int)(Tlntd.sessionCropsCollected/hr):0),x+LABEL_COL,ry+3,accent);
        } else { UI.textBoldShadow(ctx,"--",x+LABEL_COL,ry+3,UI.GRAY_D); }
        ry+=ROW_H;

        // Crops collected this session
        dataRow(ctx,x,ry,w,true);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);UI.text(ctx,"Collected",(int)((x+8)/SC),(int)((ry+3)/SC),UI.GRAY);ctx.getMatrices().popMatrix();
        UI.textBoldShadow(ctx,running?String.valueOf(Tlntd.sessionCropsCollected):"--",x+LABEL_COL,ry+3,running?UI.WHITE:UI.GRAY_D);
        cy+=statsH+4;

        // ── LIFETIME ──────────────────────────────────────────────────────────
        UI.groupHeader(ctx,x,cy,w,"LIFETIME",accent);cy+=13;
        UI.fill(ctx,x,cy,w,ROW_H,UI.SURFACE);UI.hline(ctx,x,cy,w,UI.BORDER);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);
        TlntdConfig cfgL=TlntdConfig.load();
        UI.text(ctx,"Total crops",(int)((x+8)/SC),(int)((cy+3)/SC),UI.GRAY);
        UI.textBoldShadow(ctx,String.valueOf(cfgL.itemsCollected),(int)((x+LABEL_COL)/SC),(int)((cy+3)/SC),UI.WHITE);
        ctx.getMatrices().popMatrix();
    }

    private static String fmtElapsed(long ms){
        long s=ms/1000,m=s/60,h=m/60;
        return h>0?String.format("%dh %02dm",h,m%60):String.format("%dm %02ds",m,s%60);
    }
    private void dataRow(DrawContext ctx,int x,int y,int w,boolean alt){if(alt)UI.fill(ctx,x,y,w,ROW_H,UI.withAlpha(UI.WHITE,0x04));}
    private void drawUptimeDimmed(DrawContext ctx,int x,int y){
        try{long ms=ManagementFactory.getRuntimeMXBean().getUptime();
            String hm=String.format("%02dh %02dm ",TimeUnit.MILLISECONDS.toHours(ms),TimeUnit.MILLISECONDS.toMinutes(ms)%60);
            String s2=String.format("%02ds",TimeUnit.MILLISECONDS.toSeconds(ms)%60);
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(SC,SC);
            UI.textBoldShadow(ctx,hm,(int)(x/SC),(int)(y/SC),UI.WHITE);
            UI.textBoldShadow(ctx,s2,(int)((x+UI.textWidth(hm))/SC),(int)(y/SC),UI.withAlpha(UI.WHITE,0x77));
            ctx.getMatrices().popMatrix();
        }catch(Exception e){UI.textBoldShadow(ctx,"-- -- --",x,y,UI.GRAY_D);}
    }

    @Override public boolean mouseClicked(double mx,double my,int btn,int x,int y,int w){return false;}
    @Override public void mouseReleased(){}
    @Override public void updateDragging(int mx,int startX){}
}
