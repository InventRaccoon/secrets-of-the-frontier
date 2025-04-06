package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.missions.hallowhall.SotfHFHColonyHACause;
import data.scripts.campaign.plugins.dustkeepers.SotfDustkeeperHAFactor;

public class SotfPlayerColonyScriptManager implements EveryFrameScript {

	protected IntervalUtil tracker = new IntervalUtil(2.5f, 5f);
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			//Global.getSector().getCampaignUI().addMessage("Tracker interval elapsed");
			for (MarketAPI market : Misc.getPlayerMarkets(false)) {
				//Global.getSector().getCampaignUI().addMessage("Checked market: " + market.getName());
				if (market.getPrimaryEntity() == null) continue;
				if (Global.getSector().getPlayerStats().getDynamic().getMod(SotfIDs.STAT_PROXY_PATROLS).computeEffective(0f) > 0f) {
					if (!market.hasCondition(SotfIDs.CONDITION_PROXYPATROLS) && market.getSize() > 3 && !market.isHidden() && market.getFaction().isAtWorst(SotfIDs.DUSTKEEPERS, RepLevel.INHOSPITABLE)) {
						market.addCondition(SotfIDs.CONDITION_PROXYPATROLS);
						//Global.getSector().getCampaignUI().addMessage("Added condition: " + market.getName());
					}
				}
			}
		}
	}

}
