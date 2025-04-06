package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.impl.campaign.AbandonMarketPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.ShowDefaultVisual;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfAbandonStationPlugin extends AbandonMarketPluginImpl {
	
	@Override
	public int getHandlingPriority(Object params) {
		MarketAPI market = (MarketAPI) params;
		if (market.hasCondition(SotfIDs.CONDITION_CRAMPED)) {
			return 2; // higher than vanilla and Nex
		}
		return -1;
	}

	@Override
	public void createConfirmationPrompt(MarketAPI market, TooltipMakerAPI prompt) {
		super.createConfirmationPrompt(market, prompt);

		Color h = Misc.getHighlightColor();
		prompt.addPara("In the evacuation process, the station will be disassembled, freeing up a stable location in its place.", 10f, h, "stable location");
	}

	@Override
	public void abandonConfirmed(MarketAPI market) {
		int cost = getAbandonCost(market);
		int refund = Misc.computeTotalShutdownRefund(market);

		int diff = cost - refund;

		Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(diff);

		if (diff > 0) {
			Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
					String.format("Spent %s", Misc.getDGSCredits(diff)),
					Misc.getTooltipTitleAndLightHighlightColor(), Misc.getDGSCredits(diff), Misc.getHighlightColor());
		} else if (diff < 0) {
			Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
					String.format("Received %s", Misc.getDGSCredits(-diff)),
					Misc.getTooltipTitleAndLightHighlightColor(), Misc.getDGSCredits(-diff), Misc.getHighlightColor());
		}

		DecivTracker.removeColony(market, false);

		// replace station with stable location
		SectorEntityToken station = market.getPrimaryEntity();
		LocationAPI loc = station.getContainingLocation();
		SectorEntityToken built = loc.addCustomEntity(null,
				null,
				Entities.STABLE_LOCATION, // type of object, defined in custom_entities.json
				Factions.NEUTRAL); // faction
		if (station.getOrbit() != null) {
			built.setOrbit(station.getOrbit().makeCopy());
		}
		loc.removeEntity(station);
		updateOrbitingEntities(loc, station, built);

		ListenerUtil.reportPlayerAbandonedColony(market);

		InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
		if (dialog != null && dialog.getPlugin() instanceof RuleBasedDialog) {
			dialog.dismiss();
			dialog.getVisualPanel().closeCoreUI();
		} else {
			// if abandoned from command tab (rather than by interacting with colony), go back to colony list
			Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.OUTPOSTS);
		}
	}

	public void updateOrbitingEntities(LocationAPI loc, SectorEntityToken prev, SectorEntityToken built) {
		if (loc == null) return;
		for (SectorEntityToken other : loc.getAllEntities()) {
			if (other == prev) continue;
			if (other.getOrbit() == null) continue;
			if (other.getOrbitFocus() == prev) {
				other.setOrbitFocus(built);
				if (other.hasTag(Tags.ORBITAL_JUNK)) {
					loc.removeEntity(other);
				}
			}
		}
	}
}
