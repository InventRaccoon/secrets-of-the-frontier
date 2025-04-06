package data.scripts.campaign.missions.dkcontact.cb;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TriTachyonCommerceRaiding;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHFHInadMissionCompletedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHFHPathersDestroyedFactor;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *	Altered BaseCustomBountyCreator for the Twins (mostly to change "taps datapad" text)
 */

public class SotfBaseDKCBCreator extends BaseCustomBountyCreator {

	@Override
	public void notifyCompleted(HubMissionWithBarEvent mission, CustomBountyData data) {
		super.notifyCompleted(mission, data);
		if (mission.getPerson().getId().equals(SotfPeople.INADVERTENT)) {
			SotfHFHInadMissionCompletedFactor factor = new SotfHFHInadMissionCompletedFactor(SotfHopeForHallowhallEventIntel.POINTS_PER_INAD_MISSION);
			SotfHopeForHallowhallEventIntel.addFactorIfAvailable(factor, null);
		}
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

			FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
			if (member.isFlagship()) {
				copy.setCaptain(data.fleet.getCommander());
			}
			list.add(copy);
		}

		if (!list.isEmpty()) {
			TooltipMakerAPI info = text.beginTooltip();

			String main = "Echo-Inadvertent brings forward the selected docket.";
			if (mission.getPerson().getId().equals(SotfPeople.WENDIGO)) {
				main = "Annex-Wendigo hums satisfiedly as they enlarge the selected docket.";
			} else if (mission.getPerson().getId().equals("sotf_twins")) {
				main = "Affix-Calliope taps the selected docket, and it vanishes.";
			}

			info.setParaSmallInsignia();
			info.addPara(main + " An intel assessment appears on your tripad.", 0f);
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
			} else {
				info.addPara("It appears to contain complete information about the scope of the assignment.", opad);
			}
			text.addTooltip();
		}
		return;
	}
	
}






