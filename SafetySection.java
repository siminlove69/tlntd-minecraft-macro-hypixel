package me.shimmy.tlntd;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.math.MathHelper;
import java.util.ArrayList;
import java.util.List;

public class SafetySection implements IMenuSection {
    private final TlntdMenu parent;
    private boolean wlFocused=false;
    private String wlBuffer="";
    public SafetySection(TlntdMenu p){parent=p;}
    private static final int ROW=14;
    private int _cy_r1,_cy_r2,_cy_rangeSlider,_cy_tpsToggle,_cy_tpsSlider,_cy_inputR1,_cy_wlStart,_wlCount,_cy_wlField,_cy_sugStart;
    private List<String> _suggestions=new ArrayList<>();

    @Override
    public void render(DrawContext ctx,int x,int y,int w,int mx,int my,int accent,float delta){
        int cy=y;
        UI.groupHeader(ctx,x,cy,w,"DETECTION",accent);cy+=13;
        _cy_r1=cy;cy=row(ctx,x,cy,w,mx,my,"Stop when player detected nearby",parent.config.safetyAdminStop,accent);
        _cy_r2=cy;cy=row(ctx,x,cy,w,mx,my,"Stop when Hypixel staff in tab list",parent.config.detectStaff,accent);cy+=5;

        UI.groupHeader(ctx,x,cy,w,"DETECTION RANGE",accent);cy+=13;
        _cy_rangeSlider=cy;int rv=parent.config.safetyRangeLimit;int rangeC=rv<=30?UI.GREEN:rv<=70?UI.GOLD:UI.RED;
        boolean rH=UI.hovered(mx,my,x,cy,w,ROW);if(rH)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"Range",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);UI.textShadow(ctx,rv+"b",(int)((x+50)/0.75f),(int)((cy+4)/0.75f),rangeC);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+80,cy+7,w-86,(rv-5)/95f,accent);cy+=ROW+5;

        // TPS AUTO-PAUSE
        UI.groupHeader(ctx,x,cy,w,"TPS AUTO-PAUSE",accent);cy+=13;
        _cy_tpsToggle=cy;cy=row(ctx,x,cy,w,mx,my,"Pause when TPS drops below threshold",parent.config.tpsAutoPause,accent);
        _cy_tpsSlider=cy;float tv=parent.config.tpsThreshold;int tpsC=tv>=18f?UI.GREEN:tv>=14f?UI.GOLD:UI.RED;
        boolean tH=UI.hovered(mx,my,x,cy,w,ROW);if(tH)UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.text(ctx,"Threshold",(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.GRAY);UI.textShadow(ctx,String.format("%.0f TPS",tv),(int)((x+64)/0.75f),(int)((cy+4)/0.75f),tpsC);ctx.getMatrices().popMatrix();
        UI.slider(ctx,x+100,cy+7,w-106,(tv-5f)/15f,accent);cy+=ROW+5;

        UI.groupHeader(ctx,x,cy,w,"INPUT",accent);cy+=13;
        _cy_inputR1=cy;cy=row(ctx,x,cy,w,mx,my,"Lock mouse while macro runs",parent.config.lockMouse,accent);cy+=5;

        UI.groupHeader(ctx,x,cy,w,"PLAYER WHITELIST",accent);cy+=13;
        _cy_wlStart=cy;_wlCount=parent.config.safetyWhitelist.size();
        if(parent.config.safetyWhitelist.isEmpty()){
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.text(ctx,"No players whitelisted \u2014 type below to add",(int)((x+8)/0.65f),(int)((cy+3)/0.65f),UI.GRAY_D);ctx.getMatrices().popMatrix();cy+=12;
        } else {
            for(int wi=0;wi<parent.config.safetyWhitelist.size();wi++){
                boolean rmH=UI.hovered(mx,my,x+w-16,cy,14,ROW);
                if(UI.hovered(mx,my,x,cy,w,ROW))UI.fill(ctx,x,cy,w,ROW,UI.withAlpha(UI.WHITE,0x04));
                ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textShadow(ctx,parent.config.safetyWhitelist.get(wi),(int)((x+8)/0.75f),(int)((cy+4)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
                if(rmH)UI.fill(ctx,x+w-16,cy,14,ROW,UI.withAlpha(UI.RED,0x22));
                ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textShadow(ctx,"\u00d7",(int)((x+w-10)/0.75f),(int)((cy+4)/0.75f),rmH?UI.RED:UI.GRAY_D);ctx.getMatrices().popMatrix();
                cy+=ROW;
            }
        }
        _cy_wlField=cy;boolean wf=wlFocused;
        UI.fill(ctx,x,cy,w,14,wf?UI.SURFACE2:UI.SURFACE);UI.hline(ctx,x,cy,w,wf?UI.withAlpha(accent,0x88):UI.withAlpha(UI.WHITE,0x12));
        String wlDisp=wlBuffer.isEmpty()&&!wf?"Type to add player...":wlBuffer+(wf?"\u258c":"");
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);
        UI.text(ctx,wlDisp,(int)((x+6)/0.75f),(int)((cy+3)/0.75f),wlBuffer.isEmpty()&&!wf?UI.GRAY_D:UI.WHITE);
        if(wf&&!wlBuffer.isEmpty())UI.textShadow(ctx,"ENTER",(int)((x+w-32)/0.75f),(int)((cy+3)/0.75f),UI.withAlpha(accent,0xAA));
        ctx.getMatrices().popMatrix();cy+=16;

        // Suggestions
        _cy_sugStart=cy;_suggestions=buildSuggestions();
        if(wf&&!_suggestions.isEmpty()){
            ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.text(ctx,"Nearby players:",(int)((x+6)/0.65f),(int)(cy/0.65f),UI.GRAY_D);ctx.getMatrices().popMatrix();cy+=10;
            for(int si=0;si<Math.min(_suggestions.size(),3);si++){
                String sug=_suggestions.get(si);boolean sugH=UI.hovered(mx,my,x,cy,w,12);
                UI.fill(ctx,x,cy,w,12,sugH?UI.withAlpha(accent,0x18):UI.withAlpha(UI.WHITE,0x04));UI.fill(ctx,x,cy,2,12,UI.withAlpha(accent,sugH?0x88:0x33));
                ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textShadow(ctx,sug,(int)((x+8)/0.75f),(int)((cy+2)/0.75f),sugH?accent:UI.GRAY);ctx.getMatrices().popMatrix();
                cy+=13;
            }
        }

        // Panic footer
        int panY=cy+4;UI.fill(ctx,x,panY,w,13,UI.withAlpha(UI.RED,0x0A));UI.hline(ctx,x,panY,w,UI.withAlpha(UI.RED,0x33));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.65f,0.65f);UI.textShadow(ctx,"PANIC \u2014 Press DELETE to immediately stop.",(int)((x+6)/0.65f),(int)((panY+3)/0.65f),UI.withAlpha(UI.RED,0xCC));ctx.getMatrices().popMatrix();
    }

    private List<String> buildSuggestions(){
        List<String> out=new ArrayList<>();MinecraftClient mc=MinecraftClient.getInstance();
        if(mc.getNetworkHandler()==null)return out;
        String lower=wlBuffer.toLowerCase();
        for(PlayerListEntry e:mc.getNetworkHandler().getPlayerList()){
            String name=e.getProfile().name();if(name==null||name.isEmpty())continue;
            if(mc.player!=null&&name.equals(mc.player.getName().getString()))continue;
            if(parent.config.safetyWhitelist.contains(name))continue;
            if(wlBuffer.isEmpty()||name.toLowerCase().startsWith(lower)){out.add(name);if(out.size()>=3)break;}
        }
        return out;
    }

    private int row(DrawContext ctx,int x,int y,int w,int mx,int my,String label,boolean val,int accent){
        boolean h=UI.hovered(mx,my,x,y,w,ROW);if(h)UI.fill(ctx,x,y,w,ROW,UI.withAlpha(UI.WHITE,0x06));
        if(val)UI.fill(ctx,x,y,2,ROW,UI.withAlpha(accent,0x77));
        UI.checkbox(ctx,x+8,y+2,val,accent);
        ctx.getMatrices().pushMatrix();ctx.getMatrices().scale(0.75f,0.75f);UI.textShadow(ctx,label,(int)((x+21)/0.75f),(int)((y+3)/0.75f),UI.WHITE);ctx.getMatrices().popMatrix();
        return y+ROW;
    }

    @Override public boolean mouseClicked(double mx,double my,int btn,int x,int y,int w){
        TlntdConfig c=parent.config;
        if(UI.hovered(mx,my,x,_cy_r1,w,ROW)){c.safetyAdminStop=!c.safetyAdminStop;c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x,_cy_r2,w,ROW)){c.detectStaff=!c.detectStaff;c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+80,_cy_rangeSlider+3,w-86,8)){parent.draggingRange=true;return true;}
        if(UI.hovered(mx,my,x,_cy_tpsToggle,w,ROW)){c.tpsAutoPause=!c.tpsAutoPause;c.save();parent.playClick();return true;}
        if(UI.hovered(mx,my,x+100,_cy_tpsSlider+3,w-106,8)){parent.draggingTps=true;return true;}
        if(UI.hovered(mx,my,x,_cy_inputR1,w,ROW)){c.lockMouse=!c.lockMouse;c.save();parent.playClick();return true;}
        for(int wi=0;wi<_wlCount;wi++){int ry=_cy_wlStart+(_wlCount==0?0:wi*ROW);if(!c.safetyWhitelist.isEmpty()&&UI.hovered(mx,my,x+w-16,ry,14,ROW)){c.safetyWhitelist.remove(wi);c.save();parent.playClick();return true;}}
        if(UI.hovered(mx,my,x,_cy_wlField,w,14)){wlFocused=true;return true;}
        if(wlFocused&&!_suggestions.isEmpty()){int ss=_cy_sugStart+10;for(int si=0;si<Math.min(_suggestions.size(),3);si++){if(UI.hovered(mx,my,x,ss+si*13,w,12)){wlBuffer=_suggestions.get(si);addToWhitelist(c);return true;}}}
        wlFocused=false;return false;
    }

    private void addToWhitelist(TlntdConfig c){String n=wlBuffer.trim();if(!n.isEmpty()&&!c.safetyWhitelist.contains(n)){c.safetyWhitelist.add(n);c.save();parent.playClick();}wlBuffer="";wlFocused=false;}

    @Override public boolean onKeyPressed(int key,int scan,int mods){
        if(!wlFocused)return false;
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){wlFocused=false;return true;}
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER){if(!wlBuffer.trim().isEmpty())addToWhitelist(parent.config);return true;}
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE&&!wlBuffer.isEmpty()){wlBuffer=wlBuffer.substring(0,wlBuffer.length()-1);return true;}
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_TAB&&!_suggestions.isEmpty()){wlBuffer=_suggestions.get(0);return true;}
        return false;
    }
    @Override public boolean onCharTyped(char c,int m){if(wlFocused){wlBuffer+=c;return true;}return false;}
    @Override public int preferredHeight(int w) { return 420; }
    @Override public void mouseReleased(){}
    @Override public void updateDragging(int mx,int startX){
        TlntdConfig c=parent.config;
        if(parent.draggingRange){c.safetyRangeLimit=5+(int)(MathHelper.clamp((mx-startX-80)/(float)(260-86),0f,1f)*95);c.save();}
        if(parent.draggingTps){c.tpsThreshold=5f+MathHelper.clamp((mx-startX-100)/(float)(260-106),0f,1f)*15f;c.save();}
    }
}
