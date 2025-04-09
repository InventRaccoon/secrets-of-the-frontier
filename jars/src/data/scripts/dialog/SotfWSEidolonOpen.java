package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.waywardstar.SotfWSFIDPluginImpl;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Map;

import static com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfWSEidolonOpen implements InteractionDialogPlugin {

    public static enum OptionId {
        ONE,
        TWO,
        THREE,
        REFUSE,
        TRAPPED,
        ;
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    //protected float counter_repeating = 0f;
    protected float counter = 0f;
    protected int stage = 1;

    protected CampaignFleetAPI playerFleet;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        playerFleet = Global.getSector().getPlayerFleet();
        dialog.setPromptText("");
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void backFromEngagement(EngagementResultAPI result) {

    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        OptionId option = (OptionId) optionData;

        // don't print the "no" if it was selected via a key press
        if (text != null && !option.equals(OptionId.REFUSE)) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        Color sc = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).getBaseUIColor();

        switch (option) {
            case ONE:
            case TWO:
            case THREE:
                options.clearOptions();
                options.addOption("", OptionId.TRAPPED, "...");
                options.setEnabled(OptionId.TRAPPED, false);
                stage = 6;
                counter = 0;
                break;
            case REFUSE:
                options.clearOptions();
                options.addOption("\"Of course!\"", OptionId.ONE, "Dance with me!");
                options.addOption("\"Absolutely!\"", OptionId.TWO, "Sing with me!");
                options.addOption("\"Definitely!\"", OptionId.THREE, "Won't you join us?");
                break;
        }
    }

    public void optionMousedOver(String optionText, Object optionData) {
        if (optionData == null) return;

        OptionId option = (OptionId) optionData;
        // "No" option just disappears if you hover over it
        if (option == OptionId.REFUSE) {
            options.clearOptions();
            options.addOption("\"Of course!\"", OptionId.ONE, "Dance with me!");
            options.addOption("\"Absolutely!\"", OptionId.TWO, "Sing with me!");
            options.addOption("\"Definitely!\"", OptionId.THREE, "Won't you join us?");
        }
    }

    public void advance(float amount) {
        counter += amount;
        //counter_repeating += amount;

        Color sc = SotfMisc.getEidolonColor();
        boolean sufferingMode = SotfMisc.getPlayerGuilt() >= Global.getSettings().getFloat("sotf_WSSufferingThreshold");

        // dialog sequence plays mostly on a timer, rather than to player response
        if (counter >= 5 && stage == 1) {
            if (!sufferingMode) {
                textPanel.addParagraph("Oh hello", sc);
            } else {
                textPanel.addParagraph("Oh, it's you", sc);
            }
            stage = 2;
            counter = 0;
        }
            if (counter >= 2 && stage == 2) {
                if (!sufferingMode) {
                    textPanel.addParagraph("I didn't see you there", sc);
                } else {
                    textPanel.addParagraph("I've heard a lot", sc);
                }
                stage = 3;
                counter = 0;
            }
            if (counter >= 2 && stage == 3) {
                textPanel.addParagraph("Won't you dance with me?", sc);
                stage = 4;
                counter = 0;
            }
            // who doesn't love railroading?
            if (counter >= 1 && stage == 4) {
                options.clearOptions();
                options.addOption("\"Of course!\"", OptionId.ONE, "Dance with me!");
                options.addOption("\"Absolutely!\"", OptionId.TWO, "Sing with me!");
                options.addOption("\"Definitely!\"", OptionId.THREE, "Won't you join us?");
                options.addOption("\"No.\"", OptionId.REFUSE, "I don't think so.");
                //options.setEnabled(OptionId.REFUSE, false);
                stage = 5;
                counter = 0;
            }
            if (counter >= 1 && stage == 6) {
                if (!sufferingMode) {
                    textPanel.addParagraph("Oh, just wonderful", sc);
                } else {
                    textPanel.addParagraph("If you insist so dearly", sc);
                }
                stage = 7;
            }
            if (counter >= 2 && stage == 7) {
                textPanel.addParagraph("Then let us dance!", sc);
                stage = 8;
                counter = 0;
            }
            if (counter >= 2 && stage == 8) {
                stage = 9;
                counter = 0;

                // post-fight dialogue
                SotfWSDialogScript script = new SotfWSDialogScript();
                Global.getSector().addScript(script);
                playerFleet.addEventListener(script);

                final CampaignFleetAPI enemyFleetTemp = Global.getFactory().createEmptyFleet(SotfIDs.SYMPHONY, ": : : : {'Eidolon', if you would}", false);
                enemyFleetTemp.setNoFactionInName(true);
                // they're ghosts
                enemyFleetTemp.getMemoryWithoutUpdate().set(MEMORY_KEY_NO_SHIP_RECOVERY, true);
                enemyFleetTemp.getMemoryWithoutUpdate().set("$sotf_WSEidolon", true);
                enemyFleetTemp.getMemoryWithoutUpdate().set("$combatMusicSetId", "sotf_weightlessthoughts");

                // the Lost One herself
                FleetMemberAPI vow = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sotf_vow_eidolon_WS");
                vow.setShipName("");
                vow.setFlagship(true);
                if (sufferingMode) {
                    vow.getVariant().addMod("sotf_serenity");
                }
                vow.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
                vow.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
                enemyFleetTemp.getFleetData().addFleetMember(vow);

                PersonAPI eidolon = SotfPeople.getPerson(SotfPeople.EIDOLON);
                vow.setCaptain(eidolon);
                enemyFleetTemp.setCommander(eidolon);

                // Wispmothers, Moonlight Sonata and Silent Whisper
                String wispmother_vid = "sotf_wispmother_WS";
                if (sufferingMode) {
                    // much less fair variant with AMSRMs
                    wispmother_vid = "sotf_wispmother_WS_Suffer";
                }
                FleetMemberAPI wispmother1 = enemyFleetTemp.getFleetData().addFleetMember(wispmother_vid);
                wispmother1.setShipName("Moonlight Sonata");
                wispmother1.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
                wispmother1.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
                FleetMemberAPI wispmother2 = enemyFleetTemp.getFleetData().addFleetMember(wispmother_vid);
                wispmother2.setShipName("Silent Whisper");
                wispmother2.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);
                wispmother2.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
                // wispmothers gain Wispblossom and Eid's fleet gains a Brilliant
                if (sufferingMode) {
                    wispmother1.getVariant().addMod(SotfIDs.HULLMOD_WISPBLOSSOM);
                    wispmother2.getVariant().addMod(SotfIDs.HULLMOD_WISPBLOSSOM);
                    enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "brilliant_Standard"));
                }

                // Chaff, mix of Remnants and human
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "fulgent_Assault"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "shrike_Attack"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "glimmer_Assault"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "glimmer_Assault"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "centurion_Assault"));

                for (FleetMemberAPI member : enemyFleetTemp.getFleetData().getMembersListCopy()) {
                    member.setOwner(1);
                    member.getRepairTracker().setCR(1f);
                    member.getRepairTracker().setCrashMothballed(false);
                    member.getRepairTracker().setMothballed(false);
                    // semi-incorporeal transdimensional ghosts
                    if (!member.getVariant().getHullMods().contains(SotfIDs.PHANTASMAL_SHIP)) {
                        member.getVariant().addPermaMod(SotfIDs.PHANTASMAL_SHIP);
                    }
                    if (member == vow) {
                        continue;
                    }
                    int level = 7;
                    String core_id = Commodities.ALPHA_CORE;
                    if (member.isDestroyer() && !member.getHullId().equals("sotf_wispmother")) {
                        level = 5;
                        core_id = Commodities.BETA_CORE;
                    } else if (member.isFrigate()) {
                        level = 3;
                        core_id = Commodities.GAMMA_CORE;
                    }
                    if (sufferingMode) {
                        level = 8;
                        core_id = Commodities.ALPHA_CORE;
                    }
                    // generate an officer of the desired level
                    PersonAPI person = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(SotfIDs.SYMPHONY), level, FleetFactoryV3.getSkillPrefForShip(member), true, enemyFleetTemp, true, true, level, Misc.random);
                    //person.setPortraitSprite(Global.getSettings().getSpriteName("sotf_characters", "eidolon_wraith"));
                    // if it's an AI ship, keep that mercenary's skills over the default for that core (Eid has tinkered with them)
                    if (Misc.isAutomated(member)) {
                        MutableCharacterStatsAPI stats = person.getStats();
                        person = Misc.getAICoreOfficerPlugin(core_id).createPerson(core_id, Factions.REMNANTS, Misc.random);
                        person.setStats(stats);
                        person.getMemoryWithoutUpdate().set(SotfIDs.OFFICER_NOT_FEARLESS, true);
                    }
                    if (member.getNumFlightDecks() > 0) {
                        person.setPersonality(Personalities.AGGRESSIVE);
                    } else {
                        person.setPersonality(Personalities.RECKLESS);
                    }
                    member.getCrewComposition().setCrew(0);
                    member.setCaptain(person);
                }

                // autofit the wraiths
                enemyFleetTemp.inflateIfNeeded();

                dialog.setInteractionTarget(enemyFleetTemp);

                final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
                config.leaveAlwaysAvailable = false;
                config.showCommLinkOption = false;
                config.showEngageText = false;
                config.showFleetAttitude = false;
                config.showTransponderStatus = false;
                config.showWarningDialogWhenNotHostile = false;
                config.alwaysAttackVsAttack = true;
                config.impactsAllyReputation = false;
                config.impactsEnemyReputation = false;
                config.pullInAllies = true;
                config.pullInEnemies = false;
                config.pullInStations = false;
                config.lootCredits = false;
                config.straightToEngage = true;

                config.firstTimeEngageOptionText = "Engage";
                config.afterFirstTimeEngageOptionText = "Re-engage";
                config.noSalvageLeaveOptionText = "Continue";

                config.dismissOnLeave = false;
                config.printXPToDialog = true;

                final SotfWSFIDPluginImpl plugin = new SotfWSFIDPluginImpl(config);

                final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
                config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    @Override
                    public void notifyLeave(InteractionDialogAPI dialog) {
                        // nothing in there we care about keeping; clearing to reduce savefile size
                        enemyFleetTemp.getMemoryWithoutUpdate().clear();
                        // there's a "standing down" assignment given after a battle is finished that we don't care about
                        enemyFleetTemp.clearAssignments();
                        enemyFleetTemp.deflate();

                        dialog.dismiss();
                    }
                    @Override
                    public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                        bcc.aiRetreatAllowed = false;
                        bcc.enemyDeployAll = true;
                        bcc.fightToTheLast = true;
                    }
                    @Override
                    public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {

                    }

                };

                dialog.setPlugin(plugin);
                plugin.init(dialog);
            }
        }

    public Object getContext() {
        return null;
    }
}