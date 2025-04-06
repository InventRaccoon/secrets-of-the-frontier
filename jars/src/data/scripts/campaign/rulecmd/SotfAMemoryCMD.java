// rulecommands for A Memory
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.intel.quests.SotfAMemoryIntel;
import data.scripts.dialog.SotfAMemoryDialogScript;
import data.scripts.campaign.plugins.amemory.SotfAMemoryFIDPluginImpl;

import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY;

public class SotfAMemoryCMD extends BaseCommandPlugin

{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);

        switch (cmd) {
            case "canHarmonicTuning":
                boolean phaseFrigate = false;
                for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
                    if (member.isPhaseShip() && member.isFrigate()) {
                        phaseFrigate = true;
                        break;
                    }
                }
                return phaseFrigate;
            case "startCombat":
                SotfAMemoryIntel intel = (SotfAMemoryIntel) Global.getSector().getIntelManager().getFirstIntel(SotfAMemoryIntel.class);
                if (intel != null && !intel.getStage().equals(SotfAMemoryIntel.AMemoryStage.DONE)) {
                    intel.setStage(SotfAMemoryIntel.AMemoryStage.FIGHT);
                    intel.sendUpdate(SotfAMemoryIntel.AMemoryStage.FIGHT, dialog.getTextPanel());
                } else {
                    intel = new SotfAMemoryIntel();
                    Global.getSector().getIntelManager().addIntel(intel, false, null);
                    intel.setStage(SotfAMemoryIntel.AMemoryStage.FIGHT);
                    intel.sendUpdate(SotfAMemoryIntel.AMemoryStage.FIGHT, dialog.getTextPanel());
                }

                SotfAMemoryDialogScript script = new SotfAMemoryDialogScript();
                Global.getSector().addScript(script);
                playerFleet.addEventListener(script);

                final SectorEntityToken entity = dialog.getInteractionTarget();
                final CampaignFleetAPI enemyFleetTemp = Global.getFactory().createEmptyFleet(Global.getSector().getFaction(Factions.HEGEMONY).getId(), "A memory", false);
                enemyFleetTemp.setNoFactionInName(true);
                enemyFleetTemp.getMemoryWithoutUpdate().set(MEMORY_KEY_NO_SHIP_RECOVERY, true);
                enemyFleetTemp.getMemoryWithoutUpdate().set("$sotf_AMemoryFight", null);

                // the Big One
                FleetMemberAPI phoenix = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "onslaught_Elite");
                phoenix.setShipName("HSS Phoenix");
                phoenix.setFlagship(true);
                enemyFleetTemp.getFleetData().addFleetMember(phoenix);

                // the rest of them. Enough fighter cover, Reapers and Sabots to make anyone cry. If only you could have shields AND a phase cloak...
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "condor_Strike"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "condor_Attack"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "condor_Attack"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "condor_Support"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "gryphon_Standard"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "enforcer_Assault"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "enforcer_Balanced"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hound_Standard"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hound_Standard"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "lasher_CS"));
                enemyFleetTemp.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "lasher_CS"));

                // enemy fleet gets officers to make things a bit trickier
                for (FleetMemberAPI member : enemyFleetTemp.getFleetData().getMembersListCopy()) {
                    member.setOwner(1);
                    member.getRepairTracker().setCR(0.7f);
                    member.getRepairTracker().setCrashMothballed(false);
                    member.getRepairTracker().setMothballed(false);

                    int level = 2;
                    OfficerManagerEvent.SkillPickPreference type = FleetFactoryV3.getSkillPrefForShip(member);
                    if (member.isDestroyer()) {
                        level = 3;
                    } else if (member.isCruiser()) {
                        level = 4;
                    } else if (member.isCapital()) {
                        level = 7;
                    }
                    PersonAPI person = OfficerManagerEvent.createOfficer(Global.getSector().getFaction(Factions.HEGEMONY), level, type, Misc.random);
                    member.setCaptain(person);
                }

                // would autofit the fleet, we don't do it because it needs to match Nothing Personal's loadouts exactly
                //enemyFleetTemp.inflateIfNeeded();

                //playerFleet.addEventListener(new SotfAMemoryDialogScript());

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
                config.pullInAllies = false;
                config.pullInEnemies = false;
                config.pullInStations = false;
                config.lootCredits = false;
                config.straightToEngage = true;

                config.firstTimeEngageOptionText = "Engage";
                config.afterFirstTimeEngageOptionText = "Re-engage";
                config.noSalvageLeaveOptionText = "Continue";

                config.dismissOnLeave = false;
                config.printXPToDialog = true;

                final SotfAMemoryFIDPluginImpl plugin = new SotfAMemoryFIDPluginImpl(config);

                final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
                config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    @Override
                    public void notifyLeave(InteractionDialogAPI dialog) {
                        //Global.getSector().addScript(new SotfAMemoryDialogScript());
                        // nothing in there we care about keeping; clearing to reduce savefile size
                        enemyFleetTemp.getMemoryWithoutUpdate().clear();
                        // there's a "standing down" assignment given after a battle is finished that we don't care about
                        enemyFleetTemp.clearAssignments();
                        enemyFleetTemp.deflate();

                        //dialog.setPlugin(originalPlugin);
                        //dialog.setInteractionTarget(entity);

                        //Global.getSector().getCampaignUI().clearMessages();

                        /*if (plugin.getContext() instanceof FleetEncounterContext) {
                            FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                            if (context.didPlayerWinEncounterOutright()) {

                                SalvageGenFromSeed.SDMParams p = new SalvageGenFromSeed.SDMParams();
                                p.entity = entity;
                                p.factionId = enemyFleetTemp.getFaction().getId();

                                entity.removeScriptsOfClass(FleetAdvanceScript.class);
                                dialog.dismiss();
                            } else {
                                dialog.dismiss();
                            }
                        } else {*/
                            dialog.dismiss();
                        //}
                    }
                    @Override
                    public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                        bcc.aiRetreatAllowed = false;
                        bcc.enemyDeployAll = true;
                    }
                    @Override
                    public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {

                    }

                };

                dialog.setPlugin(plugin);
                plugin.init(dialog);
                return true;
            case "checkFightStarted":
                return Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatStarted");
            case "checkFightWon":
                return Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatWon");
            default:
                return true;
        }
    }
}
