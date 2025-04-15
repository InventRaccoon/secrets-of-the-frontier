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
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.dem.DEMScript;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.text.DecimalFormat;

public class SotfWyrmfireExecutioner {

	// time between timeflow activations
	public static float EXECUTE_THRESHOLD = 0.75f;
	public static float EXECUTE_CD = 30f;

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
		public SotfTheAxeDropsListener(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
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
			if (targetShip.isFighter() || targetShip.isHulk() || targetShip.getOwner() == ship.getOwner()) {
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
				targetShip.getFluxTracker().showOverloadFloatyIfNeeded("Wyrmfire Execute!", new Color(255,85,110), 10f, true);
			}
			return null;
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

}
