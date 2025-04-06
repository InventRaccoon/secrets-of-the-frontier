package data.scripts.campaign.fleets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

/**
 *  UNUSED
 */

public class SotfSeraphLeashAssignmentAI extends BaseAssignmentAI {

	protected float elapsed = 0f;
	protected float dur = 30f + (float) Math.random() * 20f;
	protected SectorEntityToken toGuard;

	public SotfSeraphLeashAssignmentAI(CampaignFleetAPI fleet, SectorEntityToken toGuard) {
		super();
		this.fleet = fleet;
		this.toGuard = toGuard;
		
		giveInitialAssignments();
	}

	@Override
	protected void giveInitialAssignments() {
		pickNext();
	}

	@Override
	protected void pickNext() {
		fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, toGuard, 100f, "defending " + toGuard.getName());
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		
//		if (fleet.getCurrentAssignment() == null || fleet.getCurrentAssignment().getAssignment() != FleetAssignment.HOLD) {
//			fleet.addAssignmentAtStart(FleetAssignment.HOLD, null, 1000f, null);
//		}
		
		if (toGuard != null) {
			float dist = Misc.getDistance(fleet.getLocation(), toGuard.getLocation());
			if (dist > toGuard.getRadius() + fleet.getRadius() + 1500 && 
					fleet.getAI().getCurrentAssignmentType() == FleetAssignment.ORBIT_AGGRESSIVE) {
				fleet.addAssignmentAtStart(FleetAssignment.ORBIT_PASSIVE, toGuard, 1f, "returning to " + toGuard.getName(), null);
				CampaignFleetAIAPI ai = fleet.getAI();
				if (ai instanceof ModularFleetAIAPI) {
					// needed to interrupt an in-progress pursuit
					ModularFleetAIAPI m = (ModularFleetAIAPI) ai;
					m.getStrategicModule().getDoNotAttack().add(m.getTacticalModule().getTarget(), 1f);
					m.getTacticalModule().setTarget(null);
				}
			}
		}
	}
}












