package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin;
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin.*;
import data.scripts.utils.SotfMisc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.CombatUI;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.ArrayList;

import static com.fs.starfarer.api.combat.ShipAPI.HullSize.FRIGATE;
import static data.scripts.combat.special.SotfInvokeHerBlessingPlugin.haveUpgrade;
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

        float percentHeal = 0.06f;
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
        if (DefenseUtils.getMostDamagedArmorCell(ship) != null) {
            SotfMisc.repairMostDamaged(ship, ship.getArmorGrid().getArmorRating() * percentHeal);
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }
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
}
