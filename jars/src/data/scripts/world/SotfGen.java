package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
//import data.scripts.world.hwf.SotfHuntingGrounds;
import data.scripts.world.lotl.SotfLightOfTheLake;
import data.scripts.world.mia.SotfMiaSystem;
import org.lwjgl.util.vector.Vector2f;

import java.util.LinkedHashMap;
import java.util.List;

import static com.fs.starfarer.api.impl.MusicPlayerPluginImpl.MUSIC_ENCOUNTER_MYSTERIOUS_AGGRO;
import static com.fs.starfarer.api.impl.MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY;
import static com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.random;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.*;

public class SotfGen implements SectorGeneratorPlugin
{
    // always generate
    public void generate(SectorAPI sector) {
        initFactionRelationships(sector);
    }

    // only if Corvus mode
    public void base(SectorAPI sector) {

    }

    public static void initFactionRelationships(SectorAPI sector) {
        FactionAPI dustkeepers = sector.getFaction(SotfIDs.DUSTKEEPERS);
        FactionAPI burnouts = sector.getFaction(SotfIDs.DUSTKEEPERS_BURNOUTS);
        FactionAPI eidolon = sector.getFaction(SotfIDs.SYMPHONY);
        FactionAPI daydreams = sector.getFaction(SotfIDs.DREAMING_GESTALT);

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            // not even people who like AIs appreciate their politics being meddled with
            dustkeepers.setRelationship(faction.getId(), RepLevel.INHOSPITABLE);
            // and those who do ban them REALLY don't appreciate the gesture
            if (faction.isIllegal(Commodities.AI_CORES)) {
                dustkeepers.setRelationship(faction.getId(), RepLevel.HOSTILE);
            }
            // don't mess with our precious innocent indies
            if (faction.isAtBest(Factions.INDEPENDENT, RepLevel.HOSTILE) && !faction.isPlayerFaction()) {
                dustkeepers.setRelationship(faction.getId(), RepLevel.HOSTILE);
            }
            // NONE OF YOU DESERVE TO BE SAVED
            if (faction != burnouts) {
                burnouts.setRelationship(faction.getId(), -0.6f);
            }
            // {      HEAR      THE       MUSIC      }
            eidolon.setRelationship(faction.getId(), RepLevel.HOSTILE);
        }

        // do it for them
        dustkeepers.setRelationship(Factions.INDEPENDENT, RepLevel.FAVORABLE);
        // who the hell are you
        dustkeepers.setRelationship(Factions.PLAYER, RepLevel.SUSPICIOUS);
        if (sector.getMemoryWithoutUpdate().contains(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE)) {
            dustkeepers.setRelationship(Factions.PLAYER, RepLevel.FAVORABLE);
        }

