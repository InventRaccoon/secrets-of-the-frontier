package data.scripts.campaign.missions.dkcontact;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ItemInProductionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.econ.impl.ShipQuality;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel.ProductionData;
import com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 *	Haven's custom production contract, mostly equivalent to a vanilla military contact (bit pricier)
 */

public class SotfDKCustProdContr extends HubMissionWithBarEvent {

	public static float PROD_DAYS = 60f;
	
	public static float MIN_CAPACITY = 50000;
	public static float MAX_CAPACITY = 500000;
	
	public static float MAX_PROD_CAPACITY_AT_SHIP_UNITS = 10;
	public static float MAX_PROD_CAPACITY_MULT = 0.35f;
	
	public static enum Stage {
		WAITING,
		DELIVERED,
		FAILED,
	}

	protected Set<String> ships = new LinkedHashSet<String>();
	protected Set<String> weapons = new LinkedHashSet<String>();
	protected Set<String> fighters = new LinkedHashSet<String>();

	protected int maxCapacity;
	protected float costMult;
	protected ProductionData data;
	protected int cost;
	protected FactionAPI faction;
	protected MarketAPI market;
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		if (!setPersonMissionRef(person, "$cpc_ref")) {
			return false;
		}
		
		market = getPerson().getMarket();
		if (market == null) return false;
		if (Misc.getStorage(market) == null) return false;
		
		faction = person.getFaction();

		maxCapacity = getRoundNumber(MIN_CAPACITY + (MAX_CAPACITY - MIN_CAPACITY) * getQuality());
		float capMult = market.getCommodityData(Commodities.SHIPS).getMaxSupply() / MAX_PROD_CAPACITY_AT_SHIP_UNITS;
		if (capMult > 1) capMult = 1f;
		if (capMult < MAX_PROD_CAPACITY_MULT) capMult = MAX_PROD_CAPACITY_MULT;
		maxCapacity *= capMult;
		maxCapacity = getRoundNumber(maxCapacity);

		costMult = 1.1f - CustomProductionContract.MILITARY_MAX_COST_DECREASE * getRewardMultFraction();
		addBlueprints();
		if (ships.isEmpty() && weapons.isEmpty() && fighters.isEmpty()) return false;

		setStartingStage(Stage.WAITING);
		setSuccessStage(Stage.DELIVERED);
		setFailureStage(Stage.FAILED);
		setNoAbandon();
		
		connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, PROD_DAYS);
		setStageOnMarketDecivilized(Stage.FAILED, market);
		
		return true;
	}
	
	protected void addBlueprints() {
		for (String id : faction.getKnownShips()) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec(id);
			if (spec.hasTag(Tags.NO_SELL)) continue;
			if (!spec.hasTag(Tags.AUTOMATED_RECOVERABLE)) continue; // no Nexus
			if (spec.getBuiltInMods().contains(SotfIDs.HULLMOD_CWARSUITE) && getPerson().getId().equals(SotfPeople.INADVERTENT)) continue;
			if (!spec.hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY) && getPerson().getId().equals(SotfPeople.INADVERTENT) &&
					getPerson().getRelToPlayer().isAtBest(RepLevel.WELCOMING)) continue;
			ships.add(id);
		}
		for (String id : faction.getKnownWeapons()) {
			WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
			if (spec.hasTag(Tags.NO_DROP)) continue;
			if (spec.hasTag(Tags.NO_SELL)) continue;
			if (spec.getTier() > 2 && getPerson().getId().equals(SotfPeople.INADVERTENT) &&
					getPerson().getRelToPlayer().isAtBest(RepLevel.WELCOMING)) continue;
			weapons.add(id);
		}
		for (String id : faction.getKnownFighters()) {
			FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(id);
			if (spec.hasTag(Tags.NO_DROP)) continue;
			//if (spec.hasTag(Tags.NO_SELL)) continue; // otherwise can't buy rem fighters
			fighters.add(id);
		}
	}


	protected void updateInteractionDataImpl() {
		set("$cpc_maxCapacity", Misc.getWithDGS(maxCapacity));
		set("$cpc_costPercent", (int)Math.round(costMult * 100f) + "%");
		set("$cpc_days", "" + (int) PROD_DAYS);
		set("$sotfdkcpc_highRep", getPerson().getRelToPlayer().isAtWorst(RepLevel.FRIENDLY));
	}
	
	@Override
	public void addDescriptionForCurrentStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.WAITING) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(PROD_DAYS - elapsed);
			PersonAPI person = getPerson();
			
			LabelAPI label = info.addPara("The order will be delivered to storage " + market.getOnOrAt() + " " + market.getName() + 
					" in %s " + getDayOrDays(d) + ".", opad,
					Misc.getHighlightColor(), "" + d);
			label.setHighlight(market.getName(), "" + d);
			label.setHighlightColors(market.getFaction().getBaseUIColor(), h);
			
			//intel.createSmallDescription(info, width, height);
			showCargoContents(info, width, height);
			
			
		} else if (currentStage == Stage.DELIVERED) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(elapsed);
			LabelAPI label = info.addPara("The order was delivered to storage " + market.getOnOrAt() + " " + market.getName() + 
					" %s " + getDayOrDays(d) + " ago.", opad,
					Misc.getHighlightColor(), "" + d);
			label.setHighlight(market.getName(), "" + d);
			label.setHighlightColors(market.getFaction().getBaseUIColor(), h);
			
			showCargoContents(info, width, height);
			addDeleteButton(info, width);
		} else if (currentStage == Stage.FAILED) {
			if (market.hasCondition(Conditions.DECIVILIZED)) {
				info.addPara("This order will not be completed because %s" + 
						" has decivilized.", opad,
						faction.getBaseUIColor(), market.getName());
			} else {
				info.addPara("You've learned that this order will not be completed.", opad);
			}
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.WAITING) {
			float elapsed = getElapsedInCurrentStage();
			addDays(info, "until delivery", PROD_DAYS - elapsed, tc, pad);
			return true;
		} else if (currentStage == Stage.DELIVERED) {
			info.addPara("Delivered to %s", pad, tc, market.getFaction().getBaseUIColor(), market.getName());
			return true;
		}
		return false;
	}	
	
	@Override
	public String getBaseName() {
		return "Custom Production Order";
	}
	
	protected String getMissionTypeNoun() {
		return "order";
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return market.getPrimaryEntity();
	}
	
	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		float f = (float) cost / (float) maxCapacity;
		
		AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
		Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);
		adjustRep(dialog.getTextPanel(), null, RepActions.MISSION_SUCCESS);
		
		ships = null;
		fighters = null;
		weapons = null;
	}
	

	@Override
	public void setCurrentStage(Object next, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.setCurrentStage(next, dialog, memoryMap);
		
		if (currentStage == Stage.DELIVERED) {
			StoragePlugin plugin = (StoragePlugin) Misc.getStorage(getPerson().getMarket());
			if (plugin == null) return;
			plugin.setPlayerPaidToUnlock(true);
			
			CargoAPI cargo = plugin.getCargo();
			for (CargoAPI curr : data.data.values()) {
				cargo.addAll(curr, true);
			}
		}
	}
	
	
	@Override
	protected boolean callAction(final String action, final String ruleId, final InteractionDialogAPI dialog, 
								 final List<Token> params,
								 final Map<String, MemoryAPI> memoryMap) {
		if ("pickPlayerBP".equals(action)) {
			dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl() {
				@Override
				public float getCostMult() {
					return costMult;
				}
				@Override
				public float getMaximumValue() {
					return maxCapacity;
				}
				@Override
				public void notifyProductionSelected(FactionProductionAPI production) {
					convertProdToCargo(production);
					FireBest.fire(null, dialog, memoryMap, "CPCBlueprintsPicked");
				}
			});
			return true;
		}
		if ("pickContactBP".equals(action)) {
			dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl() {
				@Override
				public Set<String> getAvailableFighters() {
					return fighters;
				}
				@Override
				public Set<String> getAvailableShipHulls() {
					return ships;
				}
				@Override
				public Set<String> getAvailableWeapons() {
					return weapons;
				}
				@Override
				public float getCostMult() {
					return costMult;
				}
				@Override
				public float getMaximumValue() {
					return maxCapacity;
				}
				@Override
				public void notifyProductionSelected(FactionProductionAPI production) {
					convertProdToCargo(production);
					FireBest.fire(null, dialog, memoryMap, "CPCBlueprintsPicked");
				}
			});
			return true;
		}
		
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}
	
	
	protected void convertProdToCargo(FactionProductionAPI prod) {
		cost = prod.getTotalCurrentCost();
		data = new ProductionData();
		CargoAPI cargo = data.getCargo("Order manifest");
		
		float quality = ShipQuality.getShipQuality(market, market.getFactionId());

		CampaignFleetAPI ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true);
		ships.setCommander(Global.getSector().getPlayerPerson());
		ships.getFleetData().setShipNameRandom(genRandom);
		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = quality;
		p.mode = ShipPickMode.PRIORITY_THEN_ALL;
		p.persistent = false;
		p.seed = genRandom.nextLong();
		p.timestamp = null;
		
		FleetInflater inflater = Misc.getInflater(ships, p);
		ships.setInflater(inflater);
		
		for (ItemInProductionAPI item : prod.getCurrent()) {
			int count = item.getQuantity();
				
			if (item.getType() == ProductionItemType.SHIP) {
				for (int i = 0; i < count; i++) {
					ships.getFleetData().addFleetMember(item.getSpecId() + "_Hull");
				}
			} else if (item.getType() == ProductionItemType.FIGHTER) {
				cargo.addFighters(item.getSpecId(), count);
			} else if (item.getType() == ProductionItemType.WEAPON) {
				cargo.addWeapons(item.getSpecId(), count);
			}
		}
		
		// so that it adds d-mods
		ships.inflateIfNeeded();
		for (FleetMemberAPI member : ships.getFleetData().getMembersListCopy()) {
			// it should be due to the inflateIfNeeded() call, this is just a safety check
			if (member.getVariant().getSource() == VariantSource.REFIT) {
				member.getVariant().clear();
			}
			cargo.getMothballedShips().addFleetMember(member);
		}
	}
	
	public void showCargoContents(TooltipMakerAPI info, float width, float height) {
		if (data == null) return;
		
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float small = 3f;
		float opad = 10f;

		List<String> keys = new ArrayList<String>(data.data.keySet());
		Collections.sort(keys, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		
		for (String key : keys) {
			CargoAPI cargo = data.data.get(key);
			if (cargo.isEmpty() && 
					((cargo.getMothballedShips() == null || 
					  cargo.getMothballedShips().getMembersListCopy().isEmpty()))) {
				continue;
			}
		
			info.addSectionHeading(key, faction.getBaseUIColor(), faction.getDarkUIColor(), 
								   Alignment.MID, opad);
			
			if (!cargo.getStacksCopy().isEmpty()) {
				info.addPara("Ship weapons and fighters:", opad);
				info.showCargo(cargo, 20, true, opad);
			}
			
			if (!cargo.getMothballedShips().getMembersListCopy().isEmpty()) {
				CountingMap<String> counts = new CountingMap<String>();
				for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy()) {
					counts.add(member.getVariant().getHullSpec().getHullName() + " " + member.getVariant().getDesignation());
				}
				
				info.addPara("Ship hulls:", opad);
				info.showShips(cargo.getMothballedShips().getMembersListCopy(), 20, true,
							   getCurrentStage() == Stage.WAITING, opad);
			}
		}
	}
}











