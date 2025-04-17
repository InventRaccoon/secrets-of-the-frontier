package data.scripts.campaign.missions.hallowhall;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.*;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseIntel;
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathCellsIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TriTachyonCommerceRaiding;
import com.fs.starfarer.api.impl.campaign.intel.group.FGRaidAction;
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel;
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.*;

public class SotfHopeForHallowhallEventIntel extends BaseEventIntel implements FleetEventListener,
																			  CharacterStatsRefreshListener,
																			  CurrentLocationChangedListener,
		FleetGroupIntel.FGIEventListener,
		ColonyInteractionListener,
		PlayerColonizationListener,
		ColonySizeChangeListener {

	public static Color BAR_COLOR = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getColor();

	public static int PROGRESS_MAX = 600;
	public static int PROGRESS_1 = 125;
	public static int REVEAL_2 = 250;
	public static int PROGRESS_2 = 350;
	public static int PROGRESS_3 = 450;

	public static int FP_PER_POINT = 5;
	public static float POINTS_FOR_PATHER_BASE_PER_FP = 0.6f;
	public static int POINTS_FOR_NANOFORGE = 120;
	public static int POINTS_PER_INAD_MISSION = 30;
	public static int POINTS_FOR_HALLOWHALL_COLONY = 75;
	public static int POINTS_PER_HALLOWHALL_SIZE = 75;
	public static int POINTS_FOR_ASTROPOLIS = 50;

	public static int POINTS_FOR_RAID_DEFEATED = 75;

	public static int MAX_POINTS_FROM_CORES = 240;

	public static int GAMMA_POINTS = 10; // killing a Derelict bounty gives like 10 of them lmao
	public static int BETA_POINTS = 30;
	public static int ALPHA_POINTS = 60;

	public static int TAHLAN_DAEMON_POINTS = 50;
	public static int TAHLAN_ARCHDAEMON_POINTS = 75;

	public static int RAT_CHRONOS_POINTS = 50;
	public static int RAT_COSMOS_POINTS = 50;
	public static int RAT_SERAPH_POINTS = 75;
	public static int RAT_PRIMORDIAL_POINTS = 600;
	public static int RAT_NEURO_POINTS = 175;
	public static int RAT_EXO_POINTS = 175;

	public static int AL_GRAVEN_POINTS = 175;

	public int pointsFromCores = 0;
	public int highestHallowhallSize = 0;

	public boolean defeatedRaiders = false;

	public boolean gotCorrupted = false;
	public boolean gotPristine = false;

	public static String KEY = "$sotf_HFH_ref";

	public static enum Stage {
		START,
		PROXY_CODE_INJECTOR,
		RAIDERS,
		PROXY_PATROLS,
		RECRUIT_SERAPH,
	}

	// factions whose destroyed ships build Pather points
	public static final Set<String> PATHER_FACTIONS = new HashSet<>();
	static {
		PATHER_FACTIONS.add(Factions.LUDDIC_PATH);
		PATHER_FACTIONS.add("knights_of_eva");
	}

	// factions whose destroyed ships build Diktat points
	public static final Set<String> DIKTAT_FACTIONS = new HashSet<>();
	static {
		DIKTAT_FACTIONS.add(Factions.DIKTAT);
		DIKTAT_FACTIONS.add(Factions.LIONS_GUARD);
	}

	public static void addFactorCreateIfNecessary(EventFactor factor, InteractionDialogAPI dialog) {
		if (get() == null) {
			//TextPanelAPI text = dialog == null ? null : dialog.getTextPanel();
			//new HyperspaceTopographyEventIntel(text);
			// adding a factor anyway, so it'll show a message - don't need to double up
			new SotfHopeForHallowhallEventIntel(null, false);
		}
		if (get() != null) {
			get().addFactor(factor, dialog);
		}
	}

	public static void addFactorIfAvailable(EventFactor factor, InteractionDialogAPI dialog) {
		if (get() != null) {
			get().addFactor(factor, dialog);
		}
	}

	public static SotfHopeForHallowhallEventIntel get() {
		return (SotfHopeForHallowhallEventIntel) Global.getSector().getMemoryWithoutUpdate().get(KEY);
	}

//	public static float CHECK_DAYS = 0.1f;
//	protected IntervalUtil interval = new IntervalUtil(CHECK_DAYS * 0.8f, CHECK_DAYS * 1.2f);
//	protected float burnBasedPoints = 0f;

	public SotfHopeForHallowhallEventIntel(TextPanelAPI text, boolean withIntelNotification) {
		super();
		
		Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
		
		
		setup();
		
		// now that the event is fully constructed, add it and send notification
		Global.getSector().getIntelManager().addIntel(this, !withIntelNotification, text);
	}
	
	protected void setup() {
		factors.clear();
		stages.clear();
		
		setMaxProgress(PROGRESS_MAX);
		
		addStage(Stage.START, 0);
		addStage(Stage.PROXY_CODE_INJECTOR, PROGRESS_1, StageIconSize.MEDIUM);
		addStage(Stage.RAIDERS, PROGRESS_2, StageIconSize.MEDIUM);
		addStage(Stage.PROXY_PATROLS, PROGRESS_3, StageIconSize.MEDIUM);
		addStage(Stage.RECRUIT_SERAPH, PROGRESS_MAX, true, StageIconSize.LARGE);
		
		getDataFor(Stage.PROXY_CODE_INJECTOR).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.RAIDERS).isRepeatable = false;
		getDataFor(Stage.PROXY_PATROLS).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.RECRUIT_SERAPH).keepIconBrightWhenLaterStageReached = true;

		addFactor(new SotfHFHShipsDestroyedFactorHint());
		addFactor(new SotfHFHSellNanoforgeFactorHint());

		MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
		if (holdout == null) return;
		Industry hi = holdout.getIndustry(Industries.HEAVYINDUSTRY);
		if (hi == null) hi = holdout.getIndustry(Industries.ORBITALWORKS);
		if (hi == null) return;
		if (hi.getSpecialItem() != null) {
			if (hi.getSpecialItem().getId().equals(Items.CORRUPTED_NANOFORGE)) {
				gotCorrupted = true;
			}
			if (hi.getSpecialItem().getId().equals(Items.PRISTINE_NANOFORGE)) {
				gotPristine = true;
			}
		}
	}

	@Override
	protected void notifyEnding() {
		super.notifyEnding();
	}

	@Override
	protected void notifyEnded() {
		super.notifyEnded();
		Global.getSector().getMemoryWithoutUpdate().unset(KEY);
		Global.getSector().getListenerManager().removeListener(this);
	}
	
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, 
			   						Color tc, float initPad) {
		
		if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
			return;
		}
		
		Color h = Misc.getHighlightColor();
		if (isUpdate && getListInfoParam() instanceof EventStageData) {
			EventStageData esd = (EventStageData) getListInfoParam();
			if (esd.id == Stage.PROXY_CODE_INJECTOR) {
				info.addPara("Hyperwave Transmitters attract Dustkeeper-upgraded drones", initPad, tc, h, "Hyperwave Transmitters");
			}
			if (esd.id == Stage.PROXY_PATROLS) {
				info.addPara("Larger colonies spawn a Dustkeeper Proxy patrol", initPad, tc, h, "Dustkeeper Proxy");
			}
			if (esd.id == Stage.RECRUIT_SERAPH) {
				info.addPara("Ardent-Annex-Seraph has offered to join your fleet", tc, initPad);
			}
			return;
		}
	}
	
	public float getImageSizeForStageDesc(Object stageId) {
//		if (stageId == Stage.REVERSE_POLARITY || stageId == Stage.GENERATE_SLIPSURGE) {
//			return 48f;
//		}
		if (stageId == Stage.START) {
			return 64f;
		}
		return 48f;
	}
	public float getImageIndentForStageDesc(Object stageId) {
//		if (stageId == Stage.REVERSE_POLARITY || stageId == Stage.GENERATE_SLIPSURGE) {
//			return 16f;
//		}
		if (stageId == Stage.START) {
			return 0f;
		}
		return 16f;
	}

	@Override
	public void addStageDescriptionText(TooltipMakerAPI info, float width, Object stageId) {
		float opad = 10f;
		float small = 0f;
		Color h = Misc.getHighlightColor();
		
		//setProgress(0);
		//setProgress(199);
		//setProgress(600);
		//setProgress(899);
		//setProgress(1000);
		//setProgress(499);
		//setProgress(600);
		
		EventStageData stage = getDataFor(stageId);
		if (stage == null) return;
		
//		if (isStageActiveAndLast(stageId) &&  stageId == Stage.START) {
//			addStageDesc(info, stageId, small, false);
//		} else if (isStageActive(stageId) && stageId != Stage.START) {
//			addStageDesc(info, stageId, small, false);
//		}
		
		if (isStageActive(stageId)) {
			addStageDesc(info, stageId, small, false);
		}
	}
	
	
	public void addStageDesc(TooltipMakerAPI info, Object stageId, float initPad, boolean forTooltip) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		StarSystemAPI mia = Global.getSector().getStarSystem("sotf_mia");
		if (stageId == Stage.START) {
			info.addPara("Earn the trust of the Dustkeepers operating in the Mia's Star system. Progress is gained " +
							"by advancing colonial prospects in the system, gathering telemetry from destroying their sworn enemies, " +
							"and by assisting Rigging-Echo-Inadvertent at the forgeship by completing " +
							"missions or selling useful colony items (such as nanoforges or AI cores)",
					initPad);
		} else if (stageId == Stage.PROXY_CODE_INJECTOR) {
			info.addPara("Derelict droneships spawned by the Hyperwave Transmitter combat objective are replaced " +
							"by Dustkeeper Proxy drones, which have notably higher combat effectiveness.", initPad,
					h,
					"Hyperwave Transmitter",
					"Dustkeeper Proxy"
					);
		} else if (stageId == Stage.RAIDERS) {
			if (progress > PROGRESS_2 && defeatedRaiders) {
				LabelAPI label = info.addPara("The Luddic Path's purifier force was defeated.", initPad);
			} else if (progress > PROGRESS_2) {
				LabelAPI label = info.addPara("The Luddic Path has prepared a force of purifiers to attack Mia's Star and perform a saturation bombardment " +
						"of Holdout Forgeship and any colonies in the system. The attack is unlikely to permanently end Dustkeeper ambitions in " +
						"the system, but will certainly set them back if successful.", initPad);
				label.setHighlight("Luddic Path", "saturation bombardment");
				label.setHighlightColors(Global.getSector().getFaction(Factions.LUDDIC_PATH).getBaseUIColor(), bad);
				if (Misc.getSystemsWithPlayerColonies(false).contains(mia)) {
					info.addPara("Your colonies, however, could be eradicated entirely. Aiding the defense is paramount.", initPad);
				}
			} else if (progress > REVEAL_2) {
				LabelAPI label = info.addPara("Dustkeeper probes have picked up signs that the Luddic Path may be building up a force of purifiers to " +
						"rid Mia's Star of its AI presence - and any colonies within. The attack is unlikely to permanently end Dustkeeper ambitions in " +
						"the system, but will certainly set them back if successful.", initPad);
				if (Misc.getSystemsWithPlayerColonies(false).contains(mia)) {
					info.addPara("Your colonies, however, could be eradicated entirely. Aiding the defense is paramount.", initPad);
				}
			} else {
				info.addPara("Major AI activity is bound to attract attention at some point. The Dustkeepers have no shortage of " +
						"enemies, and it could be an opportunity to prove your worth to them.", initPad);
			}
		} else if (stageId == Stage.PROXY_PATROLS) {
			info.addPara("Your larger colonies (size 4 or greater) each spawn a Dustkeeper Proxy patrol, which fights as part of your faction. " +
							"Size 6 colonies spawn an additional patrol. This benefit is lost if you become hostile to the Dustkeepers.",
					initPad, h,
					"size 4",
					"Dustkeeper Proxy patrol");
		} else if (stageId == Stage.RECRUIT_SERAPH) {
			info.addPara("Ardent-Annex-Seraph has deemed Hallowhall to be in a stable enough state - and sufficiently trusts you - that " +
							"she is willing to join your fleet as a warmind, alongside her flagship.", initPad, h,
					"Ardent-Annex-Seraph");
		}
	}
	
	public TooltipCreator getStageTooltipImpl(Object stageId) {
		final EventStageData esd = getDataFor(stageId);
		
		if (esd != null && EnumSet.of(Stage.PROXY_CODE_INJECTOR, Stage.RAIDERS,
				Stage.PROXY_PATROLS, Stage.RECRUIT_SERAPH).contains(esd.id)) {
			return new BaseFactorTooltip() {
				@Override
				public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
					float opad = 10f;
					
					if (esd.id == Stage.PROXY_CODE_INJECTOR) {
						tooltip.addTitle("Proxy Broadcast Code Injector");
					} else if (esd.id == Stage.PROXY_PATROLS) {
						tooltip.addTitle("Proxy Drone Patrols");
					} else if (esd.id == Stage.RAIDERS && progress <= REVEAL_2) {
						tooltip.addTitle("Hostile Attention");
					} else if (esd.id == Stage.RECRUIT_SERAPH) {
						tooltip.addTitle("Ardent Oathkeeper");
					}

					addStageDesc(tooltip, esd.id, opad, true);
					
					esd.addProgressReq(tooltip, opad);
				}
			};
		}
		
		return null;
	}



	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("events", "sotf_hallowhall");
	}

	protected String getStageIconImpl(Object stageId) {
		EventStageData esd = getDataFor(stageId);
		if (esd == null) return null;

		if (stageId == Stage.START) {
			return Global.getSettings().getSpriteName("events", "sotf_hallowhall");
		}
		if (stageId == Stage.PROXY_CODE_INJECTOR) {
			return Global.getSettings().getSpriteName("events", "sotf_hallowhall_injector");
		}
		if (stageId == Stage.RAIDERS && progress >= REVEAL_2) {
			return Global.getSettings().getSpriteName("events", "sotf_hallowhall_raiders");
		}
		if (stageId == Stage.PROXY_PATROLS) {
			return Global.getSettings().getSpriteName("events", "sotf_hallowhall_patrols");
		}
		if (stageId == Stage.RECRUIT_SERAPH) {
			return Global.getSettings().getSpriteName("sotf_characters", "seraph");
		}
		return Global.getSettings().getSpriteName("events", "stage_unknown_bad");
	}
	
	
	@Override
	public Color getBarColor() {
		Color color = BAR_COLOR;
		//color = Misc.getBasePlayerColor();
		color = Misc.interpolateColor(color, Color.black, 0.25f);
		return color;
	}
	
	@Override
	public Color getBarProgressIndicatorColor() {
		return super.getBarProgressIndicatorColor();
	}

	@Override
	protected int getStageImportance(Object stageId) {
		return super.getStageImportance(stageId);
	}


	@Override
	protected String getName() {
		return "Hope for Hallowhall";
	}
	

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		
	}
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isEnded() || isEnding()) return;

		if (!battle.isPlayerInvolved()) return;

		// Check for LP stations with an associated base intel
		if (Global.getSector().getCurrentLocation() instanceof StarSystemAPI &&
				battle.getPlayerSide().contains(primaryWinner)) {
			StarSystemAPI system = (StarSystemAPI) Global.getSector().getCurrentLocation();
			for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
				if (otherFleet.isStationMode()) {
					{
						LuddicPathBaseIntel intel = LuddicPathBaseIntel.getIntelFor(system);
						if (intel != null && Misc.getStationFleet(intel.getMarket()) == otherFleet) {
							for (FleetMemberAPI member : Misc.getSnapshotMembersLost(otherFleet)) {
								if (member.isStation()) {
									int points = Math.round(member.getFleetPointCost() * POINTS_FOR_PATHER_BASE_PER_FP);
									SotfHFHPatherBaseDestroyedFactor factor = new SotfHFHPatherBaseDestroyedFactor(points);
									addFactor(factor);
								}
							}
						}
					}
				}
			}
		}

		// tally up Pather and Diktat kills
		int patherPoints = 0;
		int diktatPoints = 0;
		for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
			if (!PATHER_FACTIONS.contains(otherFleet.getFaction().getId()) && !DIKTAT_FACTIONS.contains(otherFleet.getFaction().getId())) {
				continue;
			}
			for (FleetMemberAPI member : Misc.getSnapshotMembersLost(otherFleet)) {
				if (member.isStation() && PATHER_FACTIONS.contains(otherFleet.getFaction().getId())) continue;
				if (PATHER_FACTIONS.contains(otherFleet.getFaction().getId())) {
					patherPoints += member.getFleetPointCost();
				} else if (DIKTAT_FACTIONS.contains(otherFleet.getFaction().getId())) {
					diktatPoints += member.getFleetPointCost();
					// I don't think there's really any good way to detect if a ship is Lion's Guard or not
					// (since they convert to Diktat fleets), so just check for LGS prefix
					if (member.getShipName().contains(Global.getSector().getFaction(Factions.LIONS_GUARD).getShipNamePrefix())) {
						diktatPoints += member.getFleetPointCost();
					}
				}
			}
		}

		patherPoints = computeProgressPoints(patherPoints);
		diktatPoints = computeProgressPoints(diktatPoints);
		if (patherPoints > 0) {
			SotfHFHPathersDestroyedFactor factor = new SotfHFHPathersDestroyedFactor(patherPoints);
			addFactor(factor);
		}
		if (diktatPoints > 0) {
			SotfHFHDiktatDestroyedFactor factor = new SotfHFHDiktatDestroyedFactor(diktatPoints);
			addFactor(factor);
		}
	}

	@Override
	public void reportPlayerOpenedMarket(MarketAPI market) {

	}

	@Override
	public void reportPlayerClosedMarket(MarketAPI market) {

	}

	@Override
	public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {

	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		if (!transaction.getSubmarket().getSpecId().equals(SotfIDs.FORGESHIP_MARKET)) return;
		Industry hi = transaction.getMarket().getIndustry(Industries.HEAVYINDUSTRY);
		if (hi == null) hi = transaction.getMarket().getIndustry(Industries.ORBITALWORKS);
		if (hi == null) return;
		if (transaction.getMarket().getId().equals("sotf_holdout_market")) {
			for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
				if (stack.isSpecialStack()) {
					if ((!gotPristine && stack.getSpecialDataIfSpecial().getId().equals(Items.PRISTINE_NANOFORGE))
					|| (!gotCorrupted && stack.getSpecialDataIfSpecial().getId().equals(Items.CORRUPTED_NANOFORGE))) {
						float points = POINTS_FOR_NANOFORGE;
						if (stack.getSpecialDataIfSpecial().getId().equals(Items.CORRUPTED_NANOFORGE) && !gotCorrupted && !gotPristine) {
							points = points * 0.5f;
							gotCorrupted = true;
						}
						if (stack.getSpecialDataIfSpecial().getId().equals(Items.PRISTINE_NANOFORGE) && !gotPristine) {
							if (gotCorrupted) {
								points = points * 0.5f;
							}
							gotCorrupted = true;
							gotPristine = true;
						}
						SotfHFHSoldNanoforgeFactor factor = new SotfHFHSoldNanoforgeFactor(stack.getSpecialDataIfSpecial().getId(), Math.round(points));
						addFactor(factor);
					}
				}
			}
		}
	}

	@Override
	public void reportPlayerColonizedPlanet(PlanetAPI planet) {
		if (planet.getId().equals("sotf_hallowhall") && highestHallowhallSize < 3) {
			SotfHFHColonizeHallowhallFactor factor = new SotfHFHColonizeHallowhallFactor(POINTS_FOR_HALLOWHALL_COLONY);
			addFactor(factor);
			highestHallowhallSize = 3;
		}
	}

	@Override
	public void reportPlayerAbandonedColony(MarketAPI colony) {

	}

	@Override
	public void reportColonySizeChanged(MarketAPI market, int prevSize) {
		if (market.getPlanetEntity() != null && market.getPlanetEntity().getId().equals("sotf_hallowhall") &&
				market.getSize() > highestHallowhallSize && market.getSize() > prevSize && market.getSize() > 3) {
			SotfHFHLargerHallowhallFactor factor = new SotfHFHLargerHallowhallFactor(POINTS_PER_HALLOWHALL_SIZE);
			addFactor(factor);
			highestHallowhallSize = prevSize + 1;
		}
	}

	public static int computeProgressPoints(float fleetPointsDestroyed) {
		if (fleetPointsDestroyed <= 0) return 0;

		int points = Math.round(fleetPointsDestroyed / FP_PER_POINT);
		if (points < 1) points = 1;
		return points;
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_MISSIONS);
		tags.add(Tags.INTEL_ACCEPTED);
		tags.add(SotfIDs.DUSTKEEPERS);
		return tags;
	}

	@Override
	protected void advanceImpl(float amount) {
		super.advanceImpl(amount);
		applyFleetEffects();
		
		float days = Global.getSector().getClock().convertToDays(amount);
		
		//setProgress(getProgress() + 10);
		if (!Global.getSector().getAllListeners().contains(this)) {
			Global.getSector().getListenerManager().addListener(this);
		}
	}


	
	@Override
	protected void notifyStageReached(EventStageData stage) {
		String id = "sotf_harrowhall_event";

		StatBonus stat = Global.getSector().getPlayerStats().getDynamic().getMod(SotfIDs.STAT_PROXY_REINFORCEMENTS);
		stat.unmodify(id);

		StatBonus stat2 = Global.getSector().getPlayerStats().getDynamic().getMod(SotfIDs.STAT_PROXY_PATROLS);
		stat2.unmodify(id);

		if (isStageActive(Stage.PROXY_CODE_INJECTOR)) {
			stat.modifyFlat(id, 1f);
		}

		if (stage.id == Stage.RAIDERS) {
			MarketAPI holdout = Global.getSector().getEconomy().getMarket("sotf_holdout_market");
			if (holdout == null) return;
			MarketAPI sourceMarket = null;
			LuddicPathBaseIntel base = LuddicPathCellsIntel.getClosestBase(Global.getSector().getEconomy().getMarket("sotf_holdout_market"));
			if (base != null) {
				sourceMarket = base.getMarket();
			}
			if (sourceMarket == null) {
				sourceMarket = SotfMisc.pickNPCMarket(Factions.LUDDIC_PATH);
			}
			if (sourceMarket == null) return;
			startAttack(sourceMarket, holdout, holdout.getStarSystem());
		}

		if (isStageActive(Stage.PROXY_PATROLS)) {
			stat2.modifyFlat(id, 1f);
		}

		if (stage.id == Stage.RECRUIT_SERAPH) {
			Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COMPLETED_HALLOWHALL, true);
			endAfterDelay();
		}
	}

	public void startAttack(MarketAPI source, MarketAPI target, StarSystemAPI system) {
		GenericRaidFGI.GenericRaidParams params = new GenericRaidFGI.GenericRaidParams(new Random(random.nextLong()), true);

		params.factionId = Factions.LUDDIC_PATH;
		params.source = source;

		params.prepDays = 14f + random.nextFloat() * 14f;
		params.payloadDays = 27f + 7f * random.nextFloat();

		params.raidParams.where = system;
		params.raidParams.type = FGRaidAction.FGRaidType.SEQUENTIAL;
		params.raidParams.tryToCaptureObjectives = false;
		params.raidParams.setBombardment(MarketCMD.BombardType.SATURATION);
		params.noun = "purification";
		params.forcesNoun = "purifiers";

		params.raidParams.allowedTargets.addAll(Misc.getMarketsInLocation(system));

		params.style = FleetCreatorMission.FleetStyle.STANDARD;

		float w = 40;
		w += Math.max(0f, (target.getSize() - 2)) * 10f;
		if (w < 0f) w = 0f;
		if (w > 50f) w = 50f;

		float f = w / 50f;
		float totalDifficulty = (0.25f + f * 0.75f) * 10f;

		if (random.nextFloat() < 0.33f) {
			params.style = FleetCreatorMission.FleetStyle.QUANTITY;
		}

		while (totalDifficulty > 0) {
//			float max = Math.min(10f, totalDifficulty * 0.5f);
//			float min = Math.max(2, max - 2);
//			if (max < min) max = min;
//
//			int diff = Math.round(StarSystemGenerator.getNormalRandom(r, min, max));
			int diff = (int) Math.min(6f, totalDifficulty);
			if (diff < 2) diff = 2;

			params.fleetSizes.add(diff);
			totalDifficulty -= diff;
		}


		LuddicPathBaseIntel base = LuddicPathBaseIntel.getIntelFor(source);
		if (base != null) {
			if (Misc.isHiddenBase(source) && !base.isPlayerVisible()) {
				base.makeKnown();
				base.sendUpdateIfPlayerHasIntel(LuddicPathBaseIntel.DISCOVERED_PARAM, false);
			}
		}
		GenericRaidFGI raid = new GenericRaidFGI(params);
		raid.setListener(this);
		Global.getSector().getIntelManager().addIntel(raid);
	}

	@Override
	public void reportFGIAborted(FleetGroupIntel intel) {
		SotfHFHDefeatedRaidFactor factor = new SotfHFHDefeatedRaidFactor(POINTS_FOR_RAID_DEFEATED);
		addFactor(factor);

		defeatedRaiders = true;
		Global.getSector().getPlayerMemoryWithoutUpdate().set(SotfIDs.MEM_DEFEATED_HFH_RAIDERS, true);
	}
	
	public void reportCurrentLocationChanged(LocationAPI prev, LocationAPI curr) {
		//applyFleetEffects();
	}
	
	public void reportAboutToRefreshCharacterStatEffects() {
		
	}

	public void reportRefreshedCharacterStatEffects() {
		applyFleetEffects();
	}
	
	public void applyFleetEffects() {
		String id = "sotf_harrowhall_event";
	}

	public boolean withMonthlyFactors() {
		return false;
	}
	
	protected String getSoundForStageReachedUpdate(Object stageId) {
		return super.getSoundForStageReachedUpdate(stageId);
	}

	@Override
	protected String getSoundForOneTimeFactorUpdate(EventFactor factor) {
		return null;
	}
	
	
	
}








