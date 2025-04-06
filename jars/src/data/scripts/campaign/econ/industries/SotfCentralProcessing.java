package data.scripts.campaign.econ.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.*;
import com.fs.starfarer.api.campaign.listeners.ColonyOtherFactorsListener;
import com.fs.starfarer.api.campaign.listeners.GroundRaidObjectivesListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.econ.CommRelayCondition;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.econ.impl.ConstructionQueue;
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.graid.BlueprintGroundRaidObjectivePluginImpl;
import com.fs.starfarer.api.impl.campaign.graid.GroundRaidObjectivePlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.StatModValueGetter;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import data.scripts.campaign.graid.SotfDKBPrintRaidObjectivePlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *	Modified population industry, for Dustkeeper stations, since they're operated by AIs
 */

public class SotfCentralProcessing extends BaseIndustry implements MarketImmigrationModifier, GroundRaidObjectivesListener {

	public static float OFFICER_BASE_PROB = Global.getSettings().getFloat("officerBaseProb");
	public static float OFFICER_PROB_PER_SIZE = Global.getSettings().getFloat("officerProbPerColonySize");
	public static float OFFICER_ADDITIONAL_BASE_PROB = Global.getSettings().getFloat("officerAdditionalBaseProb");
	public static float OFFICER_BASE_MERC_PROB = Global.getSettings().getFloat("officerBaseMercProb");
	public static float ADMIN_BASE_PROB = Global.getSettings().getFloat("adminBaseProb");
	public static float ADMIN_PROB_PER_SIZE = Global.getSettings().getFloat("adminProbPerColonySize");

	public static float AUTO_STABILITY_BONUS = 2f;

	public static float IMPROVE_STABILITY_BONUS = 1f;

	public static boolean HAZARD_INCREASES_DEFENSE = false;
	
	public void apply() {
		modifyStability(this, market, getModId(3));
		
		super.apply(true);
		
		int size = market.getSize();

		demand(Commodities.SUPPLIES, Math.min(size, 3));
		demand(Commodities.METALS, Math.min(size, 3));

		Global.getSector().getListenerManager().addListener(this);

		// AIs have no need for humans or their goods, though they can still produce the latter
		for (Industry industry : market.getIndustries()) {
			industry.getSupply(Commodities.FOOD).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.FOOD).getQuantity().modifyMult(id, 0);
			industry.getSupply(Commodities.ORGANS).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.ORGANS).getQuantity().modifyMult(id, 0);
			industry.getSupply(Commodities.CREW).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.CREW).getQuantity().modifyMult(id, 0);
			industry.getSupply(Commodities.MARINES).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.MARINES).getQuantity().modifyMult(id, 0);

			industry.getDemand(Commodities.DRUGS).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.DOMESTIC_GOODS).getQuantity().modifyMult(id, 0);
			industry.getDemand(Commodities.LUXURY_GOODS).getQuantity().modifyMult(id, 0);
		}

		boolean spaceportFirstInQueue = false;
		for (ConstructionQueue.ConstructionQueueItem item : market.getConstructionQueue().getItems()) {
			IndustrySpecAPI spec = Global.getSettings().getIndustrySpec(item.id);
			if (spec.hasTag(Industries.TAG_SPACEPORT)) {
				spaceportFirstInQueue = true;
			}
			break;
		}
		if (spaceportFirstInQueue && Misc.getCurrentlyBeingConstructed(market) != null) {
			spaceportFirstInQueue = false;
		}
		if (!market.hasSpaceport() && !spaceportFirstInQueue) {
			float accessibilityNoSpaceport = Global.getSettings().getFloat("accessibilityNoSpaceport");
			market.getAccessibilityMod().modifyFlat(getModId(0), accessibilityNoSpaceport, "No spaceport");
		}

		float sizeBonus = getAccessibilityBonus(size);
		if (sizeBonus > 0) {
			market.getAccessibilityMod().modifyFlat(getModId(1), sizeBonus, "Colony size");
		}

		float stability = market.getPrevStability();
		float stabilityQualityMod = FleetFactoryV3.getShipQualityModForStability(stability);
		float doctrineQualityMod = market.getFaction().getDoctrine().getShipQualityContribution();

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlatAlways(getModId(0), stabilityQualityMod,
				"Stability");

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlatAlways(getModId(1), doctrineQualityMod,
				Misc.ucFirst(market.getFaction().getEntityNamePrefix()) + " fleet doctrine");

		//float stabilityDefenseMult = 0.5f + stability / 10f;
		float stabilityDefenseMult = 0.25f + stability / 10f * 0.75f;
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMultAlways(getModId(),
				stabilityDefenseMult, "Stability");

		float baseDef = getBaseGroundDefenses(market.getSize());
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlatAlways(getModId(),
				baseDef, "Base value for a size " + market.getSize() + " colony");


		//if (market.getHazardValue() > 1f) {
		if (HAZARD_INCREASES_DEFENSE) {
			market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMultAlways(getModId(1),
					Math.max(market.getHazardValue(), 1f), "Colony hazard rating");
		}
		//}

		market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(getModId(), getMaxIndustries(), null);

