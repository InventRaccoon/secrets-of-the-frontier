package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Map;

public class SotfWSSierraSignalsPlugin implements InteractionDialogPlugin {

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

        Color sc = Global.getSector().getFaction("sotf_sierra_faction").getBaseUIColor();

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
                String kindred = (String) Global.getSector().getMemoryWithoutUpdate().get("$sotf_kindred");
                visual.showPersonInfo(SotfPeople.getPerson(SotfPeople.SIERRA), true);
                textPanel.addParagraph("Sierra's avatar appears on the feed. \"Hey, " + kindred + ".\" she says. \"I'm picking up odd signals " +
                        "in this star system. A bit like the ones back in " + tiaString + ". But these ones...\" She trails off before " +
                        "picking up again. \"They seem to be everywhere around the fleet, not centered on a specific point.\"");
                textPanel.highlightInLastPara(sc, "\"Hey, " + kindred + ".\"",
                        "\"I'm picking up odd signals in this star system. A bit like the ones back in " + tiaString + ". But these ones...\"",
                        "\"They seem to be everywhere around the fleet, not centered on a specific point.\"");
                textPanel.addParagraph("\"Not really sure what we can do with that information,\" she admits. " +
                        "\"My suggestions would be... sending out some kind of signal? Scanning for something? I'm sure you'll figure it out.\"");
                textPanel.highlightInLastPara(sc, "\"Not really sure what we can do with that information,\"",
                        "\"My suggestions would be... sending out some kind of signal? Scanning for something? I'm sure you'll figure it out.\"");
                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
                break;
            case REFUSE:
                // for heartless bastards: make Sierra self-conscious AND get denied your hint
                textPanel.addParagraph("You coldly tap the luminescent red 'deny' on your console to deny the commlink. Sierra sends a message " +
                        "about half an hour later, apologizing for potentially sending a comms request at an inconvenient time.");

                // so she won't do it again
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