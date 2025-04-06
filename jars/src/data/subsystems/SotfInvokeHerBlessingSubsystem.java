package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfInvokeHerBlessingEchoScript;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.SotfMimicLifespanListener;
import data.scripts.utils.SotfMisc;
import data.shipsystems.SotfGravispatialSurgeSystem;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.*;
import java.util.ArrayList;

import static data.scripts.combat.special.SotfInvokeHerBlessingPlugin.*;
import static data.shipsystems.SotfGravispatialSurgeSystem.*;

public class SotfInvokeHerBlessingSubsystem extends MagicSubsystem {

    public static float BASE_COOLDOWN = 0.25f;
    public static float ECHO_FP_COOLDOWN_MULT = 5f;
    public static final float ECHO_SELECT_RANGE = 25f;

    public SotfInvokeHerBlessingEchoScript echo;

    public SotfInvokeHerBlessingSubsystem(ShipAPI ship) {
        super(ship);
    }

    // sort before Dream Eater
    public int getOrder() {
        return 3;
    }

    @Override
    public float getBaseInDuration() {
        return 0.25f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0.25f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return BASE_COOLDOWN;
    }

    // manual player usage only
    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public boolean canActivate() {
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.hasListenerOfClass(SotfMimicDecayListener.class)) {
                return Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE;
            }
        }
        if (echo == null) return false;
        return Misc.getDistance(ship.getLocation(), echo.loc) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        ShipAPI flagship = Global.getCombatEngine().getPlayerShip();

        if (ship != flagship) {
            MagicSubsystemsManager.removeSubsystemFromShip(ship, SotfInvokeHerBlessingSubsystem.class);
            return;
        }

        // Subsystems don't run every frame while paused: moved this code to SotfInvokeHerBlessingPlugin's advance func
