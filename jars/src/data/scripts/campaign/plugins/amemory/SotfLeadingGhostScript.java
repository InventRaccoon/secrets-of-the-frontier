// script attached to the A Memory guide ghost
package data.scripts.campaign.plugins.amemory;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

public class SotfLeadingGhostScript implements EveryFrameScript {

	private float pingCounter = 0f;
	private SectorEntityToken entity;

	public SotfLeadingGhostScript(SectorEntityToken ghost)
	{
		this.entity = ghost;
	}

	public void advance(float amount) {
		pingCounter += amount;
		if (entity.isInCurrentLocation() && (Misc.getDistance(Global.getSector().getPlayerFleet().getLocation(), entity.getLocation()) <= 1200)) {
			if (pingCounter >= 10f) {
				entity.addFloatingText("!!!", SotfMisc.getEidolonColor(), 0.5f, true);
				Global.getSector().addPing(entity, "sotf_ping_leadingghost", SotfMisc.getEidolonColor());
				pingCounter = 0f;
			}
		}
	}

	public boolean isDone() {
		return entity == null;
	}

	public boolean runWhilePaused() {
		return false;
	}
	
}









