package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityFactor;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.missions.hallowhall.SotfHFHColonyHACause;
import data.scripts.campaign.plugins.dustkeepers.SotfDustkeeperHAFactor;

public class SotfHostileActivityFactorManager implements EveryFrameScript {

	protected IntervalUtil tracker = new IntervalUtil(0.5f, 1.5f);
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			HostileActivityEventIntel ha = HostileActivityEventIntel.get();
			if (ha == null) return;
			if (!hasFactor(ha, SotfDustkeeperHAFactor.class)) {
				SotfDustkeeperHAFactor factor = new SotfDustkeeperHAFactor(ha);
				ha.addFactor(factor);
				ha.addActivity(factor, new SotfHFHColonyHACause(ha));
			}
		}
	}

	public boolean hasFactor(HostileActivityEventIntel ha, Class c) {
		if (ha == null) return false;
		for (EventFactor factor : ha.getFactors()) {
			if (factor.getClass() == c) {
				return true;
			}
		}
		return false;
	}

}