//        if (state == State.READY) {
//            echo = findValidEcho();
//            if (echo != null) {
//                echo.select();
//            }
//        }
    }

    @Override
    public void onActivate() {
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.hasListenerOfClass(SotfMimicDecayListener.class)) {
                EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                        ship,
                        ship.getShipTarget().getLocation(), null, 10f, Color.DARK_GRAY, Color.WHITE);
                arc.setFadedOutAtStart(true);
                arc.setCoreWidthOverride(7.5f);
                Global.getSoundPlayer().playSound("sotf_invokeherblessing", 1f, 1f, ship.getLocation(), ship.getVelocity());
                Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1, 1f, ship.getShipTarget().getLocation(), new Vector2f());

                float lifespanMult = 1f;
                if (ship.getShipTarget().hasListenerOfClass(SotfMimicLifespanListener.class)) {
                    for (SotfMimicLifespanListener listener : new ArrayList<SotfMimicLifespanListener>(ship.getShipTarget().getListeners(SotfMimicLifespanListener.class))) {
                        listener.beginExpiring();
                        lifespanMult = Math.max(1f - (listener.time / listener.lifespan), DREAMEATER_REPAIR_MINIMUM);
                    }
                } else {
                    ship.getShipTarget().addListener(new SotfMimicDecayListener(ship.getShipTarget(), haveUpgrade(SotfIDs.COTL_DEATHTHROES)));
                }

                if (haveUpgrade(SotfIDs.COTL_DREAMEATER)) {
                    float percentHeal = (float) SotfMisc.forHullSize(ship.getShipTarget(), DREAMEATER_REPAIR_FRIGATE, DREAMEATER_REPAIR_DESTROYER, DREAMEATER_REPAIR_CRUISER, DREAMEATER_REPAIR_CAPITAL);
                    percentHeal *= lifespanMult;
                    ship.setHitpoints(Math.min(ship.getHitpoints() + (ship.getMaxHitpoints() * percentHeal), ship.getMaxHitpoints()));
                    if (DefenseUtils.getMostDamagedArmorCell(ship) != null) {
                        SotfMisc.repairMostDamaged(ship, ship.getArmorGrid().getArmorRating() * percentHeal);
                        ship.syncWithArmorGridState();
                        ship.syncWeaponDecalsWithArmorDamage();
                    }
                    EmpArcEntityAPI arc2 = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                            ship,
                            ship.getShipTarget().getLocation(), null, 12f, Misc.getPositiveHighlightColor(), Color.WHITE);
                    arc2.setFadedOutAtStart(true);
                    arc2.setCoreWidthOverride(6f);
                }
                return;
            }
        }
        if (echo == null) return;
        int usedDp = (int) Global.getCombatEngine().getCustomData().get(USED_DP_KEY);
        int maxDp = getMimicCapacity();
        if (echo.dp + usedDp > maxDp) {
            EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                    ship,
                    echo.loc, null, 5f, Misc.getNegativeHighlightColor(), Color.WHITE);
            arc.setCoreWidthOverride(2.5f);
            arc.setFadedOutAtStart(true);
            Global.getSoundPlayer().playSound("sotf_invokeherblessing", 0.75f, 0.75f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("mote_attractor_impact_normal", 0.75f, 1f, echo.loc, new Vector2f());
        } else {
            EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                    ship,
                    echo.loc, null, 10f, Color.DARK_GRAY, Color.WHITE);
            arc.setFadedOutAtStart(true);
            arc.setCoreWidthOverride(5f);
            Global.getSoundPlayer().playSound("sotf_invokeherblessing", 1f, 1f, ship.getLocation(), ship.getVelocity());
            Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1, 1f, echo.loc, new Vector2f());
        }

        if (haveUpgrade(SotfIDs.COTL_SHRIEKOFTHEDAMNED)) {
            shriek(echo.loc, ship);
        }

        //setCooldownDuration(echo.fp * ECHO_FP_COOLDOWN_MULT, true);
        echo.startFading();
        echo.spawnMimic();
        echo.selected = false;
        echo = null;
    }

    public static void shriek(Vector2f loc, ShipAPI ship) {
        for (ShipAPI otherShip : CombatUtils.getShipsWithinRange(loc, SHRIEK_RANGE)) {
            if (otherShip.getOwner() != 1) continue;
            strikeShip(loc, ship, otherShip);
        }
        for (MissileAPI missile : CombatUtils.getMissilesWithinRange(loc, SHRIEK_RANGE)) {
            if (missile.getOwner() != 1) continue;
            strikeMissile(loc, ship, missile);
        }
        Global.getSoundPlayer().playUISound("sotf_perfectstorm_blast", 0.65f, 1f);

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(loc, new Vector2f());
            ripple.setIntensity(75f * 0.75f);
            ripple.setSize(100f);
            ripple.fadeInSize(0.15f);
            ripple.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(ripple);
        }

        for (int i = 0; i < 6; i++) {
            Global.getCombatEngine().spawnEmpArcVisual(
                    Misc.getPointWithinRadius(loc, 200f * 0.5f),
                    null,
                    Misc.getPointWithinRadius(loc, 200f * 5),
                    null,
                    6f,
                    SotfNaniteSynthesized.COLOR_STRONGER,
                    Color.WHITE
            );
        }
    }

    private static void strikeShip(Vector2f loc, ShipAPI ship, final ShipAPI target) {
        float maxDamp = MAX_DAMP;
        float damage = DAMAGE;
        float emp = EMP;
        if (target.isFighter()) {
            damage *= FIGHTER_DAMAGE_MULT;
            emp *= FIGHTER_DAMAGE_MULT;
            maxDamp = FIGHTER_MAX_DAMP;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        for (int i = 0; i < 2; i++) {
            EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(
                    ship,
                    Misc.getPointWithinRadius(loc, 60f),
                    null,
                    target,
                    DamageType.FRAGMENTATION,
                    damage,
                    emp,
                    99999f,
                    "tachyon_lance_emp_impact",
                    12f,
                    SotfNaniteSynthesized.COLOR_STRONGER,
                    Color.WHITE
            );

            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(arc.getLocation(), target.getVelocity());
                ripple.setIntensity(30f);
                ripple.setSize(20f);
                ripple.fadeInSize(0.1f);
                ripple.fadeOutIntensity(0.3f);
                DistortionShader.addDistortion(ripple);
            }
        }

        float dampScale = target.getMassWithModules() / MASS_FOR_MIN_DAMP;
        if (dampScale > 1) {
            dampScale = 1;
        }
        target.getVelocity().scale(MAX_DAMP + (dampScale * (MIN_DAMP - maxDamp)));

        if (target.isFighter() && target.getHullLevel() < COLLAPSE_THRESHOLD && !target.hasListenerOfClass(SotfFighterGraviticCollapseScript.class)) {
            target.addListener(new SotfFighterGraviticCollapseScript(target));
        }
    }

    protected static void strikeMissile(Vector2f loc, ShipAPI ship, final MissileAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();

        float damage = 10f;
        if (target.getProjectileSpecId().contains("mote") || target.getProjectileSpecId().contains("kol_sparkle")) {
            damage = 300f;
        }

        // flame out, disarm (if possible), and damp the hell out of it
        target.flameOut();
        target.setArmedWhileFizzling(false);
        // slow zoned like a Belter kid in a makeshift racing ship
        target.getVelocity().scale(0.15f);

        engine.spawnEmpArcPierceShields(
                ship,
                Misc.getPointWithinRadius(loc, 60f),
                null,
                target,
                DamageType.FRAGMENTATION,
                damage,
                0f,
                99999f,
                "tachyon_lance_emp_impact",
                8f,
                SotfNaniteSynthesized.COLOR_STRONGER,
                Color.WHITE
        );

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
            ripple.setIntensity(30f);
            ripple.setSize(20f);
            ripple.fadeInSize(0.1f);
            ripple.fadeOutIntensity(0.3f);
            DistortionShader.addDistortion(ripple);
        }
    }

    public SotfInvokeHerBlessingEchoScript findValidEcho() {
        Vector2f from = ship.getMouseTarget();

        SotfInvokeHerBlessingEchoScript best = null;
        float minScore = Float.MAX_VALUE;

        for (SotfInvokeHerBlessingEchoScript echo : Global.getCombatEngine().getListenerManager().getListeners(SotfInvokeHerBlessingEchoScript.class)) {
            if (echo.fading) continue;
            float dist = Misc.getDistance(from, echo.loc);
            if (dist < (echo.shieldRadius * 1.5f) && dist < minScore) {
                minScore = dist;
                best = echo;
            }
        }
        return best;
    }

    // total DP of mimics (only counts those created by Invoke Her Blessing)
    public static int getUsedMimicDP() {
        float usedDp = 0;
        for (FleetMemberAPI ally : Global.getCombatEngine().getFleetManager(0).getDeployedCopy()) {
            ShipAPI shipForAlly = Global.getCombatEngine().getFleetManager(0).getShipFor(ally);
            if (shipForAlly == null) continue;
            if (!shipForAlly.isAlive()) continue;
            if (ally.getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !shipForAlly.hasListenerOfClass(SotfMimicDecayListener.class)) {
                usedDp += ally.getHullSpec().getSuppliesToRecover();
            }
        }
        return Math.round(usedDp);
    }

    // get actual player mimic capacity
    public static int getMimicCapacity() {
        int maxDp = SotfInvokeHerBlessingPlugin.BASE_DP;
        maxDp += (Global.getSector().getPlayerPerson().getStats().getLevel() - 1) * SotfInvokeHerBlessingPlugin.DP_PER_LEVEL;
        if (haveUpgrade(SotfIDs.COTL_MULTIFACETED)) {
            maxDp *= (1f + SotfInvokeHerBlessingPlugin.MULTIFACTED_MULT);
        }
        return Math.round(maxDp);
    }

    // get player mimics capacity with or without Multifaceted
    public static int getMimicCapacityTheoretical(boolean withMultifaceted) {
        int maxDp = SotfInvokeHerBlessingPlugin.BASE_DP;
        maxDp += (Global.getSector().getPlayerPerson().getStats().getLevel() - 1) * SotfInvokeHerBlessingPlugin.DP_PER_LEVEL;
        if (withMultifaceted) {
            maxDp *= (1f + SotfInvokeHerBlessingPlugin.MULTIFACTED_MULT);
        }
        return Math.round(maxDp);
    }

    @Override
    public void onFinished() {
//        if (echo == null) return;
//        echo.spawnMimic();
//        echo = null;
    }

    @Override
    public String getDisplayText() {
        String append = "no echo selected";
        if (echo != null) {
            if (Misc.getDistance(ship.getLocation(), echo.loc) > SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE) {
                append = "out of range";
            } else if (state == State.READY) {
                append = "ready";
            }
        }
        if (ship.getShipTarget() != null) {
            if (ship.getShipTarget().getVariant().hasTag(SotfPeople.SIRIUS_MIMIC) && !ship.hasListenerOfClass(SotfMimicDecayListener.class)) {
                if (Misc.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE) {
                    append = "expire order ready";
                } else {
                    append = "out of expire order range";
                }
            }
        }
        append += " " + getUsedMimicDP() + "/" + getMimicCapacity();
        if (getUsedMimicDP() > getMimicCapacity()) {
            append += " - OVERCLOCKED!";
        }
        return "Invoke Her Blessing - " + append;
    }

    @Override
    public Color getHUDColor() {
        return Color.WHITE;
    }

    @Override
    public Color getExtraInfoColor() {
        return getHUDColor().darker().darker();
    }
}
