package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;

import java.util.List;
import java.util.Map;

/**
 *  Simply shows the Burnouts faction in the intel tab
 */

public class SotfShowBurnouts extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        Global.getSector().getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS).setShowInIntelTab(true);
        return true;
    }
}
