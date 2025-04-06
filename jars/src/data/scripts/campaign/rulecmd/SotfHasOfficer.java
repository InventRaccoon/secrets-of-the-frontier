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
 * Checks if the player has a given officer
 * Match if any of the player's officers has the provided ID, or a ship's captain has that ID
 * Certain items in storage also count (e.g the instance chips of the fixed Dustkeeper allies)
 */

public class SotfHasOfficer extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

		String id = params.get(0).getString(memoryMap);
		return haveOfficer(id);
	}

	public static boolean haveOfficer(String id) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		boolean hasOfficer = false;
		if (playerFleet != null) {
			for (OfficerDataAPI data : playerFleet.getFleetData().getOfficersCopy()) {
				if (data.getPerson().getId().equals(id)) {
					hasOfficer = true;
					break;
				}
			}
			if (!hasOfficer) {
				for (FleetMemberAPI member : playerFleet.getMembersWithFightersCopy()) {
					if (member.getCaptain() != null) {
						if (member.getCaptain().getId().equals(id)) {
							hasOfficer = true;
							break;
						}
					}
				}
			}
		} else {
			return false;
		}
		// hardcoded for AIs in cargo
		if (!hasOfficer) {
			String aiCoreId = null;
			// alternative for SotfSierraCMD's method
			if (id.equals(SotfPeople.SIERRA)) {
				return SotfMisc.playerHasSierra();
			}
			if (id.equals(SotfPeople.NIGHTINGALE)) {
				aiCoreId = SotfIDs.NIGHTINGALE_CHIP;
			} else if (id.equals(SotfPeople.BARROW)) {
				aiCoreId = SotfIDs.BARROW_CHIP;
			} else if (id.equals(SotfPeople.SERAPH)) {
				aiCoreId = SotfIDs.SERAPH_CHIP;
			}
			if (aiCoreId != null) {
				if (playerFleet.getCargo().getCommodityQuantity(aiCoreId) > 0) {
					hasOfficer = true;
				}
			}
		}
		return hasOfficer;
	}
}