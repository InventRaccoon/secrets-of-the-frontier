// DOTTY'S HERE! Summons a powerful escort drone who diligently protects the ship
package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfDearDotty {

    public static String SUMMONED_DOTTY = "$sotf_summonedDotty";
    public static String INTROED_DOTTY = "$sotf_introedDotty";
    public static String DOTTY_BOND_KEY = "$sotf_dottyBond";
    public static String FIGHTING_DOTTY_KEY = "$sotf_fightingDotty";

    public static class Companionship extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            ship.addListener(new SotfDottyScript(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            ship.removeListenerOfClass(SotfDottyScript.class);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        }

        public String getEffectDescription(float level) {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            Color c = hc;
            info.addPara("Gains the assistance of Dotty, a prototype drone whose achiral figment " +
                            "manifests to escort the piloted ship",
                    c, 0f);
            info.addPara("Dotty's future figments become more powerful as she and her partner take down enemy ships",
                    c, 0f);
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class SotfDottyScript implements AdvanceableListener {
        public ShipAPI originalShip;
        public ShipAPI ship;
        public ShipAPI dotty;
        public Vector2f spawnLoc;
        public float summonTimer = 0f;
        public float timeUntilSummon = 5f;
        public boolean fadingIn = false;
        public float fadeInProgress = 0f;
        public float fadeInTime = 3f;


        public SotfDottyScript(ShipAPI ship) {
            this.ship = ship;
            this.originalShip = ship;
        }

        public void advance(float amount) {
            if (!Global.getCurrentState().equals(GameState.COMBAT)) {
                return;
            }
            if (Global.getCombatEngine().isSimulation() && !Global.getSettings().isDevMode()) {
                return;
            }
            boolean dottyIsAlly = false;
            CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(ship.getOwner());
            // summon Dotty if she hasn't been already
            if (dotty == null && !originalShip.getCustomData().containsKey(SUMMONED_DOTTY)) {
                summonTimer += amount;
                if (summonTimer >= timeUntilSummon) {
                    originalShip.setCustomData(SUMMONED_DOTTY, true);
                    String dottyVar = "sotf_dotty_" + getDottyBondLevel(originalShip.getCaptain());
                    FleetMemberAPI dotty_member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, dottyVar);
                    dotty_member.getVariant().addMod("sotf_phantasmalship");

                    PersonAPI dottyPerson = SotfPeople.getPerson(SotfPeople.DOTTY);

                    dotty_member.setOwner(originalShip.getOwner());
                    dotty_member.setCaptain(dottyPerson);
                    dotty_member.setAlly(dottyIsAlly);
                    dotty_member.setShipName("Dotty");
                    dotty_member.getRepairTracker().setCR(1f);
                    dotty_member.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);

                    spawnLoc = Misc.getPointAtRadius(originalShip.getLocation(),
                            originalShip.getShieldRadiusEvenIfNoShield() + 200f);
                    boolean wasSuppressing = fleetManager.isSuppressDeploymentMessages();
                    fleetManager.setSuppressDeploymentMessages(true);
                    dotty = fleetManager.spawnFleetMember(dotty_member, spawnLoc,
                            originalShip.getFacing(), 0f);
                    fleetManager.setSuppressDeploymentMessages(wasSuppressing);

                    //Global.getCombatEngine().addNebulaParticle(spawnLoc, new Vector2f(0f,0f), 50f, 0.2f, 0f, 1f, 3f, Misc.setAlpha(SotfMisc.getEidolonColor(), 125), true);
                    dotty.setCaptain(dottyPerson);
                    dotty.setName("Dotty");
                    dotty.setCollisionClass(CollisionClass.FIGHTER);
                    dotty.setInvalidTransferCommandTarget(true);
                    dotty.addListener(new SotfDottyListener(ship, dotty));

                    fadingIn = true;
                }
            }

            // Dotty behavior
            if (dotty != null) {
                if (dotty.isAlive()) {
                    dotty.setCollisionClass(CollisionClass.FIGHTER);
                    if (fadingIn) {
                        fadeInProgress += amount;
                        float progress = fadeInProgress / fadeInTime;
                        dotty.setHoldFireOneFrame(true);
                        dotty.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                        dotty.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                        dotty.blockCommandForOneFrame(ShipCommand.FIRE);
                        float num = 18f * (1f - progress * progress) + 7f;
                        dotty.setAlphaMult(1f - progress);
                        dotty.setJitter(this, SotfMisc.getEidolonColor(), progress, (int)Math.round(num), 10f, 20f);

                        if (progress >= 1f) {
                            Global.getCombatEngine().addFloatingText(new Vector2f(dotty.getLocation().x, dotty.getLocation().y + 50),
                                    "!!!",
                                    70f, SotfMisc.getEidolonColor(), dotty, 1f, 0f);
                            if (!Global.getCombatEngine().getCustomData().containsKey(INTROED_DOTTY) && dotty.getOwner() == 0) {
                                String dottyIntroString = " has arrived!";
                                Global.getCombatEngine().getCombatUI().addMessage(1, dotty, SotfMisc.getEidolonColor(), "Dotty", Misc.getTextColor(), dottyIntroString);
                                Global.getCombatEngine().getCustomData().put(INTROED_DOTTY, true);
                            }
                            fadingIn = false;
                        }
                    }

                    if (ship.isAlive()) {
                        if (!ship.hasListenerOfClass(SotfDottyBondTracker.class)) {
                            ship.addListener(new SotfDottyBondTracker(ship, dotty));
                        }

                        // Dotty is a bit slow by default, so speed her up if she's paired up with a Hound or whatever
                        if (ship.getMaxSpeed() > dotty.getMutableStats().getMaxSpeed().getBaseValue()) {
                            dotty.getMutableStats().getMaxSpeed().modifyFlat("$sotf_dottyCatchup",
                                    (ship.getMaxSpeed() - dotty.getMaxSpeed()) * 0.5f);
                        } else {
                            dotty.getMutableStats().getMaxSpeed().unmodify("$sotf_dottyCatchup");
                        }

                        // escort the friend
                        boolean needAssignEscort = true;
                        if (fleetManager.getTaskManager(dottyIsAlly).getAssignmentFor(dotty) != null) {
                            if (fleetManager.getTaskManager(dottyIsAlly).getAssignmentFor(dotty).getTarget() != null) {
                                if (fleetManager.getTaskManager(dottyIsAlly).getAssignmentFor(dotty).getTarget().equals(fleetManager.getDeployedFleetMember(ship))) {
                                    needAssignEscort = false;
                                }
                            }
                        }

                        if (needAssignEscort && fleetManager.getDeployedFleetMember(dotty) != null && fleetManager.getDeployedFleetMember(ship) != null) {
                            CombatFleetManagerAPI.AssignmentInfo assignment = fleetManager.getTaskManager(dottyIsAlly).createAssignment(CombatAssignmentType.LIGHT_ESCORT,
                                    fleetManager.getDeployedFleetMember(ship), false);
                            fleetManager.getTaskManager(dottyIsAlly).giveAssignment(fleetManager.getDeployedFleetMember(dotty), assignment, false);
                        }
                    }
                }
            }
        }
    }

    // tracks when Dotty should respawn
    public static class SotfDottyListener implements AdvanceableListener {
        public ShipAPI ship; // not the ship with the listener but Dotty's summoner
        public ShipAPI dotty;

        public SotfDottyListener(ShipAPI ship, ShipAPI dotty) {
            this.ship = ship;
            this.dotty = dotty;
        }

        public void advance(float amount) {
            if (Global.getCombatEngine().isEntityInPlay(ship)) {
                return;
            }
            Global.getCombatEngine().applyDamage(dotty, dotty.getLocation(), 10000000,
                    DamageType.HIGH_EXPLOSIVE, 0, true, false, null);
            dotty.removeListener(this);
        }
    }

    // applies a tracker to damaged ships so that they increment the Dotty bond on death
    public static class SotfDottyBondTracker implements DamageDealtModifier {
        public ShipAPI ship;
        public ShipAPI dotty;

        public SotfDottyBondTracker(ShipAPI ship, ShipAPI dotty) {
            this.ship = ship;
            this.dotty = dotty;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (dotty == null || ship == null || !(target instanceof ShipAPI)) {
                return null;
            }
            ShipAPI targetShip = (ShipAPI) target;
            if (dotty.isAlive() && ship.isAlive() && targetShip.isAlive() && targetShip.getOwner() != ship.getOwner()) {
                if (!targetShip.getCustomData().containsKey(FIGHTING_DOTTY_KEY)) {
                    targetShip.setCustomData(FIGHTING_DOTTY_KEY, 5f);
                    targetShip.addListener(new SotfFightingDottyTracker(targetShip, ship, dotty));
                } else {
                    targetShip.setCustomData(FIGHTING_DOTTY_KEY, 5f);
                }
            }
            return null;
        }
    }

    // increment Dotty bond level if this ship gets killed by Dotty & Co.
    public static class SotfFightingDottyTracker implements AdvanceableListener {
        public ShipAPI ship;
        public ShipAPI ally;
        public ShipAPI dotty;

        public SotfFightingDottyTracker(ShipAPI ship, ShipAPI ally, ShipAPI dotty) {
            this.ship = ship;
            this.ally = ally;
            this.dotty = dotty;
        }

        public void advance(float amount) {
            float newTime = (float) ship.getCustomData().get(FIGHTING_DOTTY_KEY) - amount;
            if (ally == null || dotty == null) {
                ship.getCustomData().remove(FIGHTING_DOTTY_KEY);
                ship.removeListener(this);
                return;
            }
            if (ship.isHulk() && ally.isAlive() && dotty.isAlive()) {
                incrementDottyBond(ally.getCaptain(), ship);
            }
            if (newTime < 0) {
                ship.getCustomData().remove(FIGHTING_DOTTY_KEY);
                ship.removeListener(this);
            } else {
                ship.setCustomData(FIGHTING_DOTTY_KEY, newTime);
            }
        }
    }

    // 1/2/4/6, +1 for each armed module, 25% for civ ships
    public static void incrementDottyBond(PersonAPI person, ShipAPI slain) {
        if (slain.isFighter() || slain.isDrone() || slain.getHullSpec().getOrdnancePoints(null) == 0f) {
            return;
        }
        float amountToIncrement = slain.getHullSize().ordinal() - 1f;
        if (slain.getHullSize().ordinal() >= 4) {
            amountToIncrement += 1f;
        }
        if (slain.getHullSize().ordinal() >= 5) {
            amountToIncrement += 1f;
        }
        if (slain.getHullSpec().isCivilianNonCarrier()) {
            amountToIncrement *= 0.25f;
        }
        if (!person.getMemoryWithoutUpdate().contains(DOTTY_BOND_KEY)) {
            person.getMemoryWithoutUpdate().set(DOTTY_BOND_KEY, amountToIncrement);
            return;
        }
        float already = person.getMemoryWithoutUpdate().getFloat(DOTTY_BOND_KEY);
        person.getMemoryWithoutUpdate().set(DOTTY_BOND_KEY, already + amountToIncrement);
    }

    public static int getDottyBondLevel(PersonAPI person) {
        float bond = 0f;
        if (person.getMemoryWithoutUpdate().contains(DOTTY_BOND_KEY)) {
            bond = person.getMemoryWithoutUpdate().getFloat(DOTTY_BOND_KEY);
        }
        int level = 0;
        if (bond >= 999999f) {
            level = 5; // Fel only
        } else if (bond >= 99999f) {
            level = 4; // Fel only
        } else if (bond >= 90f) {
            level = 3;
        } else if (bond >= 45f) {
            level = 2;
        }
        else if (bond >= 15f) {
            level = 1;
        }
        return level;
    }


}
