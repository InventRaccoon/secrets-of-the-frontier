package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 *	A PROMISE
 */

public class SotfAPromise extends HubMissionWithSearch {

    public static enum Stage {
        FIND_VENA,
        OMICRON,
        RESCUE_SIERRA,
        RETURN_WITH_SIERRA,
        RESTORE_SIERRA,
        SPEAK_WITH_COURSER,
        COMPLETED,
        SOLD_SIERRA,
    }

    // disabled frigate
    protected SectorEntityToken frigate;
    // Dusklight
    protected SectorEntityToken courser;
    // Voidwretch
    protected SectorEntityToken voidwretch;
    // station
    protected SectorEntityToken station;
    // disabled frigate system
    protected StarSystemAPI system;
    // main system
    protected StarSystemAPI system2;

    // run when the bar event starts / when we ask a contact about the mission
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        setName("A Promise");
        setStoryMission();
        setRepFactionChangesNone();
        setRepPersonChangesNone();
        setGiverFaction(Factions.INDEPENDENT);
        completedKey = "$sotf_apromiseCompleted";

        // Echo's system
        requireSystemHasColony(Factions.INDEPENDENT, 4);
        // bcs too far away
        if (Global.getSector().getStarSystem("Lethia") != null) {
            requireSystemNot(Global.getSector().getStarSystem("Lethia"));
        }
        system = pickSystem(true);

        if (system == null) {
            requireSystemTags(ReqMode.ALL, Tags.THEME_CORE_POPULATED);
            system = pickSystem(true);
        }
        if (system == null) return false;

        // Courser's system
        requireSystemInterestingAndNotUnsafeOrCore();
        preferSystemWithinRangeOf(system.getLocation(), 15f, 45f);
        preferSystemNotPulsar();
        preferSystemOnFringeOfSector();
        preferSystemUnexplored();
        requireSystemNot(system);

        system2 = pickSystem(true);
        if (system2 == null) return false;

        if (!setGlobalReference("$apromise_ref")) return false;

        frigate = spawnEntity(SotfIDs.APROMISE_VENA_ENTITY, new LocData(EntityLocationType.ORBITING_PLANET, null, system));
        frigate.getMemoryWithoutUpdate().set("$apromise_vena", true);
        //setEntityMissionRef(vox, "$apromise_ref");
        makeImportant(frigate, "$apromise", Stage.FIND_VENA);
        frigate.setFaction(SotfIDs.DUSTKEEPERS);

        LocData omicron_locdata = new LocData(EntityLocationType.ORBITING_PLANET, system2.getCenter(), system2);
        courser = spawnEntity(SotfIDs.APROMISE_OMICRON_ENTITY, omicron_locdata);
        courser.getMemoryWithoutUpdate().set("$apromise_omicron", true);
        //setEntityMissionRef(omicron, "$apromise_ref");
        makeImportant(courser, "$apromise", Stage.OMICRON, Stage.RETURN_WITH_SIERRA, Stage.SPEAK_WITH_COURSER);
        courser.setFaction(SotfIDs.DUSTKEEPERS);

        LocData omicron_flair_locdata = new LocData(courser, false);
        spawnDebrisField(360f, 1.2f, omicron_flair_locdata);
        spawnShipGraveyard(Factions.INDEPENDENT, 4, 4, omicron_flair_locdata);
        spawnShipGraveyard(SotfIDs.DUSTKEEPERS, 3,3, omicron_flair_locdata);

        voidwretch = spawnDerelict(new DerelictShipEntityPlugin.DerelictShipData(new PerShipData(Global.getSettings().getVariant("sotf_pledge_Base"), ShipCondition.PRISTINE, "Voidwretch", SotfIDs.SIERRA_FACTION, 0f), false), new LocData(EntityLocationType.ORBITING_PARAM, courser, system2));
        voidwretch.getMemoryWithoutUpdate().set("$apromise_pledge", true);
        //setEntityMissionRef(dreg, "$apromise_ref");
        makeImportant(voidwretch, "$apromise", Stage.RESTORE_SIERRA);

        station = spawnEntity(SotfIDs.APROMISE_STATION, new LocData(EntityLocationType.ORBITING_PLANET, null, system2));
        station.getMemoryWithoutUpdate().set("$apromise_anom", true);
        //setEntityMissionRef(station, "$apromise_ref");
        makeImportant(station, "$apromise", Stage.RESCUE_SIERRA);

