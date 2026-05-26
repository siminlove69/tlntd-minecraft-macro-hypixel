package me.shimmy.tlntd;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TlntdMenu extends Screen {

    static final int TAB_DASHBOARD=0,TAB_MACROS=1,TAB_SAFETY=2,TAB_HUD=3;
    static final int TAB_SETTINGS=4,TAB_CONSOLE=5,TAB_EVENTS=6;
    static final int W=350,H=250,NAV_W=90,NAV_W_COL=20;
    static final int TITLE_H=24,FOOTER_H=12,SEARCH_H=15,CORNER_R=4;
    private static final Identifier LOGO_ID=Identifier.of("tlntd","textures/gui/logo.png");

    public final TlntdConfig config=TlntdConfig.load();
    public static int currentTab=TAB_DASHBOARD;
    private final Map<Integer,IMenuSection> sections=new LinkedHashMap<>();

    private float openAnim=0,sectionAnim=1,slideAnim=1,pulseAnim=0,scanLineY=0;
    private final float[] navAnim=new float[7];
    private boolean navCollapsed=false; private float navColAnim=0;
    private int slideDir=1;

    private int dragOffX=0,dragOffY=0; private boolean draggingWindow=false; private int dragStartX,dragStartY;
    private String searchText=""; private boolean searchFocused=false;

    public boolean draggingRange,draggingESP,draggingAntiAFK,draggingHearts,draggingHudOpacity;
    public boolean draggingRed,draggingGreen,draggingBlue,draggingTps,draggingSchedule;

    private final int[] navY=new int[7],navId=new int[7]; private int navCount=0,searchBarY=0;
    private String tooltipText=null; private int tooltipX,tooltipY;
    private float progressLineAnim=0, currentScale=1f;

    // Per-tab outer scroll (pixels, virtual-space units)
    private final float[] tabScrollY=new float[7];

    private static final int QT_H=15;   // quick-toggle strip height
    // Content area bounds (virtual coords, dependent on drag offset)
    int contentY()   { return cy()+QT_H+4; }  // matches original: cy()+qtH+4
    int contentBot() { return wy()+H-FOOTER_H; }
    int availH()     { return contentBot()-contentY(); }

    // Credits/glitch
    private static final String[] CREDITS_AUTHORS={"sL.","songslover","senator.solista","shimmy","k.shimmy","siminlove69"};
    private static final int[] CREDITS_COLORS={0xFFFF8844,0xFFBB88FF,0xFFBBFF88,0xFFFF6B9D,0xFF88DDFF,0xFFFFDD44};
    private int creditsIdx=0; private long creditsSwapMs=0; private float creditsFade=1,premiumPhase=0,glitchTimer=0;
    private boolean glitching=false; private long glitchStartMs=0;

    // Hearts
    private static final int MAX_HEARTS=120;
    private final float[] hx=new float[MAX_HEARTS],hy=new float[MAX_HEARTS],hvy=new float[MAX_HEARTS];
    private final float[] hvx=new float[MAX_HEARTS],hphase=new float[MAX_HEARTS];
    private final int[] hcol=new int[MAX_HEARTS],hsz=new int[MAX_HEARTS];
    private final Random rng=new Random(); private float scanBeamY=0;

    // Stars
    private static final int MAX_STARS=90;
    private final float[] sx=new float[MAX_STARS],sy=new float[MAX_STARS],sbr=new float[MAX_STARS],sph=new float[MAX_STARS];
    private final int[] ssz=new int[MAX_STARS];

    // Matrix
    private static final int MAT_COLS=18;
    private final float[] matY=new float[MAT_COLS],matSpeed=new float[MAT_COLS];
    private final int[] matLen=new int[MAT_COLS],matCol=new int[MAT_COLS];

    int navW()  { return (int)MathHelper.lerp(navColAnim,NAV_W,NAV_W_COL); }
    int wx()    { return -W/2+dragOffX; }
    int wy()    { return -H/2+dragOffY; }
    int cx()    { return wx()+navW(); }
    int cy()    { return wy()+TITLE_H; }
    int cw()    { return W-navW(); }
    int ch()    { return H-TITLE_H-FOOTER_H; }

    public TlntdMenu(){
        super(Text.literal("TLNTD"));
        sections.put(TAB_DASHBOARD,new DashboardSection());
        sections.put(TAB_MACROS,   new MacrosSection(this));
        sections.put(TAB_SAFETY,   new SafetySection(this));
        sections.put(TAB_HUD,      new HudSection(this));
        sections.put(TAB_SETTINGS, new SettingsSection(this));
        sections.put(TAB_CONSOLE,  new ConsoleSection());
        sections.put(TAB_EVENTS,   new EventsSection());
        spawnHearts(Math.min(config.heartCount,MAX_HEARTS));
        spawnStars(); spawnMatrix();
        navCollapsed=config.navCollapsed;
    }

    private void spawnHearts(int count){
        int[]pal={0xFFFF6B9D,0xFFFF4488,0xFFFFAACC,0xFFFF8844,0xFFFFDD44,0xFF88DDFF,0xFFBB88FF,0xFFFF5566,0xFFFF3366,0xFFFF99BB,0xFFCC44FF,0xFFFF6644};
        for(int i=0;i<MAX_HEARTS;i++){hx[i]=rng.nextFloat()*W;hy[i]=i<count?rng.nextFloat()*H:-99;hvy[i]=0.15f+rng.nextFloat()*0.35f;hvx[i]=(rng.nextFloat()-0.5f)*0.18f;hphase[i]=rng.nextFloat()*(float)(Math.PI*2);hcol[i]=pal[rng.nextInt(pal.length)];hsz[i]=1+rng.nextInt(3);}
    }
    private void spawnStars(){for(int i=0;i<MAX_STARS;i++){sx[i]=rng.nextFloat()*W;sy[i]=rng.nextFloat()*H;sbr[i]=0.25f+rng.nextFloat()*0.75f;sph[i]=rng.nextFloat()*(float)(Math.PI*2);ssz[i]=rng.nextInt(3)==0?2:1;}}
    private void spawnMatrix(){for(int i=0;i<MAT_COLS;i++){matY[i]=-rng.nextFloat()*H;matSpeed[i]=0.4f+rng.nextFloat()*0.8f;matLen[i]=6+rng.nextInt(10);matCol[i]=java.awt.Color.HSBtoRGB(rng.nextFloat(),0.5f,0.9f);}}
    public void onBackgroundStyleChanged(){if(config.backgroundStyle==1)spawnStars();if(config.backgroundStyle==2)spawnMatrix();}
    public void refreshHearts(){spawnHearts(Math.min(config.heartCount,MAX_HEARTS));}
    public void playClick(){MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK,1.0f));}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta){
        openAnim=MathHelper.lerp(delta*0.18f,openAnim,1f);
        sectionAnim=MathHelper.lerp(delta*0.22f,sectionAnim,1f);
        slideAnim=MathHelper.lerp(delta*0.28f,slideAnim,1f);
        if(Tlntd.isRunning&&!Tlntd.activeBotType.equals("none")){
            int mt=Math.max(1,TlntdConfig.load().getTicks(Tlntd.activeBotType,Tlntd.currentStep));
            progressLineAnim=MathHelper.clamp((float)Tlntd.tickCounter/mt,0f,1f);
        } else { progressLineAnim=MathHelper.lerp(delta*0.08f,progressLineAnim,0f); }
        scanLineY=(scanLineY+delta*0.04f)%1f;
        pulseAnim=(float)(Math.sin(System.currentTimeMillis()/400.0)*0.5+0.5);
        navColAnim=MathHelper.lerp(delta*0.25f,navColAnim,navCollapsed?1f:0f);
        tooltipText=null;
        renderInGameBackground(ctx);
        int accent=computeAccent();
        currentScale=config.guiScale*(0.9f+openAnim*0.1f);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(this.width/2f,this.height/2f);
        ctx.getMatrices().scale(currentScale,currentScale);
        int smX=(int)((mouseX-this.width/2f)/currentScale);
        int smY=(int)((mouseY-this.height/2f)/currentScale);
        if(draggingWindow){dragOffX=smX-dragStartX;dragOffY=smY-dragStartY;}
        IMenuSection cur=sections.get(currentTab);
        if(cur!=null)cur.updateDragging(smX,cx());
        tickParticles();
        drawWindow(ctx,smX,smY,accent,cur);
        if(tooltipText!=null)UI.tooltip(ctx,tooltipX,tooltipY,tooltipText);
        ctx.getMatrices().popMatrix();
        super.render(ctx,mouseX,mouseY,delta);
    }

    private void tickParticles(){
        int count=Math.min(config.heartCount,MAX_HEARTS);
        for(int i=0;i<count;i++){hy[i]+=hvy[i];hx[i]+=hvx[i];if(hx[i]<0||hx[i]>W)hvx[i]=-hvx[i];if(hy[i]>H+10){hy[i]=-5-rng.nextFloat()*20;hx[i]=rng.nextFloat()*W;hvy[i]=0.15f+rng.nextFloat()*0.35f;hvx[i]=(rng.nextFloat()-0.5f)*0.18f;}}
        scanBeamY=(scanBeamY+0.003f)%1f; premiumPhase=(premiumPhase+0.018f)%1f; glitchTimer+=0.016f;
        if(!glitching&&glitchTimer>4f+rng.nextFloat()*3f){glitching=true;glitchStartMs=System.currentTimeMillis();glitchTimer=0;}
        if(glitching&&System.currentTimeMillis()-glitchStartMs>180)glitching=false;
        if(config.backgroundStyle==2)for(int i=0;i<MAT_COLS;i++){matY[i]+=matSpeed[i];if(matY[i]>H+matLen[i]*6){matY[i]=-rng.nextFloat()*30;matSpeed[i]=0.4f+rng.nextFloat()*0.8f;matLen[i]=6+rng.nextInt(10);}}
    }

    private static final int[][]HS={{0,1,0,1,0},{1,1,1,1,1},{0,1,1,1,0},{0,0,1,0,0}};
    private void drawHeart(DrawContext ctx,int x,int y,int sz,int col){for(int r=0;r<HS.length;r++)for(int c=0;c<HS[r].length;c++)if(HS[r][c]==1)ctx.fill(x+c*sz,y+r*sz,x+(c+1)*sz,y+(r+1)*sz,col);}
    private void drawRounded(DrawContext ctx,int x,int y,int w,int h,int col,int r){UI.fill(ctx,x+r,y,w-2*r,h,col);UI.fill(ctx,x,y+r,r,h-2*r,col);UI.fill(ctx,x+w-r,y+r,r,h-2*r,col);}
    private void drawRoundedOutline(DrawContext ctx,int x,int y,int w,int h,int col,int r){UI.hline(ctx,x+r,y,w-2*r,col);UI.hline(ctx,x+r,y+h-1,w-2*r,UI.withAlpha(col,0x99));UI.vline(ctx,x,y+r,h-2*r,UI.withAlpha(col,0x99));UI.vline(ctx,x+w-1,y+r,h-2*r,UI.withAlpha(col,0x99));}
    int computeAccent(){return config.resolveAccent((System.currentTimeMillis()%6000)/6000f);}
    void setTooltip(String t,int x,int y){tooltipText=t;tooltipX=x;tooltipY=y;}

    private void drawWindow(DrawContext ctx, int smX, int smY, int accent, IMenuSection cur){
        int wx=wx(),wy=wy(); long now=System.currentTimeMillis();

        // ── Window body ──────────────────────────────────────────────────────
        ctx.fill(wx+6,wy+6,wx+W+6,wy+H+6,0x55000000);
        ctx.fill(wx+3,wy+3,wx+W+3,wy+H+3,0x33000000);
        drawRounded(ctx,wx,wy,W,H,UI.BG,CORNER_R);
        ctx.fillGradient(wx+CORNER_R,wy-2,wx+W-CORNER_R,wy+1,0x00000000,UI.withAlpha(accent,0x28));
        for(int sl=wy+2;sl<wy+H;sl+=3)ctx.fill(wx,sl,wx+W,sl+1,0x06000000);
        for(int gx=cx()+8;gx<cx()+cw()-4;gx+=10)
            for(int gy=contentY()+4;gy<contentBot()-4;gy+=10)
                ctx.fill(gx,gy,gx+1,gy+1,0x0CFFFFFF);

        // ── Background particles ──────────────────────────────────────────────
        int bg=config.backgroundStyle;
        if(bg==0){int hc=Math.min(config.heartCount,MAX_HEARTS);for(int i=0;i<hc;i++){float hp=(float)(Math.sin(now/700.0+hphase[i])*0.5+0.5);drawHeart(ctx,wx+(int)hx[i],wy+(int)hy[i],hsz[i],UI.withAlpha(hcol[i],0x25+(int)(hp*0x38)));}}
        else if(bg==1){int sc=Math.max(1,config.heartCount*MAX_STARS/120);for(int i=0;i<sc;i++){float tw=(float)(Math.sin(now/700.0+sph[i])*0.5+0.5);int a=(int)(sbr[i]*tw*0x50);int px=wx+(int)sx[i],py=wy+(int)sy[i];ctx.fill(px,py,px+ssz[i],py+ssz[i],UI.withAlpha(UI.WHITE,a));if(ssz[i]==2&&tw>0.75f){ctx.fill(px-1,py,px+3,py+1,UI.withAlpha(UI.WHITE,a/3));ctx.fill(px,py-1,px+1,py+3,UI.withAlpha(UI.WHITE,a/3));}}}
        else if(bg==2){int mc2=Math.max(1,config.heartCount*MAT_COLS/120);int cw2=W/MAT_COLS;for(int i=0;i<mc2;i++){int colX=wx+i*cw2+cw2/2,headY=wy+(int)matY[i];for(int t=0;t<matLen[i];t++){int ty=headY-t*5;if(ty<wy||ty>wy+H)continue;float fade=1f-(float)t/matLen[i];int ta=(int)(fade*(t==0?0xCC:0x55));ctx.fill(colX-1,ty-1,colX+2,ty+2,t==0?UI.withAlpha(UI.WHITE,ta):UI.withAlpha(matCol[i],ta));}}}
        int beamY=wy+(int)(scanBeamY*H);ctx.fillGradient(wx,beamY-5,wx+W,beamY,0x00FFFFFF,0x06FFFFFF);ctx.fill(wx,beamY,wx+W,beamY+1,0x10FFFFFF);

        // ── SECTION CONTENT (scrolled) ─────────────────────────────────────
        float scroll=tabScrollY[currentTab];
        if(cur!=null){
            ctx.getMatrices().pushMatrix();
            float slideOff=(1f-slideAnim)*8f*slideDir;
            ctx.getMatrices().translate(slideOff,0f);
            // Pass scroll-offset y so section draws higher when scrolled down
            cur.render(ctx,cx()+10,contentY()-(int)scroll,cw()-20,smX,smY,accent,0);
            ctx.getMatrices().popMatrix();
        }

        // ── OVERFLOW MASK — solid fills that hide anything outside content ──
        // These extend far beyond the window in virtual space
        final int FAR=5000;
        // Above content area (title bar + above window)
        ctx.fill(wx-FAR, wy-FAR,     wx+W+FAR, contentY(),  UI.BG);
        // Below content area (footer + below window)
        ctx.fill(wx-FAR, contentBot(),wx+W+FAR, wy+H+FAR,   UI.BG);
        // Left of content (nav + left of window)
        ctx.fill(wx-FAR, contentY(), cx(),       contentBot(),UI.BG);
        // Right of content (right of window – only needed if no scrollbar)
        ctx.fill(cx()+cw(),contentY(),wx+W+FAR,  contentBot(),UI.BG);

        // ── SCROLLBAR (in content area, over mask) ─────────────────────────
        int prefH=cur!=null?cur.preferredHeight(cw()-20):availH();
        float maxScroll=Math.max(0,prefH-availH());
        if(prefH>availH()){
            int sbX=cx()+cw()-5,sbY=contentY(),sbH=availH();
            UI.fill(ctx,sbX,sbY,3,sbH,UI.withAlpha(UI.WHITE,0x0A));
            float frac=maxScroll>0?scroll/maxScroll:0f;
            int thumbH=Math.max(16,(int)(sbH*(float)availH()/prefH));
            int thumbY=sbY+(int)((sbH-thumbH)*frac);
            UI.fill(ctx,sbX,thumbY,3,thumbH,UI.withAlpha(accent,0x88));
            UI.fill(ctx,sbX+1,thumbY+1,1,thumbH-2,UI.withAlpha(UI.WHITE,0x33));
        }

        // ── TITLE BAR ─────────────────────────────────────────────────────
        ctx.fillGradient(wx,wy,wx+W/2,wy+TITLE_H,UI.withAlpha(accent,0x22),0x00000000);
        ctx.fillGradient(wx,wy,wx+W,wy+TITLE_H,0x16FFFFFF,0x00000000);
        int logoSz=12,logoX=wx+9,logoY=wy+(TITLE_H-logoSz)/2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED,LOGO_ID,logoX,logoY,0,0,logoSz,logoSz,logoSz,logoSz);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.9f,0.9f);
        UI.textShadow(ctx,"TLNTD",(int)((logoX+logoSz+5)/0.9f),(int)((wy+3)/0.9f),accent);ctx.getMatrices().popMatrix();
        // PREMIUM badge
        int premX=logoX+logoSz+5+(int)(UI.textWidth("TLNTD")*0.9f)+5,premY=wy+4;
        ctx.fillGradient(premX-2,premY-1,premX+56,premY+10,UI.withAlpha(accent,0x2A),UI.withAlpha(0xFF9966FF,0x1A));
        UI.hline(ctx,premX-2,premY-1,58,UI.withAlpha(accent,0x66));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.6f,0.6f);
        int px2=(int)(premX/0.6f); String ps="PREMIUM";
        for(int li=0;li<ps.length();li++){float hue=(premiumPhase+li*0.14f)%1f;int rc=java.awt.Color.HSBtoRGB(hue,0.6f,1.0f)|0xFF000000;float br=(float)(Math.sin(now/450.0+li*0.7)*0.15+0.85);UI.textShadow(ctx,String.valueOf(ps.charAt(li)),px2,(int)(premY/0.6f),UI.withAlpha(rc,(int)(br*0xEE)));px2+=UI.textWidth(String.valueOf(ps.charAt(li)))+1;}
        ctx.getMatrices().popMatrix();
        // Credits
        int credY=wy+TITLE_H/2+4,credX=logoX+logoSz+5;
        if(now-creditsSwapMs>2400){creditsIdx=(creditsIdx+1)%CREDITS_AUTHORS.length;creditsSwapMs=now;creditsFade=0;}
        creditsFade=Math.min(1f,creditsFade+0.07f);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.6f,0.6f);
        String aName=CREDITS_AUTHORS[creditsIdx];int aCol=CREDITS_COLORS[creditsIdx];
        float np=(float)(Math.sin(now/480.0+creditsIdx*1.3)*0.18+0.82);int nA=(int)(creditsFade*np*0xFF);
        int devW=UI.textWidth("developed by ");
        UI.text(ctx,"developed by ",(int)(credX/0.6f),(int)(credY/0.6f),UI.withAlpha(UI.GRAY_D,(int)(creditsFade*0xBB)));
        int nameX=(int)((credX+(int)(devW*0.6f))/0.6f);
        if(glitching){for(int ci=0;ci<aName.length();ci++){String ch=String.valueOf(aName.charAt(ci));UI.text(ctx,ch,nameX+(rng.nextInt(5)-2),(int)(credY/0.6f),UI.withAlpha(rng.nextInt(3)==0?accent:aCol,nA));nameX+=UI.textWidth(ch)+1;}}
        else UI.textShadow(ctx,aName,nameX,(int)(credY/0.6f),UI.withAlpha(aCol,nA));
        ctx.getMatrices().popMatrix();
        // Running badge
        if(Tlntd.isRunning){String badge=Tlntd.activeBotType.replace("_"," ").toUpperCase();int bw=(int)(UI.textWidth(badge)*0.65f),bx2=wx+W-bw-26,bY2=wy+TITLE_H/2;float gp=(float)(Math.sin(now/300.0)*0.3+0.7);UI.fill(ctx,bx2-10,bY2-6,bw+20,13,UI.withAlpha(UI.GREEN,0x1A));UI.hline(ctx,bx2-10,bY2-6,bw+20,UI.withAlpha(UI.GREEN,(int)(gp*0x55)));UI.statusDot(ctx,bx2-4,bY2,UI.withAlpha(UI.GREEN,(int)(gp*0xFF)));ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textShadow(ctx,badge,(int)((bx2+2)/0.65f),(int)((bY2-4)/0.65f),UI.GREEN);ctx.getMatrices().popMatrix();}
        // Close button
        int clx=wx+W-16,cly=wy+3; boolean closeH=UI.hovered(smX,smY,clx,cly,13,TITLE_H-6);
        if(closeH){UI.fill(ctx,clx,cly,13,TITLE_H-6,UI.withAlpha(UI.RED,0x44));UI.hline(ctx,clx,cly,13,UI.withAlpha(UI.RED,0x88));}
        UI.textCentered(ctx,"\u00d7",clx+6,wy+TITLE_H/2-4,closeH?UI.RED:UI.withAlpha(UI.WHITE,0x44));
        UI.hline(ctx,wx,wy+TITLE_H,W,UI.withAlpha(accent,0x55));

        // ── NAV ───────────────────────────────────────────────────────────
        int nw=navW();
        ctx.fillGradient(wx,wy+TITLE_H,wx+nw,wy+H,UI.withAlpha(accent,0x08),0x00000000);
        UI.vline(ctx,wx+nw-1,wy+TITLE_H,H-TITLE_H,UI.BORDER);
        renderNav(ctx,smX,smY,accent,wx,wy,nw);

        // ── QUICK TOGGLES ─────────────────────────────────────────────────
        drawQuickToggles(ctx,cx()+8,cy()+1,cw()-16,smX,smY,accent);
        UI.hline(ctx,cx(),cy()+QT_H+1,cw(),UI.withAlpha(accent,0x55));
        UI.hline(ctx,cx(),cy()+QT_H+2,cw(),UI.withAlpha(accent,0x0C));

        // ── TOASTS ────────────────────────────────────────────────────────
        renderToasts(ctx,wx,wy,now);

        // ── FOOTER ────────────────────────────────────────────────────────
        int fy=wy+H-FOOTER_H;
        ctx.fillGradient(wx,fy,wx+W,fy+FOOTER_H,UI.withAlpha(accent,0x0A),0x00000000);
        UI.hline(ctx,wx,fy,W,UI.withAlpha(accent,0x55));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.6f,0.6f);
        if(Tlntd.isRunning&&Tlntd.sessionStartTime>0){
            TlntdConfig cf=TlntdConfig.load();int ns=cf.getSteps(Tlntd.activeBotType).size();
            String stepStr="STEP "+(Tlntd.currentStep+1)+"/"+ns+"  \u00b7  "+Tlntd.activeBotType.replace("_"," ").toUpperCase();
            String timeStr=fmtShort(System.currentTimeMillis()-Tlntd.sessionStartTime);
            float rp=0.65f+pulseAnim*0.35f;
            UI.textShadow(ctx,stepStr,(int)((wx+8)/0.6f),(int)((fy+2)/0.6f),UI.withAlpha(UI.GREEN,(int)(rp*0xFF)));
            UI.textShadow(ctx,timeStr,(int)((wx+W-8-UI.textWidth(timeStr))/0.6f),(int)((fy+2)/0.6f),UI.withAlpha(UI.WHITE,0xAA));
        } else { UI.textShadow(ctx,"IDLE",(int)((wx+8)/0.6f),(int)((fy+2)/0.6f),UI.withAlpha(UI.GRAY_D,0xCC)); }
        ctx.getMatrices().popMatrix();
        UI.statusDot(ctx,wx+W-8,fy+FOOTER_H/2,UI.withAlpha(Tlntd.isRunning?UI.GREEN:UI.withAlpha(UI.WHITE,0x44),Tlntd.isRunning?(int)((0.55f+pulseAnim*0.45f)*0xFF):0x88));

        // ── SEGMENTED PROGRESS BAR ─────────────────────────────────────────
        if(Tlntd.isRunning&&!Tlntd.activeBotType.equals("none")){
            TlntdConfig cp=TlntdConfig.load();int ns=cp.getSteps(Tlntd.activeBotType).size();
            int barStart=wx+CORNER_R,barTotal=W-2*CORNER_R,segW=(barTotal-(ns-1))/ns;
            for(int si=0;si<ns;si++){int bx=barStart+si*(segW+1);float fill=si<Tlntd.currentStep?1f:si==Tlntd.currentStep?progressLineAnim:0f;UI.fill(ctx,bx,wy,segW,1,UI.withAlpha(UI.WHITE,0x10));if(fill>0){int fw=(int)(segW*fill);UI.fill(ctx,bx,wy,fw,1,accent);UI.fill(ctx,bx,wy+1,fw,1,UI.withAlpha(accent,0x40));}}
        }

        // ── CORNER BRACKETS + OUTLINE ─────────────────────────────────────
        int m=10,t=2;
        UI.fill(ctx,wx,wy,t,m,accent);UI.fill(ctx,wx,wy,m,t,accent);
        UI.fill(ctx,wx+W-t,wy,t,m,accent);UI.fill(ctx,wx+W-m,wy,m,t,accent);
        UI.fill(ctx,wx,wy+H-m,t,m,accent);UI.fill(ctx,wx,wy+H-t,m,t,accent);
        UI.fill(ctx,wx+W-t,wy+H-m,t,m,accent);UI.fill(ctx,wx+W-m,wy+H-t,m,t,accent);
        drawRoundedOutline(ctx,wx,wy,W,H,UI.withAlpha(accent,0x55),CORNER_R);
    }

    private void renderToasts(DrawContext ctx,int wx,int wy,long now){
        while(!Tlntd.toasts.isEmpty()&&Tlntd.toasts.peekFirst().expireMs()<now)Tlntd.toasts.pollFirst();
        int ti=0;
        for(Tlntd.Toast toast:Tlntd.toasts){
            if(ti>=2)break;
            long rem=toast.expireMs()-now;float alpha=Math.min((2500-rem+200)/200f,Math.min(rem/350f,1f));
            if(alpha<=0){ti++;continue;}
            int th=12,ty=wy+H-FOOTER_H-(ti+1)*(th+2)-2,tw=cw()-20,tx=cx()+10;
            UI.fill(ctx,tx,ty,tw,th,UI.withAlpha(UI.SURFACE2,(int)(alpha*0xCC)));
            UI.hline(ctx,tx,ty,tw,UI.withAlpha(toast.color(),(int)(alpha*0x88)));
            UI.fill(ctx,tx,ty,2,th,UI.withAlpha(toast.color(),(int)(alpha*0xDD)));
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);
            UI.textShadow(ctx,toast.msg(),(int)((tx+6)/0.65f),(int)((ty+2)/0.65f),UI.withAlpha(UI.WHITE,(int)(alpha*0xFF)));
            ctx.getMatrices().popMatrix();ti++;
        }
    }

    private static String fmtShort(long ms){long s=ms/1000,m=s/60,h=m/60;return h>0?String.format("%dh%02dm",h,m%60):String.format("%dm%02ds",m,s%60);}

    // ── NAV ───────────────────────────────────────────────────────────────
    private record NavItem(int id,String emoji,String label){}
    private static final NavItem[] NAV={
        new NavItem(TAB_DASHBOARD,"\uD83D\uDCCA","Dashboard"),new NavItem(TAB_MACROS,"\uD83C\uDF3F","Macros"),
        new NavItem(TAB_SAFETY,"\uD83D\uDEE1","Safety"),new NavItem(TAB_HUD,"\uD83D\uDC41","HUD"),
        new NavItem(TAB_SETTINGS,"\u2699","Settings"),new NavItem(TAB_CONSOLE,"\uD83D\uDCDF","Console"),
        new NavItem(TAB_EVENTS,"\uD83D\uDD14","Events"),
    };
    private void renderNav(DrawContext ctx,int smX,int smY,int accent,int wx,int wy,int nw){
        navCount=0; boolean collapsed=navColAnim>0.5f;
        if(!collapsed){
            searchBarY=wy+TITLE_H+3; boolean sf=searchFocused;
            if(sf||!searchText.isEmpty()){UI.fill(ctx,wx+5,searchBarY,nw-10,SEARCH_H-2,UI.withAlpha(UI.WHITE,0x06));UI.hline(ctx,wx+5,searchBarY+SEARCH_H-3,nw-10,UI.withAlpha(accent,sf?0x88:0x44));}
            String disp=searchText.isEmpty()&&!sf?"Search...":searchText+(sf?"\u258c":"");
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.text(ctx,disp,(int)((wx+8)/0.65f),(int)((searchBarY+4)/0.65f),searchText.isEmpty()&&!sf?UI.GRAY_D:sf?UI.WHITE:UI.GRAY);ctx.getMatrices().popMatrix();
        }
        int btnY=wy+(collapsed?TITLE_H+3:TITLE_H+SEARCH_H+3); boolean colH=UI.hovered(smX,smY,wx+nw-12,btnY,10,9);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.text(ctx,collapsed?"\u203a":"\u2039",(int)((wx+nw-8)/0.65f),(int)((btnY+1)/0.65f),colH?accent:UI.GRAY_D);ctx.getMatrices().popMatrix();
        int navLimit=wy+H-FOOTER_H-14,ny=wy+(collapsed?TITLE_H+16:TITLE_H+SEARCH_H+16);
        for(int i=0;i<NAV.length;i++){
            NavItem item=NAV[i];
            if(!searchText.isEmpty()&&!item.label().toLowerCase().contains(searchText.toLowerCase()))continue;
            if(ny+18>navLimit)break;
            if((i==2||i==4)&&!collapsed&&searchText.isEmpty())UI.hline(ctx,wx+8,ny-3,nw-16,UI.withAlpha(accent,0x1A));
            boolean sel=currentTab==item.id(),hov=UI.hovered(smX,smY,wx+2,ny,nw-4,18);
            navAnim[i]=MathHelper.lerp(0.18f,navAnim[i],(sel||hov)?1f:0f);
            if(sel){ctx.fillGradient(wx+2,ny,wx+nw-2,ny+18,UI.withAlpha(accent,0x2E),UI.withAlpha(accent,0x06));UI.fill(ctx,wx+2,ny,3,18,accent);}
            else if(navAnim[i]>0.02f)UI.fill(ctx,wx+4,ny,nw-6,18,UI.withAlpha(UI.WHITE,(int)(navAnim[i]*0x08)));
            int ic=sel?accent:hov?UI.WHITE:UI.GRAY;
            if(collapsed){ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.8f,0.8f);UI.text(ctx,item.emoji(),(int)((wx+nw/2-4)/0.8f),(int)((ny+5)/0.8f),ic);ctx.getMatrices().popMatrix();if(hov)setTooltip(item.label(),smX+nw+4,smY);}
            else{
                ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,item.emoji(),(int)((wx+10)/0.75f),(int)((ny+5)/0.75f),ic);UI.textShadow(ctx,item.label(),(int)((wx+22)/0.75f),(int)((ny+5)/0.75f),ic);ctx.getMatrices().popMatrix();
                int badge=navBadge(item.id());
                if(badge>0){String bs=badge>99?"99+":String.valueOf(badge);int bw=(int)(UI.textWidth(bs)*0.55f)+6,bx=wx+nw-bw-4,by=ny+5;int bc=item.id()==TAB_EVENTS&&hasSafetyEvent()?UI.RED:accent;UI.fill(ctx,bx,by,bw,9,UI.withAlpha(bc,0x28));UI.hline(ctx,bx,by,bw,UI.withAlpha(bc,0x66));ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.55f,0.55f);UI.textCentered(ctx,bs,(int)((bx+bw/2)/0.55f),(int)((by+1)/0.55f),bc);ctx.getMatrices().popMatrix();}
            }
            navY[navCount]=ny;navId[navCount]=item.id();navCount++;ny+=18;
        }
        if(!collapsed){int hintY=wy+H-FOOTER_H-11;UI.hline(ctx,wx+6,hintY,nw-12,UI.BORDER);ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.6f,0.6f);UI.textCentered(ctx,"[INS] Close",(int)((wx+nw/2)/0.6f),(int)((hintY+3)/0.6f),UI.GRAY_D);ctx.getMatrices().popMatrix();}
    }
    private int navBadge(int id){return switch(id){case TAB_SAFETY->config.safetyWhitelist.size();case TAB_CONSOLE->Math.min(Tlntd.consoleLog.size(),99);case TAB_EVENTS->Math.min(Tlntd.eventLog.size(),99);default->0;};}
    private boolean hasSafetyEvent(){for(String[]e:Tlntd.eventLog)if("SAFETY".equals(e[0]))return true;return false;}

    // Quick toggles
    private record QT(String label,String tip){}
    private static final QT[] QTS={new QT("AFK","Anti-AFK"),new QT("SAFE","Safety stop"),new QT("LOCK","Lock mouse"),new QT("STAFF","Staff detect"),new QT("FAKE","Fake activity")};
    private final int[]qtX=new int[QTS.length],qtW=new int[QTS.length]; private int qtY_r=0;
    private void drawQuickToggles(DrawContext ctx,int x,int y,int w,int smX,int smY,int accent){
        qtY_r=y; TlntdConfig c=config; boolean[]st={c.antiAFK,c.safetyAdminStop,c.lockMouse,c.detectStaff,c.fakeActivity};
        float sc=0.6f; int curX=x; long now=System.currentTimeMillis();
        for(int i=0;i<QTS.length;i++){int tw=(int)(UI.textWidth(QTS[i].label())*sc)+10,th=13; boolean on=st[i],hov=UI.hovered(smX,smY,curX,y,tw,th);if(on){float p=(float)(Math.sin(now/400.0+i)*0.15+0.85);UI.fill(ctx,curX,y,tw,th,UI.withAlpha(accent,(int)(p*0x28)));UI.hline(ctx,curX,y,tw,UI.withAlpha(accent,(int)(p*0x88)));UI.fill(ctx,curX,y,2,th,UI.withAlpha(accent,(int)(p*0xBB)));}else if(hov)UI.fill(ctx,curX,y,tw,th,UI.withAlpha(UI.WHITE,0x07));ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(sc,sc);UI.textCentered(ctx,QTS[i].label(),(int)((curX+tw/2)/sc),(int)((y+3)/sc),on?accent:hov?UI.GRAY:UI.GRAY_D);ctx.getMatrices().popMatrix();if(hov)setTooltip(QTS[i].tip(),smX+2,smY-16);qtX[i]=curX;qtW[i]=tw;curX+=tw+4;}
    }
    private boolean clickQT(double mx,double my,int btn){TlntdConfig c=config;for(int i=0;i<QTS.length;i++){if(UI.hovered(mx,my,qtX[i],qtY_r,qtW[i],13)){switch(i){case 0->c.antiAFK=!c.antiAFK;case 1->c.safetyAdminStop=!c.safetyAdminStop;case 2->c.lockMouse=!c.lockMouse;case 3->c.detectStaff=!c.detectStaff;case 4->c.fakeActivity=!c.fakeActivity;}c.save();playClick();return true;}}return false;}

    // Input
    @Override public boolean keyPressed(KeyInput input){
        int key=input.key();
        if(key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_INSERT){this.close();return true;}
        if(searchFocused&&key==GLFW.GLFW_KEY_BACKSPACE&&!searchText.isEmpty()){searchText=searchText.substring(0,searchText.length()-1);return true;}
        IMenuSection sec=sections.get(currentTab);if(sec!=null&&sec.onKeyPressed(key,0,0))return true;
        return super.keyPressed(input);
    }
    @Override public boolean charTyped(CharInput input){
        if(searchFocused){searchText+=(char)input.codepoint();return true;}
        IMenuSection sec=sections.get(currentTab);if(sec!=null&&sec.onCharTyped((char)input.codepoint(),0))return true;
        return super.charTyped(input);
    }
    @Override public boolean mouseClicked(Click click,boolean doubled){
        float sc=currentScale;
        int mx=(int)((click.x()-this.width/2f)/sc),my=(int)((click.y()-this.height/2f)/sc);
        int wx=wx(),wy=wy(),nw=navW();
        if(UI.hovered(mx,my,wx+W-16,wy+3,13,TITLE_H-6)){this.close();return true;}
        if(!navCollapsed&&UI.hovered(mx,my,wx+5,searchBarY,nw-10,SEARCH_H-2)){searchFocused=true;return true;}
        if(!UI.hovered(mx,my,wx+5,searchBarY,nw-10,SEARCH_H-2))searchFocused=false;
        int btnY=wy+(navCollapsed?TITLE_H+4:TITLE_H+SEARCH_H+4);
        if(UI.hovered(mx,my,wx+nw-12,btnY,10,9)){navCollapsed=!navCollapsed;config.navCollapsed=navCollapsed;config.save();playClick();return true;}
        if(UI.hovered(mx,my,wx+9+12+5,wy,W-9-12-30,TITLE_H)&&!draggingWindow){draggingWindow=true;dragStartX=mx-dragOffX;dragStartY=my-dragOffY;return true;}
        for(int i=0;i<navCount;i++){if(UI.hovered(mx,my,wx+2,navY[i],nw-4,18)){if(currentTab!=navId[i]){slideDir=navId[i]>currentTab?1:-1;currentTab=navId[i];sectionAnim=0;slideAnim=0;searchFocused=false;tabScrollY[currentTab]=0;playClick();}return true;}}
        if(clickQT(mx,my,click.button()))return true;
        // Adjust mouse y by scroll for section hit-testing
        IMenuSection sec=sections.get(currentTab);
        int scrolledMy=my+(int)tabScrollY[currentTab];
        if(sec!=null&&sec.mouseClicked(mx,scrolledMy,click.button(),cx()+10,contentY(),cw()-20))return true;
        searchFocused=false;
        return super.mouseClicked(click,doubled);
    }
    @Override public boolean mouseReleased(Click click){
        draggingWindow=false;draggingRange=draggingESP=draggingAntiAFK=draggingHearts=draggingHudOpacity=false;draggingRed=draggingGreen=draggingBlue=false;draggingTps=draggingSchedule=false;
        IMenuSection sec=sections.get(currentTab);if(sec!=null)sec.mouseReleased();config.save();return super.mouseReleased(click);
    }
    @Override public boolean mouseScrolled(double mx,double my,double hA,double vA){
        float sc=currentScale;
        int smX=(int)((mx-this.width/2f)/sc),smY=(int)((my-this.height/2f)/sc);
        if(UI.hovered(smX,smY,cx(),cy(),cw(),ch())){
            int dir=vA<0?1:-1;
            IMenuSection sec=sections.get(currentTab);
            // Sections with own internal scroll delegate to them
            if(sec instanceof MacrosSection ms){ms.scroll(dir);return true;}
            if(sec instanceof ConsoleSection cs){cs.scroll(dir);return true;}
            if(sec instanceof EventsSection es){es.scroll(dir);return true;}
            // Outer scroll for other tabs
            int prefH=sec!=null?sec.preferredHeight(cw()-20):availH();
            float maxScroll=Math.max(0,prefH-availH());
            tabScrollY[currentTab]=MathHelper.clamp(tabScrollY[currentTab]+dir*14,0,maxScroll);
            return true;
        }
        return super.mouseScrolled(mx,my,hA,vA);
    }
    public static String mcUptime(){try{long ms=ManagementFactory.getRuntimeMXBean().getUptime();return String.format("%02dh %02dm %02ds",TimeUnit.MILLISECONDS.toHours(ms),TimeUnit.MILLISECONDS.toMinutes(ms)%60,TimeUnit.MILLISECONDS.toSeconds(ms)%60);}catch(Exception e){return "--h --m --s";}}
}
