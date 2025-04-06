package data.scripts.campaign.missions.cb;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBStats;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.*;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode.*;

/**
 *	Project SIREN: you monster
 *  Actually maybe I'm the monster for making this
 */

public class SotfCBProjectSiren extends BaseCustomBountyCreator {

	@Override
	public float getBountyDays() {
		return CBStats.REMNANT_PLUS_DAYS;
	}

	@Override
	public float getFrequency(HubMissionWithBarEvent mission, int difficulty) {
		if (mission.getPerson().getId().equals(SotfPeople.INADVERTENT) || mission.getPerson().getId().equals(SotfPeople.WENDIGO)) return 0;
		// Project SIREN faction (usually TT) doesn't offer the bounty themselves
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_BEGAN_PROJECT_SIREN) && !mission.getPerson().getFaction().getId().equals(SotfIDs.MEM_PROJECT_SIREN_FACTION)) {
			if (Global.getSettings().isDevMode()) {
				return 9999f;
			}
			return super.getFrequency(mission, difficulty) * CBStats.REMNANT_PLUS_FREQ;
		} else {
			return 0;
		}
	}

	@Override
	protected boolean isRepeatableGlobally() {
		return false;
	}

	public String getBountyNamePostfix(HubMissionWithBarEvent mission, CustomBountyData data) {
		return " - Rogue Phase Corps";
	}

	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		CustomBountyData data = new CustomBountyData();
		data.difficulty = difficulty;

		String faction = Global.getSector().getMemoryWithoutUpdate().getString(SotfIDs.MEM_PROJECT_SIREN_FACTION);
		if (faction == null) {
			faction = Factions.TRITACHYON;
		}

		//mission.requireSystem(this);
		mission.requireSystemTags(NOT_ANY, Tags.THEME_CORE);
		mission.preferSystemUnexplored();
		mission.preferSystemInteresting();
