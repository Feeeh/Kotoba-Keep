package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.KotobaKeep;
import id.glitchkid.kotobakeep.language.Japanese;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Full panel during play; a compact strip on top of open menus, which Minecraft draws over the HUD. */
final class ChallengeHud implements HudElement {
    // Full panel: one row (icon, word, romaji over meaning, timer), the status under it, the countdown along the bottom edge.
    private static final float WORD_SCALE=2.0f, RUBY_SCALE=1.0f, ICON_SCALE=1.5f, TIMER_SCALE=1.25f, MEANING_SCALE=0.9f, STATUS_SCALE=0.9f;
    private static final int PANEL_HEIGHT=52, PANEL_MIN_WIDTH=200, PAD=10, GAP=8, ICON_SIZE=24, SIDE_MAX_WIDTH=140;
    private static final int RUBY_TOP=7, WORD_TOP=17, ICON_TOP=8, SIDE_TOP=10, SIDE_MIDDLE=16, SIDE_STEP=12, STATUS_TOP=39;
    private static final int DIVIDER_TOP=8, DIVIDER_BOTTOM=32, TIMER_TOP=9, TIMER_WIDTH=28, TIMER_HEIGHT=22;
    private static final float COMPACT_WORD_SCALE=1.5f, COMPACT_RUBY_SCALE=0.75f, SMALL_SCALE=0.75f;
    private static final int COMPACT_TOP=4, COMPACT_HEIGHT=32, COMPACT_TEXT_MAX=160;
    private static final int ACCENT_WIDTH=2, PROGRESS_HEIGHT=2, KANJI_PAD=4;
    private static final int BACKGROUND=0xF020182B, SLOT_COLOR=0xFF2E2240, BADGE_COLOR=0xFF392845, TRACK_COLOR=0xFF4B365C;
    /** The Kotoba Keep brand colour, also used for the lock boxes around mob drops. */
    static final int ACCENT=0xFFC888E8;
    private static final int ROMAJI_COLOR=0xFFE6DDED, MEANING_COLOR=0xFFB9A9C9, FAIL_COLOR=0xFFF1B2C9;
    private static final double TICKS_PER_SECOND=20;
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
        g.text(font,status,-font.width(status)/2,0,ACCENT);g.pose().popMatrix();
    }
    private void drawPanel(GuiGraphicsExtractor g) {
        var font=Minecraft.getInstance().font;
        String romaji=romaji(), meaning=meaning();
        int wordX=PAD+ICON_SIZE+GAP, wordEnd=wordX+wordWidth(WORD_SCALE,RUBY_SCALE);
        int sideWidth=Math.min(SIDE_MAX_WIDTH,(int)Math.ceil(Math.max(font.width(romaji),font.width(meaning)*MEANING_SCALE)));
        int sideX=wordEnd+GAP, rowEnd=sideWidth>0?sideX+sideWidth:wordEnd;
        int width=Math.max(PANEL_MIN_WIDTH,rowEnd+GAP+TIMER_WIDTH+PAD);
        float scale=Math.min(1f,(g.guiWidth()-16f)/width);
        g.pose().pushMatrix();g.pose().translate((g.guiWidth()-width*scale)/2f,KotobaKeep.CONFIG.hudTop);g.pose().scale(scale,scale);
        drawBackground(g,width,PANEL_HEIGHT);
        roundedRect(g,PAD-2,ICON_TOP-2,ICON_SIZE+4,ICON_SIZE+4,SLOT_COLOR);
        drawItem(g,PAD,ICON_TOP,ICON_SCALE);
        drawWord(g,wordX,RUBY_TOP,WORD_TOP,WORD_SCALE,RUBY_SCALE);
        if(sideWidth>0) drawSide(g,sideX,sideWidth,romaji,meaning);
        drawTimer(g,width-PAD-TIMER_WIDTH,TIMER_TOP,TIMER_WIDTH,TIMER_HEIGHT,TIMER_SCALE);
        scaledText(g,statusText(),PAD,STATUS_TOP,width-2*PAD,statusColor(),STATUS_SCALE);
        drawProgress(g,width,PANEL_HEIGHT);
        g.pose().popMatrix();
    }
    /** Romaji over the meaning, beside the word behind a thin divider; a single line sits centred. */
    private static void drawSide(GuiGraphicsExtractor g,int x,int width,String romaji,String meaning) {
        g.fill(x-GAP/2,DIVIDER_TOP,x-GAP/2+1,DIVIDER_BOTTOM,TRACK_COLOR);
        boolean both=!romaji.isEmpty() && !meaning.isEmpty();
        if(!romaji.isEmpty()) scaledText(g,romaji,x,both?SIDE_TOP:SIDE_MIDDLE,width,ROMAJI_COLOR,1f);
        if(!meaning.isEmpty()) scaledText(g,meaning,x,both?SIDE_TOP+SIDE_STEP:SIDE_MIDDLE,width,MEANING_COLOR,MEANING_SCALE);
    }
    private void drawCompact(GuiGraphicsExtractor g,int mouseX,int mouseY) {
        var font=Minecraft.getInstance().font;
        String detail=compactDetail(), status=statusText();
        int textX=28+wordWidth(COMPACT_WORD_SCALE,COMPACT_RUBY_SCALE)+6;
        int textWidth=Math.clamp((int)Math.ceil(Math.max(font.width(detail),font.width(status))*SMALL_SCALE),60,COMPACT_TEXT_MAX);
        int width=textX+textWidth+38;
        float scale=Math.min(1f,(g.guiWidth()-8f)/width), left=(g.guiWidth()-width*scale)/2f;
        // On small windows the strip can cover a large chest's top row; hovering it reveals the slots.
        if(mouseX>=left && mouseX<=left+width*scale && mouseY>=COMPACT_TOP && mouseY<=COMPACT_TOP+COMPACT_HEIGHT*scale)return;
        g.pose().pushMatrix();g.pose().translate(left,COMPACT_TOP);g.pose().scale(scale,scale);
        drawBackground(g,width,COMPACT_HEIGHT);
        drawItem(g,8,8,1f);
        drawWord(g,28,3,11,COMPACT_WORD_SCALE,COMPACT_RUBY_SCALE);
        scaledText(g,detail,textX,7,textWidth,ROMAJI_COLOR,SMALL_SCALE);
        scaledText(g,status,textX,17,textWidth,statusColor(),SMALL_SCALE);
        drawTimer(g,width-32,7,26,14,1f);
        drawProgress(g,width,COMPACT_HEIGHT);
        g.pose().popMatrix();
    }

    private static void drawBackground(GuiGraphicsExtractor g,int width,int height) {
        roundedRect(g,0,0,width,height,BACKGROUND);
        g.fill(0,1,ACCENT_WIDTH,height-1,ACCENT);
    }
    /** A rectangle without its corner pixels, which reads as softly rounded. */
    private static void roundedRect(GuiGraphicsExtractor g,int x,int y,int width,int height,int color) {
        g.fill(x+1,y,x+width-1,y+height,color);
        g.fill(x,y+1,x+1,y+height-1,color);g.fill(x+width-1,y+1,x+width,y+height-1,color);
    }
    private void drawTimer(GuiGraphicsExtractor g,int x,int y,int width,int height,float desiredScale) {
        var font=Minecraft.getInstance().font;
        String time=timerText();
        float textScale=Math.min(desiredScale,(width-4f)/font.width(time));
        roundedRect(g,x,y,width,height,BADGE_COLOR);
        g.pose().pushMatrix();g.pose().translate(x+width/2f,y+(height-8*textScale)/2f);g.pose().scale(textScale,textScale);
        g.text(font,time,-font.width(time)/2,0,flashing()?FAIL_COLOR:0xFFF7EEFC);g.pose().popMatrix();
    }
    private void drawItem(GuiGraphicsExtractor g,int x,int y,float scale) {
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(state.challenge.item()));
        if(item==null)return;
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.item(new ItemStack(item),0,0);g.pose().popMatrix();
    }
    /** The word with furigana centred above each kanji segment, unless the furigana has faded. */
    private void drawWord(GuiGraphicsExtractor g,int x,int rubyY,int wordY,float wordScale,float rubyScale) {
        var font=Minecraft.getInstance().font;
        boolean furigana=furigana();
        for(var ruby:state.word.ruby()) {
            int cell=segmentWidth(ruby,wordScale,rubyScale,furigana);
            if(furigana && Japanese.hasKanji(ruby.text())) {
                g.pose().pushMatrix();g.pose().translate(x+cell/2f,rubyY);g.pose().scale(rubyScale,rubyScale);
                g.text(font,ruby.reading(),-font.width(ruby.reading())/2,0,0xFFEACFF7);g.pose().popMatrix();
            }
            g.pose().pushMatrix();g.pose().translate(x+cell/2f,wordY);g.pose().scale(wordScale,wordScale);
            g.text(font,ruby.text(),-font.width(ruby.text())/2,0,ACCENT);g.pose().popMatrix();
            x+=cell;
        }
    }
    /** The countdown drains along the card's bottom edge. */
    private void drawProgress(GuiGraphicsExtractor g,int width,int height) {
        int fullTicks=state.retryRemaining>0?Math.max(1,state.retryTotalTicks):Math.max(1,state.challenge.ticks());
        float fraction=state.outcome==1?1:Math.clamp(ticksLeft()/(float)fullTicks,0,1);
        int left=ACCENT_WIDTH, right=width-1, top=height-PROGRESS_HEIGHT;
        g.fill(left,top,right,height,TRACK_COLOR);
        g.fill(left,top,left+(int)((right-left)*fraction),height,ACCENT);
    }

    private int ticksLeft() { return state.retryRemaining>0?state.retryRemaining:state.remaining; }
    /** Right after a replay the timer briefly shows what it cost instead of the time. */
    private String timerText() {
        if(state.outcome==3)return "DEMO";
        if(flashing())return replayCost();
        int ticks=ticksLeft();
        return ticks>0?seconds(ticks)+"s":"...";
    }
    private boolean flashing() { return state.replayFlash>0 && state.outcome==-1; }
    private String replayCost() { return "-"+seconds(state.challenge.replayCost())+"s"; }
    private static int seconds(int ticks) { return (int)Math.ceil(ticks/TICKS_PER_SECOND); }
    /** The name in the player's game language; empty when untranslated, blank or the same Japanese. */
    private String meaning() {
        String key=state.challenge.key(), text=Component.translatable(key).getString();
        return text.isBlank() || text.equals(key) || text.equals(state.word.text()) ? "" : text;
    }
    private String compactDetail() {
        String romaji=romaji(), meaning=meaning();
        return romaji.isEmpty() || meaning.isEmpty() ? romaji+meaning : romaji+" · "+meaning;
    }
    /** Empty when romaji is turned off or has faded for this word. */
    private String romaji() {
        if(!KotobaKeep.CONFIG.showRomaji || faded(KotobaKeep.CONFIG.hideRomajiAfter))return "";
        return state.word.ruby().stream().map(r->Romaji.of(r.reading())).reduce((a,b)->a+" "+b).orElse("");
    }
    private boolean furigana() { return !faded(KotobaKeep.CONFIG.hideFuriganaAfter); }
    /** A hint fades once the word has been answered right this many times in a row. */
    private boolean faded(int after) { return KotobaKeep.CONFIG.fadeHints && state.challenge.streak()>=after; }
    private String statusText() {
        boolean gentle=state.challenge.gentle();
        if(state.outcome==1)return "Nice! You keep it.";
        if(state.outcome==2)return gentle?"Time's up · first meeting, nothing lost":"Time's up · removed "+state.removed;
        if(state.outcome==0)return "Paused · your items are safe";
        if(state.outcome==3)return "Say it to keep it · glitchk.id";
        if(state.retryRequested)return "Restarting your attempt...";
        if(state.retryRemaining>0)return "Need more time? ["+state.retryKeyName()+"] retry · items are safe";
        if(state.listening)return gentle?"Listen... · first meeting, items are safe":"Listen...";
        if(!state.started)return "Preparing microphone...";
        if(state.submitted)return "Checking...";
        if(state.remaining==0)return "Finishing recognition...";
        // A wrong attempt is when hearing the word again helps most, so the replay hint stays.
        if(!state.heard.isEmpty())return "Heard: "+state.heard+replayHint();
        String retry=state.challenge.retriesLeft()>0?"["+state.retryKeyName()+"] retry":"final attempt";
        return "Listening"+replayHint()+" · "+retry;
    }
    private String replayHint() { return state.canReplay()?" · ["+state.replayKeyName()+"] hear "+replayCost():""; }
    private int statusColor() { return state.outcome==2?FAIL_COLOR:state.outcome==1?0xFFB5EACC:0xFFEACFF7; }
    private int wordWidth(float wordScale,float rubyScale) {
        int width=0;
        boolean furigana=furigana();
        for(var ruby:state.word.ruby())width+=segmentWidth(ruby,wordScale,rubyScale,furigana);
        return width;
    }
    /** Kana keep their natural spacing; kanji with furigana get room for it and a little air around it. */
    private static int segmentWidth(Japanese.Ruby ruby,float wordScale,float rubyScale,boolean furigana) {
        var font=Minecraft.getInstance().font;
        float text=font.width(ruby.text())*wordScale;
        if(!furigana || !Japanese.hasKanji(ruby.text()))return (int)Math.ceil(text);
        return (int)Math.ceil(Math.max(text,font.width(ruby.reading())*rubyScale))+KANJI_PAD;
    }
    private static void scaledText(GuiGraphicsExtractor g,String text,int x,int y,int maxWidth,int color,float desiredScale) {
        var font=Minecraft.getInstance().font;float scale=Math.min(desiredScale,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(font,text,0,0,color);g.pose().popMatrix();
    }
}
