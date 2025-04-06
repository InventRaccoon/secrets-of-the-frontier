// START WITH A LITTLE TUNE... Ship gains a powerful Wispersong aura that raises wisps from anything that dies nearby
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfAuraVisualScript;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class SotfGrovetender {

	public static final float WISPERING_RANGE = 2400f;

	public static final String WISPERED_KEY = "sotf_wispered";
	public static final String VISUAL_KEY = "sotf_grovetendervisual";

	public static class Spiritcaller extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.addListener(new SotfGrovetenderScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.removeListenerOfClass(SotfGrovetenderScript.class);
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

			info.addPara("Piloted ship gains an aura that generates wisps from vessels that are disabled within %s units", 0f, hc, hc,
					"" + (int) WISPERING_RANGE);
			info.addPara("Wisps seek out and attack enemy ships", 0f, hc, hc);
			info.addPara("Generates 1/2/4/6 wisps based on hull size", 0f, hc, hc);
			info.addPara("Fighters sometimes raise a single lesser wisp", 0f, hc, hc);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfGrovetenderScript implements AdvanceableListener {
		protected ShipAPI ship;
		public SotfGrovetenderScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!ship.isAlive() || ship.isFighter() || ship.isStationModule()) {
				return;
			}

			Color color = new Color(100,75,155,255);

			// create AoE visual
			if (!ship.getCustomData().containsKey(VISUAL_KEY)) {
				SotfAuraVisualScript.AuraParams p = new SotfAuraVisualScript.AuraParams();
				p.color = Misc.setAlpha(color, 125);
				p.ship = ship;
				p.radius = WISPERING_RANGE;
				Global.getCombatEngine().addLayeredRenderingPlugin(new SotfAuraVisualScript(p));
				ship.setCustomData(VISUAL_KEY, true);
			}

			ShipAPI target = findTarget(ship, Global.getCombatEngine());
			if (target != null) {
				// 1/2/4/6
				int wisps_to_spawn = target.getHullSize().ordinal() - 1;
				if (target.getHullSize().ordinal() >= 4) {
					wisps_to_spawn += 1;
				}
				if (target.getHullSize().ordinal() >= 5) {
					wisps_to_spawn += 1;
				}
				float arcThickness = 30f;
				String wispWingVar = "sotf_wisp_wing";
				//if (target.getOwner() == ship.getOwner()) {
					//wispWingVar = "sotf_wwisp_wing";
				//}
				// will only happen if the fighter actually leaves a hulk
				// so about 50% of the time?
				if (target.isFighter()) {
					wispWingVar = "sotf_lwisp_wing";
					arcThickness *= 0.5f;
					wisps_to_spawn = 1;
				}
				for (int i = 0; i < wisps_to_spawn; i++) {
					Vector2f to = Misc.getPointAtRadius(target.getLocation(), target.getCollisionRadius() + 25);

					CombatEngineAPI engine = Global.getCombatEngine();

					CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOriginalOwner());
					boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
					fleetManager.setSuppressDeploymentMessages(true);
					CombatEntityAPI wisp = engine.getFleetManager(ship.getOriginalOwner()).spawnShipOrWing(wispWingVar, to, Misc.getAngleInDegrees(target.getLocation(), to), 0f);
					Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 1f, wisp.getLocation(), new Vector2f(0,0));
					Global.getCombatEngine().spawnEmpArcVisual(Misc.getPointWithinRadius(ship.getLocation(), 50f), ship, wisp.getLocation(), wisp, arcThickness, new Color(100,25,155,255), Color.white);
					fleetManager.setSuppressDeploymentMessages(wasSuppressed);
				}
				target.setCustomData(WISPERED_KEY, true);
			}
		}

		public ShipAPI findTarget(ShipAPI ship, CombatEngineAPI engine) {
			float range = WISPERING_RANGE;
			Vector2f from = ship.getLocation();

			Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from,
					range * 2f, range * 2f);
			int owner = ship.getOwner();
			ShipAPI best = null;
			float minScore = Float.MAX_VALUE;

			while (iter.hasNext()) {
				Object o = iter.next();
				if (!(o instanceof ShipAPI)) continue;
				ShipAPI other = (ShipAPI) o;
				// player concord ships can't summon wisps from allies - Eidolon's can
				if (owner == 0 && other.getOwner() == owner) continue;

				ShipAPI otherShip = (ShipAPI) other;
				if (!otherShip.isHulk()) continue;
				if (otherShip.isPiece()) continue;
				if (otherShip.getVariant().hasHullMod(SotfIDs.HULLMOD_WISP)) continue;
				if (otherShip.getCustomData().get(WISPERED_KEY) != null) continue;
				if (otherShip.getCustomData().get(WISPERED_KEY + ship.getId()) != null) continue;

				if (other.getCollisionClass() == CollisionClass.NONE) continue;

				float radius = Misc.getTargetingRadius(from, other, false);
				float dist = Misc.getDistance(from, other.getLocation()) - radius;
				if (dist > range) {
					otherShip.setCustomData(WISPERED_KEY + ship.getId(), true);
					continue;
				}
				float score = dist;

				if (score < minScore) {
					minScore = score;
					best = other;
				}
			}
			return best;
		}
	}

}