//		mission.requireSystemTags(ReqMode.ANY, Tags.THEME_RUINS, Tags.THEME_MISC, Tags.THEME_REMNANT,
//				  Tags.THEME_DERELICT, Tags.THEME_REMNANT_DESTROYED);
		mission.requireSystemNotHasPulsar();
		mission.preferSystemNebula();
		mission.preferSystemOnFringeOfSector();

		StarSystemAPI system = mission.pickSystem();
		data.system = system;

		FleetSize size = FleetSize.VERY_LARGE;
		FleetQuality quality = FleetQuality.SMOD_2;
		OfficerQuality oQuality = OfficerQuality.UNUSUALLY_HIGH;
		OfficerNum oNum = OfficerNum.ALL_SHIPS;
		String type = FleetTypes.PATROL_LARGE;

		beginFleet(mission, data);
		mission.triggerCreateFleet(size, quality, faction, type, data.system);
		mission.triggerFleetSetName("Rogue Phase Corps");
		mission.triggerFleetSetNoFactionInName();
		mission.triggerSetFleetOfficers(oNum, oQuality);
		mission.triggerAutoAdjustFleetSize(size, size.next());
		mission.triggerSetFleetNoCommanderSkills();
		mission.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1);
		mission.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1);
		mission.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1);
		mission.triggerFleetAddCommanderSkill(Skills.NAVIGATION, 1);
		mission.triggerFleetAddCommanderSkill(Skills.PHASE_CORPS, 1);
		mission.triggerFleetSetAllWeapons();
		mission.triggerMakeHostileAndAggressive();
		mission.triggerMakeNoRepImpact(); // at this point they just want rid of her
		mission.triggerFleetAllowLongPursuit();
		mission.triggerSetFleetDoctrineComp(1, 0, 5);
		mission.triggerPickLocationAtInSystemJumpPoint(data.system);
		mission.triggerSpawnFleetAtPickedLocation(null, null);
		mission.triggerFleetSetPatrolActionText("listening to signals");
		mission.triggerOrderFleetPatrol(data.system, true, Tags.JUMP_POINT, Tags.NEUTRINO, Tags.NEUTRINO_HIGH, Tags.STATION,
				Tags.SALVAGEABLE, Tags.GAS_GIANT);

		data.fleet = createFleet(mission, data);
		if (data.fleet == null) return null;

		data.fleet.setFaction(faction, true);

		String sierraVariantId = "sotf_vow_Siren";
		if (Global.getSector().getPlayerPerson().getStats().getLevel() > 8) {
			sierraVariantId = "sotf_covenant_Siren";
		}
		if (Global.getSector().getMemoryWithoutUpdate().contains(SotfIDs.MEM_PROJECT_SIREN_VARIANT)) {
			sierraVariantId = Global.getSector().getMemoryWithoutUpdate().getString(SotfIDs.MEM_PROJECT_SIREN_VARIANT);
		}

		CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(faction, "", true);
		fleet.getFleetData().addFleetMember(sierraVariantId);
		FleetMemberAPI member = fleet.getFlagship();
		member.setShipName("Voidwraith");

		PersonAPI person = SotfPeople.getPerson(SotfPeople.PROJECT_SIREN);
		//person.setFaction(faction);
		person.setFaction(SotfIDs.SYMPHONY);
		if (faction.equals(Factions.TRITACHYON)) {
			person.getName().setFirst("TTX SIREN");
		}
		member.setCaptain(person);

		int i = data.fleet.getFleetData().getMembersListCopy().size() - 1;
		FleetMemberAPI last = data.fleet.getFleetData().getMembersListCopy().get(i);
		data.fleet.getFleetData().removeFleetMember(last);

		data.fleet.setCommander(person);
		data.fleet.getFleetData().addFleetMember(member);
		data.fleet.getFleetData().sort();
		List<FleetMemberAPI> members = data.fleet.getFleetData().getMembersListCopy();
		for (FleetMemberAPI curr : members) {
			curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
		}

		member.setVariant(member.getVariant().clone(), false, false);
		member.getVariant().setSource(VariantSource.REFIT);
		member.getVariant().addTag(Tags.VARIANT_CONSISTENT_WEAPON_DROPS);
		member.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
		if (sierraVariantId.equals("sotf_covenant_Siren")) {
			member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
		}

		data.fleet.getMemoryWithoutUpdate().set("$sotf_projectSirenFleet", true);

		setRepChangesBasedOnDifficulty(data, difficulty);
		data.baseReward = CBStats.getBaseBounty(difficulty, CBStats.REMNANT_PLUS_MULT, mission);

		return data;
	}

	@Override
	public void addIntelAssessment(TextPanelAPI text, HubMissionWithBarEvent mission, CustomBountyData data) {
		float opad = 10f;
		List<FleetMemberAPI> list = new ArrayList<FleetMemberAPI>();
		List<FleetMemberAPI> members = data.fleet.getFleetData().getMembersListCopy();
		int max = 7;
		int cols = 7;
		float iconSize = 440 / cols;
		Color h = Misc.getHighlightColor();


		for (FleetMemberAPI member : members) {
			if (list.size() >= max) break;

			if (member.isFighterWing()) continue;
			// don't show Sierra, human contacts don't know she's there
			if (member.getVariant().hasHullMod(SotfIDs.SIRENS_CONCORD)) continue;

			FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
			list.add(copy);
		}

		if (!list.isEmpty()) {
			TooltipMakerAPI info = text.beginTooltip();
			info.setParaSmallInsignia();
			info.addPara(Misc.ucFirst(mission.getPerson().getHeOrShe()) + " taps a data pad, and " +
					"an intel assessment shows up on your tripad.", 0f);
			info.addShipList(cols, 1, iconSize, data.fleet.getFaction().getBaseUIColor(), list, opad);

			int num = members.size() - list.size();

			if (num < 5) num = 0;
			else if (num < 10) num = 5;
			else if (num < 20) num = 10;
			else num = 20;

			if (num > 1) {
				info.addPara("The assessment notes the fleet may contain upwards of %s other ships" +
						" of lesser significance.", opad, h, "" + num);
			} else if (num > 0) {
				info.addPara("The assessment notes the fleet may contain several other ships" +
						" of lesser significance.", opad);
			}
			info.addPara("No data is available on the fleet's flagship.", opad);
			text.addTooltip();
		}
		return;
	}

	@Override
	public void addFleetDescription(TooltipMakerAPI info, float width, float height, HubMissionWithBarEvent mission, CustomBountyData data) {
		PersonAPI person = data.fleet.getCommander();
		FactionAPI faction = person.getFaction();
		int cols = 7;
		float iconSize = width / cols;
		float opad = 10f;
		Color h = Misc.getHighlightColor();

		boolean deflate = false;
		if (!data.fleet.isInflated()) {
			data.fleet.inflateIfNeeded();
			deflate = true;
		}

		List<FleetMemberAPI> list = new ArrayList<FleetMemberAPI>();
		Random random = new Random(person.getNameString().hashCode() * 170000);

		List<FleetMemberAPI> members = data.fleet.getFleetData().getMembersListCopy();
		int max = 7;
		for (FleetMemberAPI member : members) {
			if (list.size() >= max) break;

			if (member.isFighterWing()) continue;

			// don't show Sierra, human contacts don't know she's there
			if (member.getVariant().hasHullMod(SotfIDs.SIRENS_CONCORD)) continue;

			FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
			list.add(copy);
		}

		if (!list.isEmpty()) {
			info.addPara("The bounty posting contains partial intel on some of the ships in the target fleet.", opad);
			info.addShipList(cols, 1, iconSize, faction.getBaseUIColor(), list, opad);

			int num = members.size() - list.size();
			//num = Math.round((float)num * (1f + random.nextFloat() * 0.5f));

			if (num < 5) num = 0;
			else if (num < 10) num = 5;
			else if (num < 20) num = 10;
			else num = 20;

			if (num > 1) {
				info.addPara("The intel assessment notes the fleet may contain upwards of %s other ships" +
						" of lesser significance.", opad, h, "" + num);
			} else if (num > 0) {
				info.addPara("The intel assessment notes the fleet may contain several other ships" +
						" of lesser significance.", opad);
			}
			info.addPara("No data is available regarding the fleet's flagship.", opad);
		}

		if (deflate) {
			data.fleet.deflate();
		}
	}

	public void updateInteractionData(HubMissionWithBarEvent mission, CustomBountyData data) {
		if (data.fleet != null) {
			mission.set("$bcb_fleetFaction", data.fleet.getFaction().getDisplayNameWithArticle());
		}
	}


	@Override
	public int getMaxDifficulty() {
		return super.getMaxDifficulty();
	}

	@Override
	public int getMinDifficulty() {
		return super.getMinDifficulty();
		//return super.getMaxDifficulty();
	}

}
