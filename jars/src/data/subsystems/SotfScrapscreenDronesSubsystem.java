package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.HoveringFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;

/**
 * Salvor's Scrapscreen debris shield
 */
public class SotfScrapscreenDronesSubsystem extends MagicDroneSubsystem {

//    public static int DRONES_FRIGATE = 12;
//    public static int DRONES_DESTROYER = 16;
//    public static int DRONES_CRUISER = 18;
//    public static int DRONES_CAPITAL = 28;

    public static int DRONES_FRIGATE = 6;
    public static int DRONES_DESTROYER = 8;
    public static int DRONES_CRUISER = 10;
    public static int DRONES_CAPITAL = 12;

    public SotfScrapscreenDronesSubsystem(ShipAPI ship) {
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
            return 8f;
        } else if (ship.isCruiser()) {
            return 7f;
        } else if (ship.isCapital()) {
            return 6f;
        }
        return 9f;
    }

    @Override
    public @NotNull ShipAPI spawnDrone() {
        ShipAPI fighter = super.spawnDrone();

        PersonAPI fighterCaptain = Global.getFactory().createPerson();
        fighterCaptain.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
        fighterCaptain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        fighter.setCaptain(fighterCaptain);

        SpriteAPI sprite = Global.getSettings().getSprite("terrain", "debrisFieldSheet");
        float i = Misc.random.nextInt(4);
        float j = Misc.random.nextInt(4);
        sprite.setTexWidth(0.25f);
        sprite.setTexHeight(0.25f);
        sprite.setTexX(i * 0.25f);
        sprite.setTexY(j * 0.25f);
        sprite.setSize(64f, 64f);
        fighter.setSprite(sprite);

        return fighter;
    }

    @Override
    public boolean canActivate() {
        return false;
    }

    @Override
    public String getDisplayText() {
        return "Scraphide";
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
//        if (ship.isFrigate()) {
//            return 2;
//        } else if (ship.isDestroyer()) {
//            return 3;
//        } else if (ship.isCruiser()) {
//            return 4;
//        } else if (ship.isCapital()) {
//            return 6;
//        }
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
        return "sotf_scrapscreen_wing";
    }

    @Override
    public DroneFormation getDroneFormation() {
        return new SotfScrapscreenFormation();
        //return new HoveringFormation();
    }
}
