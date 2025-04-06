// Askonia's Dustkeeper probe pulses and data transmission
package data.scripts.campaign.plugins.mayfly;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.impl.campaign.CommRelayEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.ids.SotfIDs;

import static com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin.HACKED;

public class SotfAskoniaProbePulsePlugin extends BaseCustomEntityPlugin {

	public void init(SectorEntityToken entity, Object pluginParams) {
		super.init(entity, pluginParams);
	}

	private float sincePing = 0f;
	public void advance(float amount) {
		if (entity.isInCurrentLocation()) {
			sincePing += amount;
			if (sincePing >= 20f) {
				sincePing = 0f;
				Global.getSector().addPing(entity, "sotf_ping_askoniaprobe", Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getBaseUIColor());
			}
		}
		performTransmitToPlayer();
	}

	private void performTransmitToPlayer() {
		if (!entity.getMemoryWithoutUpdate().contains("$sotf_transmittingToPlayer")) return;
		if (Global.getSector().getPlayerFaction().isAtBest(SotfIDs.DUSTKEEPERS, RepLevel.HOSTILE)) return;

		for (CustomCampaignEntityAPI customEntity : entity.getContainingLocation().getCustomEntities()) {
			if (customEntity.hasTag(Tags.SENSOR_ARRAY) || customEntity.hasTag(Tags.NAV_BUOY)) {
				customEntity.getMemoryWithoutUpdate().set(HACKED, true, 30f);
			}
		}

		boolean playerInRelayRange = Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay();
		for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getCommQueue()) {
			if (intel instanceof CommRelayEntityPlugin.CommSnifferReadableIntel) {
				CommRelayEntityPlugin.CommSnifferReadableIntel csi = (CommRelayEntityPlugin.CommSnifferReadableIntel) intel;
				if (csi.canMakeVisibleToCommSniffer(playerInRelayRange, entity)) {
					intel.setForceAddNextFrame(true);
				}
			}
		}
	}
}









