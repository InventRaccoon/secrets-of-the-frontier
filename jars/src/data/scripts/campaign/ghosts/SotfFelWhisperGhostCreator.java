package data.scripts.campaign.ghosts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhostCreator;
import com.fs.starfarer.api.impl.campaign.ghosts.GhostFrequencies;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ghosts.types.ZigguratGhost;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class SotfFelWhisperGhostCreator extends BaseSensorGhostCreator {

	@Override
	public List<SensorGhost> createGhost(SensorGhostManager manager) {
		if (!Global.getSector().getCurrentLocation().isHyperspace()) return null;
		CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
		
		List<SensorGhost> result = new ArrayList<SensorGhost>();
		
		float dur = 2f + manager.getRandom().nextFloat() * 2f;

		Vector2f loc = null;
		SotfFelWhisperGhost ghost = new SotfFelWhisperGhost(manager, pf, dur, loc);
		if (!ghost.isCreationFailed()) {
			result.add(ghost);
		}
		
		return result;
	}

	
	@Override
	public float getFrequency(SensorGhostManager manager) {
		if (SotfMisc.getPlayerGuilt() < 7) {
			return 0f;
		}
		if (manager.hasGhostOfClass(SotfFelWhisperGhost.class)) {
			return 0f;
		}
		//return 1000000f;
		return GhostFrequencies.getRemoraFrequency(manager);
	}
	
	public boolean canSpawnWhilePlayerInOrNearSlipstream() {
		return false;
	}

	@Override
	public float getTimeoutDaysOnSuccessfulCreate(SensorGhostManager manager) {
		//return 1f;
		return 50f + manager.getRandom().nextFloat() * 50f;
	}

}
