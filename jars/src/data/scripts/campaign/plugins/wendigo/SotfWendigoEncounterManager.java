package data.scripts.campaign.plugins.wendigo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

/**
 *	Manages the appearances of one Wintry-Annex-Wendigo, Dustkeeper hunter-killer
 *  NOWHERE TO HIDE, DARLING MINE
 */

public class SotfWendigoEncounterManager implements EveryFrameScript {

	public IntervalUtil tracker = new IntervalUtil(120f, 180f); // 4-6 months between encounter attempts

	public SotfWendigoEncounterManager() {
		tracker.setElapsed(-60f);
	}
	
	public boolean isDone() {
		return false;
	}

	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		if (true) return;
		tracker.advance(Global.getSector().getClock().convertToDays(amount));
		if (tracker.intervalElapsed()) {
			pickAndSendWendigo();
		}
	}

	public void pickAndSendWendigo() {
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_COURSER_SENT_HUNTERKILLERS) &&
				!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_MET_WENDIGO_COURSERHUNT)) {
			sendWendigoCourserHunt();
			return;
		}

		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_FAILED_WENDIGO_FAVOR) &&
				!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_MET_WENDIGO_FAILUREHUNT)) {
			sendWendigoFailureHunt();
			return;
		}

		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_DUSTKEEPER_HATRED) && Misc.getRandom(Misc.genRandomSeed(), 5).nextFloat() < 0.5f) {
			sendWendigoHatredHunt();
			return;
		}

		//if (!Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_MET_WENDIGO)) {
		//	createWendigoSpoofer();
		//}
	}

	public static void sendWendigoCourserHunt() {
		SotfWendigoEncounter e = createStandardWendigoHunt("sotfWendigoCourserHunt", "$sotf_WCHuntFleet");
		e.endCreate();
	}

	public static void sendWendigoFailureHunt() {
		SotfWendigoEncounter e = createStandardWendigoHunt("sotfWendigoFailHunt", "$sotf_WFailHuntFleet");
		e.endCreate();
	}

	public static void sendWendigoHatredHunt() {
		SotfWendigoEncounter e = createStandardWendigoHunt("sotfWendigoHatredHunt", "$sotf_WHatredHuntFleet");
		e.endCreate();
	}

	/**
	 * Sends a "standard" Wendigo encounter where they intercept and attack the player, with a specified hail trigger
	 * Must call endCreate() on the resulting encounter to confirm and begin it
	 */
	public static SotfWendigoEncounter createStandardWendigoHunt(String id, String flag) {
		Random r = Misc.getRandom(Misc.genRandomSeed(), 5);
		SotfWendigoEncounter e = new SotfWendigoEncounter(r, id);
		e.setDelayNone();
		e.setDoNotAbortWhenPlayerFleetTooStrong();
		e.setLocationFringeOnly(false, SotfIDs.DUSTKEEPERS);

		e.beginCreate();
		e.triggerCreateFleet(HubMissionWithTriggers.FleetSize.MAXIMUM, HubMissionWithTriggers.FleetQuality.SMOD_2,
				SotfIDs.DUSTKEEPERS, FleetTypes.PATROL_LARGE, new Vector2f());
		e.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.ALL_SHIPS,
				HubMissionWithTriggers.OfficerQuality.UNUSUALLY_HIGH);
		// otherwise may get Wendigo in one capital and a second "fleet commander" warnet crux in another
		e.triggerGetFleetParams().commander = SotfPeople.getPerson(SotfPeople.WENDIGO);
		//e.triggerSetFleetCommander(SotfPeople.getPerson(SotfPeople.WENDIGO));
		e.triggerFleetSetFlagship("sotf_respite_Assault");

		e.triggerFleetMakeFaster(true, 0, true);
		e.triggerSetStandardAggroInterceptFlags();
		e.triggerMakeFleetIgnoreOtherFleets();

		e.triggerSetFleetGenericHailPermanent(id + "Hail");
		e.triggerSetFleetFlagPermanent(flag);
		e.triggerFleetSetNoFactionInName();
		e.triggerFleetSetName("Wendigo's Ether Wolves");
		return e;
	}

	public static void createWendigoSpoofer() {
		Random r = Misc.getRandom(Misc.genRandomSeed(), 5);
		SotfWendigoEncounter e = new SotfWendigoEncounter(r, "sotfWCHunt");
		e.setDelayNone();
		e.setDoNotAbortWhenPlayerFleetTooStrong();
		e.setEncounterFromSomewhereInSystem();
		e.setLocationFringeOnly(false, SotfIDs.DUSTKEEPERS);

		e.beginCreate();
		e.triggerCreateFleet(HubMissionWithTriggers.FleetSize.HUGE, HubMissionWithTriggers.FleetQuality.LOWER,
				Factions.LUDDIC_CHURCH, FleetTypes.PATROL_LARGE, new Vector2f());
		e.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.DEFAULT,
				HubMissionWithTriggers.OfficerQuality.DEFAULT);
		e.triggerSetFleetCommander(SotfPeople.getPerson(SotfPeople.WENDIGO_SPOOFED));
		e.triggerFleetSetFlagship("retribution_Standard");

		e.triggerFleetMakeFaster(true, 0, true);
		e.triggerOrderFleetPatrol();
		e.triggerFleetSetPatrolActionText("exploring");
		e.triggerMakeFleetIgnoreOtherFleets();

		e.triggerSetFleetGenericHailPermanent("sotfWendigoSpooferHail");
		e.triggerSetFleetFlagPermanent("$sotf_wendigoSpooferFleet");
		e.triggerFleetSetName("Exploration Flotilla");

		e.endCreate();
	}

}