//		if (market.isPlayerOwned()) {
//			System.out.println("wfwefwef");
//		}
		FactionDoctrineAPI doctrine = market.getFaction().getDoctrine();
		float doctrineShipsMult = FleetFactoryV3.getDoctrineNumShipsMult(doctrine.getNumShips());
		float marketSizeShipsMult = FleetFactoryV3.getNumShipsMultForMarketSize(market.getSize());
		float deficitShipsMult = FleetFactoryV3.getShipDeficitFleetSizeMult(market);
		float stabilityShipsMult = FleetFactoryV3.getNumShipsMultForStability(stability);

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlatAlways(getModId(0), marketSizeShipsMult,
				"Colony size");

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(1), doctrineShipsMult,
				Misc.ucFirst(market.getFaction().getEntityNamePrefix()) + " fleet doctrine");

		if (deficitShipsMult != 1f) {
			market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(getModId(2), deficitShipsMult,
					getDeficitText(Commodities.SHIPS));
		} else {
			market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(2), deficitShipsMult,
					getDeficitText(Commodities.SHIPS).replaceAll("shortage", "demand met"));
		}

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(getModId(3), stabilityShipsMult,
				"Stability");

		modifyStability2(this, market, getModId(3));

		market.addTransientImmigrationModifier(this);
	}

	// don't let the player build it
	public boolean isAvailableToBuild() {
		return false;
	}

	// don't show in industries screen
	public boolean showWhenUnavailable() {
		return false;
	}

	public void modifyIncoming(MarketAPI market, PopulationComposition incoming) {
		incoming.getWeight().modifyMult(getModId(), 0f, "No population");
	}

	// I do not care how dedicated you are to the Path of Ludd
	// You are not going to infiltrate a nexus operated by a network of AIs
	@Override public float getPatherInterest() {
		return -20f;
	}
	
	public static float getAccessibilityBonus(int marketSize) {
		if (marketSize <= 4) return 0f;
		if (marketSize == 5) return 0.1f;
		if (marketSize == 6) return 0.15f;
		if (marketSize == 7) return 0.2f;
		if (marketSize == 8) return 0.25f;
		return 0.3f;
	}
	public static float getBaseGroundDefenses(int marketSize) {
		return 350;
	}

	@Override
	public void unapply() {
		super.unapply();

		market.getStability().unmodify(getModId(0));
		market.getStability().unmodify(getModId(1));
		market.getStability().unmodify(getModId(2));

		market.getAccessibilityMod().unmodifyFlat(getModId(0));
		market.getAccessibilityMod().unmodifyFlat(getModId(1));

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).unmodifyFlat(getModId(1));

		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyFlat(getModId());
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());
		if (HAZARD_INCREASES_DEFENSE) {
			market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId(1)); // hazard value modifier
		}

		market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodifyFlat(getModId());

		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(getModId(0));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(1));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(2));
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyMult(getModId(3));

		unmodifyStability(market, getModId(3));

		market.removeTransientImmigrationModifier(this);
	}


	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		return true;
	}
	
	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			
			MutableStat stabilityMods = new MutableStat(0);
			
			float total = 0;
			for (StatMod mod : market.getStability().getFlatMods().values()) {
				if (mod.source.startsWith(getModId())) {
					stabilityMods.modifyFlat(mod.source, mod.value, mod.desc);
					total += mod.value;
				}
			}
			
			String totalStr = "+" + (int)Math.round(total);
			Color h = Misc.getHighlightColor();
			if (total < 0) {
				totalStr = "" + (int)Math.round(total);
				h = Misc.getNegativeHighlightColor();
			}
			float opad = 10f;
			float pad = 3f;
			if (total >= 0) {
				tooltip.addPara("Stability bonus: %s", opad, h, totalStr);
			} else {
				tooltip.addPara("Stability penalty: %s", opad, h, totalStr);
			}
			tooltip.addStatModGrid(400, 30, opad, pad, stabilityMods, new StatModValueGetter() {
				public String getPercentValue(StatMod mod) {
					return null;
				}
				public String getMultValue(StatMod mod) {
					return null;
				}
				public Color getModColor(StatMod mod) {
					if (mod.value < 0) return Misc.getNegativeHighlightColor();
					return null;
				}
				public String getFlatValue(StatMod mod) {
					return null;
				}
			});
			
		}
	}

	public static float getIncomeStabilityMult(float stability) {
		if (stability <= 5) {
			return Math.max(0, stability / 5f);
		}
		return 1f;
		//return 1f + (stability - 5f) * .1f;
	}

	public static float getUpkeepHazardMult(float hazard) {
		//float hazardMult = 1f + hazard;
		float hazardMult = hazard;
		float min = Global.getSettings().getFloat("minUpkeepMult");
		if (hazardMult < min) hazardMult = min;
		return hazardMult;
	}


	public static int getMismanagementPenalty() {
		int outposts = 0;
		for (MarketAPI curr : Global.getSector().getEconomy().getMarketsCopy()) {
			if (!curr.isPlayerOwned()) continue;

			if (curr.getAdmin().isPlayer()) {
				outposts++;
			}
		}

		MutableCharacterStatsAPI stats = Global.getSector().getCharacterData().getPerson().getStats();

		int maxOutposts = stats.getOutpostNumber().getModifiedInt();

		int overOutposts = outposts - maxOutposts;

		int penaltyOrBonus = (int) (overOutposts * Misc.getOutpostPenalty());

		return penaltyOrBonus;
	}

	public static void modifyStability2(Industry industry, MarketAPI market, String modId) {
		if (Misc.getNumIndustries(market) > Misc.getMaxIndustries(market)) {
			market.getStability().modifyFlat("_" + modId + "_overmax", -Misc.OVER_MAX_INDUSTRIES_PENALTY, "Maximum number of industries exceeded");
		} else {
			market.getStability().unmodifyFlat("_" + modId + "_overmax");
		}
	}

	/**
	 * Called from core code after all industry effects are applied.
	 */
	public static void modifyUpkeepByHazardRating(MarketAPI market, String modId) {
		market.getUpkeepMult().modifyMultAlways(modId, getUpkeepHazardMult(market.getHazardValue()), "Hazard rating");
	}

	public static void modifyStability(Industry industry, MarketAPI market, String modId) {
		market.getIncomeMult().modifyMultAlways(modId, getIncomeStabilityMult(market.getPrevStability()), "Stability");

		market.getStability().modifyFlat("_" + modId + "_ms", Global.getSettings().getFloat("stabilityBaseValue"), "Base value");

		market.getStability().modifyFlat(modId + "_autoBonus", AUTO_STABILITY_BONUS, "Automated station");

		float inFactionSupply = 0f;
		float totalDemand = 0f;
		for (CommodityOnMarketAPI com : market.getCommoditiesCopy()) {
			if (com.isNonEcon()) continue;

			int d = com.getMaxDemand();
			if (d <= 0) continue;

			totalDemand += d;
			CommodityMarketDataAPI cmd = com.getCommodityMarketData();
			int inFaction = Math.max(Math.min(com.getMaxSupply(), com.getAvailable()),
					Math.min(cmd.getMaxShipping(market, true), cmd.getMaxExport(market.getFactionId())));
			if (inFaction > d) inFaction = d;
			if (inFaction < d) inFaction = Math.max(Math.min(com.getMaxSupply(), com.getAvailable()), 0);

			//CommoditySourceType source = cmd.getMarketShareData(market).getSource();;
			//if (source != CommoditySourceType.GLOBAL) {
			//	inFactionSupply += Math.min(d - inFaction, com.getAvailable());
			//}
			inFactionSupply += Math.max(0, Math.min(inFaction, com.getAvailable()));
		}

		if (totalDemand > 0) {
			float max = Global.getSettings().getFloat("upkeepReductionFromInFactionImports");
			float f = inFactionSupply / totalDemand;
			if (f < 0) f = 0;
			if (f > 1) f = 1;
			if (f > 0) {
				float mult = Math.round(100f - (f * max * 100f)) / 100f;
				String desc = "Demand supplied in-faction (" + (int)Math.round(f * 100f) + "%)";
				if (f == 1f) desc = "All demand supplied in-faction";
				market.getUpkeepMult().modifyMultAlways(modId + "ifi", mult, desc);
			} else {
				market.getUpkeepMult().modifyMultAlways(modId + "ifi", 1f, "All demand supplied out-of-faction; no upkeep reduction");
			}
		}


		if (market.isPlayerOwned() && market.getAdmin().isPlayer()) {
			int penalty = getMismanagementPenalty();
			if (penalty > 0) {
				market.getStability().modifyFlat("_" + modId + "_mm", -penalty, "Mismanagement penalty");
			} else if (penalty < 0) {
				market.getStability().modifyFlat("_" + modId + "_mm", -penalty, "Management bonus");
			} else {
				market.getStability().unmodifyFlat("_" + modId + "_mm");
			}
		} else {
			market.getStability().unmodifyFlat(modId + "_mm");
		}

		if (!market.hasCondition(Conditions.COMM_RELAY)) {
			market.getStability().modifyFlat(CommRelayCondition.COMM_RELAY_MOD_ID, CommRelayCondition.NO_RELAY_PENALTY, "No active comm relay in-system");
		}
	}


	public static void unmodifyStability(MarketAPI market, String modId) {
		market.getIncomeMult().unmodifyMult(modId);
		market.getUpkeepMult().unmodifyMult(modId);
		market.getUpkeepMult().unmodifyMult(modId + "ifi");

		market.getStability().unmodifyFlat(modId);

//		for (int i = 0; i < 30; i++) {
//			market.getStability().unmodify(modId + i);
//		}
		market.getStability().unmodifyFlat("_" + modId + "_mm");
		market.getStability().unmodifyFlat("_" + modId + "_ms");
		market.getStability().unmodifyFlat("_" + modId + "_overmax");

		if (!market.hasCondition(Conditions.COMM_RELAY)) {
			market.getStability().unmodifyFlat(CommRelayCondition.COMM_RELAY_MOD_ID);
		}

	}



	@Override
	public boolean showShutDown() {
		return false;
	}

	@Override
	public String getCanNotShutDownReason() {
		//return "Use \"Abandon Colony\" instead.";
		return null;
	}

	@Override
	public boolean canShutDown() {
		return false;
	}

	@Override
	protected String getDescriptionOverride() {
		if (market.getSize() <= 4) {
			return "Being entirely automated, this station has no population, instead replaced by an extensive computing " +
					"network, subdivided into multiple artifical minds that manage it with superhuman efficiency.";
		}
		return super.getDescriptionOverride();
	}

	public String getBuildOrUpgradeProgressText() {
		return super.getBuildOrUpgradeProgressText();
	}

	@Override
	public float getBuildOrUpgradeProgress() {
		if (!super.isBuilding() && market.getSize() < Misc.MAX_COLONY_SIZE) {
			return Misc.getMarketSizeProgress(market);
		}
		return super.getBuildOrUpgradeProgress();
	}

	@Override
	public boolean isBuilding() {
		if (!super.isBuilding() && market.getSize() < Misc.MAX_COLONY_SIZE && getBuildOrUpgradeProgress() > 0) return true;

		return super.isBuilding();
	}

	@Override
	public boolean isUpgrading() {
		if (!super.isBuilding() && market.getSize() < Misc.MAX_COLONY_SIZE) return true;

		return super.isUpgrading();
	}

	private float getAICoreImpact(String coreId) {
		if (Commodities.ALPHA_CORE.equals(coreId)) return 10f;
		if (Commodities.BETA_CORE.equals(coreId)) return 4f;
		if (Commodities.GAMMA_CORE.equals(coreId)) return 1f;
		return 0f;
	}

	public boolean canBeDisrupted() {
		return false;
	}

	public int getMaxIndustries() {
		return getMaxIndustries(market.getSize());
	}

	public static int [] MAX_IND = null;
	public static int getMaxIndustries(int size) {
		if (MAX_IND == null) {
			try {
				MAX_IND = new int [10];
				JSONArray a = Global.getSettings().getJSONArray("maxIndustries");
				for (int i = 0; i < MAX_IND.length; i++) {
					MAX_IND[i] = a.getInt(i);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		}
		size--;
		if (size < 0) size = 0;
		if (size > 9) size = 9;
		return MAX_IND[size];
//		if (size <= 3) return 1;
//		if (size <= 5) return 2;
//		if (size <= 7) return 3;
//		return 4;
	}

//	@Override
//	public boolean canImprove() {
//		return true;
//	}
//
//	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
//		float opad = 10f;
//		Color highlight = Misc.getHighlightColor();
//
//
//		String str = "" + (int)Math.round(market.getSize() * IMPROVE_GROWTH_BONUS);
//
//		if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
//			info.addPara("Population growth increased by %s.", 0f, highlight,str);
//		} else {
//			info.addPara("Increases population growth by %s. Bonus is based on colony size.", 0f, highlight,str);
//		}
//
//		info.addSpacer(opad);
//		super.addImproveDesc(info, mode);
//	}

	@Override
	public boolean canImprove() {
		return true;
	}

	protected void applyImproveModifiers() {
		if (isImproved()) {
			market.getStability().modifyFlat("PAI_improve", IMPROVE_STABILITY_BONUS,
					getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
		} else {
			market.getStability().unmodifyFlat("PAI_improve");
		}
	}

	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();


		if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
			info.addPara("Stability increased by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
		} else {
			info.addPara("Increases stability by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
		}

		info.addSpacer(opad);
		super.addImproveDesc(info, mode);
	}

	public static Pair<SectorEntityToken, Float> getNearestCoronalTap(Vector2f locInHyper, boolean usable) {
		SectorEntityToken nearest = null;
		float minDist = Float.MAX_VALUE;

		for (SectorEntityToken entity : Global.getSector().getCustomEntitiesWithTag(Tags.CORONAL_TAP)) {
			if (!usable || entity.getMemoryWithoutUpdate().contains("$usable")) {
				float dist = Misc.getDistanceLY(locInHyper, entity.getLocationInHyperspace());
				if (dist > ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS &&
						Math.round(dist * 10f) <= ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS * 10f) {
					dist = ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS;
				}
				if (dist < minDist) {
					minDist = dist;
					nearest = entity;
				}
			}
		}

		if (nearest == null) return null;

		return new Pair<SectorEntityToken, Float>(nearest, minDist);
	}

	@Override
	public void modifyRaidObjectives(MarketAPI market, SectorEntityToken entity, List<GroundRaidObjectivePlugin> objectives, MarketCMD.RaidType type, int marineTokens, int priority) {
		if (priority != 8) return;
		if (market == null) return;
		if (market != getMarket()) return;
		if (type != MarketCMD.RaidType.VALUABLE) return;

		// clone list to avoid concurrent modification exceptions
		ArrayList<GroundRaidObjectivePlugin> list = new ArrayList<>(objectives);

		for (GroundRaidObjectivePlugin objective : list) {
			if (objective instanceof BlueprintGroundRaidObjectivePluginImpl) {
				objectives.remove(objective);
				objectives.add(new SotfDKBPrintRaidObjectivePlugin(market));
			}
		}
	}

	@Override
	public void reportRaidObjectivesAchieved(RaidResultData data, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

	}

	public static class CoronalTapFactor implements ColonyOtherFactorsListener {
		public boolean isActiveFactorFor(SectorEntityToken entity) {
			return getNearestCoronalTap(entity.getLocationInHyperspace(), true) != null;
		}

		public void printOtherFactors(TooltipMakerAPI text, SectorEntityToken entity) {
			Pair<SectorEntityToken, Float> p = getNearestCoronalTap(entity.getLocationInHyperspace(), true);
			if (p != null) {
				Color h = Misc.getHighlightColor();
				float opad = 10f;

				String dStr = "" + Misc.getRoundedValueMaxOneAfterDecimal(p.two);
				String lights = "light-years";
				if (dStr.equals("1")) lights = "light-year";

				if (p.two > ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS) {
					text.addPara("The nearest coronal tap is located in the " +
									p.one.getContainingLocation().getNameWithLowercaseType() + ", %s " + lights + " away. The maximum " +
									"range at a portal can connect to a tap is %s light-years.",
							opad, h,
							"" + Misc.getRoundedValueMaxOneAfterDecimal(p.two),
							"" + (int)ItemEffectsRepo.CORONAL_TAP_LIGHT_YEARS);
				} else {
					text.addPara("The nearest coronal tap is located in the " +
									p.one.getContainingLocation().getNameWithLowercaseType() + ", %s " + lights + " away, allowing " +
									"a coronal portal located here to connect to it.",
							opad, h,
							"" + Misc.getRoundedValueMaxOneAfterDecimal(p.two));
				}
			}
		}
	}

}







