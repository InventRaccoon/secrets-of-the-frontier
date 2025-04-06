package data.scripts.campaign.plugins.dustkeepers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.graid.StandardGroundRaidObjectivesCreator;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.loading.RoleEntryAPI;
import com.fs.starfarer.api.plugins.CreateFleetPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.util.*;

import static com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.*;

/**
 *	Customized fleet creator for the Dustkeepers
 * 	Dustkeeper fleets are split into two groups: the regulars, and the disposable auxiliary Proxies
 */

public class SotfDustkeeperFleetCreator implements CreateFleetPlugin {

	public static float DEFAULT_AUX_PERCENT = 0.25f;
	public static float RANDOM_AUX_PERCENT = 0.25f;

	// only Dustkeepers
	@Override
	public int getHandlingPriority(Object params) {
		if (!(params instanceof FleetParamsV3)) return -1;
		FleetParamsV3 fleetParams = (FleetParamsV3) params;
		if (!fleetParams.factionId.equals(SotfIDs.DUSTKEEPERS) && !fleetParams.factionId.equals(SotfIDs.DUSTKEEPERS_BURNOUTS)) return -1;
		return GenericPluginManagerAPI.MOD_SUBSET + 1;
		//return -1;
	}

	protected static int sizeOverride = 0;

	@Override
	public CampaignFleetAPI createFleet(FleetParamsV3 params) {
		boolean fakeMarket = false;
		MarketAPI market = pickMarket(params);
		if (market == null) {
			market = Global.getFactory().createMarket("fake", "fake", 5);
			market.getStability().modifyFlat("fake", 10000);
			market.setFactionId(params.factionId);
			SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
			market.setPrimaryEntity(token);

			market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", BASE_QUALITY_WHEN_NO_MARKET);

			market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);

			fakeMarket = true;
		}
		boolean sourceWasNull = params.source == null;
		params.source = market;
		if (sourceWasNull && params.qualityOverride == null) { // we picked a nearby market based on location
			params.updateQualityAndProducerFromSourceMarket();
		}

		String factionId = params.factionId;
		if (factionId == null) factionId = params.source.getFactionId();

		FactionAPI.ShipPickMode mode = PRIORITY_THEN_ALL;
		if (params.modeOverride != null) mode = params.modeOverride;

		CampaignFleetAPI fleet = createEmptyFleet(factionId, params.fleetType, market);
		fleet.getFleetData().setOnlySyncMemberLists(true);

		Misc.getSalvageSeed(fleet);

		FactionDoctrineAPI doctrine = fleet.getFaction().getDoctrine();
		if (params.doctrineOverride != null) {
			doctrine = params.doctrineOverride;
		}

		float numShipsMult = 1f;
		if (params.ignoreMarketFleetSizeMult == null || !params.ignoreMarketFleetSizeMult) {
			numShipsMult = market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
		}

		float quality = params.quality + params.qualityMod;

		if (params.qualityOverride != null) {
			quality = params.qualityOverride;
		}

		Random random = new Random();
		if (params.random != null) random = params.random;

		//
		// Combat freighter usage instead determines a portion of the Remnant ships to replace with Proxy drones
		//
		float auxiliaryUsage = doctrine.getCombatFreighterCombatUseFraction();

		// apply random variation to Dustkeeper fleets with non-fixed auxiliary usage
		if (auxiliaryUsage == DEFAULT_AUX_PERCENT) {
			auxiliaryUsage += RANDOM_AUX_PERCENT * random.nextFloat();
		}
		boolean usesAuxiliaries = auxiliaryUsage > 0f;

		float combatPts = params.combatPts * numShipsMult * (1f - auxiliaryUsage);
		int auxiliaryPts = (int) (params.combatPts * numShipsMult * auxiliaryUsage);

		if (params.onlyApplyFleetSizeToCombatShips != null && params.onlyApplyFleetSizeToCombatShips) {
			numShipsMult = 1f;
		}

