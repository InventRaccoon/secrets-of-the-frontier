// checks if we can start A Promise's bar event
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;

import java.util.List;
import java.util.Map;

public class SotfReverieCMD extends BaseCommandPlugin {

    protected CampaignFleetAPI playerFleet;
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;
        cmd = params.get(0).getString(memoryMap);
        String param = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }

        TextPanelAPI text = dialog.getTextPanel();

        switch (cmd) {
            case "anyShuntLeft":
                return shuntWithDefendersAvailable();
            case "addDaydreamSynthesizer":
                Global.getSector().getCharacterData().addHullMod(SotfIDs.HULLMOD_DAYDREAM_SYNTHESIZER);
                text.setFontSmallInsignia();
                text.addParagraph("Unlocked hullmod: Daydream Synthesizer", Misc.getPositiveHighlightColor());
                text.highlightInLastPara(Misc.getHighlightColor(), "Soulbond");
                text.addParagraph("- Duplicates the ship when combat starts (only triggers once for the entire fleet)", Misc.getHighlightColor());
                text.addParagraph("- Mimic is a perfect copy, with Reverie as its captain", Misc.getHighlightColor());
                text.setFontInsignia();
                return true;
            default:
                return true;
        }
    }

    public static boolean shuntWithDefendersAvailable() {
        for (SectorEntityToken tap : Global.getSector().getCustomEntitiesWithTag(Tags.CORONAL_TAP)) {
            if (!tap.getMemoryWithoutUpdate().getBoolean("$defenderFleetDefeated")) {
                return true;
            }
        }
        return false;
    }

}