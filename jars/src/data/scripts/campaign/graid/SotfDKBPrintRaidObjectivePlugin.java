package data.scripts.campaign.graid;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.graid.BaseGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.BlueprintGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class SotfDKBPrintRaidObjectivePlugin extends BlueprintGroundRaidObjectivePluginImpl {

	public SotfDKBPrintRaidObjectivePlugin(MarketAPI market) {
		super(market);
	}

	@Override
	public int performRaid(CargoAPI loot, Random random, float lootMult, TextPanelAPI text) {
		if (marinesAssigned <= 0) return 0;
		
		//random = new Random();
		
		String ship    = "MarketCMD_ship____";
		String weapon  = "MarketCMD_weapon__";
		String fighter = "MarketCMD_fighter_";
		
		FactionAPI playerFaction = Global.getSector().getPlayerFaction();
		
		Set<String> droppedBefore = getDropped();
		
		WeightedRandomPicker<String> notDroppedBefore = new WeightedRandomPicker<String>(random);
		WeightedRandomPicker<String> other = new WeightedRandomPicker<String>(random);
		for (String id : market.getFaction().getKnownShips()) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec(id);
			if (spec.hasTag(Tags.NO_BP_DROP)) continue;
			if (spec.hasTag(Tags.STATION)) continue;
			if (spec.getBuiltInMods().contains(SotfIDs.HULLMOD_CWARSUITE)) continue;
			if (!spec.hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY) && random.nextFloat() < 0.95f) continue;
			
			String id2 = ship + id;
			if (!playerFaction.knowsShip(id) && !droppedBefore.contains(id2)) {
				notDroppedBefore.add(id2, 1f);
			} else {
				other.add(id2, 1f);
			}
		}
		for (String id : market.getFaction().getKnownWeapons()) {
			if (Global.getSettings().getWeaponSpec(id).hasTag(Tags.NO_BP_DROP)) continue;
			
			String id2 = weapon + id;
			if (!playerFaction.knowsWeapon(id) && !droppedBefore.contains(id2)) {
				notDroppedBefore.add(weapon + id, 1f);
			} else {
				other.add(weapon + id, 1f);
			}
		}
		for (String id : market.getFaction().getKnownFighters()) {
			if (Global.getSettings().getFighterWingSpec(id).hasTag(Tags.NO_BP_DROP)) continue;
			
			String id2 = fighter + id;
			if (!playerFaction.knowsFighter(id) && !droppedBefore.contains(id2)) {
				notDroppedBefore.add(fighter + id, 1f);
			} else {
				other.add(fighter + id, 1f);
			}
		}
		
		looted.clear();
		
		Pair<Integer, Integer> q = getQuantityRange();
		int num = q.one + random.nextInt(q.two - q.one + 1);
		for (int i = 0; i < num && (!notDroppedBefore.isEmpty() || !other.isEmpty()); i++) {
			String id = null;
			if (random.nextFloat() < PROBABILITY_TO_DROP_BP_NOT_DROPPED_BEFORE) {
				id = notDroppedBefore.pickAndRemove();
			}
			 
			if (id == null) {
				id = other.pickAndRemove();
			}
			if (id == null) continue;
			
			droppedBefore.add(id);
			
			if (id.startsWith(ship)) {
				String specId = id.substring(ship.length());
				//if (Global.getSettings().getHullSpec(specId).hasTag(Tags.NO_BP_DROP)) continue;
				looted.addSpecial(new SpecialItemData(Items.SHIP_BP, specId), 1);
			} else if (id.startsWith(weapon)) {
				String specId = id.substring(weapon.length());
				//if (Global.getSettings().getWeaponSpec(specId).hasTag(Tags.NO_BP_DROP)) continue;
				looted.addSpecial(new SpecialItemData(Items.WEAPON_BP, specId), 1);
			} else if (id.startsWith(fighter)) {
				String specId = id.substring(fighter.length());
				//if (Global.getSettings().getFighterWingSpec(specId).hasTag(Tags.NO_BP_DROP)) continue;
				looted.addSpecial(new SpecialItemData(Items.FIGHTER_BP, specId), 1);
			}
		}
		
		int totalValue = 0;
		for (CargoStackAPI stack : looted.getStacksCopy()) {
			totalValue += stack.getBaseValuePerUnit() * stack.getSize();
		}
		
		loot.addAll(looted);
		
		xpGained = (int) (totalValue * XP_GAIN_VALUE_MULT);
		return xpGained;
	}
	
	@Override
	public boolean hasTooltip() {
		return true;
	}

	@Override
	public void createTooltip(TooltipMakerAPI t, boolean expanded) {
		float opad = 10f;
		float pad = 3f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color good = Misc.getPositiveHighlightColor();

		// scale
		// value not being predictable
		// unknown blueprints being targeted
		
		t.addPara("Blueprints that enable heavy industry to construct ships, ship weapons, and fighter LPCs. " +
				"Availability based on the scale of the biggest blueprint-using industry at the colony.", 0f);
		
//		t.addPara("The value of the recovered blueprints can vary wildly, but your marines will focus on " +
//				"acquiring unknown blueprints first.", opad);
		t.addPara("The value of the recovered blueprints can vary wildly.", opad);

		t.addPara("Automated failsafes severely impede the recovery of high-grade droneships.", bad, opad);
	}

}








