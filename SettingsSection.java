package me.shimmy.tlntd;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class SettingsSection implements IMenuSection {
    private final TlntdMenu parent;
    public SettingsSection(TlntdMenu p){parent=p;}
    private static final int ROW=14;

    private int _rgbY,_customY,_rY,_gY,_bY,_scaleY,_heartsY,_antiAfkToggleY,_antiAfkSliderY;
    private int _schedToggleY,_schedSliderY,_bgStyleY,_themePresetY,_exportY,_importY,_profileListY,_resetY;
    private boolean importFocused=false; private String importBuffer="",importStatus="";
    private boolean profileInputFocused=false; private String profileBuffer="";

    @Override
    public void render(DrawContext ctx,int x,int y,int w,int mx,int my,int accent,float delta){
        int cy=y; boolean rgb=parent.config.rgbMode,customOn=parent.config.useCustomColor&&!rgb;

        // COLOR
        UI.groupHeader(ctx,x,cy,w,"COLOR",accent);cy+=13;
        _rgbY=cy;cy=checkRow(ctx,x,cy,w,mx,my,"RGB Mode (rainbow accent)",rgb,accent);
        if(!rgb){_customY=cy;cy=checkRow(ctx,x,cy,w,mx,my,"Custom Accent Color",parent.config.useCustomColor,accent);
            if(customOn){_rY=cy;cy=colorRow(ctx,x,cy,w,mx,my,"R",parent.config.red,0xFFFF4040,accent);_gY=cy;cy=colorRow(ctx,x,cy,w,mx,my,"G",parent.config.green,0xFF40FF40,accent);_bY=cy;cy=colorRow(ctx,x,cy,w,mx,my,"B",parent.config.blue,0xFF4080FF,accent);
                UI.fill(ctx,x+w-18,_rY,14,3*ROW-1,0xFF000000|(parent.config.red<<16)|(parent.config.green<<8)|parent.config.blue);}}
        cy+=3;

        // THEME PRESET
        UI.groupHeader(ctx,x,cy,w,"THEME PRESET",accent);cy+=13;
        _themePresetY=cy; int swW=(w-6)/TlntdConfig.PRESET_COLORS.length;
        for(int pi=0;pi<TlntdConfig.PRESET_COLORS.length;pi++){
            int px=x+pi*(swW+1); boolean sel=parent.config.themePreset==pi,hovP=UI.hovered(mx,my,px,cy,swW,18); int pc=TlntdConfig.PRESET_COLORS[pi];
            UI.fill(ctx,px,cy,swW,18,UI.withAlpha(pc,sel?0x44:hovP?0x22:0x0E));UI.hline(ctx,px,cy,swW,UI.withAlpha(pc,sel?0xDD:hovP?0x77:0x33));
            if(sel)UI.hline(ctx,px,cy+17,swW,UI.withAlpha(pc,0xBB));
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.55f,0.55f);UI.textCentered(ctx,TlntdConfig.PRESET_NAMES[pi],(int)((px+swW/2)/0.55f),(int)((cy+5)/0.55f),sel?pc:UI.withAlpha(pc,0xBB));ctx.getMatrices().popMatrix();
        }
        cy+=22;

        // BACKGROUND
        UI.groupHeader(ctx,x,cy,w,"BACKGROUND",accent);cy+=13;
        _bgStyleY=cy; String[]bgNames={"Hearts","Stars","Matrix","None"}; int bgW=(w-3)/4;
        for(int bi=0;bi<bgNames.length;bi++){
            int bx=x+bi*(bgW+1); boolean selBg=parent.config.backgroundStyle==bi,hovBg=UI.hovered(mx,my,bx,cy,bgW,14);
            UI.fill(ctx,bx,cy,bgW,14,selBg?UI.withAlpha(accent,0x28):hovBg?UI.withAlpha(UI.WHITE,0x07):UI.SURFACE);
            UI.hline(ctx,bx,cy,bgW,selBg?UI.withAlpha(accent,0x88):UI.withAlpha(UI.WHITE,0x14));
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textCentered(ctx,bgNames[bi],(int)((bx+bgW/2)/0.65f),(int)((cy+3)/0.65f),selBg?accent:hovBg?UI.WHITE:UI.GRAY);ctx.getMatrices().popMatrix();
        }
        cy+=18;

        // INTERFACE
        UI.groupHeader(ctx,x,cy,w,"INTERFACE",accent);cy+=13;
        _scaleY=cy; boolean rH=UI.hovered(mx,my,x,cy,w,ROW);if(rH)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"GUI Scale",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);ctx.getMatrices().popMatrix();
        boolean mH=UI.hovered(mx,my,x+w-52,cy+2,14,10),pH=UI.hovered(mx,my,x+w-36,cy+2,14,10);
        UI.arrowBtn(ctx,x+w-52,cy+2,14,10,"-",mH,accent);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textCentered(ctx,String.format("%.1f",parent.config.guiScale)+"x",(int)((x+w-31)/0.75f),(int)((cy+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        UI.arrowBtn(ctx,x+w-36,cy+2,14,10,"+",pH,accent);cy+=ROW;

        _heartsY=cy;boolean hH=UI.hovered(mx,my,x,cy,w,ROW);if(hH)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"Particles",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);UI.text(ctx,String.valueOf(parent.config.heartCount),(int)((x+70)/0.75f),(int)((cy+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+80,cy+7,w-86,parent.config.heartCount/120f,0xFFFF6BA0);cy+=ROW+5;

        // ANTI-AFK
        UI.groupHeader(ctx,x,cy,w,"ANTI-AFK",accent);cy+=13;
        _antiAfkToggleY=cy;cy=checkRow(ctx,x,cy,w,mx,my,"Enabled",parent.config.antiAFK,accent);
        _antiAfkSliderY=cy;boolean aH=UI.hovered(mx,my,x,cy,w,ROW);if(aH)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"Interval",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);UI.text(ctx,parent.config.antiAFKInterval+"t",(int)((x+58)/0.75f),(int)((cy+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+80,cy+7,w-86,parent.config.antiAFKInterval/1200f,accent);cy+=ROW+8;

        // SCHEDULED STOP
        UI.groupHeader(ctx,x,cy,w,"SCHEDULED STOP",accent);cy+=13;
        _schedToggleY=cy;cy=checkRow(ctx,x,cy,w,mx,my,"Stop bot after time limit",parent.config.scheduledStopEnabled,accent);
        _schedSliderY=cy;boolean sH2=UI.hovered(mx,my,x,cy,w,ROW);if(sH2)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        int mins=(int)parent.config.scheduledStopMinutes; String timeStr=mins>=60?(mins/60)+"h "+(mins%60)+"m":mins+"m";
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"Duration",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);UI.textShadow(ctx,timeStr,(int)((x+62)/0.75f),(int)((cy+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+92,cy+7,w-98,(parent.config.scheduledStopMinutes-5f)/595f,accent);cy+=ROW+8;

        // CONFIG EXPORT / IMPORT
        UI.groupHeader(ctx,x,cy,w,"CONFIG EXPORT / IMPORT",accent);cy+=13;
        _exportY=cy;boolean expH=UI.hovered(mx,my,x,cy,(w/2)-2,ROW);
        UI.fill(ctx,x,cy,(w/2)-2,ROW,expH?UI.withAlpha(accent,0x22):UI.SURFACE);UI.hline(ctx,x,cy,(w/2)-2,expH?UI.withAlpha(accent,0x66):UI.BORDER);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textCentered(ctx,"Copy to Clipboard",(int)((x+(w/2-2)/2)/0.65f),(int)((cy+4)/0.65f),expH?accent:UI.GRAY);ctx.getMatrices().popMatrix();
        _importY=cy;int impX=x+(w/2)+2,impW=w-(w/2)-2;
        UI.fill(ctx,impX,cy,impW,ROW,importFocused?UI.SURFACE2:UI.SURFACE);
        UI.hline(ctx,impX,cy,impW,!importStatus.isEmpty()?(importStatus.equals("OK")?UI.withAlpha(UI.GREEN,0x88):UI.withAlpha(UI.RED,0x88)):importFocused?UI.withAlpha(accent,0x66):UI.BORDER);
        String impDisp=importBuffer.isEmpty()&&!importFocused?"Paste to import...":importBuffer.length()>14?"..."+importBuffer.substring(importBuffer.length()-11)+(importFocused?"\u258c":""):importBuffer+(importFocused?"\u258c":"");
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.6f,0.6f);UI.text(ctx,impDisp,(int)((impX+4)/0.6f),(int)((cy+4)/0.6f),!importStatus.isEmpty()?(importStatus.equals("OK")?UI.GREEN:UI.RED):importBuffer.isEmpty()&&!importFocused?UI.GRAY_D:UI.WHITE);ctx.getMatrices().popMatrix();
        cy+=ROW+8;

        // PROFILES
        UI.groupHeader(ctx,x,cy,w,"PROFILES",accent);cy+=13;
        _profileListY=cy;
        boolean pFoc=profileInputFocused;
        UI.fill(ctx,x,cy,w-36,ROW,pFoc?UI.SURFACE2:UI.SURFACE);UI.hline(ctx,x,cy,w-36,pFoc?UI.withAlpha(accent,0x66):UI.BORDER);
        String pDisp=profileBuffer.isEmpty()&&!pFoc?"Profile name...":profileBuffer+(pFoc?"\u258c":"");
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.text(ctx,pDisp,(int)((x+5)/0.65f),(int)((cy+4)/0.65f),profileBuffer.isEmpty()&&!pFoc?UI.GRAY_D:UI.WHITE);ctx.getMatrices().popMatrix();
        boolean saveH=UI.hovered(mx,my,x+w-34,cy,34,ROW);
        UI.fill(ctx,x+w-34,cy,34,ROW,saveH?UI.withAlpha(accent,0x22):UI.SURFACE);UI.hline(ctx,x+w-34,cy,34,saveH?UI.withAlpha(accent,0x66):UI.BORDER);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textCentered(ctx,"Save",(int)((x+w-17)/0.65f),(int)((cy+4)/0.65f),saveH?accent:UI.GRAY);ctx.getMatrices().popMatrix();
        cy+=ROW+3;
        List<String>profiles=TlntdConfig.listProfiles();
        for(int pi=0;pi<Math.min(profiles.size(),4);pi++){
            String name=profiles.get(pi); boolean isCur=name.equals(parent.config.activeProfile);
            boolean loadH=UI.hovered(mx,my,x,cy,w-28,12),delH=!name.equals("default")&&UI.hovered(mx,my,x+w-26,cy,24,12);
            UI.fill(ctx,x,cy,w,12,isCur?UI.withAlpha(accent,0x18):loadH?UI.withAlpha(UI.WHITE,0x06):UI.SURFACE);
            if(isCur)UI.fill(ctx,x,cy,2,12,UI.withAlpha(accent,0x88));UI.hline(ctx,x,cy,w,UI.BORDER);
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textShadow(ctx,name,(int)((x+6)/0.65f),(int)((cy+2)/0.65f),isCur?accent:loadH?UI.WHITE:UI.GRAY);
            if(!name.equals("default")){if(delH)UI.fill(ctx,x+w-26,cy,24,12,UI.withAlpha(UI.RED,0x22));UI.textShadow(ctx,"del",(int)((x+w-20)/0.65f),(int)((cy+2)/0.65f),delH?UI.RED:UI.GRAY_D);}
            ctx.getMatrices().popMatrix();cy+=13;
        }
        cy+=5;

        // RESET
        _resetY=cy;boolean resetH=UI.hovered(mx,my,x,cy,w,14);
        UI.fill(ctx,x,cy,w,14,resetH?UI.withAlpha(UI.RED,0x18):UI.SURFACE);UI.hline(ctx,x,cy,w,resetH?UI.withAlpha(UI.RED,0x55):UI.BORDER);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textCentered(ctx,"RESET CONFIG TO DEFAULTS",(int)((x+w/2)/0.65f),(int)((cy+3)/0.65f),resetH?UI.RED:UI.GRAY);ctx.getMatrices().popMatrix();
    }

    private int checkRow(DrawContext ctx,int x,int y,int w,int mx,int my,String label,boolean val,int accent){
        boolean h=UI.hovered(mx,my,x,y,w,ROW);if(h)UI.fill(ctx,x,y,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        if(val)UI.fill(ctx,x,y,2,ROW,UI.withAlpha(accent,0x77));UI.checkbox(ctx,x+8,y+3,val,accent);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textShadow(ctx,label,(int)((x+21)/0.75f),(int)((y+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        return y+ROW;
    }
    private int colorRow(DrawContext ctx,int x,int y,int w,int mx,int my,String ch,int val,int col,int accent){
        boolean h=UI.hovered(mx,my,x,y,w,ROW);if(h)UI.fill(ctx,x,y,w,ROW,UI.withAlpha(UI.WHITE,0x05));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,ch,(int)((x+8)/0.75f),(int)((y+4)/0.75f),UI.GRAY);UI.text(ctx,String.valueOf(val),(int)((x+20)/0.75f),(int)((y+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+46,y+7,w-68,val/255f,col);return y+ROW;
    }

    @Override public boolean mouseClicked(double mx,double my,int btn,int x,int y,int w){
        TlntdConfig c=parent.config; boolean rgb=c.rgbMode,customOn=c.useCustomColor&&!rgb;
        if(UI.hovered(mx,my,x,_rgbY,w,ROW)){c.rgbMode=!c.rgbMode;c.save();parent.playClick();return true;}
        if(!rgb&&UI.hovered(mx,my,x,_customY,w,ROW)){c.useCustomColor=!c.useCustomColor;c.save();parent.playClick();return true;}
        if(!rgb&&customOn){if(UI.hovered(mx,my,x+46,_rY+3,w-68,8)){parent.draggingRed=true;return true;}if(UI.hovered(mx,my,x+46,_gY+3,w-68,8)){parent.draggingGreen=true;return true;}if(UI.hovered(mx,my,x+46,_bY+3,w-68,8)){parent.draggingBlue=true;return true;}}
        int swW=(w-6)/TlntdConfig.PRESET_COLORS.length;
        for(int pi=0;pi<TlntdConfig.PRESET_COLORS.length;pi++){int px=x+pi*(swW+1);if(UI.hovered(mx,my,px,_themePresetY,swW,18)){c.themePreset=pi;if(pi<6){c.useCustomColor=false;c.rgbMode=false;}c.save();parent.playClick();return true;}}
        int bgW=(w-3)/4;for(int bi=0;bi<4;bi++){int bx=x+bi*(bgW+1);if(UI.hovered(mx,my,bx,_bgStyleY,bgW,14)){c.backgroundStyle=bi;c.save();parent.playClick();parent.onBackgroundStyleChanged();return true;}}
        if(UI.hovered(mx,my,x+w-52,_scaleY+2,14,10)){c.guiScale=Math.max(0.5f,c.guiScale-0.1f);c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+w-36,_scaleY+2,14,10)){c.guiScale=Math.min(2.0f,c.guiScale+0.1f);c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+80,_heartsY+3,w-86,8)){parent.draggingHearts=true;return true;}
        if(UI.hovered(mx,my,x,_antiAfkToggleY,w,ROW)){c.antiAFK=!c.antiAFK;c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+80,_antiAfkSliderY+3,w-86,8)){parent.draggingAntiAFK=true;return true;}
        if(UI.hovered(mx,my,x,_schedToggleY,w,ROW)){c.scheduledStopEnabled=!c.scheduledStopEnabled;c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+92,_schedSliderY+3,w-98,8)){parent.draggingSchedule=true;return true;}
        if(UI.hovered(mx,my,x,_exportY,(w/2)-2,ROW)){String b64=c.toBase64();GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(),b64);parent.playClick();return true;}
        int impX=x+(w/2)+2,impW=w-(w/2)-2;
        if(UI.hovered(mx,my,impX,_importY,impW,ROW)){importFocused=true;String clip=GLFW.glfwGetClipboardString(MinecraftClient.getInstance().getWindow().getHandle());if(clip!=null&&!clip.isBlank())importBuffer=clip.trim();return true;}
        importFocused=false;
        if(UI.hovered(mx,my,x+w-34,_profileListY,34,ROW)){String name=profileBuffer.trim();if(!name.isEmpty()){c.activeProfile=name;c.saveAs(name);profileBuffer="";profileInputFocused=false;parent.playClick();}return true;}
        if(UI.hovered(mx,my,x,_profileListY,w-36,ROW)){profileInputFocused=true;return true;}
        profileInputFocused=false;
        List<String>profiles=TlntdConfig.listProfiles(); int profRowStart=_profileListY+ROW+3;
        for(int pi=0;pi<Math.min(profiles.size(),4);pi++){String name=profiles.get(pi);int ry2=profRowStart+pi*13;if(!name.equals("default")&&UI.hovered(mx,my,x+w-26,ry2,24,12)){TlntdConfig.deleteProfile(name);parent.playClick();return true;}if(UI.hovered(mx,my,x,ry2,w-28,12)){TlntdConfig.loadProfile(name).save();parent.playClick();return true;}}
        if(UI.hovered(mx,my,x,_resetY,w,14)){new TlntdConfig().save();parent.playClick();return true;}
        return false;
    }
    @Override public boolean onKeyPressed(int key,int scan,int mods){
        if(importFocused){if(key==GLFW.GLFW_KEY_ESCAPE){importFocused=false;return true;}if(key==GLFW.GLFW_KEY_ENTER){doImport();return true;}if(key==GLFW.GLFW_KEY_BACKSPACE&&!importBuffer.isEmpty()){importBuffer=importBuffer.substring(0,importBuffer.length()-1);return true;}return false;}
        if(profileInputFocused){if(key==GLFW.GLFW_KEY_ESCAPE){profileInputFocused=false;return true;}if(key==GLFW.GLFW_KEY_BACKSPACE&&!profileBuffer.isEmpty()){profileBuffer=profileBuffer.substring(0,profileBuffer.length()-1);return true;}return false;}
        return false;
    }
    @Override public boolean onCharTyped(char c,int m){if(importFocused){importBuffer+=c;return true;}if(profileInputFocused&&(Character.isLetterOrDigit(c)||c=='_'||c=='-')){profileBuffer+=c;return true;}return false;}
    private void doImport(){TlntdConfig imp=TlntdConfig.fromBase64(importBuffer.trim());if(imp!=null){imp.save();importStatus="OK";importBuffer="";parent.playClick();}else{importStatus="ERR";}importFocused=false;}
    @Override public int preferredHeight(int w) { return 620; }
    @Override public void mouseReleased(){}
    @Override public void updateDragging(int mx,int startX){
        TlntdConfig c=parent.config;
        if(parent.draggingRed){c.red=MathHelper.clamp((int)((mx-startX-46)/(float)(260-68)*255),0,255);c.save();}
        if(parent.draggingGreen){c.green=MathHelper.clamp((int)((mx-startX-46)/(float)(260-68)*255),0,255);c.save();}
        if(parent.draggingBlue){c.blue=MathHelper.clamp((int)((mx-startX-46)/(float)(260-68)*255),0,255);c.save();}
        if(parent.draggingHearts){c.heartCount=MathHelper.clamp((int)((mx-startX-80)/(float)(260-86)*120),0,120);c.save();parent.refreshHearts();}
        if(parent.draggingAntiAFK){c.antiAFKInterval=MathHelper.clamp((int)((mx-startX-80)/(float)(260-86)*1200),20,1200);c.save();}
        if(parent.draggingSchedule){c.scheduledStopMinutes=MathHelper.clamp(5+(mx-startX-92)/(float)(260-98)*595,5,600);c.save();}
    }
}
