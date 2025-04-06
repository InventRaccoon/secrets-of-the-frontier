// makes Omicron's entity emit constant pings. Unlike normal pings, this one can be seen even if they aren't revealed
package data.scripts.campaign.plugins.apromise;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfAPOmiEntityPlugin extends BaseCustomEntityPlugin {

	public static String PING_COLOR_KEY = "$core_beaconPingColor";

	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
	}
	
	private float phase = 0f;
	private float freqMult = 1f;
	private float sincePing = 20f;
	public void advance(float amount) {
		if (entity.isInCurrentLocation() && !entity.getMemoryWithoutUpdate().contains("$APmetOmicron")) {
			sincePing += amount;
			if (sincePing >= 6f && phase > 0.1f && phase < 0.2f) {
				sincePing = 0f;
				CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
				if (playerFleet != null) {
					
					String pingId = SotfIDs.PING_REGNANT;
					freqMult = 0.05f;
					
					//Global.getSector().addPing(entity, pingId);
					
					//Color pingColor = entity.getFaction().getBrightUIColor();
					Color pingColor = null;
					if (entity.getMemoryWithoutUpdate().contains(PING_COLOR_KEY)) {
						pingColor = (Color) entity.getMemoryWithoutUpdate().get(PING_COLOR_KEY);
					}
					
					Global.getSector().addPing(entity, pingId, pingColor);
				}
			}
		}
	}

	public float getRenderRange() {
		return entity.getRadius() + 100f;
	}

}