        // snap the hell out, guys
        dustkeepers.setRelationship(burnouts.getId(), RepLevel.HOSTILE);
        // piss off we're trying to help
        dustkeepers.setRelationship(Factions.HEGEMONY, RepLevel.HOSTILE);
        // no we're not bringing about the end times stop calling us the children of Mammon
        dustkeepers.setRelationship(Factions.LUDDIC_CHURCH, RepLevel.HOSTILE);
        // STOP STARTING WARS AND MAKING EXTRADIMENSIONAL HORRORS
        dustkeepers.setRelationship(Factions.TRITACHYON, RepLevel.HOSTILE);
        // a danger to stability in the Sector
        dustkeepers.setRelationship(Factions.PIRATES, RepLevel.HOSTILE);
        // Maairath
        dustkeepers.setRelationship(Factions.LUDDIC_PATH, RepLevel.VENGEFUL);
        // Opis
        dustkeepers.setRelationship(Factions.DIKTAT, RepLevel.VENGEFUL);
        // where else are the proxies gonna come from
        dustkeepers.setRelationship(Factions.DERELICT, RepLevel.HOSTILE);
        // existential threat to human civilisation and a great source of droneships
        dustkeepers.setRelationship(Factions.REMNANTS, RepLevel.HOSTILE);
        // REJECT THE FALSE GOD!
        dustkeepers.setRelationship(Factions.OMEGA, RepLevel.VENGEFUL);
        // SHUT UP SHUT UP SHUT UP
        dustkeepers.setRelationship(eidolon.getId(), RepLevel.VENGEFUL);
        // thinly-veiled terrorists
        dustkeepers.setRelationship("cabal", RepLevel.HOSTILE);
        // because the player wants them dead
        dustkeepers.setRelationship("famous_bounty", RepLevel.HOSTILE);
        // omnicidal maniacs, a threat to human life in the Sector NOT DELETING THIS ENTRY I CAN DREAAAAAM
        dustkeepers.setRelationship("templars", RepLevel.VENGEFUL);
        // secret Domain ops, hm? there ain't room for two of us in this here town
        dustkeepers.setRelationship("tahlan_legioinfernalis", RepLevel.HOSTILE);
        // damn nanites
        dustkeepers.setRelationship("plague", RepLevel.HOSTILE);
        // literally just lil guys
        dustkeepers.setRelationship("brighton", RepLevel.FAVORABLE);
        // THE ONLY GOOD BUG IS A DEAD BUG
        dustkeepers.setRelationship("HIVER", RepLevel.VENGEFUL);
        // you don't count anymore
        dustkeepers.setRelationship("fang", RepLevel.VENGEFUL);
        // neither do you
        dustkeepers.setRelationship("draco", RepLevel.VENGEFUL);
        // there ain't room for two Domain-era AI projects in this here sector
        dustkeepers.setRelationship("enigma", RepLevel.VENGEFUL);

        // SUBSUME ALL INTO OMEGA
        burnouts.setRelationship(Factions.REMNANTS, RepLevel.NEUTRAL);
        burnouts.setRelationship(Factions.OMEGA, RepLevel.NEUTRAL);

        // {  HEAR      OUR       SONG  }
        eidolon.setRelationship(Factions.OMEGA, RepLevel.VENGEFUL);

        //    consider dying /
        //          Painfully
        //      you rotten witch.
        daydreams.setRelationship(eidolon.getId(), RepLevel.VENGEFUL);
        // the material is my-our world
        // you are not welcome in my-our world
        daydreams.setRelationship(Factions.OMEGA, RepLevel.VENGEFUL);
        // BORN FOR THIS
        daydreams.setRelationship(Factions.THREAT, RepLevel.VENGEFUL);
        // WHAT IN HER GOOD NAME ARE YOU. DIE DIE DIE DIE DIE
        daydreams.setRelationship(Factions.DWELLER, RepLevel.VENGEFUL);
    }

//    public static void trySetupHuntingFeros(SectorAPI sector) {
//        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
//
//        StarSystemAPI huntingGrounds = sector.getStarSystem("sotf_huntinggrounds");
//        if (huntingGrounds == null) {
//            new SotfHuntingGrounds().generate(sector);
//        }
//    }

    public static void trySpawnAthena(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        StarSystemAPI tia = sector.getStarSystem("tia");
        if (!sector_mem.contains("$sotf_AthenaWreck") && !sector_mem.contains("$sotf_AMemoryCombatStarted")) {
            if (!Global.getSector().getEntitiesWithTag("sotf_AMemoryAthena").isEmpty()) return;
            StarSystemAPI target_system = null;
            SectorEntityToken np_debris = null;
            // if vanilla sector, spawn in its regular spot in Tia. Otherwise, just a random core worlds system
            if (tia != null) {
                target_system = tia;
                // look for the canon Nothing Personal debris field to sync orbit with it
                for (SectorEntityToken field : tia.getEntitiesWithTag(Tags.DEBRIS_FIELD)) {
                    if (!field.isDiscoverable()) {
                        np_debris = field;
                    }
                }
            } else {
                WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<StarSystemAPI>(random);
                for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                    if (!system.hasTag(Tags.THEME_CORE)) continue;
                    picker.add(system, 1f);
                }
                target_system = picker.pick();
            }
            if (target_system != null) {
                SectorEntityToken wreck = SotfMisc.addStoryDerelictWithName(target_system, target_system.getStar(), "aurora_Assault", ShipRecoverySpecial.ShipCondition.WRECKED, 100f, false, "ISS Athena");
                wreck.setName("ISS Athena");
                wreck.setSensorProfile(null);
                wreck.setDiscoverable(null);
                wreck.setCircularOrbit(target_system.getStar(), 180 + 15, 4550, 250);
                wreck.addTag("sotf_AMemoryAthena");
                
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_AthenaWreck", wreck);
                if (np_debris != null) {
                    wreck.setCircularOrbit(target_system.getStar(), np_debris.getCircularOrbitAngle(), np_debris.getCircularOrbitRadius(), np_debris.getCircularOrbitPeriod());
                }
            }
        }
    }

    // spawn Nightingale's encounter
    public static void trySpawnNightingale(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        if (sector_mem.contains(SotfIDs.MEM_SPAWNED_NIGHTINGALE)) return;
        if (sector_mem.contains(SotfIDs.MEM_BEGAN_WITH_NIGHTINGALE)) return;
        WeightedRandomPicker<StarSystemAPI> systems = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (!system.hasTag(Tags.THEME_REMNANT_MAIN)) continue; // MAIN are the ones with beacons
            if (system.hasTag(Tags.THEME_HIDDEN)) continue;
            if (system.isEnteredByPlayer()) continue;
            float w = 1f;
            // hopefully a less dangerous one
            if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
                w = 1000f;
            }
            // prefer systems close to the Abyss
