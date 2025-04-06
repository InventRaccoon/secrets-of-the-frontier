package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class SotfHasWeapon extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

		String id = params.get(0).getString(memoryMap);
		if (playerFleet.getCargo().getNumWeapons(id) > 0) {
			return true;
		}
		boolean hasWeapon = false;
		for (FleetMemberAPI member : playerFleet.getMembersWithFightersCopy()) {
			for (String slot : member.getVariant().getNonBuiltInWeaponSlots()) {
				if (member.getVariant().getWeaponId(slot).equals(id)) {
					hasWeapon = true;
					break;
				}
			}
		}
		return hasWeapon;
	}
}