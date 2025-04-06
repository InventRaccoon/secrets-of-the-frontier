package data.scripts.campaign.abilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.DelayedActionScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.dustkeepers.SotfDustkeeperFleetCreator;
import data.scripts.campaign.plugins.wendigo.SotfWendigoEncounterManager;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.ids.FleetTypes.PATROL_MEDIUM;
import static com.fs.starfarer.api.impl.campaign.ids.FleetTypes.PATROL_SMALL;

/**
 *	COURSER PROTOCOL: Summons a Dustkeeper hunter-killer flotilla headed by Courser to escort the player
 */

public class SotfCourserProtocolAbility extends BaseDurationAbility {

	public static class AbilityUseData {
		public long timestamp;
		public Vector2f location;
		public AbilityUseData(long timestamp, Vector2f location) {
			this.timestamp = timestamp;
			this.location = location;
		}

	}

	protected boolean performed = false;

	protected TimeoutTracker<AbilityUseData> uses = new TimeoutTracker<AbilityUseData>();

	protected Object readResolve() {
		super.readResolve();
		if (uses == null) {
			uses = new TimeoutTracker<AbilityUseData>();
		}
		return this;
	}

	@Override
	protected void activateImpl() {
		if (entity.isInCurrentLocation()) {
			VisibilityLevel level = entity.getVisibilityLevelToPlayerFleet();
			if (level != VisibilityLevel.NONE) {
				Global.getSector().addPing(entity, SotfIDs.PING_COURSERPROTOCOL);
			}

			performed = false;
		}

	}

	protected String getActivationText() {
		//return Misc.ucFirst(spec.getName().toLowerCase());
		return "Broadcasting...";
	}

	@Override
	protected void applyEffect(float amount, float level) {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;

		if (!performed) {
				float delay = 1f + 1f * (float) Math.random();
				addResponseScript(delay);
			performed = true;
		}
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		float days = Global.getSector().getClock().convertToDays(amount);
		uses.advance(days);
	}

	protected void addResponseScript(float delayDays) {
		final CampaignFleetAPI player = getFleet();
		if (player == null) return;
		if (!(player.getContainingLocation() instanceof StarSystemAPI)) return;

		final StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();

		final JumpPointAPI inner = Misc.getDistressJumpPoint(system);
		if (inner == null) return;

		JumpPointAPI outerTemp = null;
		if (inner.getDestinations().size() >= 1) {
			SectorEntityToken test = inner.getDestinations().get(0).getDestination();
			if (test instanceof JumpPointAPI) {
				outerTemp = (JumpPointAPI) test;
			}
		}
		final JumpPointAPI outer = outerTemp;
		if (outer == null) return;

		addHelpScript(delayDays, system, inner, outer);

	}

