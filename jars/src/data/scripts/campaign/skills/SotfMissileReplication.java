package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.hullmods.PeriodicMissileReload;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.ids.SotfIDs;

public class SotfMissileReplication {

	public static float RELOAD_TIMER = 60f;

	public static String MR_DATA_KEY = SotfIDs.SKILL_MISSILEREPLICATION + "_reloadKey";

	public static class SotfMissileReplicationReloadData {
		IntervalUtil interval = new IntervalUtil(RELOAD_TIMER, RELOAD_TIMER);
	}

	public static class MissileReload extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SotfMissileReplicatorReload(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SotfMissileReplicatorReload.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

			info.addPara("Every %s seconds, reloads small missile weapons equal to their base ammo, and medium missile weapons equal to half their base ammo",
					0f, hc, hc,
					"" + (int) (RELOAD_TIMER)
			);
		}
	}

	public static class SotfMissileReplicatorReload implements AdvanceableListener {
		protected ShipAPI ship;
		public SotfMissileReplicatorReload(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (Global.getCurrentState() == GameState.COMBAT &&
					Global.getCombatEngine() != null) {

				CombatEngineAPI engine = Global.getCombatEngine();

				String key = MR_DATA_KEY + "_" + ship.getId();
				SotfMissileReplicationReloadData data = (SotfMissileReplicationReloadData) engine.getCustomData().get(key);
				if (data == null) {
					data = new SotfMissileReplicationReloadData();
					engine.getCustomData().put(key, data);
				}

				data.interval.advance(amount);
				if (data.interval.intervalElapsed()) {
					for (WeaponAPI w : ship.getAllWeapons()) {
						if (w.getType() != WeaponAPI.WeaponType.MISSILE) continue;
						if (w.getSize().equals(WeaponAPI.WeaponSize.LARGE)) continue;

						if (w.usesAmmo() && w.getAmmo() < w.getMaxAmmo()) {
							float mult = 1f;
							if (w.getSize().equals(WeaponAPI.WeaponSize.MEDIUM)) mult = 0.5f;

							int maxReload = w.getAmmo() + Math.round(w.getSpec().getMaxAmmo() * mult);

							w.setAmmo(Math.min(maxReload, w.getMaxAmmo()));
						}
					}
				}
			}
		}
	}
}
