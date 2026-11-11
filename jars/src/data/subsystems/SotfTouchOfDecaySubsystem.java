package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.shipsystems.SotfOntologicalAnchorScript;
import data.shipsystems.scripts.SotfRealityRotEffect;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.List;

public class SotfTouchOfDecaySubsystem extends MagicSubsystem {

    public static float PERCENT_DAMAGE = 0.08f;
    public static float BASE_COOLDOWN = 20f;
    public static float RANGE = 1600f;
    public static final Color LASH_COLOR = new Color(135, 155, 65,255);
    public static float OVERLOAD_DURATION = 2f;

    public SotfTouchOfDecaySubsystem(ShipAPI ship) {
        super(ship);
    }

    // sort before Dream Eater
    public int getOrder() {
        return 0;
    }

    @Override
    public float getBaseInDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return BASE_COOLDOWN;
    }

    // manual player usage only
    @Override
    public boolean shouldActivateAI(float amount) {
        return findTarget() != null;
    }

    @Override
    public boolean canActivate() {
        return findTarget() != null;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        ShipAPI flagship = Global.getCombatEngine().getPlayerShip();
    }

    @Override
    public void onActivate() {
        ShipAPI target = findTarget();
        if (target == null) return;

        boolean versusDaydream = target.getVariant().hasHullMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);

        CombatEngineAPI engine = Global.getCombatEngine();

        Vector2f point = CollisionUtils.getNearestPointOnBounds(
                Misc.getPointAtRadius(target.getShieldCenterEvenIfNoShield(), target.getShieldRadiusEvenIfNoShield() * 0.15f),
                target
        );

        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 8f;
        params.zigZagReductionFactor = 0.15f;
        params.fadeOutDist = 500f;
        params.minFadeOutMult = 2f;
        params.flickerRateMult = 0.4f;

        engine.addHitParticle(ship.getShieldCenterEvenIfNoShield(), ship.getVelocity(), 40f, 1f, 0.5f, LASH_COLOR);

        EmpArcEntityAPI arc = engine.spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                ship,
                point,
                target,
                75f,
                LASH_COLOR,
                Color.WHITE,
                params
        );
        arc.setCoreWidthOverride(20f);
        arc.setWarping(0.5f);
        arc.setSingleFlickerMode(true);
        arc.setFadedOutAtStart(true);

        if (SotfModPlugin.GLIB) {
            RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
            ripple.setIntensity(50f);
            ripple.setSize(100f);
            ripple.fadeInSize(0.15f);
            ripple.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(ripple);
        }

        Global.getSoundPlayer().playSound("energy_lash_fire_at_enemy", 1.1f, 1.2f, target.getLocation(), target.getVelocity());
        Global.getSoundPlayer().playSound("energy_lash_enemy_impact", 1.1f, 1.2f, ship.getLocation(), ship.getVelocity());

        if (versusDaydream) {
            target.setOverloadColor(LASH_COLOR);
            target.getFluxTracker().beginOverloadWithTotalBaseDuration(OVERLOAD_DURATION);
            final ShipAPI overloaded = target;
            Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (!overloaded.getFluxTracker().isOverloadedOrVenting()) {
                        overloaded.resetOverloadColor();
                        Global.getCombatEngine().removePlugin(this);
                    }
                }
            });
        }

        // Halt!
        SotfOntologicalAnchorScript.applyOntologicalAnchorSlow(target);

//        float flux = target.getFluxLevel();
//        if (flux > 0.6f) {
//            // Pull!
//            CombatUtils.applyForce(target, Misc.getAngleInDegrees(target.getLocation(), ship.getLocation()), 200f + target.getMassWithModules() * 1.2f);
//        } else if (flux < 0.3f) {
//            // Push!
//            CombatUtils.applyForce(target, Misc.getAngleInDegrees(ship.getLocation(), target.getLocation()), 250f + target.getMassWithModules() * 1.5f);
//        }

        float damage = target.getHitpoints() * PERCENT_DAMAGE;
        if (versusDaydream) {
            //damage *= 2f;
            engine.applyDamage(target, point, damage, DamageType.ENERGY, 0f, true, false, ship);
        }

        // Wither.
        Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
		offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
        SotfRealityRotEffect effect = new SotfRealityRotEffect(ship, target, offset, true, damage);
		CombatEntityAPI e = engine.addLayeredRenderingPlugin(effect);
		e.getLocation().set(point);
    }

    @Override
    public void onFinished() {
//        if (echo == null) return;
//        echo.spawnMimic();
//        echo = null;
    }

    @Override
    public String getDisplayText() {
        return "Touch of Decay";
    }

    @Override
    public Color getHUDColor() {
        return SotfNaniteSynthesized.COLOR_STRONGER;
    }

    @Override
    public Color getExtraInfoColor() {
        return getHUDColor().darker();
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    protected ShipAPI findTarget() {
        float range = RANGE;
        boolean player = ship == Global.getCombatEngine().getPlayerShip();
        ShipAPI target = ship.getShipTarget();

        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
            if (dist > range + radSum) target = null;
        } else {
            Misc.FindShipFilter filter = s -> isValidLashTarget(ship, s);

            if (player) {
                target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), ShipAPI.HullSize.FRIGATE, range, true, false, filter);
            } else {
                Object test = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                if (test instanceof ShipAPI) {
                    target = (ShipAPI) test;
                    float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
                    float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
                    if (dist > range + radSum) target = null;
                }
            }

            if (target == null) {
                target = Misc.findClosestShipTo(ship, ship.getLocation(), ShipAPI.HullSize.FIGHTER, range, true, false, filter);
            }
        }

        return target;
    }

    public boolean isValidLashTarget(ShipAPI ship, ShipAPI other) {
        if (other == null) return false;
        //if (!other.getVariant().hasHullMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED)) return false;
        if (other.getOwner() == ship.getOwner()) return false;
        if (other.isHulk() || other.getOwner() == 100) return false;
        if (other.isShuttlePod()) return false;
        return true;
    }
}
