package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.ids.SotfIDs;
import lunalib.lunaSettings.LunaSettings;

public class SotfBackgroundStatModifier extends BaseHullMod {

	public void advanceInCampaign(FleetMemberAPI member, float amount) {
		if (Global.getSector().getPlayerFleet() == null) {
			return;
		}
		if (!Global.getSector().getPlayerFleet().getMembersWithFightersCopy().contains(member)) {
			member.getVariant().removePermaMod("sotf_backgroundmodifier");
		}
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if (Global.getSector() != null) {
			if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_COTL_START)) {
				float dpMult = LunaSettings.getFloat("secretsofthefrontier", "sotf_cotlDPPenaltyPercent");
				stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult("sotf_cotl_dppenalty", dpMult);
			}
		}
	}
}
