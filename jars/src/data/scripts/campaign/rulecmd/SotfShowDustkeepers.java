package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

/**
 *  Simply shows the Dustkeepers faction in the intel tab
 */

public class SotfShowDustkeepers extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).setShowInIntelTab(true);
        return true;
    }
}
