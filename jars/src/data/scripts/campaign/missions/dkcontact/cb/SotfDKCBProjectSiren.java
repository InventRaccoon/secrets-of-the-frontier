package data.scripts.campaign.missions.dkcontact.cb;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.missions.cb.SotfCBProjectSiren;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *	Project SIREN but via the Twins
 */

public class SotfDKCBProjectSiren extends SotfCBProjectSiren {

	// if done via another contact, can't do via the Twins, and vice versa
	@Override
	public String getNumCompletedGlobalKey() {
		return "$SotfCBProjectSiren_numCompleted";
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
			// actually DO show Sierra, since the Twins know she's there, unlike human contacts
			//if (member.getVariant().hasHullMod(SotfIDs.SIRENS_CONCORD)) continue;

			FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, member.getVariant());
			list.add(copy);
		}

		if (!list.isEmpty()) {
			TooltipMakerAPI info = text.beginTooltip();
			info.setParaSmallInsignia();
			info.addPara("Calliope taps the selected docket, and it vanishes. An intel assessment appears on your tripad.", 0f);
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
		}

		if (deflate) {
			data.fleet.deflate();
		}
	}
}
