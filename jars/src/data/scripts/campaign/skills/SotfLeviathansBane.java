// BRING HER DOWN! Ship acts as a spotter for a powerful anti-ship artillery piece
package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfASBLockOnScript;
import data.scripts.combat.SotfAuraVisualScript;
import data.scripts.combat.SotfRingTimerVisualScript;
import data.scripts.utils.SotfMisc;
import data.scripts.weapons.SotfHidecrackerOnHitEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class SotfLeviathansBane {

    // ships will not be affected if below this DP threshold
    //public static float DP_THRESHOLD = 20f;
    public static float DP_THRESHOLD = 8f;
    // max range at which targets can be marked
    public static float MAX_SPOTTING_RANGE = 1200f;
    // cooldown after The Reminder fires a shot
    public static float RELOAD_TIME = 90f;
    // percent of hull damage applied as cooldown reduction
    public static float REFUND_PERCENT = 0.02f;

    public static String ASB_KEY = "$sotf_asbLeviathansBane";
    public static String MARKING_KEY = "$sotf_leviathansBaneMark";

    public static final String CD_KEY = "sotf_leviathansBane_cdvisual";
    public static String WEAPON_ID = "sotf_hidecracker";
    //public static String WEAPON_ID = "sotf_asb";

    public static class Hyperwhaler extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            ship.addListener(new SotfLeviathansBaneScript(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            ship.removeListenerOfClass(SotfLeviathansBaneScript.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            //stats.getBreakProb().modifyMult(id, 0f);
        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            //stats.getBreakProb().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            WeaponSpecAPI theReminderWpn = Global.getSettings().getWeaponSpec(WEAPON_ID);

            Color c = hc;
            //info.addPara("The nearest hostile vessel within %s units that is larger than the piloted ship and worth at " +
            //                "least %s deployment points is gradually marked for a shot from \"%s\", a powerful " +
            //                "anti-ship artillery weapon",
            //        0f, c, c, "" + (int) MAX_SPOTTING_TIME, "" + (int) DP_THRESHOLD, "" + theReminderWpn.getWeaponName());
            //info.addPara("Mark accumulates over %s seconds at a distance of %s units, scaling down to %s seconds at " +
            //                "%s units.",
            //        0f, c, c, "" + (int) MAX_SPOTTING_TIME, "" + (int) MAX_SPOTTING_RANGE, "" + (int) MIN_SPOTTING_TIME, "" + (int) MIN_SPOTTING_RANGE);
            info.addPara("Every %s seconds, gains the ability to mark [R] a targeted hostile vessel within %s units for " +
                            "a shot from \"%s\", a powerful anti-ship artillery weapon",
                    0f, c, c, "" + (int) RELOAD_TIME, "" + (int) MAX_SPOTTING_RANGE, "" + theReminderWpn.getWeaponName());
            info.addPara("Target must be of a larger size class and have a base deployment point cost of at least %s",
                    0f, c, c, "" + (int) DP_THRESHOLD);
            info.addPara(indent + theReminderWpn.getWeaponName() + " fires a flux-charged cryoharpoon that deals %s %s damage on impact, " +
                            "adding hard flux equal to %s of the target's flux capacity and inflicting heavy EMP damage through shields",
                    0f, tc, hc,
                    "" + (int) theReminderWpn.getDerivedStats().getDamagePerShot(), "" + theReminderWpn.getDamageType().getDisplayName().toLowerCase(), (int) (SotfHidecrackerOnHitEffect.PERCENT_MAX_FLUX * 100f) + "%");
            info.addPara("Reload timer is reduced twice as quickly if within %s units of a valid target", 0f, c, c, "" + (int) (MAX_SPOTTING_RANGE));
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class SotfLeviathansBaneScript implements AdvanceableListener {
        protected ShipAPI ship;
        protected float asbReloadTimer = 0f;
        protected float checkTimer = 0f;
        protected float internalCDTimer = RELOAD_TIME / 2; // start at half cooldown
        protected int timesFired = 0;
        protected boolean firstCheck = false;
        protected boolean playerHasSelectedTarget = true;
        protected ShipAPI playerAlreadySelected;
        protected boolean alertedPlayer = false; // heads up so player knows to pick a target

        public SotfLeviathansBaneScript(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            if (!Global.getCurrentState().equals(GameState.COMBAT)) {
                return;
            }
            if (!ship.isAlive() || ship.isFighter() || ship.isStationModule()) {
                return;
            }
            if (!Global.getCombatEngine().getListenerManager().hasListener(this)) {
                Global.getCombatEngine().getListenerManager().addListener(this);
            }
            boolean player = false;
            boolean targetInvalid = false;
            boolean playerTargetTooFar = false;
            ShipAPI playerTarget = null;
            ShipAPI target = null;
            if (findTarget(ship) != null) {
                target = findTarget(ship);
            }
            float cdMult = 1f;
            if (target != null) {
                cdMult = 2f;
            }
            internalCDTimer -= amount * Global.getCombatEngine().getTimeMult().getModifiedValue() * cdMult;
            if (internalCDTimer < 0f) {
                internalCDTimer = 0;
            }
            // if player ship, ONLY fire the Hidecracker at their selected target
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                player = true;
                playerTarget = ship.getShipTarget();
                if (playerTarget != null) {
                    targetInvalid = !isValidTarget(ship, playerTarget);
                    playerTargetTooFar = Misc.getDistance(ship.getLocation(), playerTarget.getLocation()) > MAX_SPOTTING_RANGE;
                }
            }
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                if (internalCDTimer < 2f && !alertedPlayer) {
                    Global.getCombatEngine().addFloatingText(new Vector2f(ship.getShieldCenterEvenIfNoShield().x, ship.getShieldCenterEvenIfNoShield().y + ship.getShieldRadiusEvenIfNoShield() + 50f),
                            "Designate prey [R]",
                            45f, SotfMisc.getEidolonColor(), ship, 1f, 0.2f);
                    alertedPlayer = true;
                }
                String status = "";
                if (internalCDTimer <= 0f) {
                    if (playerTarget != null && targetInvalid) {
                        status = "Prey [R] invalid";
                    } else if (playerTarget != null && playerTargetTooFar) {
                        status = "Prey [R] out of range";
                    } else {
                        status = "Mark prey [R] now!";
                    }
                } else if (internalCDTimer <= 3) {
                    status = "Searching for prey [R]";
                } else if (timesFired == 0) {
                    status = "Hidecracker loading - " + (int) internalCDTimer;
                } else {
                    status = "Hidecracker reloading - " + (int) internalCDTimer;
                    if (target != null) {
                        status += " - prey nearby";
                    }
                }
                Global.getCombatEngine().maintainStatusForPlayerShip(ASB_KEY, "graphics/icons/hullsys/lidar_barrage.png", "Leviathan's Bane", status, false);
            }

            if (!ship.getCustomData().containsKey(CD_KEY)) {
                SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
                p.color = SotfMisc.getEidolonColor();
                p.ship = ship;
                p.radius = ship.getShieldRadiusEvenIfNoShield() + 35f;
                p.thickness = 9f;
                p.maxArc = 80f;
                p.baseAlpha = 0.65f;
                //p.followFacing = true;
                p.renderDarkerCopy = true;
                p.reverseRing = true;
                p.degreeOffset = 95f;
                p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
                SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
                Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
                ship.setCustomData(CD_KEY, plugin);
            } else {
                SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(CD_KEY);
                visual.p.totalArc = 1 - (internalCDTimer / RELOAD_TIME);
            }

            if (internalCDTimer > 0f) {
                return;
            }
            //if (!firstCheck) {
            //    if (player && playerTarget != null) {
            //        playerAlreadySelected = playerTarget;
            //    }
            //}
            //firstCheck = true;
            //if (player && playerTarget != null) {
            //    if (playerAlreadySelected != playerTarget) {
            //        playerHasSelectedTarget = true;
            //    }
            //} else {
            //    playerHasSelectedTarget = true;
            //}
            if (player && playerTarget != null && !targetInvalid && !playerTargetTooFar && playerHasSelectedTarget) {
                target = playerTarget;
            } else if (player) {
                return;
            }
            if (target != null) {
                SotfASBLockOnScript.ASBParams params = new SotfASBLockOnScript.ASBParams(ship, ship.getOwner(), target, MARKING_KEY);
                params.weaponId = WEAPON_ID;
                params.statusTitle = "!! HIDECRACKER - PREY ACQUIRED !!";
                params.statusSubtext = "!! " + pickSubtextString() + ", " + pickSubtext2String() + " !!";
                params.lockedString = pickLockString();
                params.lockedColor = SotfMisc.getEidolonColor();

                SotfASBLockOnScript asbScript = new SotfASBLockOnScript(params);
                Global.getCombatEngine().addPlugin(asbScript);
                internalCDTimer = RELOAD_TIME;
                alertedPlayer = false;
                //firstCheck = false;
                //playerHasSelectedTarget = false;
                timesFired++;
            }
        }

        public ShipAPI findTarget(ShipAPI ship) {
            float range = MAX_SPOTTING_RANGE;
            Vector2f from = ship.getLocation();

            Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
                    range * 2f, range * 2f);
            int owner = ship.getOwner();
            ShipAPI best = null;
            float minScore = -9999f;

            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof ShipAPI)) continue;
                ShipAPI other = (ShipAPI) o;
                if (owner == other.getOwner()) continue;
                if (Misc.getDistance(from, other.getLocation()) > range) continue;

                ShipAPI otherShip = (ShipAPI) other;
                // can't be the target of another anti ship battery
                if (otherShip.getCustomData().containsKey(SotfIDs.ASB_TARGET)) {
                    //if (!otherShip.getCustomData().get(SotfIDs.ASB_TARGET).equals(ASB_KEY)) {
                        continue;
                    //}
                }

                if (!isValidTarget(ship, otherShip)) continue;

                float radius = Misc.getTargetingRadius(from, other, false);
                float score = range - (Misc.getDistance(from, other.getLocation()) - radius);

                // apply bonuses to priority targets

                // Prioritise capitals
                if (otherShip.isCapital()) {
                    score += 500f;
                }
                // And vulnerable stations
                if (otherShip.isStation()) {
                    score += 1000f;
                }
                // (note: modules are already invalided by isValidTarget if they belong to a vulnerable ship)
                if (otherShip.isStationModule()) {
                    score += 1000f;
                }

                ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();

                // prioritise the ship's target
                float shipTargetBonus = 500f;
                // if player ship, always shoot player target if not invalid
                if (ship == playerShip) {
                    shipTargetBonus = 99999f;
                }
                if (ship.getShipTarget() == otherShip || otherShip.getChildModulesCopy().contains(ship.getShipTarget())) {
                    score += shipTargetBonus;
                }
                // player fleet's ships prioritise ASB strikes on player targets
                if (ship.getOwner() == 0 && playerShip != null && playerShip.getShipTarget() != null) {
                    if (playerShip.getShipTarget() == otherShip || otherShip.getChildModulesCopy().contains(ship.getShipTarget())) {
                        score *= 1.5f;
                    }
                } else if (ship.getOwner() == 0 && otherShip.isRecentlyShotByPlayer()) {
                    score *= 1.5f;
                }

                // overloaded/venting targets who would be hit by an ASB shot
                if (otherShip.getFluxTracker().isOverloadedOrVenting()
                        && (otherShip.getFluxTracker().getOverloadTimeRemaining() >= 8f || otherShip.getFluxTracker().getTimeToVent() >= 8f)) {
                    score *= 3f;
                }

                if (score > minScore) {
                    minScore = score;
                    best = other;
                }
            }
            return best;
        }

        public boolean isValidTarget(ShipAPI ship, ShipAPI target) {
            boolean isValid = true;
                if (!target.isAlive() ||
                        target.getOwner() == ship.getOwner() ||
                        target.getHullSpec().getSuppliesToRecover() < DP_THRESHOLD ||
                        (target.getHullSize().ordinal() <= ship.getHullSize().ordinal() && !target.isStation() && !target.isStationModule()) ||
                target.getCollisionClass() == CollisionClass.NONE || target.getVariant().hasHullMod(HullMods.VASTBULK)) {
                    isValid = false;
            }
            if (target.isStationModule()) {
                ShipAPI station = target.getParentStation();
                // if killing the core will work, let's just do that
                if (!station.getVariant().hasHullMod(HullMods.VASTBULK)) {
                    isValid = false;
                }
            }
            return isValid;
        }

        private String pickLockString() {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add("BRING HER DOWN", 5f);
            post.add("FELL THE BEAST", 3f);
            post.add("TO SLAY A LEGEND", 2f);
            post.add("SOUND THE HORN");
            post.add("THE GREAT HUNT HAS BEGUN");
            post.add("THE GREAT HUNT NEVER ENDS");
            post.add("REMEMBER THIS");
            post.add("AIM FOR THE HEART");
            post.add("SCATTER THE HERD");
            post.add("TITANS FALL THE HARDEST");
            post.add("I SEE YOU, LITTLE FISH");
            post.add("THE JAWS CLAMP SHUT");
            post.add("GO FOR THE THROAT");
            post.add("GOODNIGHT, DEAREST PREY");
            post.add("REGARDS FROM BEYOND");
            post.add("THE BIGGER THEY ARE...");
            post.add("BOOM", 0.5f);

            float dottyQuotesWeight = 1f;
            // not during Good Hunting
            if (Global.getCombatEngine().getCustomData().containsKey(SotfIDs.GOODHUNTING_KEY)) {
                dottyQuotesWeight = 0f;
            }
            // more likely if Dotty's around
            else if (Global.getCombatEngine().getCustomData().containsKey(SotfDearDotty.INTROED_DOTTY)) {
                dottyQuotesWeight = 2f;
            }

            //post.add("THIS ONE'S FOR DOTTY", dottyQuotesWeight);
            //post.add("A GIFT FROM WALTER", dottyQuotesWeight);
            return post.pick();
        }

        private String pickSubtextString() {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add("DODGE THIS");
            post.add("HERE IT COMES");
            post.add("GOODNIGHT");
            post.add("MY REGARDS");
            return post.pick();
        }

        private String pickSubtext2String() {
            WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
            post.add("HAH HAH HAH");
            post.add("KEH HEH HEH");
            post.add("KAH HAH HAH");
            return post.pick();
        }
    }
}