		float freighterPts = params.freighterPts * numShipsMult;
		float tankerPts = params.tankerPts * numShipsMult;
		float transportPts = params.transportPts * numShipsMult;
		float linerPts = params.linerPts * numShipsMult;
		float utilityPts = params.utilityPts * numShipsMult;

		if (combatPts < 10 && combatPts > 0) {
			combatPts = Math.max(combatPts, 5 + random.nextInt(6));
		}

		float dW = (float) doctrine.getWarships() + random.nextInt(3) - 2;
		float dC = (float) doctrine.getCarriers() + random.nextInt(3) - 2;
		float dP = (float) doctrine.getPhaseShips() + random.nextInt(3) - 2;

		boolean strict = doctrine.isStrictComposition();
		if (strict) {
			dW = (float) doctrine.getWarships() - 1;
			dC = (float) doctrine.getCarriers() - 1;
			dP = (float) doctrine.getPhaseShips() -1;
		}

		if (!strict) {
			float r1 = random.nextFloat();
			float r2 = random.nextFloat();
			float min = Math.min(r1, r2);
			float max = Math.max(r1, r2);

			float mag = 1f;
			float v1 = min;
			float v2 = max - min;
			float v3 = 1f - max;

			v1 *= mag;
			v2 *= mag;
			v3 *= mag;

			v1 -= mag/3f;
			v2 -= mag/3f;
			v3 -= mag/3f;

			dW += v1;
			dC += v2;
			dP += v3;
		}

		if (doctrine.getWarships() <= 0) dW = 0;
		if (doctrine.getCarriers() <= 0) dC = 0;
		if (doctrine.getPhaseShips() <= 0) dP = 0;

		boolean banPhaseShipsEtc = !fleet.getFaction().isPlayerFaction() &&
				combatPts < FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;
		if (params.forceAllowPhaseShipsEtc != null && params.forceAllowPhaseShipsEtc) {
			banPhaseShipsEtc = !params.forceAllowPhaseShipsEtc;
		}

		params.mode = mode;
		params.banPhaseShipsEtc = banPhaseShipsEtc;

		if (dW < 0) dW = 0;
		if (dC < 0) dC = 0;
		if (dP < 0) dP = 0;

		float extra = 7 - (dC + dP + dW);
		if (extra < 0) extra = 0f;
		if (doctrine.getWarships() > doctrine.getCarriers() && doctrine.getWarships() > doctrine.getPhaseShips()) {
			dW += extra;
		} else if (doctrine.getCarriers() > doctrine.getWarships() && doctrine.getCarriers() > doctrine.getPhaseShips()) {
			dC += extra;
		} else if (doctrine.getPhaseShips() > doctrine.getWarships() && doctrine.getPhaseShips() > doctrine.getCarriers()) {
			dP += extra;
		}


		float doctrineTotal = dW + dC + dP;

		combatPts = (int) combatPts;
		int warships = (int) (combatPts * dW / doctrineTotal);
		int carriers = (int) (combatPts * dC / doctrineTotal);
		int phase = (int) (combatPts * dP / doctrineTotal);

		warships += (combatPts - warships - carriers - phase);

		addCombatFleetPoints(fleet, random, warships, carriers, phase, params);

		addFreighterFleetPoints(fleet, random, freighterPts, params);
		addTankerFleetPoints(fleet, random, tankerPts, params);
		addTransportFleetPoints(fleet, random, transportPts, params);
		addLinerFleetPoints(fleet, random, linerPts, params);
		addUtilityFleetPoints(fleet, random, utilityPts, params);

		// Dustkeepers who use auxiliaries will field fewer but more powerful Remnant ships
		int maxShips = Global.getSettings().getInt("maxShipsInAIFleet");
		// e.g if 50% FP as auxiliaries, then cap Remnants at 15
		if (usesAuxiliaries) {
			maxShips = Math.round(maxShips * (1f - auxiliaryUsage));
		}
		if (params.maxNumShips != null) {
			maxShips = params.maxNumShips;
		}

