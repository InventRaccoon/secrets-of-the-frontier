// handles all the rulecommands of outposts
package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.plugins.SotfPlayerFactionSetupNag;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class SotfPrintStationCost extends BaseCommandPlugin {

	public static final float SALVAGE_FRACTION = 0.5f;

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
			build("sotf_outpost", Factions.PLAYER);
		}
		else if (command.equals("canBuild")) {
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
		text.addPara("Upgrading this habitat to an outpost immediately establishes " +
				"a size 3 colony, with a 100% hazard rating.");
		text.highlightInLastPara("3", "100% hazard rating");
		text.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getHighlightColor());
		text.addPara("At size 4 and above, the colony suffers greatly reduced population growth.");
		text.highlightInLastPara("4", "greatly reduced population growth");
		text.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getNegativeHighlightColor());
		text.addPara("The habitat's hazard reduction will be lost. Abandoning the colony will reclaim the " +
				"stable location.");
		text.highlightInLastPara("hazard reduction", "lost");
		text.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getNegativeHighlightColor());
		
		Misc.showCost(text, null, null, getResources(), getQuantities());
		
		if (canBuild(type)) {
			text.addPara("Proceed with construction?");
		} else {
			text.addPara("You do not have the necessary resources to upgrade this structure.");
		}
	}
	
	public String [] getResources() {
		return new String[] {Commodities.CREW, Commodities.HEAVY_MACHINERY, Commodities.SUPPLIES};
	}

	public int [] getQuantities() {
		return new int[] {750, 75, 150};
	}

	public void build(String type, String factionId) {
		LocationAPI loc = entity.getContainingLocation();
		SectorEntityToken built = loc.addCustomEntity(null,
				null,
				type, // type of object, defined in custom_entities.json
				factionId); // faction
		if (entity.getOrbit() != null) {
			built.setOrbit(entity.getOrbit().makeCopy());
		}
		built.setLocation(entity.getLocation().x, entity.getLocation().y);
		loc.removeEntity(entity);

		market = Global.getFactory().createMarket(Misc.genUID(), "Station", 3);
		market.setSize(3);

		market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

		market.setFactionId(factionId);
		market.addCondition(Conditions.POPULATION_3);
		market.addCondition(SotfIDs.CONDITION_CRAMPED);

		market.addIndustry(Industries.POPULATION);
		market.addIndustry(Industries.SPACEPORT);
		market.getIndustry(Industries.SPACEPORT).startBuilding();

		market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		StoragePlugin plugin = (StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin();
		if (plugin != null) {
			plugin.setPlayerPaidToUnlock(true);
		}
		market.addSubmarket(Submarkets.LOCAL_RESOURCES);

		market.setPrimaryEntity(built);
		built.setMarket(market);

		String name = generateName();
		market.setName(name);
		built.setName(name);

		market.reapplyIndustries();

		market.setAdmin(Global.getSector().getPlayerPerson());
		market.setPlayerOwned(true);

		Global.getSector().getEconomy().addMarket(market, true);

		if (text != null) {
			removeBuildCosts();
			Global.getSoundPlayer().playUISound("ui_objective_constructed", 1f, 1f);
		}

		if (!Misc.isPlayerFactionSetUp()) {
			Global.getSector().addTransientScript(new SotfPlayerFactionSetupNag());
		}

	}

	protected String generateName() {
		MarkovNames.loadIfNeeded();

		MarkovNames.MarkovNameResult gen = null;
		for (int i = 0; i < 10; i++) {
			gen = MarkovNames.generate(null);
			if (gen != null) {
				String test = gen.name;
				if (test.toLowerCase().startsWith("the ")) continue;
				test += " Outpost";
				if (test.length() > 22) continue;

				return test;
			}
		}
		return null;
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















