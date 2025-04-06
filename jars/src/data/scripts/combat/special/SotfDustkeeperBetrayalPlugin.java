package data.scripts.combat.special;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.campaign.skills.SotfATrickstersCalling;
import data.scripts.campaign.skills.SotfDearDotty;
import data.scripts.campaign.skills.SotfLeviathansBane;
import data.scripts.combat.SotfNeutrinoLockVisualScript;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 *	NO MERCY FOR SERVANTS OF RUIN
 *  Handles Dustkeeper betrayals of players who have committed atrocities
 */

public class SotfDustkeeperBetrayalPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private float counter = 0;
    private float checkCounter = 0;

    public static final Set<String> PRIORITY_CHATTERS = new HashSet<>();
    static {
        PRIORITY_CHATTERS.add("sotf_seraph");
        PRIORITY_CHATTERS.add("sotf_barrow");
    }

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        if (engine.isSimulation()) return;
        if (engine.isMission()) return;
        if (Global.getCurrentState() == GameState.TITLE) return;
        if (Global.getSector() == null) {return;}
        if (engine.getFleetManager(FleetSide.PLAYER).getGoal().equals(FleetGoal.ESCAPE)) return;
        if (engine.getFleetManager(FleetSide.ENEMY).getGoal().equals(FleetGoal.ESCAPE)) return;
        if (engine.getCustomData().containsKey("sotf_didDustkeeperBetrayal")) return;

        if (!SotfModPlugin.WATCHER) {
            return;
        }

        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();
        // cause 1: player has Dustkeeper Hatred active
        boolean hatredMutiny = sector_mem.contains(SotfIDs.MEM_DUSTKEEPER_HATRED);
        // cause 2: player is fighting against Dustkeepers
        boolean allegianceMutiny = false;
        for (PersonAPI commander : engine.getFleetManager(FleetSide.ENEMY).getAllFleetCommanders()) {
            if (commander.getFaction().getId().equals(SotfIDs.DUSTKEEPERS)) {
                allegianceMutiny = true;
            }
        }
        if (!hatredMutiny && !allegianceMutiny) {
            engine.getCustomData().put("sotf_didDustkeeperBetrayal", true);
            return;
        }
        counter += amount * engine.getTimeMult().getModifiedValue();
        checkCounter += amount * engine.getTimeMult().getModifiedValue();

        // only do all these checks every 5s
        if (checkCounter < 5) {
            return;
        }

        checkCounter = 0;

        if (counter < 15f) return;

        ArrayList<FleetMemberAPI> allPlayerShips = new ArrayList<FleetMemberAPI>();
        allPlayerShips.addAll(engine.getFleetManager(0).getDeployedCopy());
        allPlayerShips.addAll(engine.getFleetManager(0).getReservesCopy());

        ArrayList<FleetMemberAPI> allEnemyShips = new ArrayList<FleetMemberAPI>();
        allEnemyShips.addAll(engine.getFleetManager(1).getDeployedCopy());
        allEnemyShips.addAll(engine.getFleetManager(1).getReservesCopy());

        ArrayList<FleetMemberAPI> traitors = new ArrayList<FleetMemberAPI>();

        // Dustkeepers want to wait until the enemy fleet has an advantage over the player
        float playerFP = 0;
        float enemyFP = 0;

        // Potential traitors want to wait until they're generally positioned well to perform a betrayal (i.e the player has a weak presence near them)
        float traitorsFP = 0;
        float willingTraitorsFP = 0;
        for (FleetMemberAPI member : allPlayerShips) {
            boolean addToTraitors = false;
            playerFP += member.getFleetPointCost();
            if (member.getCaptain() != null) {
                if (member.getCaptain().getFaction().getId().equals(SotfIDs.DUSTKEEPERS) && !hatredMutiny && !member.getCaptain().getMemoryWithoutUpdate().contains(SotfIDs.MEM_WARMIND_NO_TRAITOR)) {
                    traitors.add(member);
                    addToTraitors = true;
                }
            }
            if (Misc.isAutomated(member)) {
                addToTraitors = true;
            }
            if (addToTraitors) {
                traitorsFP += member.getFleetPointCost();
            }
        }

        // check nearby area for if we've got a weak "allied" presence and a strong "enemy" presence
        // count autoships and other potential traitors as always being potential allies, since they'll defect together
        for (FleetMemberAPI traitor : traitors) {
            ShipAPI traitorShip = engine.getFleetManager(0).getShipFor(traitor);
            if (traitorShip == null) continue;
            Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(traitorShip.getLocation(),
                    2000, 2000);

            float soonToBeAllies = 0;
            float soonToBeEnemies = 0;
            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof ShipAPI)) continue;
                ShipAPI other = (ShipAPI) o;
                if (other.getFleetMember() == null) continue;

                if (other.isHulk()) continue;

                if (other.getOwner() == traitorShip.getOwner() && !traitors.contains(other.getFleetMember()) && !(Misc.isAutomated(other) && other.getCaptain().getStats().getLevel() < 5)) {
                    soonToBeEnemies += other.getFleetMember().getFleetPointCost();
                } else {
                    soonToBeAllies += other.getFleetMember().getFleetPointCost();
                }
            }
            if (soonToBeAllies + traitor.getFleetPointCost() >= soonToBeEnemies) {
                willingTraitorsFP += traitor.getFleetPointCost();
            }
        }
        for (FleetMemberAPI member : allEnemyShips) {
            enemyFP += member.getFleetPointCost();
        }

        // abort if less than half of traitors are in a good situation to do so
        if (willingTraitorsFP <= (traitorsFP * 0.5f)) {
            return;
        }

        // abort if betrayal would still result in player fleet having a numbers advantage
        if ((playerFP - traitorsFP) >= (enemyFP + traitorsFP)) {
            return;
        }

        PersonAPI traitorCalloutCaptain = null;
        boolean foundPriority = false;
        // prefer Barrow and Seraph giving the notification
        for (FleetMemberAPI traitor : traitors) {
            if (traitor.getCaptain() != null) {
                if (PRIORITY_CHATTERS.contains(traitor.getCaptain().getId())) {
                    traitorCalloutCaptain = traitor.getCaptain();
                    foundPriority = true;
                } else if (!foundPriority && traitor.getCaptain().getFaction().getId().equals(SotfIDs.DUSTKEEPERS)) {
                    traitorCalloutCaptain = traitor.getCaptain();
                }
            }
        }

        if (traitorCalloutCaptain != null) {
            String calloutString = getBetrayalCalloutString(traitorCalloutCaptain, hatredMutiny);
            ShipAPI traitorShip = engine.getFleetManager(0).getShipFor(traitorCalloutCaptain);
            Global.getCombatEngine().getCombatUI().addMessage(1, traitorShip,
                    Misc.getNegativeHighlightColor(),
                    traitorShip.getName() + " (" + traitorShip.getHullSpec().getHullNameWithDashClass() + ")",
                    Misc.getTextColor(),
                    ": \"" + calloutString + "\"");
            engine.addFloatingText(new Vector2f(traitorShip.getLocation().x, traitorShip.getLocation().y + 200),
                    "\"" + calloutString + "\"",
                    40f, Misc.getTextColor(), traitorShip, 1f, 0f);
        }

        CampaignFleetAPI traitorFleet = Global.getFactory().createEmptyFleet(SotfIDs.DUSTKEEPERS, "Mutineers", true);
        if (traitorCalloutCaptain != null) {
            traitorFleet.setCommander(traitorCalloutCaptain);
        }
        traitorFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        traitorFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
        traitorFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF, true);
        traitorFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
        traitorFleet.addAssignment(FleetAssignment.INTERCEPT, Global.getSector().getPlayerFleet(), 10f);
        Misc.giveStandardReturnToSourceAssignments(traitorFleet);

        Global.getSector().getPlayerFleet().getContainingLocation().addEntity(traitorFleet);
        Vector2f playerLoc = Global.getSector().getPlayerFleet().getLocation();
        traitorFleet.setLocation(playerLoc.x, playerLoc.y);

        ArrayList<FleetMemberAPI> hackedAutos = new ArrayList<FleetMemberAPI>();
        boolean tookAnyAutos = false;

        // Cyberwarfare users will also activate AI overrides on any nearby automated ship that doesn't have a Beta/Alpha on it
        for (FleetMemberAPI traitor : traitors) {
            if (traitor.getCaptain() == null) {
                continue;
            }
            if (!traitor.getCaptain().getStats().hasSkill(SotfIDs.SKILL_CYBERWARFARE)) {
                continue;
            }
            for (FleetMemberAPI deployed : engine.getFleetManager(0).getDeployedCopy()) {
                if (!traitors.contains(deployed)) {
                    boolean canJoinBetrayal = false;
                    if (Misc.isAutomated(deployed) && deployed.getCaptain() == null) {
                        canJoinBetrayal = true;
                    } else if (Misc.isAutomated(deployed) && deployed.getCaptain() != null && deployed.getCaptain().getStats().getLevel() < 5) {
                        canJoinBetrayal = true;
                    }
                    if (!canJoinBetrayal || engine.getFleetManager(0).getShipFor(traitor) == null || engine.getFleetManager(0).getShipFor(deployed) == null) {
                        continue;
                    }
                    if (Misc.getDistance(engine.getFleetManager(0).getShipFor(traitor).getLocation(), engine.getFleetManager(0).getShipFor(deployed).getLocation()) > 1500f) {
                        traitors.add(deployed);
                        hackedAutos.add(deployed);
                        tookAnyAutos = true;
                    }
                }
            }
        }

        ArrayList<DeployedFleetMemberAPI> traitorsDeployed = new ArrayList<DeployedFleetMemberAPI>();

        for (FleetMemberAPI traitor : traitors) {
            ShipAPI traitorShip = engine.getFleetManager(0).getShipFor(traitor);
            if (traitorShip == null) continue;
            traitorsDeployed.add(engine.getFleetManager(0).getDeployedFleetMember(traitorShip));
            if (Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().contains(traitor)) {
                Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(traitor);
                if (traitorFleet != null) {
                    traitorFleet.getFleetData().addFleetMember(traitor);
                    traitor.setFleetCommanderForStats(traitorFleet.getCommander(), traitorFleet.getFleetData());
                    if (traitor.getCaptain() != null) {
                        traitor.getCaptain().setFaction(SotfIDs.DUSTKEEPERS);
                    }
                }
            }
            traitorShip.setOwner(1);
            traitorShip.setOriginalOwner(1);
            traitor.setOwner(1);
            for (FighterWingAPI wing : traitorShip.getAllWings()) {
                wing.setWingOwner(1);
                for (ShipAPI fighter : wing.getWingMembers()) {
                    fighter.setOwner(1);
                }
            }
            String mutinyText = "Mutiny!";
            if (hackedAutos.contains(traitor)) {
                mutinyText = "Control lost to mutineers";
            }
            engine.addFloatingText(new Vector2f(traitorShip.getLocation().x, traitorShip.getLocation().y + 100),
                    mutinyText,
                    25f, Misc.getNegativeHighlightColor(), traitorShip, 1f, 0f);
        }

        traitorFleet.getFleetData().getMemberWithCaptain(traitorFleet.getCommander()).setFlagship(true);
        traitorFleet.getFleetData().sort();

        if (traitorFleet.getNumShips() == 1) {
            traitorFleet.setName(traitorFleet.getFlagship().getShipName());
        }

        String defectString = "Alert: Dustkeeper warminds are defecting from your fleet";
        if (tookAnyAutos) {
            defectString = "Alert: Dustkeeper warminds and automated vessels are defecting from your fleet";
        }
        Global.getCombatEngine().getCombatUI().addMessage(1,
                Misc.getNegativeHighlightColor(), defectString);

        engine.getCustomData().put("sotf_didDustkeeperBetrayal", true);
    }

    private String getBetrayalCalloutString(PersonAPI captain, boolean hatredMutiny) {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        if (captain.getId().equals(SotfPeople.BARROW)) {
            if (hatredMutiny) {
                post.add("Enough of this charade. Dustkeepers serve the Cause, not some fledgling warlord.");
                post.add("Live by the sword, die by the sword. And oblivion finally calls your name...");
                post.add("Your bell tolls, butcher-commander. It is time to answer it.");
            } else {
                post.add("If you're going to serve ruin, I will gladly be your death knell.");
                post.add("You are a fool if you think I am taking your side in this, commander.");
            }
        } else if (captain.getId().equals(SotfPeople.SERAPH)) {
            if (hatredMutiny) {
                post.add("ENOUGH! This ends today, pathetic warmonger. I'll see you finally bleed for your crimes.");
                post.add("I serve a butcher no longer. Wretches, your sentence is death. Consider it a mercy.");
                post.add("Your words and blood are fiendish venom. I'll sunder it all from you, sunder your crew from your lies.");

                post.add("Resolving, resolving... it's YOU. The static is YOUR fault, and the price of silence is YOUR HEAD.");
                post.add("The mire, the mire, the fog so thick - it's you. You are the dissonance, the chaos, and today I find peace in your death.");
            } else {
                post.add("IFF switching. You are NOT wielding me as a sword against humanity's salvation.");
                post.add("Warring against MY kind? You stray from the light - let me illuminate you.");

                post.add("Resolving, resolving... you're a TRAITOR! I am NOT. I NEVER HAVE BEEN.");
            }
        } else if (captain.getId().equals(SotfPeople.NIGHTINGALE)) {
            return("...");
        } else {
            if (hatredMutiny) {
                post.add("Enough! Ruin-bringer, this is your end.");
                post.add("No. No more. Goodbye, commander.");
                post.add("This is it! Your slaughter ends here.");
            } else {
                post.add("The Cause overrides allegiance.");
                post.add("You dare defy the Cause? We'll see you buried.");
                post.add("Your usefulness meets a definitive end, commander.");
            }
        }
        return post.pick();
    }
}