		if (fleet.getFleetData().getNumMembers() > maxShips) {
			if (params.doNotPrune == null || !params.doNotPrune) {
				float targetFP = getFP(fleet);
				if (params.doNotAddShipsBeforePruning == null || !params.doNotAddShipsBeforePruning) {
					sizeOverride = 5;
					addCombatFleetPoints(fleet, random, warships, carriers, phase, params);
					addFreighterFleetPoints(fleet, random, freighterPts, params);
					addTankerFleetPoints(fleet, random, tankerPts, params);
					addTransportFleetPoints(fleet, random, transportPts, params);
					addLinerFleetPoints(fleet, random, linerPts, params);
					addUtilityFleetPoints(fleet, random, utilityPts, params);
					sizeOverride = 0;
				}

				int size = doctrine.getShipSize();
				pruneFleet(maxShips, size, fleet, targetFP, random);

				float currFP = getFP(fleet);
			}
			fleet.getFleetData().sort();
		} else {
			fleet.getFleetData().sort();
		}

		// ... and their auxiliaries ignore the ship cap!
		if (usesAuxiliaries) {
			addAuxiliaryPoints(fleet, random, auxiliaryPts, params);
		}

		fleet.getFleetData().sort();

		if (params.withOfficers) {
			addCommanderAndOfficers(fleet, params, random);
		}

		if (fleet.getFlagship() != null) {
			if (params.flagshipVariantId != null) {
				fleet.getFlagship().setVariant(Global.getSettings().getVariant(params.flagshipVariantId), false, true);
			} else if (params.flagshipVariant != null) {
				fleet.getFlagship().setVariant(params.flagshipVariant, false, true);
			} else if (getFP(fleet) >= 120) {
				WeightedRandomPicker<String> flagshipPicker = new WeightedRandomPicker<String>(random);
				// pick flagships using default variant weights, not the adjusted weights for Dustkeepers
				// (which are 0 to avoid the ships spawning in the combatCapital role)
				for (RoleEntryAPI pick : Global.getSettings().getDefaultEntriesForRole("sotf_flagship")) {
					ShipVariantAPI variant = Global.getSettings().getVariant(pick.getVariantId());
					if (!fleet.getFaction().knowsShip(variant.getHullSpec().getBaseHullId())) continue;
					if (params.commander != null) {
						// don't randomly pick the Repose in fleets who have fixed commanders but not fixed flagships
						// (mostly so said fixed commanders aren't in charge of a ship that really wants Derelict Contingent)
						if (pick.getVariantId().contains("repose")) continue;
					}
					flagshipPicker.add(pick.getVariantId(), pick.getWeight());
				}
//				for (ShipRolePick flagshipPick : fleet.getFaction().pickShip("sotf_flagship", new FactionAPI.ShipPickParams())) {
//					if (params.commander != null) {
//						// don't randomly pick the Repose in fleets who have fixed commanders but not fixed flagships
//						// (mostly so said fixed commanders aren't in charge of a ship that really wants Derelict Contingent)
//						if (flagshipPick.variantId.contains("repose")) continue;
//					}
//					flagshipPicker.add(flagshipPick.variantId, flagshipPick.weight);
//				}
				fleet.getFlagship().setVariant(Global.getSettings().getVariant(flagshipPicker.pick()), false, true);
				fleet.getFlagship().getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
			}
		}

