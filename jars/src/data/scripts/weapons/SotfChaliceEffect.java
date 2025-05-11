package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicTrailObject;
import org.magiclib.util.MagicTrailTracker;
import org.magiclib.weapons.MagicGuidedProjectileScript;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class SotfChaliceEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

	private static String BOLT_WEAPON_ID = "sotf_chalice_boltlauncher";
	//protected WeaponAPI boltweapon;
	protected boolean right = true;
	private IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
	//protected Color COLOR = new Color(180, 255, 75);
	protected Color COLOR = new Color(105,255,195);
	protected SpriteAPI WHIRL1 = Global.getSettings().getSprite("fx", "sotf_whirl_1");
	protected SpriteAPI WHIRL2 = Global.getSettings().getSprite("fx", "sotf_whirl_2");
	protected float whirlFade = 0f;
	protected float whirlAngle = 0f;
	private IntervalUtil whirlInterval = new IntervalUtil(0.2f, 0.25f);

	//protected IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		//interval.advance(amount);

		ShipAPI ship = weapon.getShip();
		if (ship == null) return;
		if (!ship.isAlive()) return;

		whirlAngle += amount * 160f;

		Vector2f loc = weapon.getFirePoint(0);
		boolean charging = weapon.getChargeLevel() > 0;
		if (charging) {
			whirlFade += amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue() * 2f;
			if (whirlFade > 1f) {
				whirlFade = 1f;
			}
			interval.advance(amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue());
			if (interval.intervalElapsed()) {
				engine.addHitParticle(loc, ship.getVelocity(), 25f, 2f, COLOR);
				CombatEntityAPI target = findTarget(loc, weapon, engine);
				if (target != null) {
					//weapon.getShip().getFluxTracker().showOverloadFloatyIfNeeded("TEST", Misc.getNegativeHighlightColor(), 0f, true);
					float angle = weapon.getCurrAngle() + 60f;
					if (right) {
						angle = weapon.getCurrAngle() - 60f;
					}
					float angleRandMult = 1f;
					if (Misc.random.nextFloat() > 0.5f) {
						angleRandMult = -1f;
					}
					angle += (10f * Misc.random.nextFloat() * angleRandMult);
					// fire alternatingly left/right
					right = !right;
					Global.getSoundPlayer().playSound("sotf_chalice_bolt_fire", 1f, 0.5f, loc, ship.getVelocity());
					CombatEntityAPI entity = engine.spawnProjectile(ship, weapon, BOLT_WEAPON_ID,
							loc, angle, ship.getVelocity());
					DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
					//engine.addPlugin(new SotfChaliceBoltGuidanceScript(proj, target, weapon));
					engine.addPlugin(new SotfChaliceBoltGuidanceScript(proj, target));
				}
			}
		} else {
			whirlFade -= amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue();
			if (whirlFade < 0f) {
				whirlFade = 0f;
			}
			interval.setElapsed(0f);
		}

		whirlInterval.advance(amount * whirlFade);
		if (whirlInterval.intervalElapsed()) {
			engine.addSwirlyNebulaParticle(loc, ship.getVelocity(), 12f, 2f,
					0f, 0f, 0.6f, Misc.setAlpha(COLOR, 155), false);
		}

		MagicRender.singleframe(
				WHIRL1,
				loc,
				new Vector2f(20f, 20f),
				whirlAngle,
				Misc.setAlpha(COLOR, Math.round(255 * whirlFade)),
				true
		);
		MagicRender.singleframe(
				WHIRL2,
				loc,
				new Vector2f(20f, 20f),
				whirlAngle * 2f,
				Misc.setAlpha(COLOR, Math.round(255 * whirlFade)),
				true
		);
	}

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MissileAPI missile = (MissileAPI) projectile;
		missile.setEmpResistance(10000);
		missile.setEccmChanceOverride(1f);
		//missile.setNoFlameoutOnFizzling(true);
//		MissileAIPlugin proxAI = engine.createProximityFuseAI(missile);
		engine.addPlugin(new BaseEveryFrameCombatPlugin() {
			boolean spawnedMine = false;
			MissileAPI proxyMine;
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (missile.isExpired()) {
					engine.removePlugin(this);
					return;
				}
				if (spawnedMine) {
					if (proxyMine == null) {
						engine.removePlugin(this);
						engine.removeEntity(missile);
						return;
					}
					if (proxyMine.wasRemoved()) {
						engine.removePlugin(this);
						engine.removeEntity(missile);
						return;
					}
					missile.getLocation().set(proxyMine.getLocation());
					missile.getVelocity().set(proxyMine.getVelocity());
					return;
				}
				boolean foundHostile = false;
				for (ShipAPI ship : CombatUtils.getShipsWithinRange(missile.getLocation(), 125f)) {
					if (ship.getOwner() != 0 && ship.getOwner() != 1) continue;
					if (ship.getOwner() == missile.getOwner()) continue;
					foundHostile = true;
				}
				if (!foundHostile) return;
				CombatEntityAPI proxMine = engine.spawnProjectile(missile.getSource(), missile.getWeapon(), "sotf_chalice_proxmines", missile.getLocation(), missile.getFacing(), missile.getVelocity());
				proxMine.setHitpoints(missile.getHitpoints());
				missile.setCollisionClass(CollisionClass.NONE);
				proxyMine = (MissileAPI) proxMine;
				//engine.removeEntity(missile);
				spawnedMine = true;
			}
		});
	}

	public CombatEntityAPI findTarget(Vector2f from, WeaponAPI weapon, CombatEngineAPI engine) {
		float range = weapon.getRange();

		Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
				range * 2f, range * 2f);
		int owner = weapon.getShip().getOwner();
		CombatEntityAPI best = null;
		float minScore = Float.MAX_VALUE;
		ShipAPI ship = weapon.getShip();

		if (ship.getShipTarget() != null) {
			ShipAPI target = ship.getShipTarget();
			if (Misc.getDistance(from, target.getLocation()) -
					Misc.getTargetingRadius(from, target, false) <= range
			&& target.isAlive() && target.getOwner() != owner) {
				return ship.getShipTarget();
			}
		}

		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof ShipAPI)) continue;
			CombatEntityAPI other = (CombatEntityAPI) o;
			if (other.getOwner() == owner) continue;

            ShipAPI otherShip = (ShipAPI) other;
            if (otherShip.isHulk()) continue;
            //if (!otherShip.isAlive()) continue;
            if (otherShip.isPhased()) continue;
            if (!otherShip.isTargetable()) continue;

            if (other.getCollisionClass() == CollisionClass.NONE) continue;

			float radius = Misc.getTargetingRadius(from, other, false);
			float dist = Misc.getDistance(from, other.getLocation()) - radius;
			if (dist > range) continue;

			//if (!Misc.isInArc(weapon.getCurrAngle(), 360f, from, other.getLocation())) continue;

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




