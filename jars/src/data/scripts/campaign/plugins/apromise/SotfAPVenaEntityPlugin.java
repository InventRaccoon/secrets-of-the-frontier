// this one makes Omicron's disabled splinter make constant pings, even if out of sight
package data.scripts.campaign.plugins.apromise;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfAPVenaEntityPlugin extends BaseCustomEntityPlugin {

	public static String PING_COLOR_KEY = "$core_beaconPingColor";

	public static float GLOW_FREQUENCY = 1.2f; // on/off cycles per second

	//private SectorEntityToken entity;

	transient private SpriteAPI sprite;
	transient private SpriteAPI glow;

	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
		//this.entity = entity;
		//entity.setDetectionRangeDetailsOverrideMult(0.75f);
		readResolve();
	}

	Object readResolve() {
		sprite = Global.getSettings().getSprite("campaignEntities", "warning_beacon");
		glow = Global.getSettings().getSprite("campaignEntities", "warning_beacon_glow");
		return this;
	}
	
	private float phase = 0f;
	private float freqMult = 1f;
	private float sincePing = 15f;
	public void advance(float amount) {
		phase += amount * GLOW_FREQUENCY * freqMult;
		while (phase > 1) phase --;

		if (entity.isInCurrentLocation()) {
			sincePing += amount;
			if (sincePing >= 6f && phase > 0.1f && phase < 0.2f) {
				sincePing = 0f;
				CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
				if (playerFleet != null) {

					String pingId = SotfIDs.PING_MEMOIR;
					freqMult = 0.05f;

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









