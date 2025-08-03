package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfClingingFlareVisualScript;
import data.scripts.combat.SotfClingingFlareVisualScript.SotfClingingFlareParams;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * IMPORTANT: will be multiple instances of this, as this doubles as the every frame effect and the on fire effect (same instance)
 * But also as the visual for each individual shot (created via onFire, using the non-default constructor)
 */
public class SotfChaliceEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, DamageDealtModifier {

	private static String BOLT_WEAPON_ID = "sotf_chalice_boltlauncher";
	//protected WeaponAPI boltweapon;
	protected boolean right = true;
	private IntervalUtil interval = new IntervalUtil(0.25f, 0.25f);
	//protected Color COLOR = new Color(180, 255, 75);
	protected Color COLOR = new Color(105,255,195);
	protected SpriteAPI WHIRL1 = Global.getSettings().getSprite("fx", "sotf_whirl_1");
	protected SpriteAPI WHIRL2 = Global.getSettings().getSprite("fx", "sotf_whirl_2");
	protected SpriteAPI AURA = Global.getSettings().getSprite("fx", "sotf_chalice_mine_aura");
	protected float whirlFade = 0f;
	protected float whirlAngle = 0f;
	private IntervalUtil whirlInterval = new IntervalUtil(0.15f, 0.2f);

	//protected IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		//interval.advance(amount);

		ShipAPI ship = weapon.getShip();
		if (ship == null) return;
		if (!ship.isAlive()) return;

		// listener for secondary bolt EMP to damage conversion
		if (!ship.hasListenerOfClass(SotfChaliceEffect.class)) {
			ship.addListener(this);
		}

		whirlAngle += amount * 160f;

