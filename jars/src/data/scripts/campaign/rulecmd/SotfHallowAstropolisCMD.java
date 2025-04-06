// handles rulecommands for the Hallowhall Astropolis build sequence
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadCoresFactor;
import data.scripts.campaign.missions.hallowhall.SotfHFHRestoreAstropolisFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;
import data.scripts.campaign.plugins.SotfPlayerFactionSetupNag;
import data.scripts.world.mia.SotfDustkeeperObjectiveShareScript;

import java.util.List;
import java.util.Map;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class SotfHallowAstropolisCMD extends BaseCommandPlugin {

	protected CampaignFleetAPI playerFleet;
	protected SectorEntityToken entity;
	protected FactionAPI playerFaction;
	protected FactionAPI entityFaction;
	protected MarketAPI market;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;
	protected CargoAPI playerCargo;
	protected MemoryAPI memory;
	protected InteractionDialogAPI dialog;
	protected Map<String, MemoryAPI> memoryMap;
	protected FactionAPI faction;

	protected void init(SectorEntityToken entity) {
		memory = entity.getMemoryWithoutUpdate();
		this.entity = entity;
		playerFleet = Global.getSector().getPlayerFleet();
		playerCargo = playerFleet.getCargo();

		playerFaction = Global.getSector().getPlayerFaction();
		entityFaction = entity.getFaction();

		faction = entity.getFaction();

		//DebugFlags.OBJECTIVES_DEBUG = false;
	}

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {

		this.dialog = dialog;
		this.memoryMap = memoryMap;

		String command = params.get(0).getString(memoryMap);
		if (command == null) return false;

		entity = dialog.getInteractionTarget();
		init(entity);

		memory = getEntityMemory(memoryMap);

		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();

		if (command.equals("printCost")) {
			printCost("sotf_habitat_makeshift");
		} else if (command.equals("build")) {
			build(Factions.PLAYER);
		} else if (command.equals("canBuild")) {
			return canBuild("sotf_outpost");
		}

		return true;
	}

	public boolean canBuild(String type) {
		if (DebugFlags.OBJECTIVES_DEBUG) {
			return true;
		}

		CargoAPI cargo = playerCargo;
		String[] res = getResources();
		int[] quantities = getQuantities();
		for (int i = 0; i < res.length; i++) {
			String commodityId = res[i];
			int quantity = quantities[i];
			if (quantity > cargo.getQuantity(CargoItemType.RESOURCES, commodityId)) {
				return false;
			}
		}
		return true;
	}

	public void printCost(String type) {
		text.addPara("Restoring the astropolis will create a size 3 colony with a 100% hazard rating.");
		text.highlightInLastPara("3", "100% hazard rating");
		text.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getHighlightColor());
		text.addPara("The astropolis' expansive modular structure provides ample room for the colony to grow.");
		
		Misc.showCost(text, null, null, getResources(), getQuantities());
		
		if (canBuild(type)) {
			text.addPara("Proceed with restoration?");
		} else {
			text.addPara("You do not have the necessary resources to perform the restoration.");
		}
	}
	
	public String [] getResources() {
		return new String[] {Commodities.CREW, Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES};
	}

	public int [] getQuantities() {
		return new int[] {1000, 400, 300};
	}

	public void build(String factionId) {
		market = Global.getFactory().createMarket("sotf_hallowhall_astro_market", "Hallowhall Astropolis", 3);
		market.setSize(3);

		market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

		market.setFactionId(factionId);
		market.addCondition(Conditions.POPULATION_3);
		//market.addCondition(SotfIDs.CONDITION_CRAMPED);

		market.addIndustry(Industries.POPULATION);
		market.addIndustry(Industries.SPACEPORT);
		market.getIndustry(Industries.SPACEPORT).startBuilding();

		// get the existing storage submarket
		SubmarketAPI existingStorage = entity.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE);
		StoragePlugin oldPlugin = (StoragePlugin) existingStorage.getPlugin();
		CargoAPI before = oldPlugin.getCargo();

		// add the new one to the new market we're creating, set its cargo as the old one's
		market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		StoragePlugin plugin = (StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
		if (plugin != null) {
			plugin.setPlayerPaidToUnlock(true);
			plugin.setCargo(before);
		}

		// take the old one and set it as a copy of its existing one, then clear it
		oldPlugin.setCargo(existingStorage.getCargo().createCopy());
		oldPlugin.getCargo().clear();

		market.addSubmarket(Submarkets.LOCAL_RESOURCES);

		market.setPrimaryEntity(entity);
		entity.setMarket(market);
		entity.setName(market.getName());
		entity.setFaction(factionId);
		entity.getMemoryWithoutUpdate().unset("$abandonedStation");
		entity.setInteractionImage("illustrations", "orbital");
		entity.setCustomDescriptionId("sotf_hallow_astropolis_restored");

		market.reapplyIndustries();

		market.setAdmin(Global.getSector().getPlayerPerson());
		market.setPlayerOwned(true);

		Global.getSector().getEconomy().addMarket(market, true);

		if (text != null) {
			removeBuildCosts();
			Global.getSoundPlayer().playUISound("ui_objective_constructed", 1f, 1f);
		}

		// dialog is null because this dialog will immediately close
		SotfHFHRestoreAstropolisFactor factor = new SotfHFHRestoreAstropolisFactor(SotfHopeForHallowhallEventIntel.POINTS_FOR_ASTROPOLIS);
		SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);

		if (!Misc.isPlayerFactionSetUp()) {
			Global.getSector().addTransientScript(new SotfPlayerFactionSetupNag());
		}
	}

	public void removeBuildCosts() {
		if (DebugFlags.OBJECTIVES_DEBUG) {
			return;
		}

		CargoAPI cargo = playerCargo;
		String [] res = getResources();
		int [] quantities = getQuantities();
		for (int i = 0; i < res.length; i++) {
			String commodityId = res[i];
			int quantity = quantities[i];
			cargo.removeCommodity(commodityId, quantity);
		}
	}
}















