package data.scripts.campaign.missions.dkcontact;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadMissionCompletedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;

import java.awt.*;
import java.util.Map;

public class SotfDKDeployProbe extends HubMissionWithBarEvent {

	public static float PROB_COMPLICATIONS = 0.5f;
	public static float PROB_PATROL_AFTER = 0.5f;
	public static float MISSION_DAYS = 120f;
	
	public static enum Stage {
		DROP_OFF,
		COMPLETED,
		FAILED,
	}
	
	protected String probeType;
	protected SectorEntityToken target;
	protected StarSystemAPI system;

	protected boolean isABomb;
	// scavenge THIS, you Regnant-damned scavs
	protected String bombName = "fake probe containing a colossal antimatter-enriched explosive device wired to a delayed-activation proximity detonator";
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		probeType = pickOne("Neutrino Signal Spectrometer",
						"Hyper-Topographic Data Collector",
						"Stealth-Coated Hyperdrive Detector",
						"Coronal Energy Analyzer",
						"Gravitic Field Measurement Apparatus",
						bombName
						);

		isABomb = probeType.equals(bombName);
		
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		
		if (!setPersonMissionRef(person, "$sotfdkdpm_ref")) {
			return false;
		}

		requireSystemNot(createdAt.getStarSystem());
		requireSystemInterestingAndNotUnsafeOrCore();
		preferSystemWithinRangeOf(createdAt.getLocationInHyperspace(), 10f, 30f);
		preferSystemUnexplored();
		preferSystemInDirectionOfOtherMissions();
		
		system = pickSystem();
		if (system == null) return false;

		target = spawnMissionNode(new LocData(EntityLocationType.HIDDEN_NOT_NEAR_STAR, null, system, false));
		if (!setEntityMissionRef(target, "$sotfdkdpm_ref")) return false;
		
		makeImportant(target, "$sotfdkdpm_target", Stage.DROP_OFF);
		//setMapMarkerNameColor(Misc.getGrayColor());

		setStartingStage(Stage.DROP_OFF);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		
		setStageOnMemoryFlag(Stage.COMPLETED, target, "$sotfdkdpm_completed");
		setTimeLimit(Stage.FAILED, MISSION_DAYS, null);

		setCreditReward(CreditReward.AVERAGE);
		
		if (rollProbability(PROB_COMPLICATIONS)) {
			triggerComplicationBegin(Stage.DROP_OFF, ComplicationSpawn.APPROACHING_OR_ENTERING,
					system, Factions.LUDDIC_PATH,
					"the " + getWithoutArticle(probeType.toLowerCase()), "it",
					"the " + getWithoutArticle(probeType.toLowerCase()) + " imparted to you",
					0,
					true, ComplicationRepImpact.LOW, null);
			triggerComplicationEnd(true);
		}
		
		return true;
	}

	protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		if (getPerson().getId().equals(SotfPeople.INADVERTENT)) {
			SotfHFHInadMissionCompletedFactor factor = new SotfHFHInadMissionCompletedFactor(SotfHopeForHallowhallEventIntel.POINTS_PER_INAD_MISSION);
			SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);
		}

		// spawn a non-interactable probe that lingers in orbit for a half cycle
		final SectorEntityToken probe = spawnEntity(Entities.GENERIC_PROBE, new LocData(EntityLocationType.HIDDEN_NOT_NEAR_STAR, null, system, false));
		probe.setLocation(target.getLocation().x, target.getLocation().y);
		probe.setOrbit(target.getOrbit().makeCopy());
		probe.addTag(Tags.NON_CLICKABLE);
		probe.setDiscoverable(false);
		probe.setName(probeType);
		if (isABomb) {
			probe.setName("Makeshift AM-Explosive Proximity Mine");
			probe.setCustomDescriptionId("sotf_pipebomb");
		}
		probe.forceOutIndicator();
		probe.addScript(
				new EveryFrameScript() {
					private float timer = 0;

					@Override
					public boolean isDone() {
						return probe.isExpired();
					}

					@Override
					public boolean runWhilePaused() {
						return false;
					}

					@Override
					public void advance(float amount) {
						timer += Global.getSector().getClock().convertToDays(amount);
						if (timer > 180) {
							Misc.fadeAndExpire(probe);
						}
					}
				}
		);
		// god this is silly
		if (isABomb) {
			FleetCreatorMission f = new FleetCreatorMission(Misc.random);
			f.beginFleet();
			f.createStandardFleet(3, Factions.SCAVENGERS, system.getLocation());
			f.triggerSetFleetType(FleetTypes.SCAVENGER_MEDIUM);
			f.triggerSetFleetFaction(Factions.INDEPENDENT);

			f.triggerMakeLowRepImpact();
			f.triggerFleetSetAvoidPlayerSlowly();
			f.triggerMakeFleetIgnoredByOtherFleets();

			f.triggerPickLocationAtClosestToEntityJumpPoint(system, probe);

			f.triggerSpawnFleetAtPickedLocation("$sotfdkdpm_scav", null);

			final CampaignFleetAPI scav = f.createFleet();
			scav.addAssignment(FleetAssignment.GO_TO_LOCATION, probe, 9999f, "exploring");
			scav.addAssignment(FleetAssignment.ORBIT_PASSIVE,
					probe,
					2f,
					"scavenging probe",
					new Script() {
						@Override
						public void run() {
							probe.addScript(new SotfDKDPMExplosion(probe, scav));
						}
					}
			);
		}
		Misc.fadeAndExpire(target);
	}


	protected void updateInteractionDataImpl() {
		set("$sotfdkdpm_heOrShe", getPerson().getHeOrShe());
		set("$sotfdkdpm_reward", Misc.getWithDGS(getCreditsReward()));
		set("$sotfdkdpm_days", "" + (int) MISSION_DAYS);
		
		set("$sotfdkdpm_aOrAnProbe", "a " + probeType.toLowerCase());
		set("$sotfdkdpm_probe", probeType.toLowerCase());

		if (!isABomb) {
			set("$sotfdkdpm_purpose", "provide useful data for Dustkeeper operations");
		} else {
			set("$sotfdkdpm_purpose", "obliterate an irritating scavenger fleet that keeps stealing my probes");
		}

		set("$sotfdkdpm_isABomb", isABomb);
		
		set("$sotfdkdpm_personName", getPerson().getNameString());
		set("$sotfdkdpm_systemName", system.getNameWithLowercaseTypeShort());
		set("$sotfdkdpm_dist", getDistanceLY(target));
	}
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.DROP_OFF) {
			info.addPara("Deliver " + probeType + " to the designated location at the " +
					"specified coordinates in the " + system.getNameWithLowercaseTypeShort() + ".", opad);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.DROP_OFF) {
			info.addPara("Deliver " + getWithoutArticle(probeType) + " to specified location in the " +
					system.getNameWithLowercaseTypeShort(), tc, pad);
			return true;
		}
		return false;
	}	
	
	@Override
	public String getBaseName() {
		return "Probe Deployment";
	}
	
}






