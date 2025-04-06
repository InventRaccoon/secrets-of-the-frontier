package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Map;

public class SotfAMemoryHintPlugin implements InteractionDialogPlugin {

    public static enum OptionId {
        INIT,
        ACCEPT,
        REFUSE,
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
        String tiaString = "Tia'Taxet";
        StarSystemAPI athenaSystem = Global.getSector().getStarSystem("tia");
        if (athenaSystem == null) {
            SectorEntityToken athena = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get("sotf_athenaWreck");
            if (athena != null) {
                tiaString = athena.getStarSystem().getBaseName();
            }
        }

        switch (option) {
            case INIT:
                textPanel.addParagraph("You receive an alert. Sierra has requested a comm-link.");

                options.clearOptions();
                options.addOption("Accept the comms request", OptionId.ACCEPT, null);
                options.addOption("Refuse", OptionId.REFUSE, null);
                break;
            case ACCEPT:
                visual.showPersonInfo(SotfPeople.getPerson(SotfPeople.SIERRA), true);
                String kindred = (String) Global.getSector().getMemoryWithoutUpdate().get("$sotf_kindred");
                textPanel.addParagraph("Sierra's avatar appears on the feed. \"Hi, " + kindred + "!\" she begins cheerfully. " +
                        "\"I just wanted to point out all those funny sensor ghosts. It kind of looks like they're dancing.\" " +
                        "She briefly falls silent. " + "\"Wonder what they're doing around " + tiaString + ", isn't that a pretty... empty system?\"");
                textPanel.highlightInLastPara(sc, "\"Hi, " + kindred + "!\"",
                        "\"I just wanted to point out all those funny sensor ghosts. It kind of looks like they're dancing.\"",
                        "\"Wonder what they're doing around " + tiaString + ", isn't that a pretty... empty system?\"");
                textPanel.addParagraph("\"... think it's worth checking out?\" she asks.");
                textPanel.highlightInLastPara(sc, "\"... think it's worth checking out?\"");
                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
                break;
            case REFUSE:
                // for heartless bastards: make Sierra self-conscious AND get denied your hint
                textPanel.addParagraph("You coldly tap the luminescent red 'deny' on your console to deny the commlink. Sierra sends a message " +
                        "about half an hour later, apologizing for potentially sending a comms request at an inconvenient time.");

                // so she won't do it again in WS
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierraCommsDenied", true);

                // consequences
                SotfMisc.addGuilt(0.5f);

                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
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