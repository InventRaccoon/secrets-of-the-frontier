package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.SimulatorPluginImpl;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.academy.GAProjectZiggurat;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.scripts.campaign.customstart.SotfHauntedDreamCampaignVFX;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfFelLeashAssignmentAI;
import data.scripts.utils.SotfMisc;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.ids.MemFlags.*;

/**
 *	LIGHT OF THE LAKE: HAUNTED FINALE
 */

public class SotfHauntedFinale extends HubMissionWithSearch implements FleetEventListener {

    public static enum Stage {
        SCENE_OF_THE_CRIME,
        PAY_YOUR_PENANCE,
        FIND_THE_LIGHT,
        FACE_YOUR_TORMENTOR,
        ENTER_ELYSIUM,
        SACRIFICE_YOURSELF,
        COMPLETED,
    }

    // player pre-game satbombed planet and its system
    protected PlanetAPI planet;
    protected StarSystemAPI system;
    protected SectorEntityToken killa;
    // Light of the Lake
    protected StarSystemAPI lotl;

    // run when the bar event starts / when we ask a contact about the mission
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        setName("The Light of the Lake");
        setStoryMission();
        setRepFactionChangesNone();
        setRepPersonChangesNone();
        completedKey = "$sotf_haunted_completed"; // this ends the game tho

        StarSystemAPI yma =  Global.getSector().getStarSystem("yma");
        for (SectorEntityToken curr : yma.getEntitiesWithTag(Tags.LUDDIC_SHRINE)) {
            killa = curr;
            break;
        }
        if (killa == null) return false;

        resetSearch();
        preferSystemTags(ReqMode.NOT_ANY, Tags.THEME_REMNANT_MAIN, Tags.THEME_REMNANT_SECONDARY);
        requirePlanetUnpopulated();
        requirePlanetWithRuins();
        preferPlanetNotFullySurveyed();
        preferPlanetUnexploredRuins();
        // everyone's dead
        preferPlanetConditions(ReqMode.NOT_ANY, Conditions.DECIVILIZED);
        planet = pickPlanet();

        if (planet == null) return false;
        system = planet.getStarSystem();
        if (system == null) return false;

        lotl = Global.getSector().getStarSystem("sotf_lotl");
        if (lotl == null) return false;

        if (!setGlobalReference("$sotf_haunted_ref")) return false;

        // set our starting, success and failure stages
        setStartingStage(Stage.SCENE_OF_THE_CRIME);
        setSuccessStage(Stage.COMPLETED);
        setNoAbandon();