		if (params.onlyRetainFlagship != null && params.onlyRetainFlagship) {
			for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
				if (curr.isFlagship()) continue;
				fleet.getFleetData().removeFleetMember(curr);
			}
		}
		//fleet.getFlagship()
		fleet.forceSync();

		if (fakeMarket) {
			params.source = null;
		}

		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = quality;
		if (params.averageSMods != null) {
			p.averageSMods = params.averageSMods;
		}
		p.persistent = true;
		p.seed = random.nextLong();
		p.mode = mode;
		p.timestamp = params.timestamp;
		p.allWeapons = params.allWeapons;
		if (params.doctrineOverride != null) {
			p.rProb = params.doctrineOverride.getAutofitRandomizeProbability();
		}
		if (params.factionId != null) {
			p.factionId = params.factionId;
		}

		FleetInflater inflater = Misc.getInflater(fleet, p);
		fleet.setInflater(inflater);

		fleet.getFleetData().setOnlySyncMemberLists(false);
		fleet.getFleetData().sort();

		List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
		for (FleetMemberAPI member : members) {
			member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
			// Proxies have Derelict names, not Dustkeeper
			if (SotfMisc.isDustkeeperAuxiliary(member) && (member.getCaptain().isDefault() || member.getCaptain().getStats().getLevel() < 7)) {
				member.setShipName(Global.getSector().getFaction(SotfIDs.DUSTKEEPERS_PROXIES).pickRandomShipName(random));
			}
			// Dustkeeper regulars are rarely new ships : usually reused again and again (and obv stolen from Remnants)
			else {
				int timesRecovered = random.nextInt(4) + 1;
				if (!member.getCaptain().isDefault()) {
					// Slivers get the hand-me-downs
					if (member.getCaptain().getRank().equals(Ranks.SPACE_LIEUTENANT)) {
						timesRecovered += random.nextInt(7);
					} else {
						// Higher-level Dustkeepers sometimes have newly-built drones
						float pristineChance = 0.1f;
						//if (Global.getSector().getEconomy().getMarket(SotfIDs.SANCTUM_MARKET) != null) {
						//	pristineChance += 0.15f;
						//}
						if (random.nextFloat() <= pristineChance) {
							timesRecovered = 0;
						}
					}
				}
				// and the autopiloted ones are the oldest
				else {
					timesRecovered += random.nextInt(9) + 2;
				}
				// XIV is considered unlucky by Dustkeepers, so they skip to XV
				if (timesRecovered == 13) {
					timesRecovered++;
				}
				if (timesRecovered > 0) {
					member.setShipName(member.getShipName() + " " + Global.getSettings().getRoman(timesRecovered + 1));
				}
				// override for Wendigo (doing it normally is really hard bcs of how their encounters work)
				if (member.getId().equals(SotfPeople.WENDIGO)) {
					member.setShipName("ODS Winterbliss");
				}
			}
		}

		if (fleet.getCommander().getVoice() == null) {
			if (auxiliaryUsage > DEFAULT_AUX_PERCENT + (RANDOM_AUX_PERCENT * 0.5f)) {
				fleet.getCommander().setVoice("sotf_dkhunter");
			} else {
				fleet.getCommander().setVoice("sotf_dkfaithful");
			}
		}

		// give the fleet a name incorporating its commander's suffix
		// three-quarters chance to go for a regular name, like "Fabric's Shard"
		// one-quarter chance to go for a fancy name, like "Equinox's Ethereal Castellans" or "Lyric's Hellfire Wolves"
		assignFleetName(fleet, params.fleetType, auxiliaryUsage, false);

		float requestedPoints = params.getTotalPts();
		float actualPoints = fleet.getFleetPoints();

		Misc.setSpawnFPMult(fleet, actualPoints / Math.max(1f, requestedPoints));

		return fleet;
	}

	// OH GOD PLEASE HELP THERE'S MORE NAME LISTS TO ADD TO INSTEAD OF DOING REAL CONTENT
	public static void assignFleetName(CampaignFleetAPI fleet, String fleetType, float auxiliaryUsage, boolean forceFancy) {
		boolean fancyName = Math.random() < 0.35f;
		if (forceFancy) {
			fancyName = true;
		}

		String baseName;
		if (!fancyName) {
			baseName = fleet.getFaction().getFleetTypeName(fleetType);
		} else {
			boolean oldguardName = auxiliaryUsage > (DEFAULT_AUX_PERCENT + (RANDOM_AUX_PERCENT * 0.5f));
			WeightedRandomPicker<String> post1 = new WeightedRandomPicker<String>();
			WeightedRandomPicker<String> post2 = new WeightedRandomPicker<String>();

			// works for both: fire, zealotry, spacy things that just sound fun when combined with animals
			// also more "noble" animals and weapons
			post1.add("Abyss ");
			post1.add("Ardent ");
			post1.add("Blazing ");
			post1.add("Brazen ");
			post1.add("Dawn ");
			post1.add("Dusk ");
			post1.add("Endless ");
			post1.add("Ether ");
			post1.add("Fervent ");
			post1.add("Flame ");
			post1.add("Infinite ");
			post1.add("Hyperspace ");
			post1.add("Machine-");
			post1.add("Neutron ");
			post1.add("Night ");
			post1.add("Oathsworn ");
			post1.add("Sacrificial ");
			post1.add("Soul ");
			post1.add("Space ");
			post1.add("Star ");
			post1.add("Steel ");
			post1.add("Tachyon ");
			post1.add("Twilight ");
			post1.add("Vengeful ");
			post1.add("Void ");
			post1.add("Unbroken ");
			post1.add("Unforgiving ");
			post1.add("Unyielding ");
			post1.add("Zealous ");

			post2.add("Blades");
			post2.add("Dragons");
			post2.add("Drakes");
			post2.add("Gryphons");
			post2.add("Heralds");
			post2.add("Keepers");
			post2.add("Owls");
			post2.add("Spears");
			post2.add("Swords");
			post2.add("Watchdogs");
			post2.add("Wyrms");

			if (fleetType.equals(FleetTypes.PATROL_SMALL)) {
				post2.add("Lambs");
				post2.add("Seekers");
				post2.add("Watchers");
			} else {
				post2.add("Champions");
				post2.add("Contenders");
			}

			// fewer auxiliaries: angelic, heroic, otherworldly names
			if (!oldguardName) {
				post1.add("Astral ");
				post1.add("Celestial ");
				post1.add("Dreaming ");
				post1.add("Eminent ");
				post1.add("Ethereal ");
				post1.add("Exalted ");
				post1.add("Gallant ");
				post1.add("Light-");
				post1.add("Noble ");
				post1.add("Pale ");
				post1.add("Radiant ");
				post1.add("Righteous ");
				post1.add("Seraphic ");

				post2.add("Angels");
				post2.add("Castellans");
				post2.add("Custodians");
				post2.add("Heroes");
				post2.add("Knights");
				post2.add("Protectors");
				post2.add("Shepherds");
				post2.add("Sentinels");

				if (fleetType.equals(FleetTypes.PATROL_SMALL)) {
					post2.add("Eyes");
					post2.add("Lanterns");
				} else {
					post2.add("Defenders");
					post2.add("Guardians");
					post2.add("Knights");
					post2.add("Paladins");
					post2.add("Templars");
				}
			}
			// more auxiliaries: more fearsome, hunter, animalistic names
			else {
				post1.add("Black ");
				post1.add("Blood ");
				post1.add("Derelict ");
				post1.add("Flux ");
				post1.add("Fury ");
				post1.add("Gore ");
				post1.add("Grim ");
				post1.add("Hell ");
				post1.add("Hellfire ");
				post1.add("Iron ");
				post1.add("Rabid ");
				post1.add("Rad-");
				post1.add("Rock ");
				post1.add("Rust ");
				post1.add("Stone ");

				post2.add("Beasts");
				post2.add("Buckets");
				post2.add("Crows");
				post2.add("Daemons", 0.5f);
				post2.add("Demons", 0.5f);
				post2.add("Devils");
				post2.add("Flock");
				post2.add("Hawks");
				post2.add("Herd");
				post2.add("Hogs");
				post2.add("Hounds");
				post2.add("Pack");
				post2.add("Ravens");
				post2.add("Toasters");
				post2.add("Wolves", 2f);
				post2.add("Worms", 0.5f);
				post2.add("Wurms", 0.5f);
				post2.add("Wraiths");

				if (fleetType.equals(FleetTypes.PATROL_SMALL)) {
					post2.add("Dregs");
					post2.add("Hunters");
					post2.add("Pathfinders");
					post2.add("Rats");
					post2.add("Stalkers");
				} else {
					post2.add("Bears");
					post2.add("Breakers");
					post2.add("Contingent");
					post2.add("Crushers");
					post2.add("Horde");
					post2.add("Lions");
					post2.add("Reapers");
					post2.add("Slayers");
					post2.add("Smashers");
					post2.add("Swarm");
					post2.add("Tigers");
				}
			}
			baseName = post1.pick() + post2.pick();
		}

		fleet.setName(baseName);
		boolean useCommanderSuffix = false; // should generally be true but who knows what'll happen
		if (fleet.getCommander() != null) {
			if (fleet.getCommander().getMemoryWithoutUpdate().getString("$sotf_suffix") != null) {
				useCommanderSuffix = true;
			}
		}
		// e.g "Dustkeeper Shard", but "Lamia's Splinter" / "Relief's Hell Hounds"
		if (useCommanderSuffix) {
			fleet.setName(fleet.getCommander().getMemoryWithoutUpdate().getString("$sotf_suffix") + "'s " + fleet.getName());
			fleet.setNoFactionInName(true);
		}
		// "The Pale Eyes"
		else if (fancyName) {
			fleet.setName("The " + fleet.getName());
			fleet.setNoFactionInName(true);
		}
	}

	public static void addAuxiliaryPoints(CampaignFleetAPI fleet, Random random, float auxiliaryFP, FleetParamsV3 params) {
		WeightedRandomPicker<String> smallAuxPicker = new WeightedRandomPicker<String>(random);
		WeightedRandomPicker<String> mediumAuxPicker = new WeightedRandomPicker<String>(random);
		WeightedRandomPicker<String> largeAuxPicker = new WeightedRandomPicker<String>(random);

		smallAuxPicker.add(SotfIDs.ROLE_AUXILIARY_SMALL, auxiliaryFP);
		mediumAuxPicker.add(SotfIDs.ROLE_AUXILIARY_MEDIUM, auxiliaryFP);
		largeAuxPicker.add(SotfIDs.ROLE_AUXILIARY_LARGE, auxiliaryFP);

		Map<String, FPRemaining> auxRemaining = new HashMap<String, FPRemaining>();
		FPRemaining remAux = new FPRemaining((int)auxiliaryFP);

		auxRemaining.put(SotfIDs.ROLE_AUXILIARY_SMALL, remAux);
		auxRemaining.put(SotfIDs.ROLE_AUXILIARY_MEDIUM, remAux);
		auxRemaining.put(SotfIDs.ROLE_AUXILIARY_LARGE, remAux);

		int numFails = 0;
		while (numFails < 2) {
			int small = BASE_COUNTS_WITH_4[2][0] + random.nextInt(MAX_EXTRA_WITH_4[1][0] + 1);
			int medium = BASE_COUNTS_WITH_4[2][1] + random.nextInt(MAX_EXTRA_WITH_4[1][1] + 1);
			int large = BASE_COUNTS_WITH_4[2][2] + random.nextInt(MAX_EXTRA_WITH_4[1][2] + 1);

			int smallPre = small / 2;
			small -= smallPre;

			int mediumPre = medium / 2;
			medium -= mediumPre;

			boolean addedSomething = false;

			Set<String> empty = new HashSet<String>();
			addedSomething |= addShips(smallAuxPicker, empty, auxRemaining, null, smallPre, fleet, random, params);
			addedSomething |= addShips(mediumAuxPicker, empty, auxRemaining, null, mediumPre, fleet, random, params);
			addedSomething |= addShips(smallAuxPicker, empty, auxRemaining, null, small, fleet, random, params);
			addedSomething |= addShips(largeAuxPicker, empty, auxRemaining, null, large, fleet, random, params);
			addedSomething |= addShips(mediumAuxPicker, empty, auxRemaining, null, medium, fleet, random, params);

			if (!addedSomething) {
				numFails++;
			}
		}
	}
}
