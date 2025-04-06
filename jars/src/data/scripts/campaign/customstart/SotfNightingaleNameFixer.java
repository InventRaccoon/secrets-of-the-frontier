package data.scripts.campaign.customstart;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.Tuning;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager;
import com.fs.starfarer.api.util.Misc;

/**
 * Nexerelin renames Nightingale's ship before the game starts so we need to quickly change it back to ODS Songless
 */

public class SotfNightingaleNameFixer implements EveryFrameScript
{
	protected boolean done = false;
	protected float timer = 0;

	@Override
	public boolean isDone()
	{
		return done;
	}

	@Override
	public boolean runWhilePaused()
	{
		return true;
	}
	
	@Override
	public void advance(float amount)
	{
		// Don't do anything while in a menu/dialog
		if (Global.getSector().isInNewGameAdvance() || Global.getSector().getCampaignUI().isShowingDialog() 
				|| Global.getCurrentState() == GameState.TITLE || Global.getSector().getPlayerFleet() == null) {
			return;
		}

		for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
			if (member.getHullId().contains("memoir") && !member.getShipName().contains("ODS Songless")) {
				member.setShipName("ODS Songless");
				done = true;
			}
		}

		timer += amount;

		if (timer > 10f) {
			done = true;
		}
	}
}
