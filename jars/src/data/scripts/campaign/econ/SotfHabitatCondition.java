// orbital habitat hazard reduction effect
package data.scripts.campaign.econ;

import java.util.Iterator;
import java.util.LinkedHashSet;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import data.scripts.campaign.ids.SotfIDs;


public class SotfHabitatCondition extends BaseMarketConditionPlugin {

	public static String HABITAT_MOD_ID = "sotf_habitat";

	public static float HABITAT_REDUCTION = 0.92f;
	
	public static SotfHabitatCondition get(MarketAPI market) {
		MarketConditionAPI mc = market.getCondition(SotfIDs.CONDITION_HABITAT);
		if (mc != null && mc.getPlugin() instanceof SotfHabitatCondition) {
			return (SotfHabitatCondition) mc.getPlugin();
		}
		return null;
	}

	public boolean runWhilePaused() {
		return true;
	}
	
	protected LinkedHashSet<SectorEntityToken> habitats = new LinkedHashSet<SectorEntityToken>();
	
	public LinkedHashSet<SectorEntityToken> getHabitats() {
		return habitats;
	}

	@Override
	public void advance(float amount) {
		Iterator<SectorEntityToken> iter = habitats.iterator();
		while (iter.hasNext()) {
			SectorEntityToken relay = iter.next();
			if (!relay.isAlive() || relay.getContainingLocation() != market.getContainingLocation()) {
				iter.remove();
			}
		}
		if (habitats.isEmpty()) {
			market.removeSpecificCondition(condition.getIdForPluginModifications());
		}
	}

	protected SectorEntityToken getBestRelay() {
		if (market.getContainingLocation() == null) return null;
		
		SectorEntityToken best = null;
		for (SectorEntityToken relay : habitats) {
			if (relay.getMemoryWithoutUpdate().getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) {
				continue;
			}
			if (relay.getFaction() == market.getFaction()) {
				if (best == null) {
					best = relay;
				}
			}
		}
		return best;
	}
	
	
	public void apply(String id) {
		SectorEntityToken relay = getBestRelay();
		if (relay == null) {
			unapply(id);
			return;
		}
		market.getHazard().modifyMult(HABITAT_MOD_ID, HABITAT_REDUCTION, "Orbital habitat");
			
	}
	
	public void unapply(String id) {
		market.getHazard().unmodifyMult(HABITAT_MOD_ID);
	}

	@Override
	public boolean showIcon() {
		return false;
	}
}





