// pings from "Reverie's Meal"
package data.scripts.campaign.plugins.waywardstar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.CommRelayEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

import static com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin.HACKED;

public class SotfReverieMealPlugin extends BaseCustomEntityPlugin {

	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
	}

	private float sincePing = 0f;
	public void advance(float amount) {
		if (entity.isInCurrentLocation()) {
			sincePing += amount;
			if (sincePing >= 6f) {
				sincePing = 0f;
				Global.getSector().addPing(entity, "sotf_ping_lakemessage", Color.WHITE);
			}
		}
	}
}









