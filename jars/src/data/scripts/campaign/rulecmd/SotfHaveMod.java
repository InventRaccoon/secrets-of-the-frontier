package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.util.List;
import java.util.Map;

/**
 * Checks if the provided ID matches an installed mod
 */

public class SotfHaveMod extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		String id = params.get(0).getString(memoryMap);
		// flip = looking for the mod. Else looking that it isn't there
		boolean flip = true;
		if (params.size() > 1) {
			flip = params.get(1).getBoolean(memoryMap);
		}
		if (flip) {
			return Global.getSettings().getModManager().isModEnabled(id);
		}
		return !Global.getSettings().getModManager().isModEnabled(id);
	}
}