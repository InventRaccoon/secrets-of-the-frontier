package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import data.scripts.campaign.intel.quests.SotfAMemoryIntel;
import data.scripts.utils.SotfMisc;

import java.util.Map;

public class SotfAMemoryLosePlugin implements InteractionDialogPlugin {

    public static enum OptionId {
        INIT,
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

        switch (option) {
            case INIT:
                textPanel.addParagraph("You are shunted back into realspace as the hole formed by the anchoring " +
                        "seals itself explosively, sending fragments of debris and asteroids spinning into the emptiness of space.");
                if (SotfMisc.playerHasSierra()) {
                    textPanel.addParagraph("You take a moment to breathe, now safe in a calm drift in Tia'Taxet. A drone " +
                            "spurs to life and gives you a moment to readjust.");

                    textPanel.addParagraph("Though silent, it somehow comes across as slightly dejected.");

                    SotfAMemoryIntel intel = (SotfAMemoryIntel) Global.getSector().getIntelManager().getFirstIntel(SotfAMemoryIntel.class);
                    intel.setStage(SotfAMemoryIntel.AMemoryStage.REFIGHT);
                    intel.sendUpdate(SotfAMemoryIntel.AMemoryStage.REFIGHT, textPanel);

                    options.clearOptions();
                    options.addOption("Return to your duties", OptionId.CONT, null);
                } else {
                    textPanel.addParagraph("You take a moment to breathe, drifting through Tia'Taxet in a now-destroyed " +
                            "ship. Your personal communications device immediately lights up as the rest of the fleet dispatches " +
                            "shuttles to secure the ship and retrieve you safely.");

                    textPanel.addParagraph("Though a proper recovery operation was impossible, the recovery team reports that " +
                            "some systems appear to be functional, and a reflexive blast shield has managed to save Sierra's AI " +
                            "core from irreparable damage.");

                    // spawn a derelict of Sierra's ship for another chance
                    SectorEntityToken token = playerFleet.getContainingLocation().createToken(playerFleet.getLocation().x, playerFleet.getLocation().y);
                    SotfMisc.addDerelict(playerFleet.getStarSystem(), token, SotfMisc.getSierraVariant(), ShipRecoverySpecial.ShipCondition.WRECKED, 0f, true);

                    SotfAMemoryIntel intel = (SotfAMemoryIntel) Global.getSector().getIntelManager().getFirstIntel(SotfAMemoryIntel.class);
                    intel.setStage(SotfAMemoryIntel.AMemoryStage.REFIGHT);
                    intel.sendUpdate(SotfAMemoryIntel.AMemoryStage.REFIGHT, textPanel);

                    options.clearOptions();
                    options.addOption("Return to your duties", OptionId.CONT, null);
                }
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