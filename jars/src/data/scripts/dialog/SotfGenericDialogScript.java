package data.scripts.dialog;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

public class SotfGenericDialogScript implements EveryFrameScript {

	protected IntervalUtil interval = new IntervalUtil(5f, 10f);
	protected float elapsed;
	protected String trigger;

	public SotfGenericDialogScript(String trigger) {
		this.trigger = trigger;
	}

	public void advance(float amount) {
		// Don't do anything while in a menu/dialog
		if (Global.getSector().isInNewGameAdvance() || Global.getSector().getCampaignUI().isShowingDialog()
				|| Global.getCurrentState() == GameState.TITLE || Global.getSector().getPlayerFleet() == null) {
			return;
		}
		interval.advance(amount);
		if (interval.intervalElapsed()) {
			CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
			Misc.showRuleDialog(pf, trigger);
			Global.getSector().removeScript(this);
		}
	}
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}	
}



