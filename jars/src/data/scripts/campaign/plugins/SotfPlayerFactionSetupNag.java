package data.scripts.campaign.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;

public class SotfPlayerFactionSetupNag implements EveryFrameScript
{	
	protected boolean done = false;
	
	public SotfPlayerFactionSetupNag()
	{
		if (Misc.isPlayerFactionSetUp())
		{
			done = true;
			return;
		}
	}

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
		if (Misc.isPlayerFactionSetUp()) {
			done = true;
			return;
		}
		
		// Don't do anything while in a menu/dialog		
		if (Global.getSector().isInNewGameAdvance() || Global.getSector().getCampaignUI().isShowingDialog() 
				|| Global.getCurrentState() == GameState.TITLE) {
			return;
		}

		done = true;
		Global.getSector().setPaused(true);
		Global.getSector().getMemoryWithoutUpdate().set("$shownFactionConfigDialog", true);
		Global.getSector().getCampaignUI().showPlayerFactionConfigDialog();
	}
}
