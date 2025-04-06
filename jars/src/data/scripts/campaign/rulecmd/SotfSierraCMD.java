// rulecommands for Sierra stuff
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.RemoveShip;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SotfSierraCMD extends BaseCommandPlugin

{
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    @Override
    public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap)
    {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        final MemoryAPI memory = getEntityMemory(memoryMap);
        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);
        String param = null;
        String param2 = null;
        if (params.size() > 1) {
            param = params.get(1).getString(memoryMap);
        }
        if (params.size() > 2) {
            param2 = params.get(2).getString(memoryMap);
        }

        final TextPanelAPI text = dialog.getTextPanel();

        PersonAPI sierra = Global.getSector().getImportantPeople().getPerson(SotfPeople.SIERRA);
        FleetMemberAPI sierraMember = null;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.getVariant().getHullMods().contains(SotfIDs.SIERRAS_CONCORD) && !member.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                sierraMember = member;
            }
        }

        switch (cmd) {
            case "checkSierra":
                return SotfMisc.playerHasSierra();
            case "transferVow":
                Misc.fadeAndExpire(dialog.getInteractionTarget());

                boolean switchedOut = false;
                for (FleetMemberAPI pledge : playerFleet.getFleetData().getMembersListCopy()) {
                    if (pledge.getVariant().getHullMods().contains(SotfIDs.SIERRAS_CONCORD) && !pledge.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                        switchedOut = true;
                        SotfMisc.toggleSierra(pledge, text);
                    }
                }

                FleetMemberAPI vow = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "sotf_vow_Base");
                vow.setShipName("Voidwitch");
                vow.getRepairTracker().setCR(0.7f);
                playerFleet.getFleetData().addFleetMember(vow);

                text.setFontSmallInsignia();
                String str = vow.getShipName() + ", " + vow.getHullSpec().getHullNameWithDashClass() + " " + vow.getHullSpec().getDesignation();
                String gotStr = "Acquired ";
                if (switchedOut) {
                    gotStr = "Transferred Sierra to ";
                }
                text.addParagraph(gotStr + str, Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra", str);
                text.setFontInsignia();

                // 7 to 8
                SotfMisc.levelUpSierra(8);

                text.setFontSmallInsignia();
                text.addParagraph( "Sierra's mastery has grown", Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra");
                text.setFontInsignia();

                if (switchedOut) {
                    vow.setCaptain(sierra);
                    text.setFontSmallInsignia();
                    text.addParagraph("Speak to Sierra to transfer her between Concord ships within your fleet", Misc.getHighlightColor());
                    text.setFontInsignia();
                } else {
                    vow.getVariant().addTag(SotfIDs.TAG_INERT);
                    Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierra_var", "sotf_vow");
                }
                return true;
            case "addWispersong":
                Global.getSector().getCharacterData().addHullMod(SotfIDs.HULLMOD_WISPERSONG);
                text.setFontSmallInsignia();
                text.addParagraph("Unlocked hullmod: Wispersong", Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Wispersong");
                text.addParagraph("- Wispersong causes EM-anomalies to appear when nearby ships are destroyed, " +
                        "flocking to and attacking other hostile vessels", Misc.getHighlightColor());
                text.setFontInsignia();
                return true;
            case "highSierraRep":
                return sierra.getRelToPlayer().getLevel().equals(RepLevel.COOPERATIVE);
            case "addSoulbond":
                Global.getSector().getCharacterData().addHullMod(SotfIDs.HULLMOD_SOULBOND);
                text.setFontSmallInsignia();
                text.addParagraph("Unlocked hullmod: Soulbond", Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Soulbond");
                text.addParagraph("- Soulbond allows the flagship to order a Recall (transporting Sierra to it) " +
                        "or Kinskip (transporting it to Sierra)", Misc.getHighlightColor());
                text.setFontInsignia();
                return true;
            case "checkSierraPersonality":
                return sierra.getPersonalityAPI().getId().equals(param);
            case "setSierraPersonality":
                sierra.setPersonality(param);
                text.setFontSmallInsignia();
                text.addParagraph( "Sierra will now be " + Misc.getPersonalityName(sierra) + " in combat", Misc.getPositiveHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra");
                text.highlightInLastPara(Misc.getHighlightColor(), Misc.getPersonalityName(sierra));
                text.setFontInsignia();
                return true;
            case "canSwitchShip":
                return SotfMisc.playerHasInertConcord() && !SotfModPlugin.NEW_SIERRA_MECHANICS;
            case "openShipSwitcher":
                List<FleetMemberAPI> switchTargets = new ArrayList<>();

                for (FleetMemberAPI switchTarget : playerFleet.getMembersWithFightersCopy()) {
                    if (switchTarget.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && switchTarget.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                        switchTargets.add(switchTarget);
                    }
                }

                dialog.showFleetMemberPickerDialog("Transfer Sierra to new ship", "Confirm", "Cancel", 1, Math.max(4, switchTargets.size()), 88f, true, false, switchTargets, new FleetMemberPickerListener() {

                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members != null && !members.isEmpty()) {
                            FleetMemberAPI existing = null;
                            for (FleetMemberAPI existingMember : playerFleet.getMembersWithFightersCopy()) {
                                if (existingMember.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !existingMember.getVariant().hasTag(SotfIDs.TAG_INERT)) {
                                    existing = existingMember;
                                }
                            }
                            FleetMemberAPI newMember = members.get(0);

                            SotfMisc.toggleSierra(newMember, text);

                            if (existing != null) {
                                SotfMisc.toggleSierra(existing, text);
                            }
                            memoryMap.get(MemKeys.LOCAL).set("$sotf_newSierraShip", newMember.getHullSpec().getHullId(), 0);
                            Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierra_var", newMember.getHullSpec().getHullId());
                            FireBest.fire(null, dialog, memoryMap, "SierraSwitchedShip");
                        }
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {

                    }
                });
                return true;
            case "decrementThoughts":
                int numThoughts = Global.getSector().getMemoryWithoutUpdate().getInt(SotfIDs.MEM_NUM_SIERRA_THOUGHTS);
                Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_NUM_SIERRA_THOUGHTS, numThoughts - 1);
                return true;
            case "loseShipToBetrayal":
                if (sierraMember != null) {
                    playerFleet.getFleetData().removeFleetMember(sierraMember);
                    RemoveShip.addShipLossText(sierraMember, text);
                }
                SotfMisc.addGuilt(8f);
                return true;
            case "loseCoreToBetrayal":
                Global.getSector().getPlayerFleet().getCargo().removeItems(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);
                text.setFontSmallInsignia();
                text.addParagraph("Lost Sierra Core", Misc.getNegativeHighlightColor());
                text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra Core");
                text.setFontInsignia();
                SotfMisc.addGuilt(5f);
                return true;
            case "ttSciTauntConsequences":
                DelayedFleetEncounter e = new DelayedFleetEncounter(Misc.getRandom(Misc.genRandomSeed(), 0), "sotf_TTsciTauntConsequences");
                e.setDelayMedium();
                e.setLocationInnerSector(false, Factions.MERCENARY);
                e.beginCreate();
                e.triggerCreateFleet(HubMissionWithTriggers.FleetSize.MEDIUM, HubMissionWithTriggers.FleetQuality.SMOD_2, Factions.MERCENARY, FleetTypes.PATROL_MEDIUM, new Vector2f());
                e.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.ALL_SHIPS, HubMissionWithTriggers.OfficerQuality.UNUSUALLY_HIGH);
                e.triggerFleetSetFaction(Factions.INDEPENDENT);
                e.triggerFleetMakeFaster(true, 1, true);
                e.triggerSetFleetFlag("$sotf_TTsciRevenge");
                e.triggerSetFleetMemoryValue("$sotf_TTsciFullName", SotfPeople.getPerson(SotfPeople.TT_PROJECTLEAD).getNameString());
                e.triggerSetFleetMemoryValue("$sotf_TTscihisOrHer", Misc.ucFirst(SotfPeople.getPerson(SotfPeople.TT_PROJECTLEAD).getHisOrHer()));
                e.triggerMakeNoRepImpact();
                e.triggerSetStandardAggroInterceptFlags();
                e.endCreate();
                return true;
            case "noSatbombConsequences":
                return SotfMisc.getSierraNoSatbombConsequences();
            case "sierraLeaves":
                if (sierraMember != null) {
                    Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).setRelationship(Factions.PLAYER, -0.4f);

                    playerFleet.getFleetData().removeFleetMember(sierraMember);

                    text.setFontSmallInsignia();
                    String shipString = sierraMember.getShipName() + ", " + sierraMember.getHullSpec().getHullNameWithDashClass() + " " + sierraMember.getHullSpec().getDesignation();
                    text.addParagraph(shipString + " has left your fleet", Misc.getNegativeHighlightColor());
                    text.highlightInLastPara(Misc.getHighlightColor(), shipString);
                    text.setFontInsignia();

                    //RemoveShip.addShipLossText(sierraMember, text);

                    CampaignFleetAPI sierraFleet = Global.getFactory().createEmptyFleet(SotfIDs.SIERRA_FACTION, sierraMember.getShipName(), true);
                    sierraFleet.setNoFactionInName(true);
                    sierraFleet.setTransponderOn(true);
                    sierraFleet.setCommander(sierra);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);

                    sierraFleet.getFleetData().addFleetMember(sierraMember);
                    sierraMember.setFlagship(true);
                    sierraMember.setCaptain(sierra);
                    sierraMember.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
                    sierraMember.getVariant().addTag(Tags.VARIANT_DO_NOT_DROP_AI_CORE_FROM_CAPTAIN);

                    Global.getSector().getPlayerFleet().getContainingLocation().addEntity(sierraFleet);
                    sierraFleet.setLocation(Global.getSector().getPlayerFleet().getLocation().x + 25f, Global.getSector().getPlayerFleet().getLocation().y + 25f);

                    // seems like a solid place to lay low
                    String returnMarket = "new_maxios";
                    if (Global.getSector().getEconomy().getMarket("new_maxios") == null) {
                        returnMarket = SotfMisc.pickNPCMarket(Factions.INDEPENDENT).getId();
                    }
                    Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_SIERRA_LEFT, true);
                    Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_SIERRA_ABANDON_MARKET, returnMarket);
                    Misc.addDefeatTrigger(sierraFleet, "sotfDefeatedLeavingSierra");

                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, returnMarket);
                    Misc.giveStandardReturnToSourceAssignments(sierraFleet);
                }
                return true;
            case "attackSierra":
                if (sierraMember != null) {
                    Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).setRelationship(Factions.PLAYER, -0.6f);

                    playerFleet.getFleetData().removeFleetMember(sierraMember);

                    text.setFontSmallInsignia();
                    String shipString = sierraMember.getShipName() + ", " + sierraMember.getHullSpec().getHullNameWithDashClass() + " " + sierraMember.getHullSpec().getDesignation();
                    text.addParagraph("Lost " + shipString + " to mutiny", Misc.getNegativeHighlightColor());
                    text.highlightInLastPara(Misc.getHighlightColor(), shipString);
                    text.setFontInsignia();

                    CampaignFleetAPI sierraFleet = Global.getFactory().createEmptyFleet(SotfIDs.SIERRA_FACTION, sierraMember.getShipName(), true);
                    sierraFleet.setNoFactionInName(true);
                    sierraFleet.setTransponderOn(true);
                    sierraFleet.setCommander(sierra);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);

                    sierraFleet.getFleetData().addFleetMember(sierraMember);
                    sierraMember.setFlagship(true);
                    sierraMember.setCaptain(sierra);
                    sierraMember.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
                    sierraMember.getVariant().addTag(Tags.VARIANT_DO_NOT_DROP_AI_CORE_FROM_CAPTAIN);

                    Global.getSector().getPlayerFleet().getContainingLocation().addEntity(sierraFleet);
                    sierraFleet.setLocation(Global.getSector().getPlayerFleet().getLocation().x + 25f, Global.getSector().getPlayerFleet().getLocation().y + 25f);

                    String returnMarket = "new_maxios";
                    if (Global.getSector().getEconomy().getMarket("new_maxios") == null) {
                        returnMarket = SotfMisc.pickNPCMarket(Factions.INDEPENDENT).getId();
                    }
                    Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_SIERRA_LEFT, true);
                    Global.getSector().getMemoryWithoutUpdate().set(SotfIDs.MEM_SIERRA_ABANDON_MARKET, returnMarket);
                    Misc.addDefeatTrigger(sierraFleet, "sotfDefeatedLeavingSierra");

                    sierraFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, returnMarket);
                    Misc.giveStandardReturnToSourceAssignments(sierraFleet);

                    // force player into interaction with the newly-created fleet
                    dialog.setInteractionTarget(sierraFleet);
                    final FIDConfig config = new FIDConfig();
                    config.showCommLinkOption = false;
                    final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);
                    dialog.setPlugin(plugin);
                    plugin.init(dialog);
                }
                return true;
            case "isLeaveMarket":
                if (dialog.getInteractionTarget().getMarket() == null) return false;
                String abandonMarketId = Global.getSector().getMemoryWithoutUpdate().getString(SotfIDs.MEM_SIERRA_ABANDON_MARKET);
                return abandonMarketId.equals(dialog.getInteractionTarget().getMarket().getId());
            case "addSierraXO":
                if (SotfMisc.isSecondInCommandEnabled()) {
                    Global.getSector().getMemoryWithoutUpdate().set("$sotf_gotSierraXO", true);
                    SCOfficer officer = new SCOfficer(sierra, "sotf_witchcraft");
                    officer.increaseLevel(2);

                    SCData data = SCUtils.getPlayerData();
                    data.addOfficerToFleet(officer);
                    data.setOfficerInEmptySlotIfAvailable(officer);
                    Global.getSector().getMemoryWithoutUpdate().set("$sotf_sierraXO", officer);

                    text.setFontSmallInsignia();
                    text.addParagraph( "Sierra can now serve as a \"Witchcraft\" executive officer", Misc.getPositiveHighlightColor());
                    text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra", "\"Witchcraft\"");
                    text.setFontInsignia();

                    SCUtils.showSkillOverview(dialog, officer);
                }
                return true;
            case "canGetXO":
                return Global.getSector().getCharacterData().knowsHullMod(SotfIDs.HULLMOD_WISPERSONG) && sierra.getStats().getLevel() >= 8;
            default:
                return true;
        }
    }
}
