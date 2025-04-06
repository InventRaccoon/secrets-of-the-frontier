package data.scripts.campaign.missions.dkcontact;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.academy.GABaseMission;
import com.fs.starfarer.api.impl.campaign.missions.academy.GADepartments;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadMissionCompletedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;

import java.awt.*;
import java.util.Map;

public class SotfDKDataFromRuins extends GABaseMission {
	
	public static float PIRATE_PROB = 0.5f;
	public static float MISSION_DAYS = 120f;
	public static int RAID_DIFFICULTY = 100;
	public static int MARINES_REQUIRED = RAID_DIFFICULTY / 2;
	
	
	public static enum Stage {
		GO_TO_RUINS,
		GET_IN_COMMS_RANGE,
		COMPLETED,
		FAILED,
	}
	
	public static enum Variation {
		BASIC,
		REMNANTS,
		PULSAR,
		DECIV,
	}
	
	protected PlanetAPI planet;
	protected String targetWithArticle;
	protected String target;
	protected Variation variation;
	protected int piratePayment;
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		// if this mission type was already accepted by the player, abort
		if (!setGlobalReference("$sotf_dkData_ref")) {
			return false;
		}
		
		targetWithArticle = pickOne("a library", "a datavault", "an archive", "a laboratory");
		target = targetWithArticle.substring(targetWithArticle.indexOf(" ") + 1);
		
		resetSearch();
		requireSystemTags(ReqMode.ANY, Tags.THEME_REMNANT_RESURGENT, Tags.THEME_REMNANT_SUPPRESSED,
									   Tags.THEME_DERELICT, Tags.THEME_MISC, Tags.THEME_RUINS);
		requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_REMNANT_SECONDARY); // remove not-very-dangerous remnant systems

		requirePlanetUnpopulated();
		requirePlanetWithRuins();
		preferPlanetNotFullySurveyed();
		preferPlanetUnexploredRuins();
		preferPlanetInDirectionOfOtherMissions();
		planet = pickPlanet();

		if (planet == null) {
			return false;
		}
		
		setStartingStage(Stage.GO_TO_RUINS);
		addSuccessStages(Stage.COMPLETED);
		addFailureStages(Stage.FAILED);
		
		makeImportant(planet, "$sotf_dkData_targetPlanet", Stage.GO_TO_RUINS);
		
		connectWithGlobalFlag(Stage.GO_TO_RUINS, Stage.GET_IN_COMMS_RANGE, "$sotf_dkData_gotData");
		connectWithInRangeOfCommRelay(Stage.GET_IN_COMMS_RANGE, Stage.COMPLETED);
		
		setStageOnGlobalFlag(Stage.FAILED, "$sotf_dkData_gaveCoordsToPirates");
		// doesn't really make sense for there to be a time limit on this
		//setTimeLimit(Stage.FAILED, MISSION_DAYS, planet.getStarSystem(), Stage.GET_IN_COMMS_RANGE);
		setCreditReward(CreditReward.AVERAGE);
		
		StarSystemAPI system = planet.getStarSystem();
		if (system.hasTag(Tags.THEME_REMNANT)) {
			variation = Variation.REMNANTS;
			setCreditReward(CreditReward.HIGH);
		} else if (Misc.hasPulsar(system)) {
			variation = Variation.PULSAR;
		} else if (planet.hasCondition(Conditions.DECIVILIZED)) {
			variation = Variation.DECIV;
		} else {
			variation = Variation.BASIC;
		}
		
		//PIRATE_PROB = 1f;
		if ((variation == Variation.BASIC || variation == Variation.DECIV) && rollProbability(PIRATE_PROB)) {
			beginWithinHyperspaceRangeTrigger(planet, 1f, false, Stage.GO_TO_RUINS);
			triggerCreateFleet(FleetSize.MEDIUM, FleetQuality.DEFAULT, Factions.PIRATES, FleetTypes.PATROL_MEDIUM, planet);
			triggerAutoAdjustFleetStrengthMajor();
			triggerSetStandardAggroPirateFlags();
			triggerPickLocationAtInSystemJumpPoint(planet.getStarSystem());
			triggerSpawnFleetAtPickedLocation("$sotf_dkData_pirate", null);
			triggerOrderFleetPatrol(planet);
			triggerFleetMakeImportant(null, Stage.GO_TO_RUINS);
			endTrigger();
			
			piratePayment = genRoundNumber(5000, 15000);
		}
		
		return true;
	}

	protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		if (getPerson().getId().equals(SotfPeople.INADVERTENT)) {
			SotfHFHInadMissionCompletedFactor factor = new SotfHFHInadMissionCompletedFactor(SotfHopeForHallowhallEventIntel.POINTS_PER_INAD_MISSION);
			SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);
		}
	}
	
	protected void updateInteractionDataImpl() {
		set("$sotf_dkData_target", target);
		set("$sotf_dkData_aTarget", targetWithArticle);
		set("$sotf_dkData_planetId", planet.getId());
		set("$sotf_dkData_planetName", planet.getName());
		set("$sotf_dkData_systemName", planet.getStarSystem().getNameWithNoType());
		set("$sotf_dkData_dist", getDistanceLY(planet));
		set("$sotf_dkData_reward", Misc.getWithDGS(getCreditsReward()));
		set("$sotf_dkData_piratePayment", Misc.getWithDGS(piratePayment));
		//variation = Variation.BASIC;
		set("$sotf_dkData_variation", variation);
		if (variation == Variation.DECIV) {
			set("$sotf_dkData_marinesReq", MARINES_REQUIRED);
			set("$sotf_dkData_raidDifficulty", RAID_DIFFICULTY);
		} else if (variation == Variation.PULSAR) {
			PlanetAPI pulsar = Misc.getPulsarInSystem(planet.getStarSystem());
			if (pulsar != null) {
				set("$sotf_dkData_pulsarName", planet.getStarSystem().getNameWithNoType());
			}
		}
	}
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.GO_TO_RUINS) {
			if (variation == Variation.DECIV) {
				info.addPara(getGoToPlanetTextPre(planet) +
							", and retrieve the data from " + targetWithArticle + " found in the ruins there. Around %s " +
							 "marines will be needed to perform the recovery.", opad, h, Misc.getWithDGS(MARINES_REQUIRED));
			} else {
				String extra = "";
				if (variation == Variation.REMNANTS) extra = " A Remnant fleet presence has been confirmed in the system.";
				if (variation == Variation.PULSAR) extra = " The system has a neutron star, which may pose navigational difficulties and risk of damage.";
				info.addPara(getGoToPlanetTextPre(planet) + 
							 ", and retrieve the data from " + targetWithArticle + " found in the ruins there." + extra, opad);
			}
		} else if (currentStage == Stage.GET_IN_COMMS_RANGE) {
			info.addPara(getGetWithinCommsRangeText(), opad);
		}
//		else {
//			super.addDescriptionForCurrentStage(info, width, height); // shows the completed/failed/abandoned text, if needed
//		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.GO_TO_RUINS) {
			//info.addPara("Go to " + planet.getName() + " in the " + planet.getStarSystem().getNameWithLowercaseTypeShort(), tc, pad);
			info.addPara(getGoToPlanetTextShort(planet), tc, pad);
			return true;
		} else if (currentStage == Stage.GET_IN_COMMS_RANGE) {
			info.addPara(getGetWithinCommsRangeTextShort(), tc, pad);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		return "Ruins Data Recovery";
	}
	
	@Override
	public String getBlurbText() {
		return null; // moved this to rules.csv
	}

}


