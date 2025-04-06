// aka FrontierSecretsWaywardStarFleetInteractionDialoguePluginImplementation
package data.scripts.campaign.plugins.waywardstar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DevMenuOptions;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.plugins.amemory.SotfAMemoryFEContext;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class SotfWSFIDPluginImpl extends FleetInteractionDialogPluginImpl {

    public SotfWSFIDPluginImpl() {
        this(null);
    }

    public SotfWSFIDPluginImpl(FIDConfig params) {
        super(params);
        context = new SotfAMemoryFEContext();
    }

    // only option is "Dance"
    protected void updatePreCombat() {
        options.clearOptions();

        if (playerGoal == FleetGoal.ATTACK && otherGoal == FleetGoal.ESCAPE) {
            String tooltipText = getString("tooltipPursueAutoresolve");
            options.addOption("Order your second-in-command to handle it", OptionId.AUTORESOLVE_PURSUE, tooltipText);
            options.addOption("Take command of the action", OptionId.CONTINUE_INTO_BATTLE, null);
        } else {
            if (playerGoal == FleetGoal.ESCAPE) {
                List<FleetMemberAPI> choices = getCrashMothballable(playerFleet.getFleetData().getCombatReadyMembersListCopy());

                options.addOption("Crash-mothball some of your ships to prevent malfunctions", OptionId.CRASH_MOTHBALL, null);
                if (choices.isEmpty()) {
                    options.setEnabled(OptionId.CRASH_MOTHBALL, false);
                    options.setTooltip(OptionId.CRASH_MOTHBALL, getString("tooltipCrashMothballUnavailable"));
                } else {
                    options.setTooltip(OptionId.CRASH_MOTHBALL, getString("tooltipCrashMothball"));
                }
            }
            if (config.straightToEngage) {
                options.addOption("Dance", OptionId.CONTINUE_INTO_BATTLE, null);
            } else {
                options.addOption("Dance", OptionId.CONTINUE_INTO_BATTLE, null);
            }
        }

        boolean canGoBack = ongoingBattle || otherGoal == FleetGoal.ESCAPE || Global.getSettings().isDevMode();
        if (canGoBack) {
            options.addOption("Go back", OptionId.GO_TO_MAIN, null);
            options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
        }
    }

    // everything here is just so the "your fleet is too large to disengage" text doesn't appear and spoil the moment
    protected void updateEngagementChoice(boolean withText) {
        allyEngagementChoiceNoBattle = false;
        //options.clearOptions();
        if (isFightingOver()) {
            goToEncounterEndPath();
            return;
        }
        //options.clearOptions();

        BattleAPI b = context.getBattle();

        if (ongoingBattle && b.getPlayerSide() != null && b.getPlayerSide().size() <= 1) {
            //if (ongoingBattle && b.getPlayerSide() != null && b.isPlayerPrimary()) {
            ongoingBattle = false;
            if (config.showCommLinkOption) {
                options.addOption("Open a comm link", OptionId.OPEN_COMM, null);
            }
        }

        playerGoal = null;
        otherGoal = null;

        boolean alliedWantsToFight = alliedFleetWantsToFight();
        boolean alliedWantsToRun = alliedFleetWantsToDisengage() && alliedCanDisengage();
        boolean alliedHolding = alliedFleetHoldingVsStrongerEnemy();
        boolean otherWantsToFight = otherFleetWantsToFight();
        boolean otherWantsToRun = otherFleetWantsToDisengage() && otherCanDisengage();
        //otherWantsToRun = otherFleetWantsToDisengage() && otherCanDisengage();
        boolean otherHolding = otherFleetHoldingVsStrongerEnemy();

        //boolean otherWantsToRun = otherFleetWantsToDisengage() && otherCanDisengage();
        boolean playerHasReadyShips = false;
        boolean allyHasReadyShips = false;
        for (FleetMemberAPI member : playerFleet.getFleetData().getCombatReadyMembersListCopy()) {
            if (member.isAlly() && !member.isStation()) {
                allyHasReadyShips = true;
            } else {
                playerHasReadyShips = true;
            }
        }

        if (otherWantsToRun && canDisengageCleanly(otherFleet)) {
//			if (didEnoughToDisengage(otherFleet)) {
//				if (context.getBattle().getPlayerSide().size() > 1) {
//					if (withText) addText(getString("enemyDisruptedPlayerSide"), Misc.getNegativeHighlightColor());
//				} else {
//					if (withText) addText(getString("enemyDisruptedPlayer"), Misc.getNegativeHighlightColor());
//				}
//			} else {
            if (context.getBattle().getPlayerSide().size() > 1) {
                if (withText) addText(getString("enemyCleanDisengageSide"));
            } else {
                if (withText) addText(getString("enemyCleanDisengage"));
            }
//			}
            goToEncounterEndPath();
        } else if (otherWantsToRun) {
            String pursueTooltip = "tooltipPursue";
            String harassTooltip = "tooltipHarassRetreat";
            String letThemGoTooltip = "tooltipLetThemGo";
            if (!context.isEngagedInHostilities()) {
                letThemGoTooltip = "tooltipLetThemGoNoPenalty";
            }

            boolean canPursue = false;
            boolean canHasass = false;
            //PursueAvailability pa = context.getPursuitAvailability(playerFleet, otherFleet);
            FleetEncounterContextPlugin.PursueAvailability pa = getPursuitAvailability(playerFleet);
            //List<FleetMemberAPI> members = getPursuitCapablePlayerShips();
            //if (members.isEmpty()) pa = PursueAvailability.NO_READY_SHIPS;

            FleetEncounterContextPlugin.DisengageHarryAvailability dha = context.getDisengageHarryAvailability(playerFleet, otherFleet);

            switch (pa) {
                case AVAILABLE:
                    canPursue = true;
                    break;
                case LOST_LAST_ENGAGEMENT:
                    pursueTooltip = "tooltipPursueLostLast";
                    break;
                case NO_READY_SHIPS:
                    pursueTooltip = "tooltipNoReadyShips";
                    break;
                case TOOK_SERIOUS_LOSSES:
                    if (context.getBattle().getPlayerSide().size() > 1) {
                        if (withText) addText(getString("enemyDisruptedPlayerSide"), getString("highlightDisruptedPlayer"), Misc.getNegativeHighlightColor());
                    } else {
                        if (withText) addText(getString("enemyDisruptedPlayer"), getString("highlightDisruptedPlayer"), Misc.getNegativeHighlightColor());
                    }
                    pursueTooltip = "tooltipPursueSeriousLosses";
                    break;
                case TOO_SLOW:
                    pursueTooltip = "tooltipPursueTooSlow";
                    break;
            }

            switch (dha) {
                case AVAILABLE:
                    canHasass = true;
                    break;
                case NO_READY_SHIPS:
                    harassTooltip = "tooltipNoReadyShips";
                    break;
            }

            if (ongoingBattle) {
                boolean station = false;
                if (playerFleet != null) {
                    for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                        if (member.isStation()) {
                            station = true;
                            break;
                        }
                    }
                }

                //boolean letGo = (!canPursue && !canHasass) || !allyHasReadyShips || station;
                boolean letGo = (!canPursue && !canHasass) || !allyHasReadyShips;// || station;
                //if (!letGo) {
                //PursuitOption po = playerFleet.getAI().pickPursuitOption(context, otherFleet);
                CampaignFleetAIAPI.PursuitOption po = pickPursuitOption(playerFleet, otherFleet, context);
                po = CampaignFleetAIAPI.PursuitOption.PURSUE;
                if (alliedWantsToRun || alliedHolding || !alliedWantsToFight || letGo) {
                    po = CampaignFleetAIAPI.PursuitOption.LET_THEM_GO;
                }
                if (!canPursue && canHasass) {
                    po = CampaignFleetAIAPI.PursuitOption.HARRY;
                }
                //po = PursuitOption.LET_THEM_GO;
                //po = PursuitOption.HARRY;
                switch (po) {
                    case PURSUE:
                        if (withText) addText(getString("ongoingBattlePursue"));
                        playerGoal = FleetGoal.ATTACK;
                        otherGoal = FleetGoal.ESCAPE;
                        options.addOption("Join the pursuit", OptionId.CONTINUE_ONGOING_BATTLE, getString(pursueTooltip));
                        if (!canPursue || !playerHasReadyShips) {
                            options.setEnabled(OptionId.CONTINUE_ONGOING_BATTLE, false);
                        }
                        break;
                    case HARRY:
                        // CR loss from harrying
                        context.applyPursuitOption(playerFleet, otherFleet, po);

                        if (withText) addText(getString("ongoingBattleHarass"));
                        context.setEngagedInHostilities(true);
                        context.getDataFor(playerFleet).setDisengaged(false);
                        context.getDataFor(otherFleet).setDisengaged(true);
                        allyEngagementChoiceNoBattle = true;
                        harryEndedBattle = true;
                        //rememberWasBeaten();
                        break;
                    case LET_THEM_GO:
                        letGo = true;
                        if (context.isEngagedInHostilities()) {
                            context.getDataFor(playerFleet).setDisengaged(false);
                            context.getDataFor(otherFleet).setDisengaged(true);
                        }
                        allyEngagementChoiceNoBattle = true;
                        //rememberWasBeaten();
                        break;
                }
                //}
                if (letGo) {
                    if (withText) addText(getString("ongoingBattleLetGo"));
                    allyEngagementChoiceNoBattle = true;
                }

                if (context.isEngagedInHostilities() && context.isBattleOver()) {
                    goToEncounterEndPath();
                } else {
                    if (context.isEngagedInHostilities()) {
                        options.addOption("Perform a salvage operation, then leave", OptionId.LOOT_THEN_LEAVE, null);
                        options.setShortcut(OptionId.LOOT_THEN_LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                    } else {
                        options.addOption("Leave", OptionId.LEAVE, null);
                        options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                    }
                }
            } else {
                CampaignFleetAIAPI ai = otherFleet.getAI();
                boolean hostile = false;
                if (ai != null) {
                    hostile = ai.isHostileTo(playerFleet) || context.isEngagedInHostilities();
                }

                options.addOption("Pursue them", OptionId.PURSUE, getString(pursueTooltip));

                if (playerHasReadyShips) {
                    options.addOption("Maneuver to force a pitched battle", OptionId.FORCE_ENGAGE, "Outmaneuver the opposing fleet, forcing them to fight you head on.");
                    boolean knows = context.getBattle() != null && context.getBattle().getNonPlayerSide() != null &&
                            context.getBattle().knowsWhoPlayerIs(context.getBattle().getNonPlayerSide());
                    boolean lowImpact = context.isLowRepImpact() || context.isNoRepImpact();
                    FactionAPI nonHostile = getNonHostileOtherFaction();
                    //if (!playerFleet.getFaction().isHostileTo(otherFleet.getFaction()) && knows && !context.isEngagedInHostilities()) {
                    if (nonHostile != null && knows && !lowImpact && !context.isEngagedInHostilities() &&
                            config.showWarningDialogWhenNotHostile) {
                        options.addOptionConfirmation(OptionId.FORCE_ENGAGE, "The " + nonHostile.getDisplayNameLong() + " " + nonHostile.getDisplayNameIsOrAre() + " not currently hostile, and you have been positively identified. Are you sure you want to attack one of their fleets?", "Yes", "Never mind");
                    }
                } else {
                    options.addOption("Maneuver to force a pitched battle", OptionId.ENGAGE, getString("tooltipNoReadyShips"));
                    options.setEnabled(OptionId.FORCE_ENGAGE, false);
                }
                SetStoryOption.set(dialog, 1, OptionId.FORCE_ENGAGE, "forceBattle", Sounds.STORY_POINT_SPEND_COMBAT,
                        "Maneuvered to force pitched battle with " + otherFleet.getNameWithFactionKeepCase());

                options.addOption("Harry their retreat", OptionId.HARRY_PURSUE, getString(harassTooltip));
                boolean knows = context.getBattle() != null && context.getBattle().getNonPlayerSide() != null &&
                        context.getBattle().knowsWhoPlayerIs(context.getBattle().getNonPlayerSide());
                boolean lowImpact = context.isLowRepImpact() || context.isNoRepImpact();
                FactionAPI nonHostile = getNonHostileOtherFaction();
                //if (!playerFleet.getFaction().isHostileTo(otherFleet.getFaction()) && knows && !context.isEngagedInHostilities()) {
                if (nonHostile != null && knows && !lowImpact && !context.isEngagedInHostilities() &&
                        config.showWarningDialogWhenNotHostile) {
                    options.addOptionConfirmation(OptionId.HARRY_PURSUE, "The " + nonHostile.getDisplayNameLong() + " " + nonHostile.getDisplayNameIsOrAre() + " not currently hostile, and you have been positively identified. Are you sure you want to engage in hostilities with one of their fleets?", "Yes", "Never mind");
                    options.addOptionConfirmation(OptionId.PURSUE, "The " + nonHostile.getDisplayNameLong() + " " + nonHostile.getDisplayNameIsOrAre() + " not currently hostile, and you have been positively identified. Are you sure you want to engage in hostilities with one of their fleets?", "Yes", "Never mind");
                }
                if (hostile) {
                    options.addOption("Let them go", OptionId.LET_THEM_GO, getString(letThemGoTooltip));
                } else {
                    options.addOption("Leave", OptionId.LEAVE, null);
                    options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
//					options.addOption("Go back", OptionId.GO_TO_MAIN, null);
//					options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
                }

                if (!canPursue || !playerHasReadyShips) {
                    options.setEnabled(OptionId.PURSUE, false);
                }
                if (!canHasass || !playerHasReadyShips) {
                    options.setEnabled(OptionId.HARRY_PURSUE, false);
                }
            }
        } else {
            if (ongoingBattle) {
                if (alliedWantsToRun) {
                    if (withText && !config.straightToEngage) addText(getString("ongoingBattleDisengage"));
                    playerGoal = FleetGoal.ESCAPE;
                    otherGoal = FleetGoal.ATTACK;
                    options.addOption("Join the disengage attempt", OptionId.CONTINUE_ONGOING_BATTLE, null);
                } else {
                    boolean station = false;
                    if (playerFleet != null) {
                        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                            if (member.isStation()) {
                                station = true;
                                break;
                            }
                        }
                    }

                    if (withText && !config.straightToEngage) {
                        if (station) {
                            addText(getString("ongoingBattleStation"));
                        } else {
                            addText(getString("ongoingBattleEngage"));
                        }
                    }
                    playerGoal = FleetGoal.ATTACK;
                    otherGoal = FleetGoal.ATTACK;

                    if (playerHasReadyShips) {
                        options.addOption("Join the engagement", OptionId.CONTINUE_ONGOING_BATTLE, null);
                    } else {
                        options.addOption("Join the engagement", OptionId.CONTINUE_ONGOING_BATTLE, getString("tooltipNoReadyShips"));
                        options.setEnabled(OptionId.CONTINUE_ONGOING_BATTLE, false);
                    }

                    options.addOption("Leave", OptionId.LEAVE, null);
                    options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                }
            } else {
                String engageText = "Move in to engage";
                if (config.firstTimeEngageOptionText != null && !context.isEngagedInHostilities()) {
                    engageText = config.firstTimeEngageOptionText;
                }
                if (config.afterFirstTimeEngageOptionText != null && context.isEngagedInHostilities()) {
                    engageText = config.afterFirstTimeEngageOptionText;
                }
                if (playerHasReadyShips) {
                    options.addOption(engageText, OptionId.ENGAGE, getString("tooltipEngage"));
                    boolean knows = context.getBattle() != null && context.getBattle().getNonPlayerSide() != null &&
                            context.getBattle().knowsWhoPlayerIs(context.getBattle().getNonPlayerSide());
                    boolean lowImpact = context.isLowRepImpact() || context.isNoRepImpact();
                    FactionAPI nonHostile = getNonHostileOtherFaction();
                    //if (!playerFleet.getFaction().isHostileTo(otherFleet.getFaction()) && knows && !context.isEngagedInHostilities()) {
                    if (nonHostile != null && knows && !lowImpact && !context.isEngagedInHostilities() &&
                            config.showWarningDialogWhenNotHostile) {
                        options.addOptionConfirmation(OptionId.ENGAGE, "The " + nonHostile.getDisplayNameLong() + " " + nonHostile.getDisplayNameIsOrAre() + " not currently hostile, and you have been positively identified. Are you sure you want to attack one of their fleets?", "Yes", "Never mind");
                    }
                } else {
                    options.addOption(engageText, OptionId.ENGAGE, getString("tooltipNoReadyShips"));
                    options.setEnabled(OptionId.ENGAGE, false);
                }
                CampaignFleetAIAPI ai = otherFleet.getAI();
                boolean hostile = false;
                if (ai != null) {
                    hostile = ai.isHostileTo(playerFleet) || context.isEngagedInHostilities();
                }
                if (!config.leaveAlwaysAvailable &&
                        (otherFleetWantsToFight() || (hostile && !otherFleetWantsToDisengage()))) {
                    if (canDisengageCleanly(playerFleet)) {
                        options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipCleanDisengage"));
                    } else if (canDisengageWithoutPursuit(playerFleet) && !(!otherFleetWantsToFight() && !otherFleetWantsToDisengage())) {
                        options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipHarrassableDisengage"));
                    } else {
                        if (otherFleetHoldingVsStrongerEnemy() || (!otherFleetWantsToFight() && !otherFleetWantsToDisengage())) {
                            options.addOption("Leave", OptionId.LEAVE, null);
                            options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
//							options.addOption("Go back", OptionId.GO_TO_MAIN, null);
//							options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
                        } else {
                            boolean addSPDisengage = true;
                            if (canDisengage() || !playerHasReadyShips) {
                                options.addOption("Attempt to disengage", OptionId.ATTEMPT_TO_DISENGAGE, getString("tootipAttemptToDisengage"));

                                addSPDisengage = true;

                            } else {
                                boolean hasStation = false;
                                boolean allStation = true;
                                for (CampaignFleetAPI curr : context.getBattle().getSideFor(otherFleet)) {
                                    allStation &= curr.isStationMode();
                                    hasStation |= curr.isStationMode();
                                }

                                if (hasStation) {
                                    if (allStation) {
                                        options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipCleanDisengage"));
                                    } else {
                                        options.addOption("Disengage", OptionId.DISENGAGE, getString("tooltipHarrassableDisengage"));
                                    }
                                    addSPDisengage = false;
                                }
                            }
                            if (addSPDisengage) {
                                //options.addOption("Execute a series of special maneuvers, allowing you to disengage cleanly", OptionId.DISENGAGE);
                                options.addOption("Disengage by executing a series of special maneuvers", OptionId.CLEAN_DISENGAGE,
                                        "Allows your fleet to disengage without being pursued.");
                                SetStoryOption.set(dialog, 1, OptionId.CLEAN_DISENGAGE, "cleanDisengage", Sounds.STORY_POINT_SPEND_COMBAT,
                                        "Maneuvered to disengage from " + otherFleet.getNameWithFactionKeepCase());

                                addEmergencyRepairsOption();
                            }
                        }
                    }
                } else {
                    options.addOption("Leave", OptionId.LEAVE, null);
                    options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
//					options.addOption("Go back", OptionId.GO_TO_MAIN, null);
//					options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
                }
            }
        }

        if (playerOutBeforeAllies()) {
            if (!options.hasOption(OptionId.LEAVE) &&
                    !options.hasOption(OptionId.LET_THEM_GO) &&
                    !options.hasOption(OptionId.DISENGAGE)) {
                options.addOption("Leave", OptionId.LEAVE, null);
                options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
            }
        }

        if (Global.getSettings().isDevMode()) {
            DevMenuOptions.addOptions(dialog);
        }

        // if it's an ongoing battle, this will all get cleared out by a subsequent call to updatePreCombat()
//		if (!options.hasOption(OptionId.GO_TO_MAIN)) {
//			options.addOption("Go back", OptionId.GO_TO_MAIN, null);
//			options.setShortcut(OptionId.GO_TO_MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
//		}

//		if (Global.getSettings().isDevMode()) {
//			DevMenuOptions.addOptions(dialog);
//		}
    }
}
