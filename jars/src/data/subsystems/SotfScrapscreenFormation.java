package data.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.PIDController;

import java.util.Iterator;
import java.util.Map;

/**
 *  basic MagicLib circle formation but rewritten in Java so I can tweak the rotational speed (3x faster)
 */

public class SotfScrapscreenFormation extends DroneFormation {

    private float currentRotation = MathUtils.getRandomNumberInRange(30f, 90f);
    public float rotationSpeed = 0.1f;

    public void advance(ShipAPI ship, Map<ShipAPI, ? extends PIDController> drones, float amount) {
        float angleIncrease = 360 / drones.size();
        float angle = 0f;

        currentRotation += rotationSpeed;
        angle += currentRotation;

        for (Map.Entry<ShipAPI, ? extends PIDController> entry : drones.entrySet()) {
            ShipAPI drone = entry.getKey();
            PIDController controller = entry.getValue();

            Vector2f shipLoc = ship.getLocation();
            Vector2f point = MathUtils.getPointOnCircumference(shipLoc, ship.getCollisionRadius() * 3f, angle);
            controller.move(point, drone);

            ShipAPI target = null;
            float distance = 100000f;
            for (Iterator<Object> iter = Global.getCombatEngine().getShipGrid().getCheckIterator(drone.getLocation(), 1000f, 1000f); iter.hasNext();) {
                CombatEntityAPI tmp = (CombatEntityAPI) iter.next();
                if (tmp instanceof ShipAPI) {
                    ShipAPI targetShip = (ShipAPI) tmp;
                    if (targetShip.isFighter()) continue;
                    if (targetShip.getOwner() == drone.getOwner()) continue;
                    if (targetShip.isHulk()) continue;
                    float distanceBetween = MathUtils.getDistance(targetShip, ship);
                    if (distance > distanceBetween) {
                        distance = distanceBetween;
                        target = targetShip;
                    }
                }
            }

            if (target != null) {
                controller.rotate(Misc.getAngleInDegrees(drone.getLocation(), target.getLocation()), drone);
            } else {
                controller.rotate(ship.getFacing() + MathUtils.getRandomNumberInRange(-10f, 10f), drone);
            }

            angle += angleIncrease;
        }
    }

}
