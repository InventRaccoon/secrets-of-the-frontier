package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.apromise.SotfAPVenaEntityPlugin;
import data.scripts.utils.SotfMisc;

import java.util.Random;

/**
 *	Modifies salvage defender fleets
 */

public class SotfSalDefModPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

	public float getStrength(SDMParams p, float strength, Random random, boolean withOverride) {
		return strength;
	}
	public float getMinSize(SDMParams p, float minSize, Random random, boolean withOverride) {
		return minSize;
	}
	public float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride) {
		if (withOverride) return maxSize;
		return maxSize;
	}
	public float getProbability(SDMParams p, float probability, Random random, boolean withOverride) {
		if (withOverride) return probability;
		return probability;
	}
	public float getQuality(SDMParams p, float quality, Random random, boolean withOverride) {
		return quality;
	}
	public void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
		if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(SotfIDs.HYPNOS_CRYO)) {
			Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_DEFEATED_BARROW, true);
			SectorEntityToken ship = SotfMisc.addStoryDerelictWithName(entity.getStarSystem(), entity, SotfIDs.BARROW_VARIANT,
					ShipRecoverySpecial.ShipCondition.WRECKED, entity.getRadius() + 50f, true, "ODS Northstar II");
			Global.getSector().addPing(ship, SotfIDs.PING_COURSERPROTOCOL);
		}
	}
	public void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
		if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(SotfIDs.HYPNOS_CRYO)) {
			FleetMemberAPI flagship = fleet.getFleetData().addFleetMember(SotfIDs.BARROW_VARIANT);
			flagship.setShipName("ODS Northstar II");
			flagship.getVariant().addTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER);
			flagship.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
			PersonAPI barrow = SotfPeople.getPerson(SotfPeople.BARROW_D);
			fleet.setCommander(barrow);

			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				member.setFlagship(member == flagship);
			}

			fleet.getFlagship().setCaptain(barrow);
			fleet.setNoFactionInName(true);
			fleet.setName("The Rust Crows");

			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				// any cruisers are captained by Rust Crow oldguards
				if (member == flagship) {
					member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
					continue;
				}
				if (member.isCruiser()) {
					PersonAPI person = SotfPeople.genDustkeeperAnnex();
					SotfMisc.reassignAICoreSkills(person, member, fleet, random);
					person.getMemoryWithoutUpdate().set("$chatterChar", "sotf_dustkeeper_oldguard");
					// which means we're getting TANKY
					person.getStats().setLevel(8);
					person.getStats().setSkillLevel(SotfIDs.SKILL_DERELICTCONTINGENTP, 2);
					// NO FEAR
					person.setPersonality(Personalities.RECKLESS);
					member.setCaptain(person);
					int timesRecovered = random.nextInt(4) + 7;
					member.setShipName(Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).pickRandomShipName() + " " + Global.getSettings().getRoman(timesRecovered));
				} else {
					PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, SotfIDs.DUSTKEEPERS_PROXIES, random);
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
					// all assault auxiliaries get Polarized Armor
					else {
						person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
					}
					member.setCaptain(person);
				}
				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
			}
			fleet.getMemoryWithoutUpdate().set(SotfIDs.MEM_BARROW_FLEET, true);
			// in case it is regenerated, make sure he remembers to deny comm links
			if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_barrowIgnoreCommRequests")) {
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
			}

			CargoAPI extraLoot = Global.getFactory().createCargo(true);
			extraLoot.addCommodity(SotfIDs.BARROW_CHIP_D, 1);
			BaseSalvageSpecial.addExtraSalvage(fleet, extraLoot);

			// convert from Dustkeepers Proxies to Dustkeeper Burnouts faction
			if (p.factionId.equals(SotfIDs.DUSTKEEPERS_PROXIES)) {
				fleet.setFaction(SotfIDs.DUSTKEEPERS_BURNOUTS, true);
			}
		}

		if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(SotfIDs.HYPNOS_LAB)) {
			fleet.setNoFactionInName(true);
			fleet.setName("Rust Crows Proxy Detachment");

			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				PersonAPI person = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, SotfIDs.DUSTKEEPERS_PROXIES, random);
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
				// all assault auxiliaries get Polarized Armor
				else {
					person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
				}
				member.setCaptain(person);
				member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
			}

			// convert from Dustkeepers Proxies to Dustkeeper Burnouts faction
			if (p.factionId.equals(SotfIDs.DUSTKEEPERS_PROXIES)) {
				fleet.setFaction(SotfIDs.DUSTKEEPERS_BURNOUTS, true);
			}
		}
	}
	@Override
	public int getHandlingPriority(Object params) {
		if (!(params instanceof SDMParams)) return 0;
		SDMParams p = (SDMParams) params;

		if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(SotfIDs.HYPNOS_CRYO)) {
			return 100;
		} else if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(SotfIDs.HYPNOS_LAB)) {
			return 100;
		} else if (p.factionId.contains(SotfIDs.DUSTKEEPERS)) {
			return 100;
		}
		return -1;
	}
}