    // creates the fleet
	protected void addHelpScript(float delayDays,
								 final StarSystemAPI system,
								 final JumpPointAPI inner,
								 final JumpPointAPI outer) {
		Global.getSector().addScript(new DelayedActionScript(delayDays) {
			@Override
			public void doAction() {
				CampaignFleetAPI player = Global.getSector().getPlayerFleet();
				if (player == null) return;

				boolean hostile = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).isAtBest(Factions.PLAYER, RepLevel.HOSTILE);

				if (hostile) {
					// Courser doesn't bother, and Wendigo decides to go and kill you
					if (Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_COURSER_SENT_HUNTERKILLERS) == null) {
						SotfWendigoEncounterManager.sendWendigoCourserHunt();
						Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COURSER_SENT_HUNTERKILLERS, true);
						if (Global.getSettings().isDevMode()) {
							Global.getSector().getCampaignUI().addMessage("DEV: Sending WCH");
						}
						//if (Global.getSector().hasScript(SotfWendigoEncounterManager.class)) {
						//	for (EveryFrameScript script : Global.getSector().getScripts()) {
						//		if (script instanceof SotfWendigoEncounterManager) {
						//			SotfWendigoEncounterManager wem = (SotfWendigoEncounterManager) script;
						//			wem.tracker.setElapsed(0f);
						//			return;
						//		}
						//	}
						//}
					}
					return;
				}

				// get the player's fleet points - this is later used so that Courser's fleet spawns with fleet points
				// that are the same as the player
				float points = player.getFleetPoints();

				// TODO: spoilery thing.
				//MarketAPI sanctum = Global.getSector().getEconomy().getMarket(SotfIDs.SANCTUM_MARKET);
				//if (sanctum == null) {
				//	points = Math.min(points, 75f);
				//}

				int summons = 0;
				if (Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_COURSER_SUMMONS) != null) {
					summons = (int) Global.getSector().getMemoryWithoutUpdate().get(SotfIDs.MEM_COURSER_SUMMONS);
				}
				summons++;
				Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_COURSER_SUMMONS, summons);

				String fleetType = PATROL_MEDIUM;
				if (points < 30) {
					fleetType = PATROL_SMALL;
				}

				// creates fleet
				FleetParamsV3 params = new FleetParamsV3(
						// TODO: spoilery thing.
						null,
						null,
						SotfIDs.DUSTKEEPERS,
						null,
						fleetType,
						points, // combatPts
						0f, // freighterPts
						0f, // tankerPts
						0f, // transportPts
						0f, // linerPts
						0f, // utilityPts
						0.25f // qualityMod
				);
				FactionDoctrineAPI doctrine = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).getDoctrine().clone();
				//doctrine.setShipSize(4);
				doctrine.setCombatFreighterCombatUseFraction(0.3f); // always 30% of FP as Proxies
				params.doctrineOverride = doctrine;
				params.commander = SotfPeople.getPerson(SotfPeople.COURSER);
				CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
				fleet.getFleetData().ensureHasFlagship();
				FleetMemberAPI flagship = fleet.getFlagship();

				int startingRecoveries = 11; // ODS Dusklight count on campaign start
				// otherwise will crash if summoned more than ~3988 times per playthrough (look, I'm just covering my bases)
				if (summons > (3999 - startingRecoveries)) {
					summons = 3999 - startingRecoveries;
				}
				// The ODS Dusklight takes many forms, but Courser keeps their tally
				flagship.setShipName("ODS Dusklight " + Global.getSettings().getRoman(summons + startingRecoveries));
				flagship.setCaptain(SotfPeople.getPerson(SotfPeople.COURSER));

				// if not already built in, smod in ECM and ECCM on the Dusklight
				fleet.inflateIfNeeded();
				flagship.setVariant(flagship.getVariant().clone(), false, false);
				flagship.getVariant().setSource(VariantSource.REFIT);

				if (flagship.getVariant().getNonBuiltInHullmods().contains(HullMods.ECM)) {
					flagship.getVariant().removeMod(HullMods.ECM);
					flagship.getVariant().addPermaMod(HullMods.ECM, true);
				} else if (!flagship.getVariant().hasHullMod(HullMods.ECM)) {
					flagship.getVariant().addPermaMod(HullMods.ECM, true);
				}

				if (flagship.getVariant().getNonBuiltInHullmods().contains(HullMods.ECCM)) {
					flagship.getVariant().removeMod(HullMods.ECCM);
					flagship.getVariant().addPermaMod(HullMods.ECCM, true);
				} else if (!flagship.getVariant().hasHullMod(HullMods.ECCM)) {
					flagship.getVariant().addPermaMod(HullMods.ECCM, true);
				}
				fleet.inflateIfNeeded();

				//fleet.setNoFactionInName(true);
				//fleet.setName("Courser's Shard");


				SotfDustkeeperFleetCreator.assignFleetName(fleet, fleetType,
						SotfDustkeeperFleetCreator.DEFAULT_AUX_PERCENT +
								(SotfDustkeeperFleetCreator.RANDOM_AUX_PERCENT * Misc.random.nextFloat()), true);

				// stop Courser bugging about transponders
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
				fleet.getMemoryWithoutUpdate().set(SotfIDs.MEM_COURSER_FLEET, true);
				fleet.setTransponderOn(true);
				fleet.addAbility(Abilities.TRANSVERSE_JUMP);

				// updates fleet visuals so you can see the transverse jump visuals
				fleet.updateFleetView();

				// spawns the fleet
				Global.getSector().getHyperspace().addEntity(fleet);

					float dir = (float) Math.random() * 360f;
					if (player.isInHyperspace()) {
						dir = Misc.getAngleInDegrees(player.getLocation(), system.getLocation());
						dir += (float) Math.random() * 120f - 60f;
					}
					Vector2f loc = Misc.getUnitVectorAtDegreeAngle(dir);
					loc.scale(500f * (float) Math.random());
					Vector2f.add(system.getLocation(), loc, loc);
					fleet.setLocation(loc.x, loc.y + fleet.getRadius() + 100f);

				// Fracture Jump augment - available by default nowadays, commented-out code to have it an unlock
				//if (Global.getSector().getMemoryWithoutUpdate().contains("$FSFractureAugmentAcquired")) {
				loc = Misc.getPointAtRadius(player.getLocation(), 400f + fleet.getRadius());
				SectorEntityToken token = player.getContainingLocation().createToken(loc.x, loc.y);
				JumpDestination dest = new JumpDestination(token, null);
				Global.getSector().doHyperspaceTransition(fleet, null, dest);

				//}

				fleet.addScript(new SotfCourserAssignmentAI(fleet, system, inner, outer));
			}
		});
	}

	public boolean isUsable() {
		if (!super.isUsable()) return false;
		if (getFleet() == null) return false;

		CampaignFleetAPI fleet = getFleet();
		if (fleet.isInHyperspace() || fleet.isInHyperspaceTransition()) return false;

		if (fleet.getContainingLocation() != null && fleet.getContainingLocation().hasTag(Tags.SYSTEM_ABYSSAL)) {
			return false;
		}

		return true;
	}

	@Override
	protected void deactivateImpl() {
		cleanupImpl();
	}

	@Override
	protected void cleanupImpl() {
		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;
	}

	@Override
	public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {

		CampaignFleetAPI fleet = getFleet();
		if (fleet == null) return;

		Color gray = Misc.getGrayColor();
		Color highlight = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		FactionAPI dustkeepers = Global.getSector().getFaction(SotfIDs.DUSTKEEPERS);
		Color dk = dustkeepers.getBaseUIColor();

		LabelAPI title = tooltip.addTitle(spec.getName());

		float pad = 10f;

		int followDur = Math.round(SotfCourserAssignmentAI.ESCORT_DURATION);
		int joinRange = Math.round(SotfCourserFIDPluginImpl.COURSER_JOIN_RANGE);
		// be generous with our range description
		if (joinRange > 1000) {
			joinRange -= 250;
		}

		LabelAPI label = tooltip.addPara("Emits an encrypted communication pulse, calling for aid. " +
				"Outrider-Annex-Courser will arrive to the system via a transverse jump and escort you " +
				"for " + followDur + " days, joining you in combat " +
				"as long as they are within " + joinRange + " units.", pad);
		label.setHighlight("Outrider-Annex-Courser", followDur + " days", joinRange + " units");
		label.setHighlightColors(dk, highlight, highlight);

		if (expanded) {
			tooltip.addPara("Expect a capable fleet commander at the head of an automated hunter-killer " +
							"fleet %s. They are ready to take losses and will salvage more drones for the next " +
							"time you need them.", pad, highlight,
					"of similar size to your own fleet");

			//MarketAPI sanctum = Global.getSector().getEconomy().getMarket(SotfIDs.SANCTUM_MARKET);
			//if (sanctum == null && fleet.getFleetPoints() > 75f) {
			//	tooltip.addPara("Courser's fleet will be somewhat smaller than your own due to limited allocatable " +
			//			"resources.", bad, pad);
			//}

			tooltip.addPara("Time taken for broadcast, arrival, escort, and reinforcement " +
					"adds up to %s before another call can be made.", pad, highlight, "180 days");

			if (fleet.getFaction().isAtWorst(dustkeepers, RepLevel.FRIENDLY)) {
				tooltip.addPara("Because of your friendly relationship with the Contingency, Courser will join you against " +
						"almost any fleet that is not explicitly friendly to the Dustkeepers.", pad);
			} else if (fleet.getFaction().isAtWorst(dustkeepers, RepLevel.INHOSPITABLE)) {
				tooltip.addPara("Because of your neutral relationship with the Contingency, Courser will only join you " +
						"against fleets that are hostile to the Dustkeepers, which includes any factions that outlaw AI cores " +
						"or that are hostile to the Independents.", pad);
			}
		}

		// Fracture Jump augment, which is available by default nowadays
		//if (Global.getSector().getMemoryWithoutUpdate().contains("$FSFractureAugmentAcquired")) {
		//		tooltip.addPara("Augment: Fracture Jump", o, pad);
//
		//	tooltip.addPara("Omicron's fleet will warp into the system beside you.", o, pad);
		//}
		if (fleet.getFaction().isAtBest(dustkeepers, RepLevel.HOSTILE)) {
			tooltip.addPara("You are hostile to the Dustkeepers, and may wish to consider if you should emit a communication " +
					"pulse with your exact location attached.", bad, pad);
		}
		if (fleet.isInHyperspace()) {
			tooltip.addPara("Can not be used in hyperspace.", bad, pad);
		}
		if (isOnCooldown()) {
			label = tooltip.addPara("On cooldown for another " + Math.round(getCooldownLeft()) + " days", bad, pad);
			label.setHighlight("" + Math.round(getCooldownLeft()));
			label.setHighlightColors(highlight);
		}

		tooltip.addPara("*2000 units = 1 map grid cell", gray, pad);

		addIncompatibleToTooltip(tooltip, expanded);

	}

	public boolean hasTooltip() {
		return true;
	}

}





