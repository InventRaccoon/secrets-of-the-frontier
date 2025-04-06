package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.BaseGenerateFleetOfficersPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.util.*;

import static java.util.Collections.*;

/**
 *	Generates officers for Dustkeeper fleets - Remnants get proper AI officers, while Proxies get spare gammas
 */

public class SotfDustkeeperOfficerPlugin extends BaseGenerateFleetOfficersPlugin {

	@Override
	public int getHandlingPriority(Object params) {
		if (!(params instanceof GenerateFleetOfficersPickData)) return -1;

		GenerateFleetOfficersPickData data = (GenerateFleetOfficersPickData) params;
		if (data.params != null && !data.params.withOfficers) return -1;
		// "contains", so includes Burnouts and Proxies subfactions
		if (data.fleet == null || !data.fleet.getFaction().getId().contains(SotfIDs.DUSTKEEPERS)) return -1;
		return GenericPluginManagerAPI.MOD_SUBSET;
	}

	@Override
	public void addCommanderAndOfficers(CampaignFleetAPI fleet, FleetParamsV3 params, Random random) {
		if (random == null) random = Misc.random;
		FactionAPI faction = fleet.getFaction();
		FactionDoctrineAPI doctrine = faction.getDoctrine();
		if (params != null && params.doctrineOverride != null) {
			doctrine = params.doctrineOverride;
		}
		List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
		List<FleetMemberAPI> regulars = new ArrayList<>();
		List<FleetMemberAPI> auxiliaries = new ArrayList<>();
		if (members.isEmpty()) return;

		FleetMemberAPI flagship = null;

		Map<String, AICoreOfficerPlugin> plugins = new HashMap<String, AICoreOfficerPlugin>();

		plugins.put(Commodities.ALPHA_CORE, Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE));
		plugins.put(Commodities.BETA_CORE, Misc.getAICoreOfficerPlugin(Commodities.BETA_CORE));
		plugins.put(Commodities.GAMMA_CORE, Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE));
		String nothing = "nothing";

		float regularsFP = 0f;
		float auxiliariesFP = 0f;
		for (FleetMemberAPI member : members) {
			if (SotfMisc.isDustkeeperAuxiliary(member)) {
				auxiliaries.add(member);
				auxiliariesFP += member.getFleetPointCost();
			} else {
				regulars.add(member);
				regularsFP += member.getFleetPointCost();
			}
		}
		boolean auxOnly = regularsFP == 0;
		boolean allowAlphaAnywhere = regularsFP > 150f;
		boolean allowBetaAnywhere = regularsFP > 75f;
		//boolean barrowMode = fleet.getMemoryWithoutUpdate().contains(SotfIDs.MEM_BARROW_FLEET); // flag isn't set yet

		int numCommanderSkills = 1;
		if (allowBetaAnywhere) numCommanderSkills++;
		if (allowAlphaAnywhere) numCommanderSkills++;
		if (params != null && params.noCommanderSkills != null && params.noCommanderSkills) numCommanderSkills = 0;

		//float fpPerCore = 20f;
		float fpPerCore = Global.getSettings().getFloat("baseFPPerAICore");

		int minCores = (int) (regularsFP / fpPerCore * (params != null ? params.officerNumberMult : 1f));
		if (params != null) {
			minCores += params.officerNumberBonus;
		}
		if (minCores < 1) minCores = 1;

		float fpPerAuxCore = fpPerCore / (params != null ? params.officerNumberMult : 1f);
		if (auxOnly) {
			fpPerAuxCore = fpPerAuxCore / 2f;
		}
		int auxiliaryCores = (int) (auxiliariesFP / fpPerAuxCore);

		WeightedRandomPicker<FleetMemberAPI> withOfficers = new WeightedRandomPicker<FleetMemberAPI>(random);

		int maxSize = 0;
		for (FleetMemberAPI member : regulars) {
			if (member.isFighterWing()) continue;
			if (member.isCivilian()) continue;
			int size = member.getHullSpec().getHullSize().ordinal();
			if (size > maxSize) {
				maxSize = size;
			}
		}

		List<FleetMemberAPI> allWithOfficers = new ArrayList<FleetMemberAPI>();
		int addedCores = 0;

		float effectiveQuality = doctrine.getOfficerQuality();
		if (params != null) {
			effectiveQuality += params.officerLevelBonus;
		}

		/*
		 	Remnant ships are given proper Dustkeeper warminds
		 */
		for (FleetMemberAPI member : regulars) {
			if (member.isCivilian()) continue;
			if (member.isFighterWing()) continue;
			if (!member.getVariant().hasHullMod(HullMods.AUTOMATED)) continue;

			if (member.getVariant().hasHullMod(SotfIDs.HULLMOD_CWARSUITE) && member.isCapital()) {
				flagship = member;
			}

			float fp = member.getFleetPointCost();

			WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);

			float sliverWeight = 12f;
			float echoWeight = 6f;
			float annexWeight = 3f;
			switch (member.getHullSpec().getHullSize()) {
				case FIGHTER:
				case FRIGATE:
					break;
				case DESTROYER:
					sliverWeight *= 0.5f;
					break;
				case CRUISER:
					sliverWeight *= 0.35f;
					annexWeight *= 1.25f;
					break;
				case CAPITAL_SHIP:
					sliverWeight *= 0f;
					echoWeight *= 0.5f;
					annexWeight *= 2f;
					break;
			}

			sliverWeight -= effectiveQuality;
			annexWeight += effectiveQuality;

			if (sliverWeight < 0f) {
				sliverWeight = 0f;
			}

			// at 0.5f effectiveness, increases Echo weight to 3x and Annex to 1.5x, and reduces Sliver weight to 0.33x
			//float officerQualityMult = (1f + ((effectiveQuality - 1f) * 0.5f));

			//picker.add(Commodities.ALPHA_CORE, annexWeight * (officerQualityMult * 0.5f));
			//picker.add(Commodities.BETA_CORE, echoWeight * officerQualityMult);
			//picker.add(Commodities.GAMMA_CORE, sliverWeight / officerQualityMult);

			picker.add(Commodities.ALPHA_CORE, annexWeight);
			picker.add(Commodities.BETA_CORE, echoWeight);
			picker.add(Commodities.GAMMA_CORE, sliverWeight);


			if (addedCores >= minCores) {
				picker.add(nothing, 10f * picker.getTotal()/fp);
			}

			String pick = picker.pick();
			AICoreOfficerPlugin plugin = plugins.get(pick);
			if (plugin != null) {
				addedCores++;

				PersonAPI person = SotfPeople.genDustkeeperForCore(pick);
				person.setFaction(faction.getId());
				SotfMisc.reassignAICoreSkills(person, member, fleet, random);
				member.setCaptain(person);

				if (!member.isFighterWing() && !member.isCivilian() && member.getHullSpec().getHullSize().ordinal() >= maxSize) {
					withOfficers.add(member, fp * fp);
				}

				allWithOfficers.add(member);
			}

			if (addedCores > 0 && params != null && params.officerNumberMult <= 0) {
				break; // only want to add the fleet commander
			}
		}

		/*
		 	Proxies get gamma cores... and sometimes a surprise
		 */
		addedCores = 0;
		int oldGuards = 0;
		for (FleetMemberAPI member : auxiliaries) {
			if (params != null && params.officerNumberMult <= 0) {
				break;
			}

			if (member.isCivilian()) continue;
			if (member.isFighterWing()) continue;
			if (!member.getVariant().hasHullMod(HullMods.AUTOMATED)) continue;

			float fp = member.getFleetPointCost();

			WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);

			/*
			 	Dustkeepers got their start using Derelict ships
			  	Some oldguards still remain, and they take these clunkers far beyond what a Gamma can manage
			 */

			// 5%/10%/15% at 3/4/5 officer quality, 0% otherwise, always in Repose-class, cannot outnumber Gammas
			float oldguardProb = 0.05f * (effectiveQuality - 2);
			if (member.isCapital()) oldguardProb = 1f; // although this doesn't usually happen
			if (fleet.getFaction().getId().equals(SotfIDs.DUSTKEEPERS_PROXIES)) oldguardProb = 0;
			if (!auxOnly && random.nextFloat() <= oldguardProb && (member.isCruiser() || member.isCapital()) && (oldGuards + 1 <= auxiliaryCores * 0.5f)) {
				AICoreOfficerPlugin plugin = plugins.get(Commodities.ALPHA_CORE);
				if (plugin != null) {
					addedCores++;
					oldGuards++;

					PersonAPI person = SotfPeople.genDustkeeperAnnex();
					person.setFaction(faction.getId());
					SotfMisc.reassignAICoreSkills(person, member, fleet, random);
					person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_dustkeeper_oldguard");
					// BORN TO TANK
					// THE SECTOR IS A FUCK
					person.getStats().setLevel(8);
					person.getStats().setSkillLevel(SotfIDs.SKILL_DERELICTCONTINGENTP, 2);
					person.setPersonality(Personalities.RECKLESS);
					member.setCaptain(person);

					allWithOfficers.add(member);
				}
				continue;
			}

			float nothingWeight = 1f;
			// 50/50 chance of core
			if (addedCores < auxiliaryCores) {
				picker.add(Commodities.GAMMA_CORE);
				// always put cores in cruisers if possible
				if (member.isCruiser()) {
					nothingWeight = 0f;
				} else if (member.isDestroyer()) {
					nothingWeight = 0.5f;
				}
			}
			picker.add(nothing, nothingWeight);

			String pick = picker.pick();
			AICoreOfficerPlugin plugin = plugins.get(pick);
			if (plugin != null) {
				addedCores++;

				PersonAPI person = plugin.createPerson(Commodities.GAMMA_CORE, SotfIDs.DUSTKEEPERS_PROXIES, random);
				person.getStats().setLevel(4); // integrated
				if (member.getVariant().hasHullMod(SotfIDs.HULLMOD_AUX_ESCORT)) {
					// Sentries get Missile Spec, Defender/Bastillon/Keeper get Point Defense
					if (FleetFactoryV3.getSkillPrefForShip(member).toString().contains("YES_MISSILE") && member.getNumFlightDecks() == 0) {
						person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
					} else {
						person.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
					}
					person.setPersonality(Personalities.STEADY);
				}
				// all assault auxiliaries and the Picket get Polarized Armor
				else {
					person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
				}
				member.setCaptain(person);

				allWithOfficers.add(member);
			}
		}

		if (withOfficers.isEmpty() && !allWithOfficers.isEmpty()) {
			withOfficers.add(allWithOfficers.get(0), 1f);
		}

		/*
		 	Dustkeeper fleet commanders are always Annexes with Cyberwarfare
		 */
		if (flagship == null) {
			flagship = withOfficers.pick();
		}
		if (flagship != null) {
			PersonAPI commander = flagship.getCaptain();
			if (params != null && params.commander != null) {
				commander = params.commander;
			} else {
				if (commander.getStats().getLevel() < 8 && !commander.getFaction().getId().equals(SotfIDs.DUSTKEEPERS_PROXIES)) {
					//int cyberwarfare = 1;
					//if (allowBetaAnywhere) cyberwarfare = 2;
					commander.setStats(plugins.get(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, faction.getId(), random).getStats());
					commander.getStats().setLevel(8);
					commander.getStats().setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2);
					commander.setAICoreId(Commodities.ALPHA_CORE);
					SotfMisc.dustkeeperifyAICore(commander);
					commander.setFaction(faction.getId());

					// if flagship is a Repose, then also Oldguard-ify the commander
					if (flagship.getVariant().hasHullMod("rugged")) {
						commander.getStats().setLevel(9);
						commander.getStats().setSkillLevel(SotfIDs.SKILL_DERELICTCONTINGENTP, 2);
						commander.setPersonality(Personalities.RECKLESS);
						commander.getMemoryWithoutUpdate().set("$chatterChar", "sotf_dustkeeper_oldguard");
					}
				}
				commander.setRankId(Ranks.SPACE_COMMANDER);
				commander.setPostId(Ranks.POST_FLEET_COMMANDER);
			}
			fleet.setCommander(commander);
			fleet.getFleetData().setFlagship(flagship);
			addCommanderSkills(commander, fleet, params, numCommanderSkills, random);
		}
	}

	public static void addCommanderSkills(PersonAPI commander, CampaignFleetAPI fleet, FleetParamsV3 params, int numSkills, Random random) {
		if (random == null) random = new Random();
		if (numSkills <= 0) return;

		MutableCharacterStatsAPI stats = commander.getStats();

		FactionDoctrineAPI doctrine = fleet.getFaction().getDoctrine();
		if (params != null && params.doctrineOverride != null) {
			doctrine = params.doctrineOverride;
		}

		List<String> skills = new ArrayList<String>(doctrine.getCommanderSkills());
		if (skills.isEmpty()) return;

		if (random.nextFloat() < doctrine.getCommanderSkillsShuffleProbability()) {
			shuffle(skills, random);
		}

		// Dustkeeper fleets with a decent number of Proxies also use Support Doctrine
		if (doctrine.getCombatFreighterCombatUseFraction() >= 0.35f) {
			skills.add(1, Skills.SUPPORT_DOCTRINE);
		}

		stats.setSkipRefresh(true);

		boolean debug = true;
		debug = false;
		if (debug) System.out.println("Generating Dustkeeper commander skills, skills: " + numSkills);
		int picks = 0;
		for (String skillId : skills) {
			if (debug) System.out.println("Selected skill: [" + skillId + "]");
			stats.setSkillLevel(skillId, 1);
			picks++;
			if (picks >= numSkills) {
				break;
			}
		}
		if (debug) System.out.println("Done generating Dustkeeper commander skills\n");

		stats.setSkipRefresh(false);
		stats.refreshCharacterStatsEffects();
	}
}
