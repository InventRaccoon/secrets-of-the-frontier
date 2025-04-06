// IN VICTORY, SACRIFICE. At high flux, ship can dump its flux into an ally
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfAuraVisualScript;
import data.scripts.combat.SotfRingTimerVisualScript;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Iterator;

public class SotfInSacrificeMeaning {

	public static float COOLDOWN_SECONDS = 25f;
	public static float FLUX_THRESHOLD = 0.8f;
	public static float UNLOAD_PERCENT = 0.3f;

	public static float RANGE = 1000f;

	public static final String AURA_VISUAL_KEY = "sotf_inmeaningsacrifice_auravisual";
	public static final String COOLDOWN_KEY = "sotf_inmeaningsacrifice_cdvisual";

	public static float VFX_BEAM_FULL = 0.25f;
	public static float VFX_BEAM_FADEOUT = 0.5f;

	public static Color COLOR = new Color(100,75,155,255);
	public static final Color OVERLOAD_COLOR = new Color(255,155,255,255);

	public static class Scapegoating extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.addListener(new SotfScapegoatingScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return;
			ship.removeListenerOfClass(SotfScapegoatingScript.class);
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

			String seconds = "seconds";
			if (COOLDOWN_SECONDS == 1f) {
				seconds = "second";
			}

			info.addPara("When above %s hard flux and within %s units of an allied ship, automatically unload %s of maximum flux into the ally's capacitors", 0f, hc, hc,
					"" + (int) (FLUX_THRESHOLD * 100f) + "%", "" + (int) (UNLOAD_PERCENT * 100f) + "%", seconds);
			info.addPara("This effect cannot trigger for %s seconds after a flux unload, nor while the ship is phased or already overloading", 0f, hc, hc, "" + (int) COOLDOWN_SECONDS);
			info.addPara("Always unloads flux into the capacitors of the lowest-flux ally within range, and dumps soft flux before hard flux", hc, 0f);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfScapegoatingScript implements AdvanceableListener {
		protected ShipAPI ship;

		protected float cooldown = COOLDOWN_SECONDS;
		protected float reaction_timer = 0.25f;

		protected float cdBlinking = 0;
		protected boolean cdBlinkReverse = false;
		public SotfScapegoatingScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!ship.isAlive() || ship.isFighter() || ship.isStationModule()) {
				return;
			}

			Color color = new Color(100,75,155,255);

			// create AoE visual
			if (!ship.getCustomData().containsKey(AURA_VISUAL_KEY)) {
				SotfAuraVisualScript.AuraParams p = new SotfAuraVisualScript.AuraParams();
				p.color = Misc.setAlpha(COLOR, 125);
				p.ship = ship;
				p.thickness = 15f;
				p.radius = RANGE;
				SotfAuraVisualScript plugin = new SotfAuraVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(AURA_VISUAL_KEY, plugin);
			} else {
				SotfAuraVisualScript visual = (SotfAuraVisualScript) ship.getCustomData().get(AURA_VISUAL_KEY);
				visual.p.baseAlpha = 0.25f;
				visual.p.playerAlpha = 0.5f;
			}

			if (!ship.getCustomData().containsKey(COOLDOWN_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = COLOR;
				p.ship = ship;
				p.radius = ship.getShieldRadiusEvenIfNoShield() + 26f;
				p.thickness = 10f;
				p.baseAlpha = 0.4f;
				p.maxArc = 60f;
				//p.followFacing = true;
				p.renderDarkerCopy = true;
				p.reverseRing = true;
				p.degreeOffset = 120;
				p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(COOLDOWN_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(COOLDOWN_KEY);
				visual.p.totalArc = 1f - (cooldown / COOLDOWN_SECONDS);
				if (ship.getHardFluxLevel() > FLUX_THRESHOLD) {
					visual.p.baseAlpha = 0.4f + (0.4f * cdBlinking);
				}
			}

			float dirMult = 1f;
			if (cdBlinkReverse) {
				dirMult *= -1f;
			}
			cdBlinking += amount * Global.getCombatEngine().getTimeMult().getModifiedValue() * dirMult;
			if (cdBlinking > 1) {
				cdBlinkReverse = true;
			} else if (cdBlinking < 0) {
				cdBlinking = 0;
				cdBlinkReverse = false;
			}

			float timeMult = (Global.getCombatEngine().getTimeMult().getModifiedValue() * ship.getMutableStats().getTimeMult().getModifiedValue());
			cooldown -= amount * timeMult;
			if (cooldown < 0) {
				cooldown = 0;
			}

			if (cooldown > 0 || (ship.getHardFluxLevel() < FLUX_THRESHOLD) || ship.isPhased() || ship.getFluxTracker().isOverloaded()) {
				return;
			}

			ShipAPI targetShip = findTarget(ship);
			if (targetShip == null) {
				return;
			}
			float fluxToDump = ship.getMaxFlux() * UNLOAD_PERCENT;
			float fluxSoft = ship.getCurrFlux() - ship.getFluxTracker().getHardFlux();
			ship.getFluxTracker().decreaseFlux(fluxToDump);
			targetShip.getFluxTracker().increaseFlux(fluxSoft, false);
			targetShip.getFluxTracker().increaseFlux(Math.max(0, fluxToDump - fluxSoft), true);

			ship.getFluxTracker().showOverloadFloatyIfNeeded("Flux unloaded!", OVERLOAD_COLOR, 6f, true);
			targetShip.getFluxTracker().showOverloadFloatyIfNeeded("Scapegoated!", OVERLOAD_COLOR, 6f, true);

			for (int i = 0; i < 5; i++) {
				Vector2f from = Misc.getPointWithinRadius(ship.getShieldCenterEvenIfNoShield(), ship.getCollisionRadius() * 0.15f);
				Vector2f to = Misc.getPointWithinRadius(targetShip.getShieldCenterEvenIfNoShield(), targetShip.getCollisionRadius() * 0.15f);
				MagicFakeBeamPlugin.addBeam(
						1f,
						VFX_BEAM_FADEOUT,
						5f,
						from,
						Misc.getAngleInDegrees(from, to),
						Misc.getDistance(from, to),
						Color.WHITE,
						OVERLOAD_COLOR
				);
			}

			cooldown = COOLDOWN_SECONDS;
		}

		public ShipAPI findTarget(ShipAPI ship) {
			float range = RANGE;
			Vector2f from = ship.getLocation();

			Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
					range * 2f, range * 2f);
			int owner = ship.getOwner();
			ShipAPI best = null;
			float minScore = 9999f;

			while (iter.hasNext()) {
				Object o = iter.next();
				if (!(o instanceof ShipAPI)) continue;
				ShipAPI other = (ShipAPI) o;
				if (ship == other) continue;
				if (owner != other.getOwner()) continue;
				if (Misc.getDistance(from, other.getLocation()) > range) continue;

				ShipAPI otherShip = (ShipAPI) other;

				if (!isValidTarget(ship, otherShip)) continue;

				float score = ship.getCurrFlux();

				if (score < minScore) {
					minScore = score;
					best = other;
				}
			}
			return best;
		}

		public boolean isValidTarget(ShipAPI ship, ShipAPI target) {
			boolean isValid = true;
			if (!target.isAlive() ||
					target.getOwner() != ship.getOwner() ||
					target.isFighter() ||
					target.isPhased() ||
					target.getCollisionClass() == CollisionClass.NONE ||
					target.getVariant().hasHullMod(HullMods.VASTBULK) ||
					target.isStation() ||
					target.isStationModule()) {
				isValid = false;
			}
			if (target.getFluxTracker().isOverloadedOrVenting()) isValid = false;
			return isValid;
		}
	}

}
