package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;

public class SotfGravispatialSurgeSystem extends BaseShipSystemScript {

    // pulse only happens if there are nearby enemies to strike
    public static boolean REQUIRE_NEARBY = false;
    public static float DETECT_RANGE = 1250f;
    public static float PULSE_RANGE = 850f;

    public static float DAMAGE = 300;
    public static float EMP = 150;
    public static float FIGHTER_DAMAGE_MULT = 2f;
    public static float COLLAPSE_THRESHOLD = 0.4f;

    public static float FIGHTER_MAX_DAMP = 0.1f;
    public static float MAX_DAMP = 0.15f;
    public static float MIN_DAMP = 0.65f;
    public static float MASS_FOR_MIN_DAMP = 3500f;

    private boolean didDamp = false;
    private boolean pulseReady = false;

    public static final Color COLLAPSE_COLOR = new Color(155,205,255,255);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (state == ShipSystemStatsScript.State.IN && !didDamp) {
            ship.getVelocity().scale(0.2f);
            didDamp = true;
        }
        if (state == ShipSystemStatsScript.State.ACTIVE) {
            pulseReady = true;
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getAcceleration().unmodify(id);
            didDamp = false;

            if (pulseReady) {
                ship.getVelocity().scale(0.2f);
                pulseReady = false;
                boolean shouldPulse = true;
                if (REQUIRE_NEARBY) {
                    shouldPulse = !AIUtils.getNearbyEnemies(ship, DETECT_RANGE).isEmpty() || !AIUtils.getNearbyEnemyMissiles(ship, DETECT_RANGE).isEmpty();
                }
                if (shouldPulse) {
                    ship.addListener(new SotfGravispatialSurgePulseScript(ship));
                }
            }
        } else {
            stats.getMaxSpeed().modifyFlat(id, 800f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 3600f * effectLevel);
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
    }

    public static class SotfGravispatialSurgePulseScript implements AdvanceableListener {
        protected ShipAPI ship;

        public static float TIME_UNTIL_PULSE = 1f;
        protected float timer = TIME_UNTIL_PULSE;
        public SotfGravispatialSurgePulseScript(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            if (!ship.isAlive()) {
                Global.getCombatEngine().getListenerManager().removeListener(this);
                return;
            }

            float timeMult = Global.getCombatEngine().getTimeMult().getModifiedValue();
            timer -= amount * timeMult;
            if (timer < 0) {
                timer = 0;
            }

            float prog = (TIME_UNTIL_PULSE - timer) / TIME_UNTIL_PULSE;
            if (prog > 1) {
                prog = 1;
            } else if (prog < 0) {
                prog = 0;
            }
            ship.setJitter(this, COLLAPSE_COLOR, 3f * prog, 8, 2f, 4f);
            Global.getSoundPlayer().playUILoop("sotf_perfectstorm_loop", 0.75f + (0.5f * prog), 0.5f + (1.25f * prog));

            if (timer > 0) {
                return;
            }

            for (ShipAPI otherShip : AIUtils.getNearbyEnemies(ship, PULSE_RANGE)) {
                strikeShip(otherShip);
            }
            for (MissileAPI missile : AIUtils.getNearbyEnemyMissiles(ship, PULSE_RANGE)) {
                strikeMissile(missile);
            }
            Global.getSoundPlayer().playUISound("sotf_perfectstorm_blast", 1f, 1f);

            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                ripple.setIntensity(ship.getCollisionRadius() * 0.75f);
                ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
                ripple.fadeInSize(0.15f);
                ripple.fadeOutIntensity(0.5f);
                DistortionShader.addDistortion(ripple);
            }

