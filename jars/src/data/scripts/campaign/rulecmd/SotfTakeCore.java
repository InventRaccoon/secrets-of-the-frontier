// remove an item from the player's inventory
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.util.List;
import java.util.Map;

public class SotfTakeCore extends BaseCommandPlugin
{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        final MemoryAPI localMemory = memoryMap.get(MemKeys.LOCAL);
        if (localMemory == null) return false;

        final CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        fleet.getCargo().removeCommodity(SotfIDs.SIERRA_CORE, 1f);
        localMemory.unset("$sotf_sierracore");
        return true;
    }
}