        makeImportant(planet, "$sotf_haunted", Stage.SCENE_OF_THE_CRIME);
        makeImportant(killa, "$sotf_haunted", Stage.PAY_YOUR_PENANCE);
        makeImportant(lotl.getEntityById("sotf_elysium"), "$sotf_haunted", Stage.ENTER_ELYSIUM, Stage.SACRIFICE_YOURSELF);

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.PAY_YOUR_PENANCE, "$sotf_haunted_gotokilla");
        setStageOnGlobalFlag(Stage.FIND_THE_LIGHT, "$sotf_haunted_gotolotl");
        setStageOnEnteredLocation(Stage.FACE_YOUR_TORMENTOR, lotl);
        setStageOnGlobalFlag(Stage.ENTER_ELYSIUM, "$sotf_haunted_gotoelysium");
        setStageOnGlobalFlag(Stage.SACRIFICE_YOURSELF, "$sotf_haunted_sacrifice");
        setStageOnGlobalFlag(Stage.COMPLETED, "$sotf_haunted_completed");
        return true;
    }

    @Override
    protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

    }

    @Override
    public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.acceptImpl(dialog, memoryMap);

        spawnFleet();

        //addFelComposition(fleetData, corePicker.pickAndRemove());
    }

    public static void spawnFleet() {
        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(SotfIDs.DREAMING_GESTALT, FleetTypes.PATROL_LARGE, null);
        fleet.setInflater(null); // no autofit tyvm
        fleet.setNoFactionInName(true);
        fleet.setName("The Revenant Host");
        fleet.getMemoryWithoutUpdate().set(MEMORY_KEY_NO_SHIP_RECOVERY, true);
        fleet.getMemoryWithoutUpdate().set(FLEET_IGNORED_BY_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(FLEET_IGNORES_OTHER_FLEETS, true);
        fleet.getMemoryWithoutUpdate().set(FLEET_DO_NOT_IGNORE_PLAYER, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        //fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true);
        fleet.getMemoryWithoutUpdate().set("$sotf_haunted_felFleet", true);

        fleet.setCommander(SotfPeople.getPerson(SotfPeople.FEL));

        if (!SotfMisc.isSecondInCommandEnabled()) {
            PersonAPI fel = fleet.getCommander();
            // yeah this is probably too much
//            fel.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1f);
//            fel.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1f);
//            fel.getStats().setSkillLevel(Skills.PHASE_CORPS, 1f);
//            fel.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1f);
//            fel.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1f);
        }

        FleetDataAPI fleetData = fleet.getFleetData();

        WeightedRandomPicker<String> corePicker = new WeightedRandomPicker<String>(Misc.random);
        corePicker.add("decisive_battle");
        corePicker.add("hit_and_run");
        corePicker.add("unrelenting");

        String pick1 = corePicker.pickAndRemove();
        String pick2 = corePicker.pickAndRemove();
        String pick3 = corePicker.pickAndRemove();

        addFelComposition(fleetData, pick1);
        addFelComposition(fleetData, pick2);
        addFelComposition(fleetData, pick3);

        if (Global.getSettings().isDevMode()) {
            Global.getSector().getCampaignUI().addMessage("Fel core comp picks: " + pick1 + " + " + pick2 + " + " + "pick3");
        }

        fleetData.sort();
        fleetData.syncIfNeeded();

        LocationAPI lotl = Global.getSector().getStarSystem("sotf_lotl");
        SectorEntityToken elysium = lotl.getEntityById("sotf_elysium");
        lotl.addEntity(fleet);
        fleet.setLocation(elysium.getLocation().x, elysium.getLocation().y + 100f);
        // make sure they can't be sneaked past although you do need to fight them anyway
        fleet.getStats().getFleetwideMaxBurnMod().modifyMult("sotf_fel", 3f);
        fleet.getStats().getAccelerationMult().modifyMult("sotf_fel", 3f);
        fleet.getStats().getSensorRangeMod().modifyMult("sotf_fel", 2f);
        fleet.addScript(new SotfFelLeashAssignmentAI(fleet, elysium));
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new SotfFelFIDConfigGen());
//        CargoAPI extraLoot = Global.getFactory().createCargo(true);
//        SpecialItemData item = WormholeManager.createWormholeAnchor("sotf_lotl", "sotf_styx", "Styx");
//        extraLoot.addSpecial(item, 1f);
//        BaseSalvageSpecial.addExtraSalvage(fleet, extraLoot);
        Misc.addDefeatTrigger(fleet, "sotfHauntedBeatFel");

        Misc.makeImportant(fleet, "$sotf_haunted");
    }

    public static void addFelComposition(FleetDataAPI fleetData, String comp) {
        boolean hard = SotfMisc.getHFinaleHardMode();
        switch (comp) {
            case "decisive_battle":
                addFelMember(fleetData, "onslaught_xiv_Elite", Personalities.AGGRESSIVE, SotfIDs.SKILL_DEARDOTTY);

                addFelMember(fleetData, "dominator_Assault", Personalities.AGGRESSIVE, SotfIDs.SKILL_SCRAPSCREEN);
                //addFelMember(fleetData, "dominator_Assault", Personalities.AGGRESSIVE);
                addFelMember(fleetData, "mora_Torpedo", Personalities.CAUTIOUS, SotfIDs.SKILL_WYRMFIRE);
                //addFelMember(fleetData, "mora_Torpedo", Personalities.CAUTIOUS);

                addFelMember(fleetData, "enforcer_XIV_Elite", Personalities.STEADY, SotfIDs.SKILL_HELLIONSHELLHIDE);
                addFelMember(fleetData, "enforcer_XIV_Elite", Personalities.STEADY, SotfIDs.SKILL_HELLIONSHELLHIDE);
                addFelMember(fleetData, "manticore_Balanced", Personalities.STEADY, SotfIDs.SKILL_MANTLEOFTHORNS);
                //addFelMember(fleetData, "manticore_Balanced", Personalities.STEADY);

                if (hard) {
                    addFelMember(fleetData, "dominator_AntiCV", Personalities.AGGRESSIVE);
                    addFelMember(fleetData, "mora_Torpedo", Personalities.CAUTIOUS);
                    addFelMember(fleetData, "manticore_Balanced", Personalities.STEADY);
                }
                break;
            case "hit_and_run":
                addFelMember(fleetData, "sotf_respite_Assault", Personalities.AGGRESSIVE, SotfIDs.SKILL_JUBILANTSIREN);

                //addFelMember(fleetData, "aurora_Attack", Personalities.RECKLESS, SotfIDs.SKILL_ATRICKSTERSCALLING);
//                if (!hard) {
//                    addFelMember(fleetData, "fury_Attack", Personalities.AGGRESSIVE, SotfIDs.SKILL_ATRICKSTERSCALLING);
//                } else {
//                    addFelMember(fleetData, "doom_Strike", Personalities.AGGRESSIVE, SotfIDs.SKILL_ATRICKSTERSCALLING);
//                }

                addFelMember(fleetData, "doom_Strike", Personalities.AGGRESSIVE, SotfIDs.SKILL_ATRICKSTERSCALLING);

                addFelMember(fleetData, "sotf_medusa_Hybrid", Personalities.STEADY, SotfIDs.SKILL_TICKTOCK);
                addFelMember(fleetData, "sotf_medusa_Hybrid", Personalities.STEADY, SotfIDs.SKILL_INSACRIFICEMEANING);

                addFelMember(fleetData, "sotf_wolf_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_LEVIATHANSBANE);
                addFelMember(fleetData, "sotf_wolf_Overdriven", Personalities.RECKLESS);
                addFelMember(fleetData, "sotf_wolf_Overdriven", Personalities.RECKLESS);
                //addFelMember(fleetData, "sotf_wolf_Overdriven", Personalities.RECKLESS);
                if (hard) {
                    addFelMember(fleetData, "aurora_Attack", Personalities.RECKLESS, SotfIDs.SKILL_TICKTOCK);
                    addFelMember(fleetData, "sotf_wolf_Overdriven", Personalities.RECKLESS);
                }
                break;
            case "unrelenting":
                //addFelMember(fleetData, "sotf_repose_Steadfast", Personalities.RECKLESS, SotfIDs.SKILL_SCRAPSCREEN);

                //addFelMember(fleetData, "sotf_eradicator_Overdriven", Personalities.AGGRESSIVE);
//                if (!hard) {
//                    addFelMember(fleetData, "eradicator_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_WYRMFIRE);
//                } else {
//                    // no shield shunt
//                    addFelMember(fleetData, "sotf_eradicator_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_WYRMFIRE);
//                }
                addFelMember(fleetData, "sotf_eradicator_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_WYRMFIRE);

                addFelMember(fleetData, "vanguard_Strike", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "vanguard_Strike", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "vanguard_Strike", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "vanguard_Strike", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "sotf_warden_aux_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "sotf_warden_aux_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);
                addFelMember(fleetData, "sotf_warden_aux_Overdriven", Personalities.RECKLESS, SotfIDs.SKILL_HATREDBEYONDDEATH);

                if (hard) {
                    addFelMember(fleetData, "sotf_repose_Steadfast", Personalities.RECKLESS, SotfIDs.SKILL_SCRAPSCREEN);
                    addFelMember(fleetData, "sotf_eradicator_Overdriven", Personalities.AGGRESSIVE);
                }
                break;
            // ADAPTIVE PICKS
            case "omega":
                addFelMember(fleetData, "tesseract_Strike", Personalities.RECKLESS);
                break;
            case "eidolon":
                addFelMember(fleetData, "sotf_pact_eidolon_Fervor", Personalities.RECKLESS);
                break;
            case "capital_hunters":
                addFelMember(fleetData, "hyperion_Attack", Personalities.AGGRESSIVE, SotfIDs.SKILL_LEVIATHANSBANE);
                if (hard) {
                    addFelMember(fleetData, "hyperion_Strike", Personalities.AGGRESSIVE, SotfIDs.SKILL_LEVIATHANSBANE);
                }
                break;
            case "pointdefense":
                addFelMember(fleetData, "anubis_Standard", Personalities.STEADY, SotfIDs.SKILL_GROVETENDER, SotfIDs.SKILL_MANTLEOFTHORNS);
                if (hard) {
                    addFelMember(fleetData, "omen_PD", Personalities.STEADY, SotfIDs.SKILL_GROVETENDER);
                    addFelMember(fleetData, "omen_PD", Personalities.STEADY, SotfIDs.SKILL_GROVETENDER);
                }
                break;
        }
    }

    public static void addFelMember(FleetDataAPI fleetData, String variantId, String personality, String... skills) {
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
        boolean hard = SotfMisc.getHFinaleHardMode();

        if (variantId.contains("tesseract")) {
            PersonAPI captain = Misc.getAICoreOfficerPlugin(Commodities.OMEGA_CORE).createPerson(Commodities.OMEGA_CORE, Factions.OMEGA, Misc.random);
            member.setCaptain(captain);
            member.setShipName(Global.getSector().getFaction(Factions.OMEGA).pickRandomShipName());
            fleetData.addFleetMember(member);
            member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
            return;
        }
        if (variantId.contains("eidolon")) {
            PersonAPI captain = SotfPeople.getPerson(SotfPeople.EIDOLON);
            member.setCaptain(captain);
            member.setShipName("Pact");
            member.getVariant().addPermaMod(SotfIDs.PHANTASMAL_SHIP);
            fleetData.addFleetMember(member);
            // hack: delete from fleet if banished bcs phantasmal ships sometimes don't die properly
            member.getVariant().addTag("sotf_delete_from_fleet");
            return;
        }

        if (!Misc.isAutomated(member)) {
            member.getVariant().addPermaMod(HullMods.AUTOMATED);
        }
        member.getVariant().addPermaMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);

        member.getVariant().setVariantDisplayName(SotfMisc.pickOne("Vengeful", "Retributive", "Judgement", "Hateful", "Reckoner", "Scornful"));

        PersonAPI captain = SotfPeople.genFelSubswarm();
        captain.setPersonality(personality);
        captain.getStats().setLevel(7 + skills.length);
        for (String skill : skills) {
            captain.getStats().setSkillLevel(skill, 2f);
        }
        if (!hard) {
            // mine spam/temp shell spam is very rough
            if (variantId.contains("doom") || variantId.contains("respite")) {
                captain.getStats().setSkillLevel(SotfIDs.SKILL_SPATIALEXPERTISE, 0f);
            }
            if (variantId.contains("doom")) {
                captain.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 1f);
            }
            // this shit sure doesn't FEEL like a glass cannon
            if (variantId.contains("respite")) {
                captain.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 1f);
                //captain.getStats().setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, 1f);
            }
//            // bruh die already pls I beg you
//            if (variantId.contains("mora") || variantId.contains("onslaught") || variantId.contains("dominator")) {
//                captain.getStats().setSkillLevel(SotfIDs.SKILL_FIELDSRESONANCE, 0f);
//            }
        }
        member.setVariant(member.getVariant().clone(), false, false);
        member.getVariant().setSource(VariantSource.REFIT);
        member.setCaptain(captain);
        fleetData.addFleetMember(member);
    }

    public static class SotfFelFIDConfigGen implements FleetInteractionDialogPluginImpl.FIDConfigGen {
        public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
            FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

//			config.alwaysAttackVsAttack = true;
//			config.leaveAlwaysAvailable = true;
            config.showTransponderStatus = false;
            config.showEngageText = false;
            config.alwaysPursue = true;
            config.impactsAllyReputation = false;
            config.impactsEnemyReputation = false;
            config.printXPToDialog = true;

            config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                    new RemnantSeededFleetManager.RemnantFleetInteractionConfigGen().createConfig().delegate.
                            postPlayerSalvageGeneration(dialog, context, salvage);
                }

                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false;
                    bcc.fightToTheLast = true;
                    bcc.enemyDeployAll = true;
                }
            };
            return config;
        }
    }

    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (isDone() || result != null) return;

        if (!battle.isPlayerInvolved()) return;

        //CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (!battle.wasFleetDefeated(fleet, primaryWinner)) {
            return;
        }
    }

    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {

        }
    }

    // when Call-ing something that isn't a default option for a mission, it'll try and run this method
    // with "action" being the first parameter
    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        TextPanelAPI text = dialog.getTextPanel();
        if (action.equals("unlockLotl")){

            text.setFontSmallInsignia();
            if (SharedUnlockData.get().addToSet("sotf_persistent", "sotf_haunted_completed")) {
                SharedUnlockData.get().saveIfNeeded();
                text.addParagraph("Unlocked \"Child of the Lake\" background.", Misc.getHighlightColor());
            }
            text.setFontInsignia();

            FullName.Gender playerGender = Global.getSector().getPlayerPerson().getGender();
            String his = "his";
            if (playerGender == FullName.Gender.FEMALE) {
                his = "her";
            } else if (playerGender == FullName.Gender.ANY) {
                his = "their";
            }

            //text.addPara("With " + his + " sacrifice, the story of " + Global.getSector().getPlayerPerson().getNameString() + " has ended.", Misc.getHighlightColor(), Global.getSector().getPlayerPerson().getNameString());
            return true;
        } else if (action.equals("quickLoad")){
            dialog.dismiss();
            Global.getSector().getCampaignUI().quickLoad();
            return true;
        } else if (action.equals("endTheGame")){
            dialog.dismiss();
            Global.getSector().getCampaignUI().cmdExitWithoutSaving();
            return true;
        }
        return false;
    }



    protected void updateInteractionDataImpl() {
        set("$sotf_haunted_stage", getCurrentStage());
        set("$sotf_haunted_satbombPlanet", planet.getId());
    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        int storyPointsRequired = 5;
        if (currentStage == Stage.SCENE_OF_THE_CRIME) {
            storyPointsRequired = 20;
            info.addPara("It's finally time to act on that glimmer of hope.", opad);
            info.addPara(getGoToSystemTextShort(system) + ". Your path began there, and will again.", opad);
        } else if (currentStage == Stage.PAY_YOUR_PENANCE) {
            storyPointsRequired = 15;
            info.addPara("You need to pay your penance for this to ever end.", opad);
            info.addPara("Go to the Killa Ossuary and find out how to reach the light of the lake.", opad);
        } else if (currentStage == Stage.FIND_THE_LIGHT) {
            storyPointsRequired = 10;
            info.addPara("The way is clear.", opad);
            info.addPara("The Light of the Lake is directly north of the sector's center.", opad, h, "directly north", "sector's center");
            info.addPara("Prepare yourself.", bad, opad);
        } else if (currentStage == Stage.FACE_YOUR_TORMENTOR) {
            storyPointsRequired = 10;
            info.addPara("Your tormentor blocks the way to Elysium.", opad, bad, "tormentor");
            info.addPara("You know what to do.", opad);
        } else if (currentStage == Stage.ENTER_ELYSIUM) {
            storyPointsRequired = 10;
            info.addPara("Descend to Elysium. Your fate lies there.", opad, h, "Elysium");
        } else if (currentStage == Stage.SACRIFICE_YOURSELF) {
            info.addPara("In the ruined temple of %s, %s offered you a way out.", opad, h, "Elysium", "\"Sirius\"");
            info.addPara("Take it.", h, opad);
            info.addPara("There is no other way.", bad, opad);
        }
        info.addPara("\nYou still need a total of %s story points to escape your torment.", opad, Misc.getGrayColor(), Misc.getStoryDarkBrighterColor(), "" + storyPointsRequired);
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.SCENE_OF_THE_CRIME) {
            info.addPara("Return to the " +
                    system.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.PAY_YOUR_PENANCE) {
            info.addPara("Find your way at Killa's Luddic shrine.", tc, pad);
            return true;
        } else if (currentStage == Stage.FIND_THE_LIGHT) {
            info.addPara("Find the Light of the Lake in the northern abyss.", tc, pad);
            return true;
        } else if (currentStage == Stage.FACE_YOUR_TORMENTOR) {
            info.addPara("Face your tormentor.", tc, pad);
            return true;
        } else if (currentStage == Stage.ENTER_ELYSIUM) {
            info.addPara("Descend into Elysium.", tc, pad);
            return true;
        } else if (currentStage == Stage.SACRIFICE_YOURSELF) {
            info.addPara("Sacrifice, and be absolved.", tc, pad);
            return true;
        }
        return false;
    }

    // where on the map the intel screen tells us to go
    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (currentStage == Stage.SCENE_OF_THE_CRIME) {
            return getMapLocationFor(system.getCenter());
        } else if (currentStage == Stage.PAY_YOUR_PENANCE) {
            return getMapLocationFor(killa.getStarSystem().getCenter());
        } else {
            return getMapLocationFor(lotl.getCenter());
        }
    }

}
