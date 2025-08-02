package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CampaignObjective;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.misc.CommSnifferIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddShip;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.missions.hallowhall.SotfHopeForHallowhallEventIntel;
import data.scripts.utils.SotfMisc;
import data.scripts.world.mia.SotfPersonalFleetSeraph;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Objectives.SALVAGE_FRACTION;

public class SotfHallowCMD extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		//if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI)) return false;

		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
		CampaignFleetAPI other = null;
		if (dialog.getInteractionTarget() instanceof CampaignFleetAPI) {
			other = (CampaignFleetAPI) dialog.getInteractionTarget();
		}

		TextPanelAPI text = dialog.getTextPanel();

		String cmd = null;

		cmd = params.get(0).getString(memoryMap);
		String param = null;
		if (params.size() > 1) {
			param = params.get(1).getString(memoryMap);
		}

		Color hl = Misc.getHighlightColor();
		Color red = Misc.getNegativeHighlightColor();

		PersonAPI seraph = SotfPeople.getPerson(SotfPeople.SERAPH);

		switch (cmd) {
			case "cargoScanRigged":
				text.setFontSmallInsignia();

				text.addParagraph("-----------------------------------------------------------------------------");

				text.addParagraph("Contraband found!", red);
				text.addParagraph("Planetkiller " + Strings.X + " 1");
				text.highlightInLastPara(hl, "1");

				text.addParagraph("-----------------------------------------------------------------------------");

				text.setFontInsignia();
				return true;
			case "cargoScanClean":
				text.setFontSmallInsignia();

				text.addParagraph("-----------------------------------------------------------------------------");

				text.addParagraph("No contraband or suspicious cargo found.");

				text.addParagraph("-----------------------------------------------------------------------------");

				text.setFontInsignia();
				return true;
			case "noMoretOff":
				StarSystemAPI system = playerFleet.getStarSystem();
				if (system == null) system = Global.getSector().getStarSystem("sotf_mia");
				for (CampaignFleetAPI fleet : Misc.getFleetsInOrNearSystem(system)) {
					if (!fleet.getFaction().getId().equals(SotfIDs.DUSTKEEPERS)) continue;
					fleet.getMemoryWithoutUpdate().set("$hassleComplete", true);
					fleet.getMemoryWithoutUpdate().unset(MemFlags.WILL_HASSLE_PLAYER);
				}
				for (CampaignFleetAPI fleet : Global.getSector().getHyperspace().getFleets()) {
					if (!fleet.getFaction().getId().equals(SotfIDs.DUSTKEEPERS)) continue;
					if (!fleet.isInOrNearSystem(system)) continue;
					fleet.getMemoryWithoutUpdate().set("$hassleComplete", true);
					fleet.getMemoryWithoutUpdate().unset(MemFlags.WILL_HASSLE_PLAYER);
				}
				return true;
			case "showHallow":
				dialog.getVisualPanel().showLargePlanet(Global.getSector().getEntityById("sotf_hallowhall"));
				return true;
			case "addHallowhallEvent":
				new SotfHopeForHallowhallEventIntel(text, true);
				return true;
			case "printSkills":
				MutableCharacterStatsAPI stats = seraph.getStats();

				for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : seraph.getStats().getSkillsCopy()) {
					if (skillLevel.getSkill().isAdmiralSkill()) {
						seraph.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
					}
				}

				text.addSkillPanel(seraph, false);

				text.setFontSmallInsignia();
				String personality = Misc.lcFirst(Misc.getPersonalityName(seraph));
				String autoMultString = new DecimalFormat("#.##").format(seraph.getMemoryWithoutUpdate().getFloat(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));

				text.addParagraph("Personality: " + personality + ", level: " + stats.getLevel() + " (fixed)" + ", automated ship points multiplier: " + autoMultString + "x");
				text.highlightInLastPara(hl, personality, "" + stats.getLevel(), autoMultString + "x");

				text.addParagraph("In combat, this warmind will prefer to engage at a range that allows the use of " +
						"all of their ship's weapons and will employ any fighters under their command aggressively.");

				text.addPara("Warminds act similarly to AI cores. They can only be assigned to automated ships, " +
						"their non-unique skills can be reassigned at will, and they can be fully integrated " +
						"into ships to gain an extra level.", Misc.getGrayColor().brighter());

				text.setFontInsignia();
				return true;
			case "recruitSeraph":
				FleetMemberAPI flagship = other.getFlagship();

				other.getFleetData().removeFleetMember(flagship);

				text.setFontSmallInsignia();
				text.addParagraph(flagship.getShipName() + " joined your fleet", Misc.getPositiveHighlightColor());
				text.highlightInLastPara(Misc.getHighlightColor(), flagship.getShipName());
				text.setFontInsignia();

				FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, flagship.getVariant());
				playerFleet.getFleetData().addFleetMember(copy);
				copy.setCaptain(seraph);
				copy.setShipName(flagship.getShipName());
				seraph.setPostId(Ranks.POST_OFFICER);

				// clear admiral skills because they count against the number of skills they can pick
				for (MutableCharacterStatsAPI.SkillLevelAPI skillLevel : seraph.getStats().getSkillsCopy()) {
					if (skillLevel.getSkill().isAdmiralSkill()) {
						seraph.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
					}
				}

				// assign a proxy as fleet commander
				FleetMemberAPI newFlagship = other.getFleetData().getMembersInPriorityOrder().get(0);
				if (newFlagship != null) {
					AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE);
					PersonAPI newCommander = plugin.createPerson(Commodities.GAMMA_CORE, SotfIDs.DUSTKEEPERS_PROXIES, Misc.random);
					newCommander.getStats().setLevel(4); // integrated
					newCommander.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);

					other.setCommander(newCommander);
					newFlagship.setFlagship(true);
					other.getMemoryWithoutUpdate().set("$sotf_holdoutProxy", true);
					other.getFleetData().sort();
					other.getFleetData().setSyncNeeded();
					other.getFleetData().syncIfNeeded();
					Misc.giveStandardReturnToSourceAssignments(other);
				} else {
					other.despawn();
				}
				Global.getSector().removeScriptsOfClass(SotfPersonalFleetSeraph.class);
				return true;
			case "showSeraFleet":
				dialog.getVisualPanel().showFleetInfo("Your forces", playerFleet, "Seraph's Astral Castellans", other);
				return true;
			case "oblivion":
				FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, Global.getSettings().getVariant("sotf_anamnesis_Oblivion"));
				member.setShipName("ODS Morrowshield IV");
				member.getVariant().addTag("sotf_morrowshield");
				Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
				AddShip.addShipGainText(member, dialog.getTextPanel());
				return true;
			case "hasAutomated":
				return Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE) || SotfMisc.playerHasNoAutoPenaltyShip();
			default:
				return true;
		}
	}
}















