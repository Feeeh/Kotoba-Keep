package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.language.Japanese;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Full panel during play; a compact strip on top of open menus, which Minecraft draws over the HUD. */
final class ChallengeHud implements HudElement {
    private static final float WORD_SCALE=2.0f, RUBY_SCALE=1.0f, ROMAJI_SCALE=1.1f;
    private static final float COMPACT_WORD_SCALE=1.5f, COMPACT_RUBY_SCALE=0.75f, SMALL_SCALE=0.75f;
    private static final int COMPACT_TOP=4, COMPACT_HEIGHT=32, COMPACT_TEXT_MAX=160;
    private final KotobaClient state;
    ChallengeHud(KotobaClient state){this.state=state;}

    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.gui.hud.isHidden() || mc.gui.screen()!=null)return;
        if(state.challenge==null){ if(!state.available()) drawStatusLine(g); return; }
        if(state.word!=null) drawPanel(g);
    }
    /** Called after any open menu has drawn, so the challenge stays visible above it. */
    void drawOverScreen(GuiGraphicsExtractor g,int mouseX,int mouseY) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.gui.hud.isHidden() || state.challenge==null || state.word==null)return;
        drawCompact(g,mouseX,mouseY);
    }

    private void drawStatusLine(GuiGraphicsExtractor g) {
        var font=Minecraft.getInstance().font;
        String status="Kotoba Keep · " + state.status();
        float scale=Math.min(1f,(g.guiWidth()-16f)/Math.max(1,font.width(status)));
        g.pose().pushMatrix();g.pose().translate(g.guiWidth()/2f,KotobaKeep.CONFIG.hudTop);g.pose().scale(scale,scale);
        g.text(font,status,-font.width(status)/2,0,KotobaKeep.CONFIG.accentColor());g.pose().popMatrix();
    }
    private void drawPanel(GuiGraphicsExtractor g) {
        int accent=KotobaKeep.CONFIG.accentColor();
        int width=Math.max(300,wordWidth(WORD_SCALE,RUBY_SCALE)+88), height=KotobaKeep.CONFIG.showRomaji?122:105;
        float scale=Math.min(1f,(g.guiWidth()-16f)/width);
        g.pose().pushMatrix();g.pose().translate((g.guiWidth()-width*scale)/2f,KotobaKeep.CONFIG.hudTop);g.pose().scale(scale,scale);
        drawBackground(g,width,height,accent);
        scaledText(g,state.retryRemaining>0?"NEED MORE TIME?":"SAY IT IN JAPANESE",56,10,width-108,0xFFEACFF7,1.05f);
        drawTimer(g,width-40,7,32,19,1.1f);
        drawItem(g,13,42,2f);
        drawWord(g,56,30,46,WORD_SCALE,RUBY_SCALE,accent);
        if(KotobaKeep.CONFIG.showRomaji) scaledText(g,romaji(),56,73,width-66,0xFFE6DDED,ROMAJI_SCALE);
        drawProgress(g,56,height-32,width-12,4,accent);
        scaledText(g,statusText(),56,height-20,width-66,statusColor(),1f);
        g.pose().popMatrix();
    }
    private void drawCompact(GuiGraphicsExtractor g,int mouseX,int mouseY) {
        var font=Minecraft.getInstance().font;
        int accent=KotobaKeep.CONFIG.accentColor();
        String detail=KotobaKeep.CONFIG.showRomaji?romaji():"", status=statusText();
        int textX=28+wordWidth(COMPACT_WORD_SCALE,COMPACT_RUBY_SCALE)+6;
        int textWidth=Math.clamp((int)Math.ceil(Math.max(font.width(detail),font.width(status))*SMALL_SCALE),60,COMPACT_TEXT_MAX);
        int width=textX+textWidth+38;
        float scale=Math.min(1f,(g.guiWidth()-8f)/width), left=(g.guiWidth()-width*scale)/2f;
        // On small windows the strip can cover a large chest's top row; hovering it reveals the slots.
        if(mouseX>=left && mouseX<=left+width*scale && mouseY>=COMPACT_TOP && mouseY<=COMPACT_TOP+COMPACT_HEIGHT*scale)return;
        g.pose().pushMatrix();g.pose().translate(left,COMPACT_TOP);g.pose().scale(scale,scale);
        drawBackground(g,width,COMPACT_HEIGHT,accent);
        drawItem(g,8,8,1f);
        drawWord(g,28,3,11,COMPACT_WORD_SCALE,COMPACT_RUBY_SCALE,accent);
        scaledText(g,detail,textX,7,textWidth,0xFFE6DDED,SMALL_SCALE);
        scaledText(g,status,textX,17,textWidth,statusColor(),SMALL_SCALE);
        drawTimer(g,width-32,7,26,14,1f);
        drawProgress(g,4,COMPACT_HEIGHT-4,width-4,2,accent);
        g.pose().popMatrix();
    }

    private static void drawBackground(GuiGraphicsExtractor g,int width,int height,int accent) {
        g.fill(0,0,width,height,0xF020182B);g.fill(0,0,3,height,accent);
        g.fill(3,0,width,1,accent);g.fill(3,height-1,width,height,0xFF694A7C);
    }
    private void drawTimer(GuiGraphicsExtractor g,int x,int y,int width,int height,float textScale) {
        var font=Minecraft.getInstance().font;
        int ticks=ticksLeft();
        String time=state.outcome==3?"DEMO":ticks>0?(int)Math.ceil(ticks/20.0)+"s":"...";
        g.fill(x,y,x+width,y+height,0xFF392845);
        g.pose().pushMatrix();g.pose().translate(x+width/2f,y+(height-8*textScale)/2f);g.pose().scale(textScale,textScale);
        g.text(font,time,-font.width(time)/2,0,0xFFF7EEFC);g.pose().popMatrix();
    }
    private void drawItem(GuiGraphicsExtractor g,int x,int y,float scale) {
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(state.challenge.item()));
        if(item==null)return;
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.item(new ItemStack(item),0,0);g.pose().popMatrix();
    }
    /** The word with furigana centred above each kanji segment. */
    private void drawWord(GuiGraphicsExtractor g,int x,int rubyY,int wordY,float wordScale,float rubyScale,int accent) {
        var font=Minecraft.getInstance().font;
        for(var ruby:state.word.ruby()) {
            int cell=segmentWidth(ruby,wordScale,rubyScale);
            if(Japanese.hasKanji(ruby.text())) {
                g.pose().pushMatrix();g.pose().translate(x+cell/2f,rubyY);g.pose().scale(rubyScale,rubyScale);
                g.text(font,ruby.reading(),-font.width(ruby.reading())/2,0,0xFFEACFF7);g.pose().popMatrix();
            }
            g.pose().pushMatrix();g.pose().translate(x+cell/2f,wordY);g.pose().scale(wordScale,wordScale);
            g.text(font,ruby.text(),-font.width(ruby.text())/2,0,accent);g.pose().popMatrix();
            x+=cell;
        }
    }
    private void drawProgress(GuiGraphicsExtractor g,int x,int y,int right,int height,int accent) {
        int fullTicks=state.retryRemaining>0?Math.max(1,state.retryTotalTicks):Math.max(1,state.challenge.ticks());
        float fraction=state.outcome==1?1:Math.clamp(ticksLeft()/(float)fullTicks,0,1);
        g.fill(x,y,right,y+height,0xFF4B365C);
        g.fill(x,y,x+(int)((right-x)*fraction),y+height,accent);
    }

    private int ticksLeft() { return state.retryRemaining>0?state.retryRemaining:state.remaining; }
    private String romaji() { return state.word.ruby().stream().map(r->Romaji.of(r.reading())).reduce((a,b)->a+" "+b).orElse(""); }
    private String statusText() {
        if(state.outcome==1)return "Nice! You keep it.";
        if(state.outcome==2)return "Time's up · removed "+state.removed;
        if(state.outcome==0)return "Paused · your items are safe";
        if(state.outcome==3)return "Say it to keep it · glitchk.id";
        if(state.retryRequested)return "Restarting your attempt...";
        if(state.retryRemaining>0)return "Press ["+state.retryKeyName()+"] to retry · items are safe";
        if(!state.started)return "Preparing microphone...";
        if(state.submitted)return "Checking...";
        if(state.remaining==0)return "Finishing recognition...";
        if(!state.heard.isEmpty())return "Heard: "+state.heard;
        return state.challenge.retriesLeft()>0?"Listening · ["+state.retryKeyName()+"] retry":"Listening · final attempt";
    }
    private int statusColor() { return state.outcome==2?0xFFF1B2C9:state.outcome==1?0xFFB5EACC:0xFFEACFF7; }
    private int wordWidth(float wordScale,float rubyScale) {
        int width=0;
        for(var ruby:state.word.ruby())width+=segmentWidth(ruby,wordScale,rubyScale);
        return width;
    }
    private static int segmentWidth(Japanese.Ruby ruby,float wordScale,float rubyScale) {
        var font=Minecraft.getInstance().font;
        return (int)Math.ceil(Math.max(font.width(ruby.text())*wordScale,Japanese.hasKanji(ruby.text())?font.width(ruby.reading())*rubyScale:0))+4;
    }
    private static void scaledText(GuiGraphicsExtractor g,String text,int x,int y,int maxWidth,int color,float desiredScale) {
        var font=Minecraft.getInstance().font;float scale=Math.min(desiredScale,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(font,text,0,0,color);g.pose().popMatrix();
    }
}
