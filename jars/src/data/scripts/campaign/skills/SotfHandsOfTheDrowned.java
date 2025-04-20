// LITTLE FISH, LITTLE FISH, WHERE ARE YOU FOUND? Triggers a passive minefield that pulls targets hit
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.hullmods.StealthMinefield;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SotfHandsOfTheDrowned {

	public static int MAX_PER_WAVE_BASE = 5;
	public static int MAX_PER_WAVE_RANDOM = 5;

	public static float DELAY_MIN = 0.85f;
	public static float DELAY_MAX = 1.65f;
	public static float MINE_CHANCE = 0.25f;

	public static final Color JITTER_UNDER_COLOR = new Color(55,155,255,155);

	public static String MINEFIELD_DATA_KEY = "sotf_handsofthedrowned_minefield_key";

	public static class DeadlyWaters extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			ship.addListener(new SotfDeadlyWatersScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			ship.removeListenerOfClass(SotfDeadlyWatersScript.class);
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

			String timeBetweenMines = new DecimalFormat("#.##").format(((DELAY_MIN + DELAY_MAX) / 2));

			info.addPara("Triggers a stealth minefield that assails the enemy fleet with gravitic mines until the ship is destroyed or retreats", hc, 0f);
			info.addPara("A wave of mines appears every %s on average, with between %s and %s mines per wave, with no more than one mine appearing to strike a given hostile ship", 0f, hc, hc,
					"" + timeBetweenMines, "" + MAX_PER_WAVE_BASE, "" + (MAX_PER_WAVE_BASE + MAX_PER_WAVE_RANDOM));
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfDeadlyWatersScript implements AdvanceableListener {
		protected ShipAPI ship;

		public SotfDeadlyWatersScript(ShipAPI ship) {
			this.ship = ship;
		}

		public static class IncomingMine {
			Vector2f mineLoc;
			float delay;
			ShipAPI target;
		}

		public static class MinefieldData {
			ShipAPI source;
			IntervalUtil tracker = new IntervalUtil(0.85f, 1.65f);
			List<IncomingMine> incoming = new ArrayList<IncomingMine>();
		}

		public void advance(float amount) {
			CombatEngineAPI engine = Global.getCombatEngine();
			if (ship.isFighter() || ship.isStationModule()) return;
			if (!ship.isAlive()) {
				engine.getCustomData().remove(MINEFIELD_DATA_KEY);
				return;
			}

			MinefieldData data = (MinefieldData) engine.getCustomData().get(MINEFIELD_DATA_KEY);
			if (data == null) {
				data = new MinefieldData();
				data.source = ship;
				engine.getCustomData().put(MINEFIELD_DATA_KEY, data);
			}

			if (data.source != ship) return;

			Global.getCombatEngine().maintainStatusForPlayerShip(MINEFIELD_DATA_KEY, Global.getSettings().getSpriteName("ui", "sotf_handsofthedrowned_deadlywaters"),
					"Hands of the Drowned", "Gravitic Minefield Active", true);

			for (IncomingMine inc : new ArrayList<IncomingMine>(data.incoming)) {
				inc.delay -= amount;
				if (inc.delay <= 0) {
					spawnMine(ship, inc.mineLoc, inc.target);
					data.incoming.remove(inc);
				}
			}

			data.tracker.advance(amount / ship.getMutableStats().getTimeMult().getModifiedValue());
			if (!data.tracker.intervalElapsed()) return;

			WeightedRandomPicker<IncomingMine> picker = new WeightedRandomPicker<IncomingMine>();

			for (ShipAPI enemy : engine.getShips()) {
				if (enemy == ship) continue;
				if (enemy.isHulk()) continue;
				if (enemy.getOwner() == ship.getOwner()) continue;
				if (enemy.isFighter()) continue;
				if (enemy.isDrone()) continue;
				if (enemy.isStation()) continue;
				if (enemy.isStationModule()) continue;
				if (enemy.getTravelDrive() != null && enemy.getTravelDrive().isActive()) continue;

				if ((float) Math.random() > MINE_CHANCE) continue;

				Vector2f mineLoc = Misc.getPointAtRadius(enemy.getLocation(),
						enemy.getCollisionRadius() + 400f + 200f * (float) Math.random());
				float minOk = 400f + enemy.getCollisionRadius();
				if (!isAreaClear(mineLoc, minOk)) continue;

				IncomingMine inc = new IncomingMine();
				inc.delay = (float) Math.random() * 1.5f;
				inc.target = enemy;
				inc.mineLoc = mineLoc;

				picker.add(inc);
			}

			// 5-10 mines
			int numToSpawn = Math.max(1, Math.min(new Random().nextInt(MAX_PER_WAVE_BASE + 1) + MAX_PER_WAVE_RANDOM, picker.getItems().size()));

			for (int i = 0; i < numToSpawn && !picker.isEmpty(); i++) {
				IncomingMine inc = picker.pickAndRemove();
				data.incoming.add(inc);
			}
		}

		public void spawnMine(ShipAPI source, Vector2f mineLoc, ShipAPI target) {
			float mineDir = Misc.getAngleInDegrees(mineLoc, target.getLocation());
			CombatEngineAPI engine = Global.getCombatEngine();
			Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 50f + (float) Math.random() * 50f);
			MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
					"sotf_graviticminelayer",
					currLoc,
					mineDir, null);
			if (source != null) {
				Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
						source, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
			}

			mine.setFlightTime((float) Math.random());
			mine.fadeOutThenIn(0.5f);
			Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, 0.5f));

			Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
		}

		public boolean isAreaClear(Vector2f loc, float range) {
			CombatEngineAPI engine = Global.getCombatEngine();
			for (ShipAPI other : engine.getShips()) {
				if (other.isFighter()) continue;
				if (other.isDrone()) continue;

				float dist = Misc.getDistance(loc, other.getLocation());
				if (dist < range) {
					return false;
				}
			}

			for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
				float dist = Misc.getDistance(loc, other.getLocation());
				if (dist < other.getCollisionRadius() + 100f) {
					return false;
				}
			}

			return true;
		}

		protected EveryFrameCombatPlugin createMissileJitterPlugin(final MissileAPI mine, final float fadeInTime) {
			return new BaseEveryFrameCombatPlugin() {
				float elapsed = 0f;
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) return;

					elapsed += amount;

					float jitterLevel = mine.getCurrentBaseAlpha();
					if (jitterLevel < 0.5f) {
						jitterLevel *= 2f;
					} else {
						jitterLevel = (1f - jitterLevel) * 2f;
					}

					float jitterRange = 1f - mine.getCurrentBaseAlpha();
					//jitterRange = (float) Math.sqrt(jitterRange);
					float maxRangeBonus = 50f;
					float jitterRangeBonus = jitterRange * maxRangeBonus;
					Color c = JITTER_UNDER_COLOR;
					c = Misc.setAlpha(c, 70);
					//mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0.1f, jitterRangeBonus);
					mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0, jitterRangeBonus);

					if (jitterLevel >= 1 || elapsed > fadeInTime) {
						Global.getCombatEngine().removePlugin(this);
					}
				}
			};
		}
	}

}
