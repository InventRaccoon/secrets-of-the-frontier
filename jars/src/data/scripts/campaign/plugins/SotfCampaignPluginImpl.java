// important for replacing plugins and other useful things
package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.CommodityIconProvider;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.abilities.SotfCourserFIDPluginImpl;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.amemory.SotfAMemoryFIDPluginImpl;
import data.scripts.campaign.plugins.amemory.SotfAMemoryBCPImpl;

import data.scripts.campaign.plugins.dustkeepers.*;
import data.scripts.campaign.plugins.waywardstar.SotfWSBCPImpl;
import data.scripts.plugins.SotfBattleCreationPluginImpl;
import data.scripts.campaign.plugins.sierra.SotfSierraOfficerPlugin;
import data.scripts.utils.SotfMisc;

public class SotfCampaignPluginImpl extends BaseCampaignPlugin {

	public String getId() {
		return "SotfCampaignPluginImpl";
	}

	// when the player fights someone, use our battle creation plugin, not vanilla's, so we get custom objectives
	public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
		if  (opponent instanceof CampaignFleetAPI && opponent.getMemoryWithoutUpdate().contains("$sotf_AMemoryFight")) {
			return new PluginPick<BattleCreationPlugin>(new SotfAMemoryBCPImpl(), PickPriority.MOD_SPECIFIC);
		} else if  (opponent instanceof CampaignFleetAPI && opponent.getMemoryWithoutUpdate().contains("$sotf_WSEidolon")) {
			return new PluginPick<BattleCreationPlugin>(new SotfWSBCPImpl(), PickPriority.MOD_SPECIFIC);
		}
		else if (opponent instanceof CampaignFleetAPI && SotfModPlugin.TACTICAL) {
			return new PluginPick<BattleCreationPlugin>(new SotfBattleCreationPluginImpl(), PickPriority.MOD_GENERAL);
		}
		return null;
	}

	// used to force a custom fleet interaction
	public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
		if (interactionTarget instanceof CampaignFleetAPI && interactionTarget.getMemoryWithoutUpdate().contains("$sotf_AMemoryFight")) {
			return new PluginPick<InteractionDialogPlugin>(new SotfAMemoryFIDPluginImpl(), PickPriority.MOD_SPECIFIC);
		}
		else if (interactionTarget instanceof CampaignFleetAPI && SotfMisc.courserNearby()) {
			return new PluginPick<InteractionDialogPlugin>(new SotfCourserFIDPluginImpl(), PickPriority.MOD_SET);
		}
		return null;
	}

	public PluginPick<FleetInflater> pickFleetInflater(CampaignFleetAPI fleet, Object params) {
		if (params instanceof DefaultFleetInflaterParams) {
			DefaultFleetInflaterParams p = (DefaultFleetInflaterParams) params;
			// custom Dustkeeper inflater for auxiliaries
			if (fleet.getFaction().getId().contains(SotfIDs.DUSTKEEPERS)) {
				return new PluginPick<FleetInflater>(new SotfDustkeeperFleetInflater(p), PickPriority.MOD_SET);
			}
		}
		return null;
	}

	public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
		if (SotfIDs.SIERRA_CORE_OFFICER.equals(commodityId)) {
			return new PluginPick<AICoreOfficerPlugin>(new SotfSierraOfficerPlugin(), PickPriority.MOD_SET);
		} else switch (commodityId) {
			case SotfIDs.NIGHTINGALE_CHIP:
			case SotfIDs.BARROW_CHIP:
			case SotfIDs.SERAPH_CHIP:
			case SotfIDs.SLIVER_CHIP_1:
			case SotfIDs.SLIVER_CHIP_2:
			case SotfIDs.ECHO_CHIP_1:
			case SotfIDs.ECHO_CHIP_2:
				return new PluginPick<AICoreOfficerPlugin>(new SotfDustkeeperWarmindChipPlugin(), PickPriority.MOD_SET);
			default:
				return null;
		}
	}

}