        // set our starting, success and failure stages
        setStartingStage(Stage.FIND_VENA);
        setSuccessStage(Stage.COMPLETED);
        setFailureStage(Stage.SOLD_SIERRA);
        setNoAbandon();

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.OMICRON, "$apromise_foundvena");
        setStageOnGlobalFlag(Stage.RESCUE_SIERRA, "$apromise_omicrontasked");
        connectWithGlobalFlag(Stage.RESCUE_SIERRA, Stage.RETURN_WITH_SIERRA, "$apromise_rescuedsierra");
        setStageOnGlobalFlag(Stage.RESTORE_SIERRA, "$apromise_returnedsierra");
        setStageOnGlobalFlag(Stage.SPEAK_WITH_COURSER, "$apromise_restoredsierra");
        setStageOnGlobalFlag(Stage.COMPLETED, "$apromise_completed");
        setStageOnGlobalFlag(Stage.SOLD_SIERRA, "$apromise_soldsierra");
        return true;
    }

    // 30 day delay before Plausible Deniability can start (give Courser some time to set things up behind the scenes, yknow)
    @Override
    protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_WAITING_FOR_PLAUSDEN, true, 30);
    }

    // when Call-ing something that isn't a default option for a mission, it'll try and run this method
    // with "action" being the first parameter
    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        TextPanelAPI text = dialog.getTextPanel();
        // give Sierra core with special text
        if (action.equals("giveCore")){
            Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);

            text.setFontSmallInsignia();
            text.addParagraph("Gained Sierra Core", Misc.getPositiveHighlightColor());
            text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra Core");
            text.setFontInsignia();
            return true;
        } else if (action.equals("installCore")) {
            Global.getSector().getPlayerFleet().getCargo().removeItems(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);
            return true;
        } else if (action.equals("addSierra")){
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            Misc.fadeAndExpire(voidwretch);

            PersonAPI sierra = Global.getSector().getImportantPeople().getPerson(SotfPeople.SIERRA);

            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sotf_pledge_Base");
            member.setShipName("Voidwretch");
            member.setCaptain(sierra);

            playerFleet.getFleetData().addFleetMember(member);
            //Misc.setUnremovable(sierra, true);

            Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierra_var", "sotf_pledge");

            text.setFontSmallInsignia();
            String str = sierra.getName().getFirst() + " (level " + sierra.getStats().getLevel() +")";
            text.addParagraph(str + " has joined your fleet", Misc.getPositiveHighlightColor());
            text.highlightInLastPara(SotfMisc.getSierraColor(), str);

            text.addParagraph("You can speak to her using the \"Contacts\" tab of the Intel screen", Misc.getHighlightColor());

            Global.getSector().getCharacterData().addHullMod(SotfIDs.HULLMOD_SERENITY);
            Global.getSector().getCharacterData().addHullMod(SotfIDs.HULLMOD_FERVOR);

            str = member.getShipName() + ", " + member.getHullSpec().getHullNameWithDashClass() + " " + member.getHullSpec().getDesignation();
            text.addParagraph("Acquired " + str, Misc.getPositiveHighlightColor());
            text.highlightInLastPara(SotfMisc.getSierraColor(), str);

            // cut down for brevity - hullmods from new tags are now visible by default in 0.96a
            //text.addParagraph("Unlocked hullmods: Serenity & Fervor (available under \"Anomalous Phase-Tech\" tag)", Misc.getPositiveHighlightColor());
            text.addParagraph("Unlocked hullmods: Serenity & Fervor", Misc.getPositiveHighlightColor());
            text.highlightInLastPara(SotfMisc.getSierraColor(), "Serenity", "Fervor");
            text.setFontInsignia();
            return true;
        }
        return false;
    }



    protected void updateInteractionDataImpl() {
        set("$apromise_system1SName", system.getNameWithNoType());
        set("$apromise_system2SName", system2.getNameWithNoType());
        // no memory past c184
        set("$sierra_amnesiaYears", "" + (Global.getSector().getClock().getCycle() - 184));
    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (currentStage == Stage.FIND_VENA) {
            info.addPara("You've heard rumors from a patrol officer about a mysterious ship " +
                    "that was attacked. It may be worth investigating to see what happened.", opad);
            info.addPara(getGoToSystemTextShort(system) + " and see if you can find out more about " +
                    "the incident.", opad);
        } else if (currentStage == Stage.OMICRON) {
            info.addPara("You found a disabled automated frigate that gave you rendezvous co-ordinates to the " +
                    system2.getNameWithLowercaseTypeShort() + ".", opad);
            info.addPara("Whoever it was going to meet, it seemed to put a great importance on this task. ", opad);
        } else if (currentStage == Stage.RESCUE_SIERRA) {
            info.addPara("You met an AI by the name of Outrider-Annex-Courser, who has asked you to free " +
                    "\"Sierra\" from a station in the " +
                    system2.getNameWithLowercaseTypeShort() + ".", opad);
            info.addPara("Defeat the station's Remnant defenders and rescue her.", opad);
        } else if (currentStage == Stage.RETURN_WITH_SIERRA) {
            info.addPara("You met Courser in the " +
                    system2.getNameWithLowercaseTypeShort() + " and rescued a strange AI by the name of Sierra by their request.", opad);
            info.addPara("Return Sierra to them.", opad);
        } else if (currentStage == Stage.RESTORE_SIERRA) {
            info.addPara("You met Courser in the " +
                    system2.getNameWithLowercaseTypeShort() + " and rescued a strange AI by the name of Sierra by their request.", opad);
            info.addPara("Restore Sierra to her host ship in the debris field around the ODS Dusklight.", opad);
        } else if (currentStage == Stage.SPEAK_WITH_COURSER) {
            info.addPara("You restored Sierra to her host ship and brought her back online. Courser likely has something to say." +
                    system2.getNameWithLowercaseTypeShort() + ".", opad);
            info.addPara("Report back to Courser.", opad);
        }
        if (isDevMode()) {
            info.addPara("DEV: COURSER LOCATION: " + system2.getNameWithLowercaseTypeShort(), opad);
        }
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.FIND_VENA) {
            info.addPara("Search the " +
                    system.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.OMICRON) {
            info.addPara("Investigate the " +
                    system2.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.RESCUE_SIERRA) {
            info.addPara("Rescue Sierra from the research station", tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_WITH_SIERRA) {
            info.addPara("Return Sierra to Courser", tc, pad);
            return true;
        } else if (currentStage == Stage.RESTORE_SIERRA) {
            info.addPara("Restore Sierra to her host ship", tc, pad);
            return true;
        } else if (currentStage == Stage.SPEAK_WITH_COURSER) {
            info.addPara("Talk to Courser", tc, pad);
            return true;
        }
        return false;
    }

    // where on the map the intel screen tells us to go
    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (currentStage == Stage.FIND_VENA) {
            return getMapLocationFor(system.getCenter());
        } else {
            return getMapLocationFor(system2.getCenter());
        }
    }

}
