// orbital habitat hazard reduction effect
package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.missions.hallowhall.SotfPlayerProxyFleetManager;

import java.util.Iterator;
import java.util.LinkedHashSet;


public class SotfProxyPatrolsCondition extends BaseMarketConditionPlugin {

	public SotfPlayerProxyFleetManager script;
	
	public static SotfProxyPatrolsCondition get(MarketAPI market) {
		MarketConditionAPI mc = market.getCondition(SotfIDs.CONDITION_PROXYPATROLS);
		if (mc != null && mc.getPlugin() instanceof SotfProxyPatrolsCondition) {
			return (SotfProxyPatrolsCondition) mc.getPlugin();
		}
		return null;
	}
	


	@Override
	public void advance(float amount) {
		if (market.getFaction().isAtBest(SotfIDs.DUSTKEEPERS, RepLevel.HOSTILE) || market.getSize() < 4
				|| !market.getFactionId().equals(Factions.PLAYER)) {
			market.removeSpecificCondition(condition.getIdForPluginModifications());
			return;
		}
	}
	
	
	public void apply(String id) {
		if (script == null) {
			script = new SotfPlayerProxyFleetManager(market);
		}
		market.getPrimaryEntity().addScript(script);
	}
	
	public void unapply(String id) {
		if (script != null) {
			market.getPrimaryEntity().removeScript(script);
		}
	}

	@Override
	public boolean showIcon() {
		return true;
	}

	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		super.createTooltipAfterDescription(tooltip, expanded);

		String surfaceOf = "on the surface of";
		if (!(market.getPrimaryEntity() instanceof PlanetAPI)) {
			surfaceOf = "within";
		}
		tooltip.addPara("Hidden drone manufactories " + surfaceOf + " " + market.getName() + " churn day and night, " +
						"fielding a force of Dustkeeper Proxy war drones.",
				10f);

		tooltip.addPara("Spawns 1 medium proxy patrol at size 4 or larger, increasing to 2 patrols at size 6. These patrols use " +
						"Dustkeeper Proxy doctrine but act as part of your faction and use this planet's fleet size percentage.",
				10f, Misc.getHighlightColor(),
				"1 proxy patrol", "size 4", "2 patrols", "size 6");

		tooltip.addPara("This benefit will become temporarily inactive if the colony has no functional spaceport, if its size falls below 4, or if the " +
						"holding faction becomes hostile to the Dustkeeper Contingency.",
				10f, Misc.getHighlightColor(),
				"1 proxy patrol", "size 4", "2 patrols", "size 6");

		if (!market.hasSpaceport()) {
			tooltip.addPara("Disabled: colony lacks a functional spaceport",
					Misc.getNegativeHighlightColor(), 10f);
		}
	}
}





