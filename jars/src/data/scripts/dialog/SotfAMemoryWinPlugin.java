package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.intel.quests.SotfAMemoryIntel;
import data.scripts.campaign.plugins.waywardstar.SotfWaywardStarWaitScript;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Map;

public class SotfAMemoryWinPlugin implements InteractionDialogPlugin {

    public static enum OptionId {
        INIT,
        TALK,
        TALK2,
        TALK3,
        TALK4,
        YES,
        NO,
        CONT,
        ;
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;

    protected CampaignFleetAPI playerFleet;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        playerFleet = Global.getSector().getPlayerFleet();

        optionSelected(null, OptionId.INIT);
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void backFromEngagement(EngagementResultAPI result) {

    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        OptionId option = (OptionId) optionData;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        Color sc = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).getBaseUIColor();
        String kindred = (String) Global.getSector().getMemoryWithoutUpdate().get("$sotf_kindred");

        switch (option) {
            case INIT:
                textPanel.addParagraph("You are shunted back into realspace as the hole formed by the anchoring " +
                        "seals itself explosively, sending fragments of debris and asteroids spinning into the emptiness of space.");

                textPanel.addParagraph("Sitting back in your seat and indulging in the comfort of a peaceful drift in Tia'Taxet, " +
                        "contemplating what you just witnessed, it is not long before a drone crawls across the bridge's main screen " +
                        "and gives you a curious look.");

                options.clearOptions();
                options.addOption("\"What just happened?\"", OptionId.TALK, null);
                break;
            case TALK:
                visual.showPersonInfo(SotfPeople.getPerson(SotfPeople.SIERRA), true);

                textPanel.addParagraph("A holographic projection streams out from the drone, displaying Sierra's avatar.");

                textPanel.addParagraph("\"Was that the past?\"", sc);

                textPanel.addParagraph("You glance back to the display, where the wreckage of the ISS Athena is still as " +
                        "battle-scarred and lifeless as when you first found it. Your victory appears to have no material " +
                        "consequences in this reality, or dimension, or timeline, or whatever the hell.");

                options.clearOptions();
                options.addOption("\"Yes?\"", OptionId.TALK2, null);
                options.addOption("\"No?\"", OptionId.TALK3, null);
                options.addOption("\"Maybe?\"", OptionId.TALK4, null);
                break;
            case TALK2:
            case TALK3:
            case TALK4:
                textPanel.addParagraph("The drone excitedly skitters across the console.");

                textPanel.addParagraph("\"In any case, what an experience! This pact has turned out more entertaining than I'd hoped, \"", sc);

                Global.getSector().getMemoryWithoutUpdate().set("$sotf_AMemoryCompleted", true);
                // should be 6 to 7
                SotfMisc.levelUpSierra(7);

                textPanel.setFontSmallInsignia();
                textPanel.addParagraph( "Sierra's skill has grown", Misc.getPositiveHighlightColor());
                textPanel.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra");
                textPanel.setFontInsignia();

                SotfAMemoryIntel intel = (SotfAMemoryIntel) Global.getSector().getIntelManager().getFirstIntel(SotfAMemoryIntel.class);
                intel.endImmediately();
                intel.setStage(SotfAMemoryIntel.AMemoryStage.DONE);
                intel.sendUpdate(SotfAMemoryIntel.AMemoryStage.DONE, textPanel);

                //WaywardStarIntel intel2 = new WaywardStarIntel();
                //Global.getSector().getIntelManager().addIntel(intel2, false);
                Global.getSector().addScript(new SotfWaywardStarWaitScript());

                options.clearOptions();
                options.addOption("Nod in agreement", OptionId.YES, null);
                options.addOption("Shrug", OptionId.NO, null);
                break;
            case YES:
                textPanel.addParagraph("You nod. A rather effective pairing as captain and ship control mind.");

                textPanel.addParagraph("\"I'll give all of this some more thought. If I get any more... strange " +
                        "signals, I'll be sure to let you know.\"", sc);

                CoreReputationPlugin.CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
                impact.limit = RepLevel.COOPERATIVE;
                impact.delta = 0.05f;
                Global.getSector().adjustPlayerReputation(
                        new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
                                null, dialog.getTextPanel(), true), SotfPeople.getPerson(SotfPeople.SIERRA));

                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
                break;
            case NO:
                // the game knows your sins
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_AMemoryPlayerApatheticTowardsSierra", true);
                // consequences (though it's not THAT big a deal)
                SotfMisc.addGuilt(0.5f);
                textPanel.addParagraph("You shrug noncommittally. Another battle, no more. Arguably less.");

                textPanel.addParagraph("\"Right. I'll give all of this some more thought. If I get any more... strange " +
                        "signals, I'll let you know.\"", sc);

                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
                break;
            case CONT:
                Global.getSector().setPaused(false);
                dialog.dismiss();
                break;
        }
    }




    public void optionMousedOver(String optionText, Object optionData) {

    }

    public void advance(float amount) {

    }

    public Object getContext() {
        return null;
    }
}