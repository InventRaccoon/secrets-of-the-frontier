package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SotfTormentPulseEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    public static float DAMPING = 0.25f;
    public static int MAX_ARCS = 5;
    public static float DISCREPANCY_ERASE_COOLDOWN = 12f;

    public static List<String> DISCREPANCIES = new ArrayList<>();
    static {
        DISCREPANCIES.add("realitydisruptor_shot");
        DISCREPANCIES.add("assaying_rift_proj"); // Hungering Rift
        DISCREPANCIES.add("shrouded_vortex"); // Churning Locus
    }

    private IntervalUtil interval = new IntervalUtil(1f, 1.25f);
    private CombatEntityAPI toDelete = null;
    private float deleteCooldown = 0f;

    protected Color COLOR = new Color(255,105,105);
    protected SpriteAPI glow = Global.getSettings().getSprite("fx", "sotf_rose_glow_back");
    protected float level = 0f;
    protected float cooldown = 0f;
    protected static float RESET_TIME = 0.25f;
    protected float systemGlowLevel = 0f;
    protected float systemGlowCooldown = 0f;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //interval.advance(amount);

        ShipAPI ship = weapon.getShip();
        if (ship == null) return;
        if (!ship.isAlive()) return;

        deleteCooldown -= amount;

        boolean charging = weapon.getChargeLevel() > 0 || toDelete != null;

        interval.advance(amount);
        if (interval.intervalElapsed() && deleteCooldown <= 0f) {
            if (toDelete != null) {
                if (!engine.isEntityInPlay(toDelete)) {
                    toDelete = null;
                } else {
                    Vector2f loc = weapon.getFirePoint(0);

                    EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                    params.maxZigZagMult = 2f;

                    EmpArcEntityAPI arc = engine.spawnEmpArc(ship, loc, weapon.getShip(),
                            toDelete,
                            DamageType.ENERGY,
                            0f,
                            0f,
                            100000f, // max range
                            "mote_attractor_targeted_ship",
                            50f, // thickness
                            weapon.getSpec().getGlowColor(),
                            new Color(255,255,255,255)
                    );
                    arc.setFadedOutAtStart(true);
                    arc.setSingleFlickerMode();
                    arc.setWarping(1f);

                    engine.addFloatingText(toDelete.getLocation(), "RESOLVED",
                            20f, Misc.getNegativeHighlightColor(), toDelete, 0f, 0f);

                    if (SotfModPlugin.GLIB) {
                        RippleDistortion ripple = new RippleDistortion(toDelete.getLocation(), new Vector2f());
                        ripple.setIntensity(100f);
                        ripple.setSize(300f);
                        ripple.fadeInSize(0.25f);
                        ripple.fadeOutIntensity(0.5f);
                        DistortionShader.addDistortion(ripple);
                    }

                    engine.removeEntity(toDelete);
                    deleteCooldown = DISCREPANCY_ERASE_COOLDOWN;
                }
            } else {
                CombatEntityAPI discrepancy = findDiscrepancy(weapon, engine);
                if (discrepancy != null) {
                    toDelete = discrepancy;
                    engine.addFloatingText(toDelete.getLocation(), "DISCREPANCY IDENTIFIED",
                            20f, Misc.getNegativeHighlightColor(), toDelete, 0f, 0f);
                }
            }
        }

        if (charging) {
//			whirlFade += amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue() * 2f;
//			if (whirlFade > 1f) {
//				whirlFade = 1f;
//			}
            level += amount * 4f;
            cooldown = RESET_TIME;
            if (toDelete != null) {
                level += amount * 4f;
            }
            if (level > 1f) {
                level = 1f;
            }
        } else {
            cooldown -= amount;
        }

        if (cooldown <= 0f) {
            level -= amount / 0.75f;
        }

        if (ship.getSystem().isOn()) {
            level += amount * 6f;
            if (level > 1f) {
                level = 1f;
                cooldown = RESET_TIME;
            }
            systemGlowLevel += amount * 5f;
            if (systemGlowLevel > 1f) {
                systemGlowLevel = 1f;
                systemGlowCooldown = RESET_TIME;
            }
        } else {
            systemGlowCooldown -= amount;
        }

        if (systemGlowCooldown <= 0f) {
            systemGlowLevel -= amount / 0.25f;
            if (systemGlowLevel < 0f) {
                systemGlowLevel = 0f;
            }
        }

        if (level <= 0f) {
            level = 0f;
            return;
        }

        Color glowColor = Misc.interpolateColor(COLOR, SotfInvokeHerBlessingPlugin.UI_COLOR, systemGlowLevel);

        MagicRender.singleframe(
                glow,
                weapon.getShip().getLocation(),
                new Vector2f(glow.getWidth(), glow.getHeight()),
                weapon.getShip().getFacing() - 90f,
                Misc.setAlpha(glowColor, Math.round(255 * (level * level))),
                true
        );
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float emp = projectile.getEmpAmount();
        float dam = projectile.getDamageAmount();

        CombatEntityAPI target = findTarget(projectile, weapon, engine);
        float thickness = 20f;
        float coreWidthMult = 0.67f;
        Color color = weapon.getSpec().getGlowColor();
        if (target != null) {
            boolean shouldArc = false;
            if (target instanceof MissileAPI) {
                shouldArc = true;
            } else if (target instanceof ShipAPI targetShip) {
                if (targetShip.isFighter()) {
                    shouldArc = true;
                }
            }
            if (shouldArc) {
                dam *= 0.5f;
                emp *= 0.5f;
                target.getVelocity().scale(1f - DAMPING);
            }
            EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
            params.flamesOutMissiles = false;
            EmpArcEntityAPI arc = engine.spawnEmpArc(projectile.getSource(), projectile.getLocation(), weapon.getShip(),
                    target,
                    DamageType.ENERGY,
                    dam,
                    emp, // emp
                    100000f, // max range
                    "shock_repeater_emp_impact",
                    thickness, // thickness
                    color,
                    new Color(255,255,255,255),
                    params
            );
            arc.setFadedOutAtStart(true);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
            Vector2f endpoint = arc.getTargetLocation();
            if (shouldArc) {
                List<CombatEntityAPI> targets = new ArrayList<>();
                arc(engine, weapon, endpoint, targets, dam, emp, MAX_ARCS - 1);
            }
        } else {
            Vector2f from = new Vector2f(projectile.getLocation());
            Vector2f to = pickNoTargetDest(projectile, weapon, engine);
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, weapon.getShip(), to, weapon.getShip(), thickness, color, Color.white);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
            //Global.getSoundPlayer().playSound("shock_repeater_emp_impact", 1f, 1f, to, new Vector2f());
        }
    }

    public Vector2f pickNoTargetDest(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float spread = 50f;
        float range = weapon.getRange() - spread;
        Vector2f from = projectile.getLocation();
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
        dir.scale(range);
        Vector2f.add(from, dir, dir);
        dir = Misc.getPointWithinRadius(dir, spread);
        return dir;
    }

    public CombatEntityAPI findTarget(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float range = weapon.getRange();
        Vector2f from = projectile.getLocation();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;

        ShipAPI ship = weapon.getShip();
        boolean ignoreFlares = ship != null && ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
        ignoreFlares |= weapon.hasAIHint(WeaponAPI.AIHints.IGNORES_FLARES);

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    //!(o instanceof CombatAsteroidAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;

            boolean phaseHit = false;
            if (other instanceof ShipAPI otherShip) {
                if (otherShip.isHulk()) continue;
                //if (!otherShip.isAlive()) continue;
                if (otherShip.isPhased()) {
                    phaseHit = true;
                }
                if (!otherShip.isTargetable()) continue;
            }

            if (!phaseHit && other.getCollisionClass() == CollisionClass.NONE) continue;

            if (ignoreFlares && other instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) other;
                if (missile.isFlare()) continue;
            }

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) continue;

            //float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
            //float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
            float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }

    public void arc(CombatEngineAPI engine, WeaponAPI weapon, Vector2f start, List<CombatEntityAPI> targets, float damage, float emp, int maxTargetsLeft) {
        CombatEntityAPI newTarget = findArcTarget(start, weapon, targets);
        if (newTarget != null) {
            targets.add(newTarget);

            newTarget.getVelocity().scale(1f - DAMPING);

            float thickness = 20f;
            float coreWidthMult = 0.67f;
            EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
            params.flamesOutMissiles = false;
            EmpArcEntityAPI arc = engine.spawnEmpArc(weapon.getShip(), start, weapon.getShip(),
                    newTarget,
                    DamageType.ENERGY,
                    damage,
                    emp, // emp
                    100000f, // max range
                    "shock_repeater_emp_impact",
                    thickness, // thickness
                    weapon.getSpec().getGlowColor(),
                    new Color(255,255,255,255),
                    params
            );
            arc.setFadedOutAtStart(true);
            arc.setRenderGlowAtStart(false);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            arc.setSingleFlickerMode();
            Vector2f endpoint = arc.getTargetLocation();
            if (maxTargetsLeft > 1) {
                arc(engine, weapon, endpoint, targets, damage, emp, maxTargetsLeft - 1);
            }
        }
    }

    public CombatEntityAPI findArcTarget(Vector2f from, WeaponAPI weapon, List<CombatEntityAPI> targets) {
        float range = 400f;

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;

        ShipAPI ship = weapon.getShip();
        boolean ignoreFlares = ship != null && ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
        ignoreFlares |= weapon.hasAIHint(WeaponAPI.AIHints.IGNORES_FLARES);

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    //!(o instanceof CombatAsteroidAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;
            if (targets.contains(other)) continue;

            boolean phaseHit = false;
            if (other instanceof ShipAPI otherShip) {
                if (!otherShip.isFighter()) continue;
                if (otherShip.isStationModule()) continue;
                if (otherShip.isHulk()) continue;
                //if (!otherShip.isAlive()) continue;
                if (otherShip.isPhased()) {
                    phaseHit = true;
                }
                if (!otherShip.isTargetable()) continue;
            }

            if (!phaseHit && other.getCollisionClass() == CollisionClass.NONE) continue;

            if (ignoreFlares && other instanceof MissileAPI missile) {
                if (missile.isFlare()) continue;
            }

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) continue;

            float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }

    public CombatEntityAPI findDiscrepancy(WeaponAPI weapon, CombatEngineAPI engine) {
        float range = weapon.getRange() * 2f;
        Vector2f from = weapon.getLocation();

        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                range * 2f, range * 2f);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;

        while (iter.hasNext()) {
            Object o = iter.next();
            if (!(o instanceof MissileAPI) &&
                    !(o instanceof ShipAPI)) continue;
            CombatEntityAPI other = (CombatEntityAPI) o;
            if (other.getOwner() == owner) continue;

            if (other instanceof ShipAPI otherShip) {
                if (otherShip.isHulk()) continue;
                if (!DISCREPANCIES.contains(otherShip.getHullSpec().getHullId())) {
                    continue;
                }
            }

            if (other instanceof MissileAPI missile) {
                if (missile.isFlare()) continue;
                if (!DISCREPANCIES.contains(missile.getProjectileSpecId())) {
                    continue;
                }
            }

            float radius = Misc.getTargetingRadius(from, other, false);
            float dist = Misc.getDistance(from, other.getLocation()) - radius;
            if (dist > range) continue;

            float score = dist;

            if (score < minScore) {
                minScore = score;
                best = other;
            }
        }
        return best;
    }
}
