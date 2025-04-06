package data.scripts.world.mia;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.util.Random;

public class SotfHoldoutDemandNegator implements EconomyUpdateListener, EveryFrameScript {

	public MarketAPI market;

	public SotfHoldoutDemandNegator(MarketAPI market) {
		this.market = market;
	}

	@Override
	public void advance(float amount) {

	}

	@Override
	public void commodityUpdated(String commodityId) {
		CommodityOnMarketAPI com = market.getCommodityData(commodityId);
		int curr = 0;
		String modId = market.getId();
		MutableStat.StatMod mod = com.getAvailableStat().getFlatStatMod(modId);
		if (mod != null) {
			curr = Math.round(mod.value);
		}

		int avWithoutPenalties = (int) Math.round(com.getAvailableStat().getBaseValue());
		for (MutableStat.StatMod m : com.getAvailableStat().getFlatMods().values()) {
			if (m.value < 0) continue;
			avWithoutPenalties += (int) Math.round(m.value);
		}

		int a = com.getAvailable() - curr;
		a = avWithoutPenalties - curr;
		int d = com.getMaxDemand();
		if (d > a) {
			int supply = Math.max(1, d - a);
			String desc = "Scavenged and re-used";
			if (commodityId.equals(Commodities.VOLATILES)) {
				desc = "Mined from Gor's Little Lie";
			} else if (commodityId.contains(Commodities.ORE)) {
				desc = "Mined from asteroids";
			} else if (commodityId.equals(Commodities.FUEL)) {
				desc = "Internal fuel production";
			}
			com.getAvailableStat().modifyFlat(modId, supply, desc);
		}
	}

	@Override
	public void economyUpdated() {

	}

	@Override
	public boolean isEconomyListenerExpired() {
		return isDone();
	}

	@Override
	public boolean isDone() {
		return market == null;
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}
}




