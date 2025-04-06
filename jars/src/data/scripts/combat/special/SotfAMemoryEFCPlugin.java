// every frame plugin for the A Memory fight
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfFervor;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static data.hullmods.SotfFervor.CR_LOSS_MULT_FOR_EMERGENCY_DIVE;

public class SotfAMemoryEFCPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private BattleCreationContext context;
    // tracks # of seconds passed in combat
    private float counter = 0;
    // tracks # of seconds passed with a Concord ship deployed
    private float sierraCounter = 0;
    // is this the first time the player has fought this battle?
    private boolean repeat = true;
    // player has Harmonic Tuning story point option
    private boolean harmonicTuning = false;
    // various bits of dialogue
    private boolean initial = false;
    private boolean first = false;
    private boolean second = false;
    private boolean third = false;
    private boolean fourth = false;
    private boolean fifth = false;
    private boolean sixth = false;
    private boolean last = false;
    // the player's Concord ship
    private ShipAPI sierra = null;
    // ISS Athena
    private ShipAPI athena = null;
    // harmonically tuned phase frigate
    private ShipAPI tuned = null;

    private boolean didStressWarning = false;
    private boolean didStressCallout = false;

    public SotfAMemoryEFCPlugin(BattleCreationContext context) {
        this.context = context;
    }
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.setDoNotEndCombat(true);
        if (!Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatStarted")) {
            Global.getSector().getMemoryWithoutUpdate().set("$sotf_AMemoryCombatStarted", true);
            repeat = false;
        }
        if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMHarmonicTuning")) {
            harmonicTuning = true;
        }
        engine.getCustomData().put("$sotf_noSierraChatter", true);
        engine.getCustomData().put("$sotf_AMemory", true);
        engine.getCustomData().put(SotfIDs.INVASION_NEVER_KEY, true);
    }

    public void advance(float amount, List<InputEventAPI> events) {
        Color sc = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).getBaseUIColor();
        CombatFleetManagerAPI playerManager = engine.getFleetManager(0);
        boolean sierraAlive = false;
        boolean athenaAlive = false;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == 0) {
                if (ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) {
                    sierra = ship;
                    ship.getFleetMember().getCrewComposition().setCrew(0);
                    // add a free phase dive
                    if (!ship.hasListenerOfClass(SotfPhaseDiveScript.class) && !ship.hasListenerOfClass(SotfFervor.SotfFervorScript.class)) {
                        ship.addListener(new SotfPhaseDiveScript(ship));
                    }
                } else if (ship.getHullSpec().getHullId().equals("aurora") && ship.getName().equals("ISS Athena")) {
                    athena = ship;
                } else if (ship.isFrigate() && ship.getHullSpec().isPhase() && tuned == null && harmonicTuning) {
                    tuned = ship;
                }
            }
            // do we smell a cheater? I DON'T THINK SO, PAL
            if (ship.getOwner() == 0 && !ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !ship.getVariant().hasTag("sotf_amemory_immunity") && !ship.isFighter() && ship != tuned) {
                ship.setDefenseDisabled(true);
                ship.setShipSystemDisabled(true);
                ship.getMutableStats().getBallisticWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult("sotf_AMPhaseStress", 0.5f);
                ship.getMutableStats().getEnergyWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult("sotf_AMPhaseStress", 0.5f);
                ship.getMutableStats().getMissileWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                ship.getMutableStats().getMissileHealthBonus().modifyMult("sotf_AMPhaseStress", 0.25f);

                ship.getMutableStats().getHullDamageTakenMult().modifyMult("sotf_AMPhaseStress", 3f);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult("sotf_AMPhaseStress", 3f);
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult("sotf_AMPhaseStress", 3f);
                ship.getMutableStats().getWeaponMalfunctionChance().modifyPercent("sotf_AMPhaseStress", 50f);
                ship.getMutableStats().getCriticalMalfunctionChance().modifyPercent("sotf_AMPhaseStress", 30f);
                ship.getMutableStats().getFighterRefitTimeMult().modifyMult("sotf_AMPhaseStress", 3f);
                ship.getMutableStats().getMaxSpeed().modifyMult("sotf_AMPhaseStress", 0.5f);

                // AND YOUR FIGHTERS TOO, THEIR KNEECAPS AREN'T SAFE
                for (FighterWingAPI wing : ship.getAllWings()) {
                    for (ShipAPI fighter : wing.getWingMembers()) {
                        fighter.getMutableStats().getHullDamageTakenMult().modifyMult("sotf_AMPhaseStress", 6f);
                        fighter.getMutableStats().getArmorDamageTakenMult().modifyMult("sotf_AMPhaseStress", 6f);
                        fighter.getMutableStats().getShieldDamageTakenMult().modifyMult("sotf_AMPhaseStress", 6f);
                        fighter.getMutableStats().getMaxSpeed().modifyMult("sotf_AMPhaseStress", 0.5f);
                        fighter.getMutableStats().getBallisticWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                        fighter.getMutableStats().getEnergyWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                        fighter.getMutableStats().getMissileWeaponDamageMult().modifyMult("sotf_AMPhaseStress", 0.5f);
                        ship.getMutableStats().getMissileHealthBonus().modifyMult("sotf_AMPhaseStress", 0.25f);
                    }
                }

                ship.setJitter(this, SotfMisc.getSierraColor(), 0.3f, 3, 3);
                if (!didStressCallout) {
                    didStressCallout = true;
                    String phaseStressString = "Detected extreme dimensional instability, heavily recommend retreating unprotected vessels";
                    Global.getCombatEngine().getCombatUI().addMessage(0, ship, Misc.getNegativeHighlightColor(), phaseStressString);
                    Global.getSoundPlayer().playUISound("cr_allied_critical", 1f, 1f);
                }
            }
        }
        // this doesn't work??? wtf does removeFromReserves do?
        //for (FleetMemberAPI member : playerManager.getReservesCopy()) {
        //    if (!member.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !(member.isFrigate() && member.getHullSpec().isPhase() && tuned == null && harmonicTuning)) {
        //        playerManager.removeFromReserves(member);
        //    }
        //}

        // fine, we'll just yell at them instead and give them a million debuffs
        for (FleetMemberAPI deployable : engine.getCombatUI().getCurrentlySelectedInFleetDeploymentDialog()) {
            if (!deployable.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !(deployable.isFrigate() && deployable.getHullSpec().isPhase() && tuned == null && harmonicTuning)) {
                if (!didStressWarning) {
                    didStressWarning = true;
                    String phaseStressString = "Detected extreme dimensional instability, deployment of unprotected vessels is heavily unadvised";
                    Global.getCombatEngine().getCombatUI().addMessage(0, Misc.getNegativeHighlightColor(), phaseStressString);
                    Global.getSoundPlayer().playUISound("cr_allied_malfunction", 1f, 1f);
                }
            }
        }

        if (!engine.isPaused()) {
            counter += amount;
            if (sierra != null) {
                if (sierra.isAlive()) {
                    sierraAlive = true;
                    sierraCounter += amount;
                }
            }
            if (athena != null) {
                if (athena.isAlive()) {
                    athenaAlive = true;
                }
            }
        }
        if (counter > 1 && !initial && athenaAlive) {
            Global.getCombatEngine().getCombatUI().addMessage(1, athena, Misc.getPositiveHighlightColor(), "ISS Athena (Aurora-class)", Misc.getTextColor(), ": \"What the hell...? We've got warships on radar!\"");
            engine.addFloatingText(new Vector2f(athena.getLocation().x, athena.getLocation().y + 100),
                    "\"What the hell...? We've got warships on radar!\"",
                    40f, Misc.getTextColor(), athena, 1f, 0f);
            initial = true;
        }
        if (sierraCounter > 4 && !first && sierraAlive) {
            if (!repeat) {
            Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"What's... going...?\"");
            engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                    "\"What's... going...?\"",
                    40f, sc, sierra, 1f, 0f);
            } else {
                Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"Once more unto the breach!\"");
                engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                        "\"Once more unto the breach!\"",
                        40f, sc, sierra, 1f, 0f);
            }
            first = true;

        }
        if (counter > 7 && !second && athenaAlive) {
            Global.getCombatEngine().getCombatUI().addMessage(1, athena, Misc.getPositiveHighlightColor(), "ISS Athena (Aurora-class)", Misc.getTextColor(), ": \"Spread out! Don't get surrounded!\"");
            engine.addFloatingText(new Vector2f(athena.getLocation().x, athena.getLocation().y + 100),
                    "\"Spread out! Don't get surrounded!\"",
                    40f, Misc.getTextColor(), athena, 1f, 0f);
            second = true;
        }
        if (sierraCounter > 10 && !third && sierraAlive && !repeat) {
            Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"... Oh...\"");
            engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                    "\"... Oh...\"",
                    40f, sc, sierra, 1f, 0f);
            third = true;
        }
        if (sierraCounter > 13 && !fourth && sierraAlive && !repeat) {
            Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"I think we're in for a fight, Captain.\"");
            engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                    "\"I think we're in for a fight, Captain.\"",
                    40f, sc, sierra, 1f, 0f);
            fourth = true;
        }
        if (counter > 15 && !fifth && athenaAlive && sierraAlive) {
            if (!AIUtils.getNearbyAllies(sierra, 500f).isEmpty()) {
                Global.getCombatEngine().getCombatUI().addMessage(1, athena, Misc.getPositiveHighlightColor(), "ISS Athena (Aurora-class)", Misc.getTextColor(), ": \"Unknown vessel on our flanks. IFF friendly, watch your fire!\"");
                engine.addFloatingText(new Vector2f(athena.getLocation().x, athena.getLocation().y + 100),
                        "\"Unknown vessel on our flanks. IFF friendly, watch your fire!\"",
                        40f, Misc.getTextColor(), athena, 1f, 0f);
                fifth = true;
            }
        }
        if (counter > 20 && !sixth && sierraAlive && !AIUtils.getNearbyEnemies(sierra, 1200f).isEmpty() && !repeat) {
            if (!AIUtils.getNearbyEnemies(sierra, 1200f).isEmpty()) {
                Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"Are those... Hegemony ships?\"");
                engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                        "\"Are those... Hegemony ships?\"",
                        40f, sc, sierra, 1f, 0f);
                sixth = true;
            }
        }
        if (counter > 20 && engine.getFleetManager(1).getDeployedCopyDFM().isEmpty() && !last) {
            engine.setDoNotEndCombat(false);
            if (athenaAlive) {
                Global.getCombatEngine().getCombatUI().addMessage(1, athena, Misc.getPositiveHighlightColor(), "ISS Athena (Aurora-class)", Misc.getTextColor(), ": \"... we're clear! We're clear. Holy...\"");
                engine.addFloatingText(new Vector2f(athena.getLocation().x, athena.getLocation().y + 100),
                        "\"... we're clear! We're clear. Holy...\"",
                        40f, Misc.getTextColor(), athena, 1f, 0f);
            }
            if (sierraAlive) {
                Global.getCombatEngine().getCombatUI().addMessage(1, sierra, sc, sierra.getName() + " (" + sierra.getHullSpec().getHullNameWithDashClass() + "): \"Good work, Captain. Let's get out of here.\"");
                engine.addFloatingText(new Vector2f(sierra.getLocation().x, sierra.getLocation().y + 100),
                        "\"Good work, Captain. Let's get out of here.\"",
                        40f, sc, sierra, 1f, 0f);
            }
            last = true;
        }
        // end combat if the player loses their Concord ship
        if (sierra != null) {
            if (!sierra.isAlive()) {
                engine.endCombat(2f, FleetSide.ENEMY);
            }
        }
        // end combat if the player attempts to retreat
        if (engine.getFleetManager(0).getTaskManager(false).isInFullRetreat()) {
            engine.endCombat(10f, FleetSide.ENEMY);
        }
    }

    // grants a free phase dive so you don't die
    public static class SotfPhaseDiveScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public ShipAPI ship;
        public boolean emergencyDive = false;
        public float diveProgress = 0f;
        public FaderUtil diveFader = new FaderUtil(1f, 1f);
        public SotfPhaseDiveScript(ShipAPI ship) {
            this.ship = ship;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (!emergencyDive) {
                String key = "phaseAnchor_canDive";
                boolean canDive = !Global.getCombatEngine().getCustomData().containsKey(key);
                float depCost = 0f;
                if (ship.getFleetMember() != null) {
                    depCost = ship.getFleetMember().getDeployCost();
                }
                float crLoss = CR_LOSS_MULT_FOR_EMERGENCY_DIVE * depCost;

                float hull = ship.getHitpoints();
                if (damageAmount >= hull && canDive) {
                    ship.setHitpoints(1f);

                    if (ship.getFleetMember() != null) { // fleet member is fake during simulation, so this is fine
                        ship.getFleetMember().getRepairTracker().applyCREvent(-crLoss, "Emergency phase dive");
                    }
                    emergencyDive = true;
                    Global.getCombatEngine().getCustomData().put(key, true);

                    if (!ship.isPhased()) {
                        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                }
            }

            if (emergencyDive) {
                return true;
            }

            return false;
        }

        public void advance(float amount) {
            ShipSystemAPI cloak = ship.getSystem();
            String id = "phase_anchor_modifier";
            if (emergencyDive) {
                Color c = cloak.getSpecAPI().getEffectColor2();
                c = Misc.setAlpha(c, 255);
                c = Misc.interpolateColor(c, Color.white, 0.5f);

                if (diveProgress == 0f) {
                    if (ship.getFluxTracker().showFloaty()) {
                        float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
                        Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                                "Emergency dive!",
                                NeuralLinkScript.getFloatySize(ship), c, ship, 16f * timeMult, 3.2f/timeMult, 1f/timeMult, 0f, 0f,
                                1f);
                    }
                }

                diveFader.advance(amount);
                ship.setRetreating(true, false);

                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                diveProgress += amount * cloak.getChargeUpDur();
                float curr = ship.getExtraAlphaMult();
                cloak.forceState(ShipSystemAPI.SystemState.IN, Math.min(1f, Math.max(curr, diveProgress)));
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);

                if (diveProgress >= 1f) {
                    if (diveFader.isIdle()) {
                        Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                    diveFader.fadeOut();
                    diveFader.advance(amount);
                    float b = diveFader.getBrightness();
                    ship.setExtraAlphaMult2(b);

                    float r = ship.getCollisionRadius() * 5f;
                    ship.setJitter(this, c, b, 20, r * (1f - b));

                    if (diveFader.isFadedOut()) {
                        ship.getLocation().set(0, -1000000f);
                    }
                }
            }
        }
    }
}