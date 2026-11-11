package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.List;

/**
 *	EveryFrame for Anti-Ship Battery projectiles (i.e EXTREME range, EXTREME speed) to avoid premature collision
 */

public class SotfASBEveryFrameScript extends BaseEveryFrameCombatPlugin {

	public DamagingProjectileAPI shot;
	public ShipAPI target;

	public SotfASBEveryFrameScript(DamagingProjectileAPI shot, ShipAPI target) {
		this.shot = shot;
		this.target = target;
	}

	public void advance(float amount, List<InputEventAPI> events) {
		if (shot.isExpired() || shot.didDamage()) {
			Global.getCombatEngine().removePlugin(this);
		}

		// if there's no ships nearby, remove the shot's collision so it doesn't hit any asteroids or fighters in the way
		boolean canCollide = false;
		// scales check range inversely with FPS - amount * 2f is 2 frames of movement at current FPS
		List<ShipAPI> nearby = CombatUtils.getShipsWithinRange(shot.getLocation(), 4000f * amount * 2f);
		for (ShipAPI nearbyShip : nearby) {
			if (nearbyShip.getOwner() != shot.getOwner() && !nearbyShip.isFighter() && !nearbyShip.isDrone()) {
				canCollide = true;
			}
		}
		if (canCollide) {
			shot.setCollisionClass(CollisionClass.RAY_FIGHTER);
		} else {
			shot.setCollisionClass(CollisionClass.NONE);
		}
	}

}
