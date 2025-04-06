package data.scripts.dialog.haunted;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import data.scripts.campaign.customstart.HauntedDreamCampaignVFX;

import java.util.Map;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfHauntedDreamBase implements InteractionDialogPlugin {

    public static enum OptionId {
        WAKE_UP,
        DISMISS,
        ;
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    protected float counter = 0f;
    protected int stage = 0;

    // text delays
    protected float s = 2.25f;
    protected float m = 3.5f;
    protected float l = 4.5f;

    protected CampaignFleetAPI playerFleet;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        dialog.setBackgroundDimAmount(0.4f);

        playerFleet = Global.getSector().getPlayerFleet();
        dialog.setPromptText("");
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
        Global.getSoundPlayer().playCustomMusic(1, 1, "sotf_scavenge_ambience", true);
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void backFromEngagement(EngagementResultAPI result) {

    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;
        // handle defaultLeave when quitting the dialogue
        if (!(optionData instanceof OptionId option)) {
            Global.getSector().setPaused(false);
            dialog.dismiss();
            return;
        }

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        switch (option) {
            case WAKE_UP:
                dialog.setInteractionTarget(playerFleet);
                stage = 99;
                counter = -100f;
                options.clearOptions();
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
                Global.getSoundPlayer().restartCurrentMusic();
                dialog.setPromptText("You decide to...");
//                addPostWakeupText();
//                options.addOption("Return to your duties", OptionId.DISMISS);
                RuleBasedInteractionDialogPluginImpl delegate = new RuleBasedInteractionDialogPluginImpl();
                delegate.setEmbeddedMode(true);
                delegate.init(dialog);
                delegate.fireBest(getEndingTrigger());
                //FireBest.fire(null, dialog, getMemoryMap(), getEndingTrigger());

                HauntedDreamCampaignVFX.fadeOut(0.5f);

                break;
            case DISMISS:
                Global.getSector().setPaused(false);
                dialog.dismiss();
                break;
        }
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void advance(float amount) {
        counter += amount;
    }

    public Object getContext() {
        return null;
    }

    public void addLine(String text, int atStage, float delayAfter) {
        if (stage == atStage && counter >= 0) {
            stage++;
            textPanel.addPara(text);
            counter = 0 - delayAfter;
        }
    }

    public void addWakeUpOption() {
        options.clearOptions();
        options.addOption("Wake up", OptionId.WAKE_UP);
    }

    public String getEndingTrigger() {
        return "sotfHauntedDream1End";
    }
}