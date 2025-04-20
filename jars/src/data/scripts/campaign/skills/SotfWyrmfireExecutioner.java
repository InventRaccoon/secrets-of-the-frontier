// TIME'S A RIVER AND ITS FLOW IS OURS TO CONTROl. Periodic timeflow boost
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.impl.combat.dem.DEMScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.JitterUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

public class SotfWyrmfireExecutioner {

	// time between timeflow activations
	public static float EXECUTE_THRESHOLD = 0.5f;
	public static float EXECUTE_CD = 60f;

	public static Color COLOR = new Color(255,85,110);
	public static Color COLOR_2 = new Color(255,110,85);

	public static class TheAxeDrops extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.addListener(new SotfTheAxeDropsListener(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.removeListenerOfClass(SotfTheAxeDropsListener.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {

		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {

		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			init(stats, skill);

			info.addPara("Hitting a hostile ship below %s hull with a missile fires a Dragonfire DEM Torpedo at " +
					"it, twice if the target is a capital ship",0f, hc, hc, "" + (int) EXECUTE_THRESHOLD * 100f + "%");

			info.addPara("Cannot trigger on the same target for %s seconds", 0f, hc, hc,
					"" + (int) EXECUTE_CD);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfTheAxeDropsListener implements AdvanceableListener, DamageDealtModifier {
		protected ShipAPI ship;
		protected float timer = 0f;
		protected float effectLevel = 0f;
		protected float duration = 0f;
		protected boolean active = false;
		IntervalUtil interval = new IntervalUtil(0.075f, 0.125f);

		public SotfTheAxeDropsListener(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			ArrayList<ShipAPI> executables = findExecutablesFor(ship);

			interval.advance(amount / ship.getMutableStats().getTimeMult().getModifiedValue());
			if (interval.intervalElapsed()) {
				CombatEngineAPI engine = Global.getCombatEngine();

				Color c = RiftLanceEffect.getColorForDarkening(COLOR_2);
				c = Misc.setAlpha(c, 75);
				float baseDuration = 2f;
				for (ShipAPI toSmoke : executables) {
					Vector2f vel = new Vector2f(toSmoke.getVelocity());
					float size = toSmoke.getCollisionRadius() * 0.2f;
					for (int i = 0; i < 4; i++) {
						Vector2f point = new Vector2f(toSmoke.getShieldCenterEvenIfNoShield());
						point = Misc.getPointWithinRadiusUniform(point, toSmoke.getCollisionRadius() * 0.1f, Misc.random);
						float dur = baseDuration + baseDuration * (float) Math.random();
						float nSize = size;
						Vector2f pt = Misc.getPointWithinRadius(point, nSize * 0.5f);
						Vector2f v = Misc.getUnitVectorAtDegreeAngle((float) Math.random() * 360f);
						v.scale(nSize + nSize * (float) Math.random() * 0.5f);
						v.scale(0.2f);
						Vector2f.add(vel, v, v);

						float maxSpeed = nSize * 1.5f * 0.2f;
						float minSpeed = nSize * 1f * 0.2f;
						float overMin = v.length() - minSpeed;
						if (overMin > 0) {
							float durMult = 1f - overMin / (maxSpeed - minSpeed);
							if (durMult < 0.1f) durMult = 0.1f;
							dur *= 0.5f + 0.5f * durMult;
						}
						engine.addNegativeNebulaParticle(pt, v, nSize, 2f,
								0.5f / dur, 0f, dur, c);
					}
				}
			}
		}

		@Override
		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (!(target instanceof ShipAPI targetShip) || !(param instanceof DamagingProjectileAPI)) {
				return null;
			}
			if (!(param instanceof MissileAPI) && !((DamagingProjectileAPI) param).isFromMissile()) {
				return null;
			}
            if (targetShip.hasListenerOfClass(SotfTheAxeDropsCDListener.class)) {
				return null;
			}
			if (targetShip.isFighter() || targetShip.isHulk() || targetShip.getOwner() == ship.getOwner() || shieldHit) {
				return null;
			}
			if ((target.getHitpoints() / target.getMaxHitpoints()) <= EXECUTE_THRESHOLD) {
				int missiles = 1;
				if (targetShip.isCapital()) {
					missiles = 2;
				}
				for (int i = 0; i < missiles; i++) {
					CombatEntityAPI proj = Global.getCombatEngine().spawnProjectile(ship, null, "dragon",
							Misc.getPointWithinRadius(ship.getShieldCenterEvenIfNoShield(), ship.getCollisionRadius() * 0.4f), ship.getFacing(), ship.getVelocity());
					MissileAPI missile = (MissileAPI) proj;
					GuidedMissileAI ai = (GuidedMissileAI) missile.getAI();
					ai.setTarget(targetShip);
					missile.setMaxFlightTime(20f);

					DEMScript script = new DEMScript(missile, ship, null);
					Global.getCombatEngine().addPlugin(script);
				}
				targetShip.addListener(new SotfTheAxeDropsCDListener(targetShip));
				targetShip.getFluxTracker().showOverloadFloatyIfNeeded("Wyrmfire Execute!", COLOR, 10f, true);
				Global.getSoundPlayer().playSound("sotf_wyrmfire_execute", 1f, 1f, targetShip.getLocation(), targetShip.getVelocity());
				Global.getCombatEngine().addLayeredRenderingPlugin(new SotfFadingSkullVisual(targetShip));
			}
			return null;
		}

		// find a valid Wispersong hulk for a ship and invalidate far-away ships from future checks by that ship
		public ArrayList<ShipAPI> findExecutablesFor(ShipAPI ship) {
			float range = 2000f;
			Vector2f from = ship.getLocation();

			Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
					range * 2f, range * 2f);
			int owner = ship.getOwner();

			ArrayList<ShipAPI> list = new ArrayList<>();

			while (iter.hasNext()) {
				Object o = iter.next();
				if (!(o instanceof ShipAPI)) continue;
				ShipAPI other = (ShipAPI) o;
				if (other.getOwner() == owner) continue;

				ShipAPI otherShip = (ShipAPI) other;
				if (otherShip.isHulk()) continue;
				if (otherShip.isPiece()) continue;
				if (otherShip.hasListenerOfClass(SotfTheAxeDropsCDListener.class)) continue;
				if (otherShip.getHitpoints() / otherShip.getMaxHitpoints() > EXECUTE_THRESHOLD) continue;
				if (otherShip.isFighter()) continue;

				list.add(otherShip);
			}
			return list;
		}
	}

	public static class SotfTheAxeDropsCDListener implements AdvanceableListener {
		protected ShipAPI ship;
		protected float timer = EXECUTE_CD;
		public SotfTheAxeDropsCDListener(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			timer -= amount / ship.getMutableStats().getTimeMult().getModifiedValue();
			if (timer <= 0f) {
				ship.removeListener(this);
			}
		}
	}

	public static class SotfFadingSkullVisual extends BaseCombatLayeredRenderingPlugin {

		protected SpriteAPI sprite = Global.getSettings().getSprite("ui", "sotf_skull_icon");
		public ShipAPI ship;
		float duration = 0.5f;
		float fade = 1f;
		float fadeIn = 0f;
		protected JitterUtil jitter = new JitterUtil();

		public SotfFadingSkullVisual(ShipAPI ship) {
			this.ship = ship;
			jitter.setUseCircularJitter(true);
			jitter.setSetSeedOnRender(false);
			sprite.setNormalBlend();
			sprite.setColor(COLOR_2);
			sprite.setSize(ship.getShieldRadiusEvenIfNoShield() * 0.8f, ship.getShieldRadiusEvenIfNoShield() * 0.8f);
		}

		public float getRenderRadius() {
			return ship.getShieldRadiusEvenIfNoShield();
		}

		@Override
		public EnumSet<CombatEngineLayers> getActiveLayers() {
			return EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS);
		}

		public void render(CombatEngineLayers layer, ViewportAPI viewport) {
			if (layer == CombatEngineLayers.JUST_BELOW_WIDGETS) {
				sprite.setAlphaMult(fade * fadeIn * 0.5f);
				jitter.render(sprite, entity.getLocation().x, entity.getLocation().y, 20f - (20f * fade), 5);
				//sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
			}
		}

		public void init(CombatEntityAPI entity) {
			super.init(entity);
			entity.getLocation().set(ship.getShieldCenterEvenIfNoShield());
		}

		public void advance(float amount) {
			if (duration > 0f) {
				duration -= amount;
			} else {
				fade -= amount;
			}
			if (fadeIn < 1f) {
				fadeIn += amount * 4f;
				if (fadeIn > 1f) fadeIn = 1f;
			}
			if (fade < 0f) fade = 0f;
			if (ship == null) return;
			entity.getLocation().set(ship.getShieldCenterEvenIfNoShield().x, ship.getShieldCenterEvenIfNoShield().y);
		}

		public boolean isExpired() {
			return fade <= 0f;
		}

	}

}
