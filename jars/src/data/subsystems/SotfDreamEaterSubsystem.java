package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.CombatUI;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.Iterator;

import static data.scripts.combat.special.SotfInvokeHerBlessingPlugin.*;
import static data.shipsystems.SotfGravispatialSurgeSystem.*;
import static data.subsystems.SotfInvokeHerBlessingSubsystem.shriek;

public class SotfDreamEaterSubsystem extends MagicSubsystem {

    public static float BASE_COOLDOWN = 0.25f;

    public SotfInvokeHerBlessingEchoScript echo;

    public SotfDreamEaterSubsystem(ShipAPI ship) {
        super(ship);
    }

    public int getOrder() {
        return 2;
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
        if (echo == null) return false;
        return Misc.getDistance(ship.getLocation(), echo.loc) <= SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE && !echo.fading;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        //ShipAPI flagship = Global.getCombatEngine().getPlayerShip();

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
        if (echo == null) return;
        if (echo.fading) return;

        EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                ship,
                echo.loc, null, 10f, Color.DARK_GRAY, Color.WHITE);
        arc.setFadedOutAtStart(true);
        arc.setCoreWidthOverride(7.5f);
        Global.getSoundPlayer().playSound("sotf_invokeherblessing", 1f, 1f, ship.getLocation(), ship.getVelocity());
        Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1, 1f, echo.loc, new Vector2f());

        float percentHeal = SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_FRIGATE;
        String wingId = "sotf_sbd_frig_wing";
        switch (echo.hullSize) {
            case DESTROYER:
                percentHeal = SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_DESTROYER;
                wingId = "sotf_sbd_des_wing";
                break;
            case CRUISER:
                percentHeal = SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_CRUISER;
                wingId = "sotf_sbd_cru_wing";
                break;
            case CAPITAL_SHIP:
                percentHeal = SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_CAPITAL;
                wingId = "sotf_sbd_cap_wing";
                break;
        }
        ship.setHitpoints(Math.min(ship.getHitpoints() + (ship.getMaxHitpoints() * percentHeal), ship.getMaxHitpoints()));
        SotfMisc.repairEvenly(ship, percentHeal);

        EmpArcEntityAPI arc2 = Global.getCombatEngine().spawnEmpArcVisual(ship.getShieldCenterEvenIfNoShield(),
                ship,
                echo.loc, null, 12f, Misc.getPositiveHighlightColor(), Color.WHITE);
        arc2.setFadedOutAtStart(true);
        arc2.setCoreWidthOverride(6f);

        if (haveUpgrade(SotfIDs.COTL_SHRIEKOFTHEDAMNED)) {
            shriek(echo.loc, ship);
        }

        if (haveUpgrade(SotfIDs.COTL_SERVICEBEYONDDEATH)) {
            Global.getCombatEngine().addPlugin(
                    new SotfNaniteSynthesized.NaniteShipFadeInPlugin(wingId,
                            ship, echo.loc, 0.25f, 1f, echo.angle));
        }

        if (haveUpgrade(SotfIDs.COTL_DEATHTHROES)) {
            CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(ship.getOwner());
            manager.setSuppressDeploymentMessages(true);
            ShipAPI leader = manager.spawnShipOrWing("sotf_deaththroes_wing",
                    echo.loc, echo.angle, 0f, null);
            manager.removeDeployed(leader.getWing(), false);
            manager.setSuppressDeploymentMessages(false);

            leader.getVariant().addMod(HullMods.AUXILIARY_THRUSTERS);
            leader.getMutableStats().getMaxSpeed().modifyMult("sotf_turn_it_down_a_notch", 0.75f);

            leader.addListener(new SotfDeathThroesSwarmListener(leader, (float) SotfMisc.forHullSize(echo.hullSize, 1f,
                    THROES_DESTROYER_MULT,
                    THROES_CRUISER_MULT,
                    THROES_CAPITAL_MULT))
            );
        }

        // disintegrate the hulk and its pieces if they're still around
        if (echo.hulk != null) {
            if (Global.getCombatEngine().isEntityInPlay(echo.hulk)) {
                Global.getCombatEngine().addPlugin(SotfNaniteSynthesized.createNaniteFadeOutPlugin(echo.hulk, 1f, true));
                for (ShipAPI curr : Global.getCombatEngine().getShips()) {
                    if (curr.getParentPieceId() != null && curr.getParentPieceId().equals(echo.hulk.getId())) {
                        Global.getCombatEngine().addPlugin(SotfNaniteSynthesized.createNaniteFadeOutPlugin(curr, 1f, true));
                    }
                }
            }
        }

        //setCooldownDuration(echo.fp * ECHO_FP_COOLDOWN_MULT, true);
        echo.eaten = true;
        echo.startFading();
        echo.selected = false;
        echo = null;
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
        return "Dream Eater - " + append;
    }

    @Override
    public Color getHUDColor() {
        return SotfNaniteSynthesized.COLOR_STRONGER;
    }

    @Override
    public Color getExtraInfoColor() {
        return getHUDColor().darker().darker();
    }

    @Override
    public void drawHUDBar(ViewportAPI viewport, Vector2f rootLoc, Vector2f barLoc, boolean displayAdditionalInfo, float longestNameWidth) {
        String nameText = getDisplayText();
        String keyText = getKeyText();

        if (!displayAdditionalInfo && !keyText.equals(BLANK_KEY)) {
            nameText = MagicTxt.getString("subsystemNameWithKeyText", nameText, keyText);
        }

        boolean displayStateText = true;
        if (requiresTarget()) {
            if (ship.getShipTarget() == null) {
                displayStateText = false;
            } else if (targetOnlyEnemies() && ship.getOwner() == ship.getShipTarget().getOwner()) {
                displayStateText = false;
            } else if (calcRange() >= 0 && MathUtils.getDistance(ship, ship.getShipTarget()) > calcRange()) {
                displayStateText = false;
            }
        }

        if (getFluxCostFlatOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostFlatOnActivation() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        if (getFluxCostPercentOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        String stateText = getStateText();
        if (!displayStateText) {
            stateText = null;
        }

        float additionalBarPadding = Math.max(0f, longestNameWidth - CombatUI.STATUS_BAR_PADDING);
        CombatUI.drawSubsystemStatus(
                ship,
                getBarFill(),
                nameText,
                getHUDColor(),
                getExtraInfoText(),
                getExtraInfoColor(),
                stateText,
                keyText,
                getBriefText(),
                displayAdditionalInfo,
                getNumHUDBars(),
                barLoc,
                additionalBarPadding,
                rootLoc
        );
    }

    // expires the mimic & handles Death Throes arcs if applicable
    public static class SotfDeathThroesSwarmListener implements AdvanceableListener {
        public ShipAPI ship;
        public float damageMult;
        public float timeUntilNextArc = THROES_AVERAGE_TIME;
        private float timer = 0f;
        // slightly more total output than normal since the swarm is less likely to be in a good position
        private float maxDuration = (1f / MIMIC_EXPIRE_RATE) * 1.15f;

        public SotfDeathThroesSwarmListener(ShipAPI ship, float damageMult) {
            this.ship = ship;
            this.damageMult = damageMult;
        }

        public void advance(float amount) {
            if (!ship.isAlive()) {
                ship.removeListener(this);
                return;
            }

            timer += amount;
            if (timer >= maxDuration) {
                Global.getCombatEngine().addPlugin(SotfNaniteSynthesized.createNaniteFadeOutPlugin(ship, 0.5f, false));
                ship.removeListener(this);
                return;
            }

            timeUntilNextArc -= amount;
            if (timeUntilNextArc <= 0) {
                float thickness = 20f;
                float coreWidthMult = 0.67f;
                CombatEntityAPI empTarget = findTarget(ship);
                if (empTarget != null) {
                    Global.getCombatEngine().spawnEmpArc(ship,
                            ship.getShieldCenterEvenIfNoShield(),
                            ship, empTarget,
                            DamageType.ENERGY,
                            THROES_DAMAGE * damageMult,
                            THROES_EMP * damageMult,
                            100000f,
                            "tachyon_lance_emp_impact",
                            12f * damageMult,
                            SotfNaniteSynthesized.COLOR_STRONGER,
                            Color.white
                    );
                    if (empTarget instanceof ShipAPI targetShip) {
                        float dampScale = targetShip.getMassWithModules() / MASS_FOR_MIN_DAMP;
                        if (dampScale > 1) {
                            dampScale = 1;
                        }
                        empTarget.getVelocity().scale(MAX_DAMP + (dampScale * (MIN_DAMP - MAX_DAMP)));
                    } else {
                        empTarget.getVelocity().scale(0.1f);
                    }
                } else {
                    Vector2f to = Misc.getPointAtRadius(ship.getLocation(), 100f + (50f * (float) Math.random()));
                    EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(
                            ship.getShieldCenterEvenIfNoShield(),
                            ship,
                            to,
                            ship,
                            thickness,
                            SotfNaniteSynthesized.COLOR_STRONGER,
                            Color.white
                    );
                    arc.setCoreWidthOverride(thickness * coreWidthMult);
                    arc.setSingleFlickerMode();
                }
                float randomFactor = (float) Math.random();
                if (Math.random() > 0.5f) {
                    randomFactor *= -1f;
                }
                timeUntilNextArc = THROES_AVERAGE_TIME + (0.1f * randomFactor);
                Global.getCombatEngine().addHitParticle(ship.getLocation(), ship.getVelocity(), 50f * damageMult + (30f * randomFactor), 1f, 0.35f, SotfNaniteSynthesized.COLOR_STRONGER);
            }
        }

        public CombatEntityAPI findTarget(ShipAPI ship) {
            float range = SotfInvokeHerBlessingPlugin.THROES_RANGE + ship.getShieldRadiusEvenIfNoShield();
            Vector2f from = ship.getLocation();

            Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                    range * 2f, range * 2f);
            int owner = ship.getOwner();
            CombatEntityAPI best = null;
            float minScore = Float.MAX_VALUE;

            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof MissileAPI) &&
                        //!(o instanceof CombatAsteroidAPI) &&
                        !(o instanceof ShipAPI)) continue;
                CombatEntityAPI other = (CombatEntityAPI) o;
                if (other.getOwner() == owner) continue;

                if (other instanceof ShipAPI) {
                    ShipAPI otherShip = (ShipAPI) other;
                    if (otherShip.isHulk()) continue;
                    //if (!otherShip.isAlive()) continue;
                    if (otherShip.isPhased()) continue;
                }

                if (other.getCollisionClass() == CollisionClass.NONE) continue;

                if (other instanceof MissileAPI) {
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
    }
}
