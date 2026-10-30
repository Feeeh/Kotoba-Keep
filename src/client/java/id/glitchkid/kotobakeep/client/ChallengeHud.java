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

final class ChallengeHud implements HudElement {
    private static final float WORD_SCALE=2.0f, RUBY_SCALE=1.0f, ROMAJI_SCALE=1.1f;
    private final KotobaClient state;
    ChallengeHud(KotobaClient state){this.state=state;}
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.gui.hud.isHidden())return;
        int accent=KotobaKeep.CONFIG.accentColor();
        if(state.challenge==null){
            if(!state.available()) {
                String status="Kotoba Keep · " + state.status();
                float scale=Math.min(1f,(g.guiWidth()-16f)/Math.max(1,mc.font.width(status)));
                g.pose().pushMatrix();g.pose().translate(g.guiWidth()/2f,KotobaKeep.CONFIG.hudTop);g.pose().scale(scale,scale);
                g.text(mc.font,status,-mc.font.width(status)/2,0,accent);g.pose().popMatrix();
            }
            return;
        }
        if(state.word==null)return;
        int wordWidth=0;
        for(var ruby:state.word.ruby())wordWidth+=segmentWidth(ruby);
        int width=Math.max(300,wordWidth+88), height=KotobaKeep.CONFIG.showRomaji?122:105;
        float scale=Math.min(1f,(g.guiWidth()-16f)/width);
        g.pose().pushMatrix();g.pose().translate((g.guiWidth()-width*scale)/2f,KotobaKeep.CONFIG.hudTop);g.pose().scale(scale,scale);
        g.fill(0,0,width,height,0xF020182B);g.fill(0,0,3,height,accent);
        g.fill(3,0,width,1,accent);g.fill(3,height-1,width,height,0xFF694A7C);
        String title=state.retryRemaining>0?"NEED MORE TIME?":"SAY IT IN JAPANESE";
        scaledText(g,title,56,10,width-108,0xFFEACFF7,1.05f);
        int seconds=state.retryRemaining>0?state.retryRemaining:state.remaining;
        String time=state.outcome==3?"DEMO":seconds>0?(int)Math.ceil(seconds/20.0)+"s":"...";
        g.fill(width-40,7,width-8,26,0xFF392845);
        g.pose().pushMatrix();g.pose().translate(width-24,12);g.pose().scale(1.1f,1.1f);
        g.text(mc.font,time,-mc.font.width(time)/2,0,0xFFF7EEFC);g.pose().popMatrix();
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(state.challenge.item()));
        if(item!=null){g.pose().pushMatrix();g.pose().translate(13,42);g.pose().scale(2f,2f);g.item(new ItemStack(item),0,0);g.pose().popMatrix();}
        int x=56;
        for(var ruby:state.word.ruby()) {
            int cell=segmentWidth(ruby);
            if(Japanese.hasKanji(ruby.text())) {
                g.pose().pushMatrix();g.pose().translate(x+cell/2f,30);g.pose().scale(RUBY_SCALE,RUBY_SCALE);
                g.text(mc.font,ruby.reading(),-mc.font.width(ruby.reading())/2,0,0xFFEACFF7);g.pose().popMatrix();
            }
            g.pose().pushMatrix();g.pose().translate(x+cell/2f,46);g.pose().scale(WORD_SCALE,WORD_SCALE);
            g.text(mc.font,ruby.text(),-mc.font.width(ruby.text())/2,0,accent);g.pose().popMatrix();
            x+=cell;
        }
        if(KotobaKeep.CONFIG.showRomaji) {
            String romaji=state.word.ruby().stream().map(r->Romaji.of(r.reading())).reduce((a,b)->a+" "+b).orElse("");
            scaledText(g,romaji,56,73,width-66,0xFFE6DDED,ROMAJI_SCALE);
        }
        int barY=height-32;
        g.fill(56,barY,width-12,barY+4,0xFF4B365C);
        int fullTicks=state.retryRemaining>0?Math.max(1,state.retryTotalTicks):Math.max(1,state.challenge.ticks());
        float fraction=Math.clamp(seconds/(float)fullTicks,0,1);
        if(state.outcome==1)fraction=1;
        g.fill(56,barY,56+(int)((width-68)*fraction),barY+4,accent);
        String status=state.outcome==1?"Nice! You keep it.":state.outcome==2?"Time's up · removed "+state.removed:state.outcome==0?"Paused · your items are safe":state.outcome==3?"Say it to keep it · glitchk.id":state.retryRequested?"Restarting your attempt...":state.retryRemaining>0?"Press ["+state.retryKeyName()+"] to retry · items are safe":!state.started?"Preparing microphone...":state.submitted?"Checking...":state.remaining==0?"Finishing recognition...":state.heard.isEmpty()?(state.challenge.retriesLeft()>0?"Listening · ["+state.retryKeyName()+"] retry":"Listening · final attempt"):"Heard: "+state.heard;
        scaledText(g,status,56,height-20,width-66,state.outcome==2?0xFFF1B2C9:state.outcome==1?0xFFB5EACC:0xFFEACFF7,1f);
        g.pose().popMatrix();
    }
    private static int segmentWidth(Japanese.Ruby ruby) {
        var font=Minecraft.getInstance().font;
        return (int)Math.ceil(Math.max(font.width(ruby.text())*WORD_SCALE,Japanese.hasKanji(ruby.text())?font.width(ruby.reading())*RUBY_SCALE:0))+4;
    }
    private static void scaledText(GuiGraphicsExtractor g,String text,int x,int y,int maxWidth,int color,float desiredScale) {
        var font=Minecraft.getInstance().font;float scale=Math.min(desiredScale,maxWidth/(float)Math.max(1,font.width(text)));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);g.text(font,text,0,0,color);g.pose().popMatrix();
    }
}
