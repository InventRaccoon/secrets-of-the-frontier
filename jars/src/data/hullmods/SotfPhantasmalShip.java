// Won't you dance with me?
// Ship becomes a phase ghost, partly transparent, 110% time mult, is "banished" on death instead of exploding
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.skills.NeuralLinkScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfPhantasmalShip extends BaseHullMod

{

    // attempt dive early but fail and explode
    public static final String TAG_MALFUNCTIONING_SUBMERGER = "sotf_malfunctioning_submerger";

    // Instead of dying normally, play a short animation and then vanish
    public static class SotfBanishmentDeathScript implements AdvanceableListener, HullDamageAboutToBeTakenListener {
        public ShipAPI ship;
        public boolean emergencyDive = false;
        public float diveProgress = 0f;
        public FaderUtil diveFader = new FaderUtil(0.4f, 1f);
        public SotfBanishmentDeathScript(ShipAPI ship) {
            this.ship = ship;
        }

        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (ship.isFighter()) return false; // fighters die instead of being banished because I don't want to deal with that
            if (!emergencyDive) {
                float hull = ship.getHitpoints();
                if (hull - damageAmount < ship.getMaxHitpoints() * 0.2f && ship.getVariant().hasTag(TAG_MALFUNCTIONING_SUBMERGER)) {
                    emergencyDive = true;

                    Global.getCombatEngine().addFloatingTextAlways(ship.getLocation(),
                            "Phase submerger failed!",
                            NeuralLinkScript.getFloatySize(ship) * 2f,
                            SotfMisc.getEidolonColor(),
                            ship,
                            16f,
                            1f,
                            1f,
                            0.5f,
                            0.5f,
                            1f);

                    if (!ship.isPhased()) {
                        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                    // so we don't get oneshot before the mitigation kicks in
                    return true;
                }
                if (damageAmount >= hull) {
                    ship.setHitpoints(1f);

                    emergencyDive = true;

                    if (!ship.isPhased()) {
                        Global.getSoundPlayer().playSound("system_phase_cloak_activate", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                }
            }

            if (emergencyDive && !ship.getVariant().hasTag(TAG_MALFUNCTIONING_SUBMERGER)) {
                return true;
            }

            return false;
        }

        public void advance(float amount) {
            String id = "sotf_banishment_modifier";
            if (emergencyDive) {
                diveFader.advance(amount);

                ship.setControlsLocked(true);
                ship.getFluxTracker().beginOverloadWithTotalBaseDuration(5f);
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                ship.setHoldFireOneFrame(true);
                ship.getEngineController().forceFlameout(true);
                ship.setCollisionClass(CollisionClass.NONE);
                diveProgress += amount;
                if (ship.getVariant().hasTag(TAG_MALFUNCTIONING_SUBMERGER)) {
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.1f);
                } else {
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0f);
                }

                if (diveProgress >= 1f) {
                    if (ship.getVariant().hasTag(TAG_MALFUNCTIONING_SUBMERGER)) {
                        Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 10000000, DamageType.HIGH_EXPLOSIVE, 0, true, false, null);
                        diveFader.forceIn();
                        ship.setCollisionClass(CollisionClass.SHIP);
                        ship.removeListener(this);
                        return;
                    }

                    if (diveFader.isIdle()) {
                        Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    }
                    diveFader.fadeOut();
                    diveFader.advance(amount);
                    float b = diveFader.getBrightness();
                    ship.setExtraAlphaMult2(b);
                    float r = ship.getCollisionRadius() * 5f;
                    ship.setJitter(this, SotfMisc.getSierraColor(), b, 20, r * (1f - b));

                    if (diveFader.isFadedOut()) {
                        String text = ship.getHullSpec().getHullName();
                        String banishedText = " banished";
                        if (text.equals("")) {
                            text = ship.getHullSpec().getDesignation();
                        }
                        if (ship.getHullSpec().getHullId().contains("dotty")) {
                            text = "Dotty";
                            banishedText = " was banished";
                        }
                        Global.getCombatEngine().getCombatUI().addMessage(1, ship, Misc.getNegativeHighlightColor(), text, Misc.getTextColor(), banishedText);

                        // Eidolon/Dotty use the lighter intelligible whispers
                        if (ship.getHullSpec().getHullId().contains("eidolon") || ship.getHullSpec().getHullId().contains("dotty")) {
                            Global.getSoundPlayer().playUISound("sotf_ghost_playful", 1f, 1.2f);
                        }
                        // Other wraiths use the low and threatening ones
                        else {
                            Global.getSoundPlayer().playUISound("sotf_ghost_angry", 1f, 1.2f);
                        }
                        ship.setHullSize(HullSize.FIGHTER);
                        ship.getLocation().set(0, -1000000f);
                        ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                        Global.getCombatEngine().applyDamage(ship, ship.getLocation(), 10000000, DamageType.HIGH_EXPLOSIVE, 0, true, false, null);
                        ship.setHulk(true);
                        ship.removeListener(this);
                        //Global.getCombatEngine().removeEntity(ship);
                    }
                }
            }

            Color sc = SotfMisc.getEidolonColor();
            Color sca = Misc.setAlpha(sc, 50);


            ship.setJitter(this, sca, 2f, 2, 6f);
            ship.setAlphaMult(0.5f);
            ship.setApplyExtraAlphaToEngines(true);

            // time mult. Uses Eidolon's Concord ID so it doesn't stack with her time boost
            boolean player = ship == Global.getCombatEngine().getPlayerShip();
            if (player && ship.isAlive()) {
                ship.getMutableStats().getTimeMult().modifyMult(SotfIDs.EIDOLONS_CONCORD, 1.1f);
                Global.getCombatEngine().getTimeMult().modifyMult(SotfIDs.EIDOLONS_CONCORD, 1f / 1.1f);
            } else {
                ship.getMutableStats().getTimeMult().modifyMult(SotfIDs.EIDOLONS_CONCORD, 1.1f);
                Global.getCombatEngine().getTimeMult().unmodify(SotfIDs.EIDOLONS_CONCORD);
            }
        }

    }

    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        // Fighters are also phantasmal
        fighter.getVariant().addMod(id);
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new SotfBanishmentDeathScript(ship));
        ship.addListener(new SotfSierrasConcord.SotfSierraAfterImageScript(ship, SotfMisc.getEidolonColor()));
    }

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMinCrewMod().modifyMult(id, 0f);
    }

}