		Vector2f loc = weapon.getFirePoint(0);
		boolean charging = weapon.getChargeLevel() > 0;
		if (charging) {
//			whirlFade += amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue() * 2f;
//			if (whirlFade > 1f) {
//				whirlFade = 1f;
//			}
			interval.advance(amount * ship.getMutableStats().getEnergyRoFMult().getModifiedValue());
			if (interval.intervalElapsed() && !ship.isPhased()) {
				CombatEntityAPI target = findTarget(loc, weapon, engine);
				if (target != null) {
					//weapon.getShip().getFluxTracker().showOverloadFloatyIfNeeded("TEST", Misc.getNegativeHighlightColor(), 0f, true);
					engine.addHitParticle(loc, ship.getVelocity(), 25f, 2f, COLOR);
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
//			whirlFade -= amount * ship.getMutableStats().getMissileRoFMult().getModifiedValue();
//			if (whirlFade < 0f) {
//				whirlFade = 0f;
//			}
			interval.setElapsed(0f);
		}

		whirlFade = weapon.getChargeLevel();

		whirlInterval.advance(amount * whirlFade);
		if (whirlInterval.intervalElapsed()) {
			engine.addSwirlyNebulaParticle(loc, ship.getVelocity(), 24f, 2f,
					0f, 0f, 0.7f, Misc.setAlpha(COLOR, 185), false);
		}

		MagicRender.singleframe(
				WHIRL1,
				loc,
				new Vector2f(40f, 40f),
				whirlAngle,
				Misc.setAlpha(COLOR, Math.round(155 * whirlFade)),
				true
		);
		MagicRender.singleframe(
				WHIRL2,
				loc,
				new Vector2f(40f, 40f),
				whirlAngle * 2f,
				Misc.setAlpha(COLOR, Math.round(155 * whirlFade)),
				true
		);
		MagicRender.singleframe(
				AURA,
				loc,
				new Vector2f(25f + 25f * whirlFade, 25f + 25f * whirlFade),
				whirlAngle * 0.8f,
				Misc.setAlpha(COLOR, Math.round(155 * whirlFade)),
				true
		);
	}

	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MissileAPI missile = (MissileAPI) projectile;
		missile.setEmpResistance(10000);
		missile.setEccmChanceOverride(1f);
		//missile.setNoFlameoutOnFizzling(true);
//		MissileAIPlugin proxAI = engine.createProximityFuseAI(missile);

		// plugin that handles proxy mine behaviour
		// waits until there is a nearby ship and then spawns an actual proxy mine that handles the countdown and explosion
		// (proxy mines aren't very suitable for the normal-missile behaviour of approaching Chalice orbs)
		engine.addPlugin(new BaseEveryFrameCombatPlugin() {
			boolean spawnedMine = false;
			MissileAPI proxyMine;
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (missile.isExpired() || missile.isFading() || missile.isFizzling()) {
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
				// uses missile velocity as "ship" velocity
				// this adds the proxy mine weapon's launch velocity on top so it lunges forwards
				CombatEntityAPI proxMine = engine.spawnProjectile(missile.getSource(), missile.getWeapon(),
						"sotf_chalice_proxmines", missile.getLocation(), missile.getFacing(), missile.getVelocity());
				// hand over collision duty to the proxy mine - have it inherit the missile's existing healthbar
				proxMine.setHitpoints(missile.getHitpoints());
				missile.setCollisionClass(CollisionClass.NONE);
				// ensure missile does not despawn while the proxy mine is active
				missile.setFlightTime(0f);
				proxyMine = (MissileAPI) proxMine;
				//engine.removeEntity(missile);
				spawnedMine = true;
			}
		});

		SotfClingingFlareVisualScript flare = new SotfClingingFlareVisualScript(
				new SotfClingingFlareParams(missile, COLOR, 120f, 60f)
		);
		flare.p.baseBrightness = 0.25f;
		flare.p.angle = 0f;
		engine.addLayeredRenderingPlugin(
				flare
		);
		missile.setCustomData("sotf_flare", flare);

		engine.addPlugin(new BaseEveryFrameCombatPlugin() {
			float alpha = 0f;
			float auraAngle = 0f;
			float glowBounce = 0f;
			boolean down = false;
			float glowBounceRateMult = 0.5f + (Misc.random.nextFloat() * 0.1f);
			protected SpriteAPI AURA = Global.getSettings().getSprite("fx", "sotf_chalice_mine_aura");
			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (engine.isPaused()) return;
				if (missile == null) {
					engine.removePlugin(this);
					return;
				}
				if (missile.wasRemoved()) {
					engine.removePlugin(this);
					return;
				}
				if (down) {
					glowBounce -= amount * glowBounceRateMult;
				} else {
					glowBounce += amount * glowBounceRateMult;
				}
				if (glowBounce > 1f) {
					down = true;
				} else if (glowBounce < 0f) {
					down = false;
				}
				if (!missile.isFizzling()) {
					alpha += amount * 7f;
					if (alpha > 1f) {
						alpha = 1f;
					}
				} else {
					alpha -= amount * 7f;
					if (alpha < 0f) {
						alpha = 0f;
					}
				}
				auraAngle += amount * 120f;
				missile.setSpriteAlphaOverride(alpha);
				MagicRender.singleframe(
						AURA,
						missile.getLocation(),
						new Vector2f(50f, 50f),
						auraAngle,
						Misc.setAlpha(COLOR, Math.round(alpha * (155 + (95 * glowBounce)))),
						true
				);
				SotfClingingFlareVisualScript flare = (SotfClingingFlareVisualScript) missile.getCustomData().get("sotf_flare");
				flare.p.baseBrightness = (0.5f * alpha);
				missile.getEngineController().extendFlame("sotf_chalicemine_bounce", 1f, 1f, alpha + (MagicAnim.smooth(glowBounce) * 0.5f));
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

	public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
		// bolt EMP to damage conversion
		// would do as an on-hit extra damage instance but it would interact weirdly with armor strength
		if (!shieldHit && param instanceof DamagingProjectileAPI proj && target instanceof ShipAPI targetShip) {
			if (proj.getWeapon() != null && proj.getWeapon().getSpec().getWeaponId().equals("sotf_chalice_boltlauncher") && proj.getEmpAmount() > 0) {
				float empDamageMult = targetShip.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
				if (empDamageMult < 0f) empDamageMult = 0f;
				// Dweller has no direct EMP resistance but their weapons/engines can't take damage - count it as 100% resist
				if (targetShip.getHullSpec().hasTag(Tags.DWELLER)) empDamageMult = 0f;
				float empProportion = proj.getEmpAmount() / proj.getDamageAmount();
				damage.getModifier().modifyMult("sotf_daydream_empconvert", 1f + (empProportion * (1f - empDamageMult)));
				return "sotf_daydream_empconvert_mod";
			}
		}
		return null;
	}
}