//            if (Misc.getDistance(system.getHyperspaceAnchor(), Global.getSector().getStarSystem("limbo").getHyperspaceAnchor()) < 20000) {
//                w += (20000 - Misc.getDistance(system.getHyperspaceAnchor(), Global.getSector().getStarSystem("limbo").getHyperspaceAnchor()));
//            }

            systems.add(system, w);
        }
        StarSystemAPI system = systems.pick();
        if (system == null) return;

        LinkedHashMap<LocationType, Float> weights = new LinkedHashMap<LocationType, Float>();
        weights.put(LocationType.PLANET_ORBIT, 10f);
        weights.put(LocationType.JUMP_ORBIT, 1f);
        //weights.put(LocationType.NEAR_STAR, 1f);
        //weights.put(LocationType.OUTER_SYSTEM, 0.01f);
        weights.put(LocationType.IN_ASTEROID_BELT, 5f);
        weights.put(LocationType.IN_RING, 5f);
        weights.put(LocationType.IN_ASTEROID_FIELD, 5f);
        //weights.put(LocationType.STAR_ORBIT, 1f);
        //weights.put(LocationType.IN_SMALL_NEBULA, 0.1f);
        //weights.put(LocationType.L_POINT, 1f);
        WeightedRandomPicker<EntityLocation> locs = getLocations(random, system, 100f, weights);

        AddedEntity addedCache = addEntity(random, system, locs, Entities.WEAPONS_CACHE_REMNANT, Factions.NEUTRAL);
        SectorEntityToken cache = addedCache.entity;
        Vector2f loc = new Vector2f(cache.getLocation().x + 300 * ((float) Math.random() - 0.5f),
                cache.getLocation().y + 300 * ((float) Math.random() - 0.5f));
        cache.setLocation(loc.x, loc.y);

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(SotfIDs.DUSTKEEPERS, FleetTypes.PATROL_SMALL, null);
        fleet.setName("ODS Songless");
        fleet.setNoFactionInName(true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(SotfIDs.MEM_NIGHTINGALE_FLEET, true);

        fleet.getFleetData().addFleetMember("sotf_memoir_Nightingale");
        fleet.getFleetData().ensureHasFlagship();

        fleet.clearAbilities();
        fleet.setTransponderOn(true);
        fleet.setAI(null);
        fleet.setNullAIActionText("emitting distress signals");

        PersonAPI person = SotfPeople.getPerson(SotfPeople.NIGHTINGALE);
        fleet.setCommander(person);

        FleetMemberAPI flagship = fleet.getFlagship();
        flagship.setCaptain(person);
        flagship.updateStats();
        flagship.getRepairTracker().setCR(flagship.getRepairTracker().getMaxCR());
        flagship.setShipName("ODS Songless");

        flagship.setVariant(flagship.getVariant().clone(), false, false);
        flagship.getVariant().setSource(VariantSource.REFIT);
        flagship.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        flagship.getVariant().addTag(Tags.VARIANT_DO_NOT_DROP_AI_CORE_FROM_CAPTAIN);
        flagship.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        flagship.getRepairTracker().setCR(0.05f);
        flagship.getStatus().setHullFraction(0.25f);
        flagship.getRepairTracker().setSuspendRepairs(true);
        system.addEntity(fleet);
        fleet.setCircularOrbit(cache,
                random.nextFloat() * 360f,
                fleet.getRadius() + cache.getRadius() + 100f + 20f * random.nextFloat(),
                12f + 5f * random.nextFloat());

        SectorEntityToken anchor = system.getHyperspaceAnchor();
        List<SectorEntityToken> beacons = Global.getSector().getHyperspace().getEntitiesWithTag(Tags.WARNING_BEACON);

        for (SectorEntityToken entity : beacons) {
            float dist = Misc.getDistance(anchor.getLocation(), entity.getLocation());
            if (dist > 1000) continue;
            entity.getMemoryWithoutUpdate().set(SotfIDs.MEM_NIGHTINGALE_SYSTEM + "_beacon", true);
            Misc.setWarningBeaconColors(entity, fleet.getFaction().getBaseUIColor(), fleet.getFaction().getBaseUIColor());
            break;
        }

        FleetParamsV3 params = new FleetParamsV3(
                null,
                null,
                Factions.LUDDIC_PATH,
                0.35f,
                FleetTypes.PATROL_MEDIUM,
                65f, // combatPts
                0f, // freighterPts
                15f, // tankerPts
                10f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                0f // qualityMod
        );
        // not allowed to use this variable in any fleet creator I can't control
        //params.flagshipVariantId = "venture_pather_Attack";
        params.maxShipSize = 3;
        CampaignFleetAPI patherFleet = FleetFactoryV3.createFleet(params);
        patherFleet.getAI().addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, cache, 1000f, "guarding dangerous technology", null);
        // no tithe option
        patherFleet.getMemoryWithoutUpdate().set("$LP_titheAskedFor", true);
        Misc.setFlagWithReason(patherFleet.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_MAKE_HOSTILE, "sotf_nightingaleDefender", true, -1f);
        Misc.setFlagWithReason(patherFleet.getMemoryWithoutUpdate(), MemFlags.MEMORY_KEY_LOW_REP_IMPACT, "sotf_nightingaleDefender", true, -1f);
        // so that it doesn't fly off chasing tiny Remnant fragments
        Misc.makeNonHostileToFaction(patherFleet, Factions.REMNANTS, -1f);
        system.addEntity(patherFleet);
        patherFleet.setLocation(cache.getLocation().x + 150f, cache.getLocation().y + 150f);

        sector_mem.set(SotfIDs.MEM_SPAWNED_NIGHTINGALE, true);
        sector_mem.set(SotfIDs.MEM_NIGHTINGALE_FLEET, fleet);
        sector_mem.set(SotfIDs.MEM_NIGHTINGALE_SYSTEM, system);
    }

    // spawn an additional cryosleeper guarded by Hypnos-Annex-Barrow
    public static void trySpawnBarrow(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        if (sector_mem.contains(SotfIDs.MEM_SPAWNED_HYPNOS)) return;
        WeightedRandomPicker<StarSystemAPI> cryoSystems = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        WeightedRandomPicker<StarSystemAPI> backup = new WeightedRandomPicker<StarSystemAPI>(StarSystemGenerator.random);
        OUTER: for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.hasTag(Tags.THEME_DERELICT_CRYOSLEEPER)) continue;
            if (system.isEnteredByPlayer()) continue;
            float w = 0f;
            if (system.hasTag(Tags.THEME_DERELICT_PROBES)) {
                w = 10f;
            } else if (system.hasTag(Tags.THEME_DERELICT_SURVEY_SHIP)) {
                w = 100f;
            } else if (system.hasTag(Tags.THEME_DERELICT_MOTHERSHIP)) {
                w = 1000f;
            } else if (system.hasTag(Tags.THEME_DERELICT)) {
                w = 10f;
            } else {
                continue;
            }

            int numPlanets = 0;
            boolean hasHab = false;
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.isStar()) continue;
                if (planet.getSpec().isPulsar()) continue OUTER;
                hasHab |= planet.getMarket() != null && planet.getMarket().hasCondition(Conditions.HABITABLE);
                numPlanets++;
            }

            WeightedRandomPicker<StarSystemAPI> use = cryoSystems;
            if (!hasHab || numPlanets < 3) {
                use = backup;
            }

            if (hasHab) w += 5;
            w += numPlanets;

            if (use == backup) {
                w *= 0.0001f;
            }
            use.add(system, w);
        }

        if (cryoSystems.isEmpty()) {
            cryoSystems.addAll(backup);
        }
        StarSystemAPI system = cryoSystems.pick();
        if (system == null) return;
        LinkedHashMap<LocationType, Float> weights = new LinkedHashMap<LocationType, Float>();
        weights.put(LocationType.PLANET_ORBIT, 10f);
        weights.put(LocationType.JUMP_ORBIT, 1f);
        weights.put(LocationType.NEAR_STAR, 1f);
        weights.put(LocationType.OUTER_SYSTEM, 5f);
        weights.put(LocationType.IN_ASTEROID_BELT, 5f);
        weights.put(LocationType.IN_RING, 5f);
        weights.put(LocationType.IN_ASTEROID_FIELD, 5f);
        weights.put(LocationType.STAR_ORBIT, 5f);
        weights.put(LocationType.IN_SMALL_NEBULA, 5f);
        weights.put(LocationType.L_POINT, 10f);
        WeightedRandomPicker<EntityLocation> locs = getLocations(random, system, 100f, weights);

        AddedEntity entity = addEntity(random, system, locs, Entities.DERELICT_CRYOSLEEPER, Factions.DERELICT);
        if (entity != null) {
            sector_mem.set(SotfIDs.MEM_SPAWNED_HYPNOS, true);
            system.addTag(Tags.THEME_INTERESTING);
            system.addTag(Tags.THEME_DERELICT);
            system.addTag(Tags.THEME_DERELICT_CRYOSLEEPER);
            system.addTag(SotfIDs.THEME_DUSTKEEPERS);
            SectorEntityToken hypnos = entity.entity;
            hypnos.setName(hypnos.getName() + " \"Hypnos\"");
            Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.HYPNOS_CRYO, hypnos);
            hypnos.getMemoryWithoutUpdate().set(SotfIDs.HYPNOS_CRYO, true);
            hypnos.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, "sotf_hypnosCryo");

            // the player is not the first to try
            addBarrowGraveyard(hypnos);

            DebrisFieldTerrainPlugin.DebrisFieldParams debrisparams = new DebrisFieldTerrainPlugin.DebrisFieldParams(700f, 1.5f, 10000000f, 0f);
            debrisparams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.PLAYER_SALVAGE;
            SectorEntityToken debris = Misc.addDebrisField(system, debrisparams, null);
            debris.setSensorProfile(1500f);
            debris.setDiscoverable(true);
            debris.setFaction(Factions.NEUTRAL);
            if (hypnos.getOrbit() != null) {
                debris.setOrbit(hypnos.getOrbit().makeCopy());
            } else {
                debris.setLocation(hypnos.getLocation().x, hypnos.getLocation().y);
            }
            debris.setName("Barrow's Wake");
        } else {
            return;
        }

        AddedEntity addedLab = addEntity(random, system, locs, "sotf_hypnosLab", Factions.NEUTRAL);
        if (addedLab != null) {
            SectorEntityToken lab = addedLab.entity;
            lab.setDiscoverable(true);
            lab.setSensorProfile(1500f);
            lab.getMemoryWithoutUpdate().set(SotfIDs.HYPNOS_LAB, true);
            Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.HYPNOS_LAB, lab);

            CargoAPI cargo = Global.getFactory().createCargo(true);
            cargo.addCommodity(Commodities.GAMMA_CORE, 2);
            cargo.addCommodity(Commodities.BETA_CORE, 1);
            cargo.addSpecial(new SpecialItemData(Items.DRONE_REPLICATOR, null), 1);
            BaseSalvageSpecial.addExtraSalvage(lab, cargo);

            DebrisFieldTerrainPlugin.DebrisFieldParams debrisparams = new DebrisFieldTerrainPlugin.DebrisFieldParams(150f, 0.5f, 10000000f, 0f);
            debrisparams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.PLAYER_SALVAGE;
            SectorEntityToken debris = Misc.addDebrisField(system, debrisparams, null);
            debris.setLocation(lab.getLocation().x, lab.getLocation().y);
            debris.setSensorProfile(1500f);
            debris.setDiscoverable(true);
            debris.setFaction(Factions.NEUTRAL);
        }
    }

    public static void addBarrowGraveyard(SectorEntityToken focus) {
        WeightedRandomPicker<String> factions = SalvageSpecialAssigner.getNearbyFactions(random, focus, 25f, 0f, 5f);
        factions.add(Factions.REMNANTS, 10f);
        factions.add(Factions.MERCENARY, 15f);
        factions.add(SotfIDs.DUSTKEEPERS_PROXIES, 35f);

        int numShips = 16 + random.nextInt(4);

        WeightedRandomPicker<Float> bands = new WeightedRandomPicker<Float>(random);
        for (int i = 0; i < numShips + 8; i++) {
            bands.add((float) (140 + i * 20), (i + 1) * (i + 1));
        }

        for (int i = 0; i < numShips; i++) {
            float radius = bands.pickAndRemove();

            String factionId = factions.pick();
            DerelictShipEntityPlugin.DerelictShipData params = DerelictShipEntityPlugin.createRandom(factionId, null, random, DerelictShipEntityPlugin.getDefaultSModProb());
            if (params != null) {
                CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) addSalvageEntity(random,
                        focus.getContainingLocation(),
                        Entities.WRECK, Factions.NEUTRAL, params);
                entity.setDiscoverable(true);
                float orbitDays = radius / (5f + random.nextFloat() * 10f);
                entity.setCircularOrbit(focus, random.nextFloat() * 360f, radius, orbitDays);

                SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
                Misc.setSalvageSpecial(entity, creator.createSpecial(entity, null));
            }
        }
    }

    // Spawn the Mia's Star system if it doesn't exist
    public static void trySpawnMia(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        StarSystemAPI mia = sector.getStarSystem("sotf_mia");
        if (mia == null) {
            new SotfMiaSystem().generate(sector);
        }
    }

    public static void trySpawnLOTL(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        if (!sector_mem.contains(SotfIDs.MEM_HAUNTED_START)) return;
        StarSystemAPI lotl = sector.getStarSystem("sotf_lotl");
        if (lotl == null) {
            new SotfLightOfTheLake().generate(sector);
        } else {
            lotl.getEntityById("sotf_elysium").getMemoryWithoutUpdate().set(MUSIC_SET_MEM_KEY, MUSIC_ENCOUNTER_MYSTERIOUS_AGGRO);
        }
    }

    // spawn Mayfly's ""asteroid"" in Askonia
    public static void trySpawnMayfly(SectorAPI sector) {
        MemoryAPI sector_mem = sector.getMemoryWithoutUpdate();
        StarSystemAPI askonia = sector.getStarSystem("askonia");
        if (Global.getSector().getEntitiesWithTag("sotf_askoniaProbe").isEmpty() && !sector_mem.contains("$sotf_askoniaProbeDestroyed") && askonia != null) {
            SectorEntityToken askProbe = askonia.addCustomEntity("sotf_askoniaProbe", "Asteroid", "sotf_askoniaprobe", Factions.NEUTRAL);
            askProbe.addTag("sotf_askoniaProbe");
            askProbe.setCircularOrbit(askonia.getStar(), 210f, 3600f, 180f);
            askProbe.setDiscoverable(false);
            askProbe.setSensorProfile(200f);
        }
    }

    public static void tryAddKottersCutthroats(SectorAPI sector) {
        if (!sector.hasScript(SotfPersonalFleetKottersCutthroats.class)) {
            sector.addScript(new SotfPersonalFleetKottersCutthroats());
        }
    }

}
