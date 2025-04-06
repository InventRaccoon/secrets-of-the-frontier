package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.campaign.ids.SotfIDs;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.SpinningCircleFormation;

/**
 * Advanced Countermeasures wasp drone subsystem
 */
public class SotfNaniteDronesSubsystem extends MagicDroneSubsystem {

    public static int DRONES_FRIGATE = 2;
    public static int DRONES_DESTROYER = 4;
    public static int DRONES_CRUISER = 6;
    public static int DRONES_CAPITAL = 8;

    public SotfNaniteDronesSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public boolean canAssignKey() {
        return false;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 0;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return canActivate();
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        if (ship.isDestroyer()) {
            return 5f;
        } else if (ship.isCruiser()) {
            return 4f;
        } else if (ship.isCapital()) {
            return 3f;
        }
        return 6f;
    }

    @Override
    public @NotNull ShipAPI spawnDrone() {
        ShipAPI fighter = super.spawnDrone();

        fighter.getVariant().addMod(SotfIDs.HULLMOD_NANITE_SYNTHESIZED);

        PersonAPI fighterCaptain = Global.getFactory().createPerson();
        fighterCaptain.getStats().setSkillLevel(SotfIDs.SKILL_GUNNERYUPLINK, 2);
        fighterCaptain.getStats().setSkillLevel(SotfIDs.SKILL_ORDNANCEMASTERY, 2);
        // no recursive drones pls thanks
        fighterCaptain.getStats().setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, 1);
        fighter.setCaptain(fighterCaptain);

        return fighter;
    }

    @Override
    public boolean canActivate() {
        return false;
    }

    @Override
    public String getDisplayText() {
        return "Countermeasure Escorts";
    }

    @Override
    public String getStateText() {
        return "";
    }

    @Override
    public float getBarFill() {
        float fill = 0f;
        if (charges < calcMaxCharges()) {
            fill = chargeInterval.getElapsed() / chargeInterval.getIntervalDuration();
        }

        return fill;
    }

    @Override
    public int getMaxCharges() {
        if (ship.isFrigate()) {
            return 1;
        } else if (ship.isDestroyer()) {
            return 2;
        } else if (ship.isCruiser()) {
            return 3;
        } else if (ship.isCapital()) {
            return 4;
        }
        return 0;
    }

    @Override
    public int getMaxDeployedDrones() {
        if (ship.isFrigate()) {
            return DRONES_FRIGATE;
        } else if (ship.isDestroyer()) {
            return DRONES_DESTROYER;
        } else if (ship.isCruiser()) {
            return DRONES_CRUISER;
        } else if (ship.isCapital()) {
            return DRONES_CAPITAL;
        }
        return 0;
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public @NotNull String getDroneVariant() {
        return "sotf_nanitedrones_wing";
    }

    @Override
    public DroneFormation getDroneFormation() {
        return new SotfSpinningCircleFormation();
    }
}
