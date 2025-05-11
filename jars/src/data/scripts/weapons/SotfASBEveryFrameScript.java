package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.List;

/**
 *	EveryFrame for Anti-Ship Battery projectiles (i.e EXTREME range, EXTREME speed) to avoid premature collision and AI foolery
 */

public class SotfASBEveryFrameScript extends BaseEveryFrameCombatPlugin {

	public DamagingProjectileAPI shot;
	public ShipAPI target;
	boolean passedNearTarget = false;
	boolean passedByTarget = false;
	boolean first = true;

	public SotfASBEveryFrameScript(DamagingProjectileAPI shot, ShipAPI target) {
		this.shot = shot;
		this.target = target;
	}

	public void advance(float amount, List<InputEventAPI> events) {
		if (shot.isExpired() || shot.didDamage()) {
			Global.getCombatEngine().removePlugin(this);
		}
		if (target != null) {
			if (target.isAlive()) {
				if (Misc.getDistance(shot.getLocation(), target.getLocation()) <= 1000f) {
					passedNearTarget = true;
				} else if (passedNearTarget) {
					passedByTarget = true;
				}
				if (target.getOwner() == 0 || target.getOwner() == 1) {
					Global.getCombatEngine().getFogOfWar(target.getOwner()).revealAroundPoint(target,
							shot.getLocation().x,
							shot.getLocation().y,
							400f);
				}
				if (!passedByTarget && !target.equals(Global.getCombatEngine().getPlayerShip())) {
					//target.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE);
					if (target.getShield() != null) {
						// toggle shield off and then turn it on towards the ASB
						if (first) {
							target.getShield().toggleOff();
						}
						target.setShieldTargetOverride(shot.getLocation().x, shot.getLocation().y);
						target.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON);
					}
					if (target.getPhaseCloak() != null) {
						target.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.STAY_PHASED);
					} else {
						target.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF);
					}
					first = false;
				}
			}
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