            for (int i = 0; i < 6; i++) {
                Global.getCombatEngine().spawnEmpArcVisual(
                        Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 0.35f),
                        ship,
                        Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield()),
                        ship,
                        6f,
                        COLLAPSE_COLOR,
                        Color.BLACK
                );
            }

            for (int i = 0; i < 8; i++) {
                Global.getCombatEngine().spawnEmpArcVisual(
                        Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 0.5f),
                        ship,
                        Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 5),
                        ship,
                        6f,
                        COLLAPSE_COLOR,
                        Color.BLACK
                );
            }

            ship.getListenerManager().removeListener(this);
        }

        protected void strikeShip(final ShipAPI target) {
            float maxDamp = MAX_DAMP;
            float damage = DAMAGE;
            float emp = EMP;
            if (target.isFighter()) {
                damage *= FIGHTER_DAMAGE_MULT;
                emp *= FIGHTER_DAMAGE_MULT;
                maxDamp = FIGHTER_MAX_DAMP;
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            for (int i = 0; i < 3; i++) {
                EmpArcEntityAPI arc = engine.spawnEmpArcPierceShields(
                        ship,
                        Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 0.35f),
                        ship,
                        target,
                        DamageType.FRAGMENTATION,
                        damage,
                        emp,
                        99999f,
                        "tachyon_lance_emp_impact",
                        12f,
                        COLLAPSE_COLOR,
                        Color.BLACK
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

        protected void strikeMissile(final MissileAPI target) {
            CombatEngineAPI engine = Global.getCombatEngine();

            float damage = 0f;
            if (target.getProjectileSpecId().contains("mote")) {
                damage = 300f;
            }

            engine.spawnEmpArcPierceShields(
                    ship,
                    Misc.getPointWithinRadius(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 0.35f),
                    ship,
                    target,
                    DamageType.FRAGMENTATION,
                    0f, // bcs we're collapsing it anyway
                    0f,
                    99999f,
                    "tachyon_lance_emp_impact",
                    8f,
                    COLLAPSE_COLOR,
                    Color.BLACK
            );

            // flame out, disarm (if possible), and damp the fuck out of it
            target.flameOut();
            target.setArmedWhileFizzling(false);
            // slow zoned like a Belter kid in a makeshift racing ship
            target.getVelocity().scale(0.1f);

//            NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(new Color(255,255,255,155), target.getCollisionRadius());
//            p.fadeOut = 0.15f;
//            p.hitGlowSizeMult = 0.25f;
//            p.underglow = new Color(255,255,255, 50);
//            p.withHitGlow = false;
//            p.noiseMag = 0f;

            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
                ripple.setIntensity(30f);
                ripple.setSize(20f);
                ripple.fadeInSize(0.1f);
                ripple.fadeOutIntensity(0.3f);
                DistortionShader.addDistortion(ripple);
            }

            //engine.removeEntity(target);
        }
    }

    public static class SotfFighterGraviticCollapseScript implements AdvanceableListener {
        protected ShipAPI ship;

        public static float TIME_UNTIL_COLLAPSE = 1f;
        protected float timer = TIME_UNTIL_COLLAPSE;
        public SotfFighterGraviticCollapseScript(ShipAPI ship) {
            this.ship = ship;
            ship.getFluxTracker().showOverloadFloatyIfNeeded("Collapsing!", COLLAPSE_COLOR, 5f, true);
        }

        public void advance(float amount) {
            float timeMult = Global.getCombatEngine().getTimeMult().getModifiedValue();
            timer -= amount * timeMult;
            if (timer < 0) {
                timer = 0;
            }

            float prog = (TIME_UNTIL_COLLAPSE - timer) / TIME_UNTIL_COLLAPSE;
            if (prog > 1) {
                prog = 1;
            } else if (prog < 0) {
                prog = 0;
            }
            ship.setJitter(this, COLLAPSE_COLOR, 2f * prog, 8, 2f, 4f);

            if (timer > 0) {
                return;
            }

            NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(COLLAPSE_COLOR, ship.getShieldRadiusEvenIfNoShield());
            p.fadeOut = 0.15f;
            p.hitGlowSizeMult = 0.25f;
            p.underglow = new Color(255,255,255, 50);
            p.withHitGlow = false;
            p.noiseMag = 0f;

            if (SotfModPlugin.GLIB) {
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                ripple.setIntensity(ship.getCollisionRadius() * 0.75f);
                ripple.setSize(ship.getShieldRadiusEvenIfNoShield());
                ripple.fadeInSize(0.15f);
                ripple.fadeOutIntensity(0.5f);
                DistortionShader.addDistortion(ripple);
            }

            Global.getCombatEngine().removeEntity(ship);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased engine power", false);
        }
        return null;
    }

//    public float getActiveOverride(ShipAPI ship) {
////		if (ship.getHullSize() == HullSize.FRIGATE) {
////			return 1.25f;
////		}
////		if (ship.getHullSize() == HullSize.DESTROYER) {
////			return 0.75f;
////		}
////		if (ship.getHullSize() == HullSize.CRUISER) {
////			return 0.5f;
////		}
//        return -1;
//    }
//    public float getInOverride(ShipAPI ship) {
//        return -1;
//    }
//    public float getOutOverride(ShipAPI ship) {
//        return -1;
//    }
//
//    public float getRegenOverride(ShipAPI ship) {
//        return -1;
//    }
//
//    public int getUsesOverride(ShipAPI ship) {
//        if (ship.getHullSize() == HullSize.FRIGATE) {
//            return 2;
//        }
//        if (ship.getHullSize() == HullSize.DESTROYER) {
//            return 2;
//        }
//        if (ship.getHullSize() == HullSize.CRUISER) {
//            return 2;
//        }
//        return -1;
//    }

}
