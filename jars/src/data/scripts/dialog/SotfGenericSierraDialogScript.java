package data.scripts.dialog;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

public class SotfGenericSierraDialogScript implements EveryFrameScript {

	protected IntervalUtil interval = new IntervalUtil(5f, 10f);
	protected float elapsed;
	protected String trigger;

	public SotfGenericSierraDialogScript(String trigger) {
		this.trigger = trigger;
	}

	public void advance(float amount) {
		interval.advance(amount);
		if (interval.intervalElapsed() && SotfMisc.playerHasSierra()) {
			CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
			Misc.showRuleDialog(pf, trigger);
		}
	}
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}	
}



