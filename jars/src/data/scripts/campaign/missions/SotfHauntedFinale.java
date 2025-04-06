package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.SharedUnlockData;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.terrain.Planet;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 *	LIGHT OF THE LAKE: HAUNTED FINALE
 */

public class SotfHauntedFinale extends HubMissionWithSearch {

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
        completedKey = "$sotf_haunt_completed"; // this ends the game tho

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

        if (!setGlobalReference("$sotf_haunt_ref")) return false;

        // set our starting, success and failure stages
        setStartingStage(Stage.SCENE_OF_THE_CRIME);
        setSuccessStage(Stage.COMPLETED);
        setNoAbandon();

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.PAY_YOUR_PENANCE, "$sotf_haunt_gotokilla");
        setStageOnGlobalFlag(Stage.FIND_THE_LIGHT, "$sotf_haunt_gotolotl");
        setStageOnGlobalFlag(Stage.FACE_YOUR_TORMENTOR, "$sotf_haunt_fightfel");
        setStageOnGlobalFlag(Stage.ENTER_ELYSIUM, "$sotf_haunt_gotoelysium");
        setStageOnGlobalFlag(Stage.SACRIFICE_YOURSELF, "$sotf_haunt_sacrifice");
        setStageOnGlobalFlag(Stage.COMPLETED, "$sotf_haunt_completed");
        return true;
    }

    // 30 day delay before Plausible Deniability can start (give Courser some time to set things up behind the scenes, yknow)
    @Override
    protected void endSuccessImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

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
                text.addParagraph("Unlocked \"Child of the Lake\" custom start.", Misc.getHighlightColor());
            }
            text.setFontInsignia();

            FullName.Gender playerGender = Global.getSector().getPlayerPerson().getGender();
            String his = "his";
            if (playerGender == FullName.Gender.FEMALE) {
                his = "her";
            } else if (playerGender == FullName.Gender.ANY) {
                his = "their";
            }

            text.addPara("With " + his + " sacrifice, the story of " + Global.getSector().getPlayerPerson().getNameString() + " has ended.", Misc.getHighlightColor(), Global.getSector().getPlayerPerson().getNameString());
            return true;
        } else if (action.equals("quickLoad")){
            Global.getSector().getCampaignUI().quickLoad();
            return true;
        } else if (action.equals("endTheGame")){
            Global.getSector().getCampaignUI().cmdExitWithoutSaving();
            return true;
        }
        return false;
    }



    protected void updateInteractionDataImpl() {
    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        if (currentStage == Stage.SCENE_OF_THE_CRIME) {
            info.addPara("It's finally time to act on that glimmer of hope.", opad);
            info.addPara(getGoToSystemTextShort(system) + ". Your path began there, and will again.", opad);
        } else if (currentStage == Stage.PAY_YOUR_PENANCE) {
            info.addPara("You need to pay your penance for this to ever end.", opad);
            info.addPara("Go to the Killa Ossuary and find out how to reach the light of the lake.", opad);
        } else if (currentStage == Stage.FIND_THE_LIGHT) {
            info.addPara("The way is clear.", opad);
            info.addPara("The Light of the Lake is directly north of the sector's center.", opad, h, "directly north", "sector's center");
            info.addPara("Prepare yourself.", opad);
        } else if (currentStage == Stage.FACE_YOUR_TORMENTOR) {
            info.addPara("Your tormentor blocks the way to Elysium.", opad, bad, "tormentor");
            info.addPara("You know what to do.", opad);
        } else if (currentStage == Stage.ENTER_ELYSIUM) {
            info.addPara("Descend to Elysium. Your fate lies there.", opad, h, "Elysium");
        } else if (currentStage == Stage.SACRIFICE_YOURSELF) {
            info.addPara("In the ruined temple of %s, %s offered you a way out.", opad, h, "Elysium", "\"Sirius\"");
            info.addPara("Take it.", h, opad);
            info.addPara("There is no other way.", bad, opad);
        }
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.SCENE_OF_THE_CRIME) {
            info.addPara("Return to " +
                    system.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.PAY_YOUR_PENANCE) {
            info.addPara("Find your way at Killa's Luddic shrine", tc, pad);
            return true;
        } else if (currentStage == Stage.FIND_THE_LIGHT) {
            info.addPara("Find the Light of the Lake", tc, pad);
            return true;
        } else if (currentStage == Stage.FACE_YOUR_TORMENTOR) {
            info.addPara("Face your tormentor", tc, pad);
            return true;
        } else if (currentStage == Stage.ENTER_ELYSIUM) {
            info.addPara("Descend into Elysium", tc, pad);
            return true;
        } else if (currentStage == Stage.SACRIFICE_YOURSELF) {
            info.addPara("Sacrifice, and be absolved", tc, pad);
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
