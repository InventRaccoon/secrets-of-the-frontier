// TIME'S A RIVER AND ITS FLOW IS OURS TO CONTROl. Periodic timeflow boost
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
import data.hullmods.SotfNaniteSynthesized;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.combat.SotfRingTimerVisualScript;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.EnumSet;

public class SotfTickTock {

	// time between timeflow activations
	public static float TIMEFLOW_CD = 30f;
	// duration of fully active timeflow boost
	public static float TIMEFLOW_DURATION = 3f;
	// duration of linearly ramping up/down timeflow at start/end
	public static float TIMEFLOW_RAMP = 0.5f;
	public static float TIMEFLOW_MULT = 3f;

	public static final String TIMER_KEY = "sotf_ticktockcdvisual";

	public static Color COLOR = new Color(90,165,255);
	public static final Color JITTER_COLOR = new Color(90,165,255,55);
	public static final Color JITTER_UNDER_COLOR = new Color(90,165,255,155);

	public static class PerfectTiming extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.addListener(new SotfTickTockListener(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.removeListenerOfClass(SotfTickTockListener.class);
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

			String timeflowDuration = new DecimalFormat("#.##").format(TIMEFLOW_DURATION);
			String seconds = "seconds";
			if (TIMEFLOW_DURATION == 1f) {
				seconds = "second";
			}

			info.addPara("Every %s seconds, the ship gains a %s timeflow multiplier for %s %s", 0f, hc, hc,
					"" + (int) TIMEFLOW_CD, (int) TIMEFLOW_MULT + "x", timeflowDuration, seconds);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfTickTockListener implements AdvanceableListener {
		protected ShipAPI ship;
		protected float timer = 0f;
		protected float effectLevel = 0f;
		protected float duration = 0f;
		protected boolean active = false;
		public SotfTickTockListener(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!ship.isAlive()) {
				ship.getMutableStats().getTimeMult().unmodify(SotfIDs.SKILL_TICKTOCK);
				Global.getCombatEngine().getTimeMult().unmodify(SotfIDs.SKILL_TICKTOCK);
				ship.removeListener(this);
				return;
			}
			boolean player = ship == Global.getCombatEngine().getPlayerShip();
			if (completelyInactive()) {
				timer += amount;
			}
			if (timer >= TIMEFLOW_CD) {
				timer = 0f;
				active = true;
				effectLevel = 0f;
				duration = TIMEFLOW_DURATION + TIMEFLOW_RAMP;
				Global.getSoundPlayer().playSound("system_temporalshell", 1f, 1f, ship.getLocation(), ship.getVelocity());
			}

			if (!ship.getCustomData().containsKey(TIMER_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = COLOR;
				p.ship = ship;
				p.radius = ship.getShieldRadiusEvenIfNoShield() + 24f;
				p.thickness = 13f;
				p.baseAlpha = 0.4f;
				p.maxArc = 60f;
				//p.followFacing = true;
				p.renderDarkerCopy = true;
				p.reverseRing = true;
				p.degreeOffset = 60f;
				p.layer = CombatEngineLayers.JUST_BELOW_WIDGETS;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(TIMER_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(TIMER_KEY);
				if (active) {
					visual.p.baseAlpha = 0.4f + (0.4f * effectLevel);
					visual.p.totalArc = duration / (TIMEFLOW_DURATION + TIMEFLOW_RAMP);
				} else {
					visual.p.baseAlpha = 0.4f;
					visual.p.totalArc = timer / TIMEFLOW_CD;
				}
			}

			ship.setJitter(this, JITTER_COLOR, effectLevel, 3, 0, 10f * effectLevel);
			ship.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 25, 0f, 17f * effectLevel);

			if (!active || completelyInactive()) {
				ship.getMutableStats().getTimeMult().unmodify(SotfIDs.SKILL_TICKTOCK);
				Global.getCombatEngine().getTimeMult().unmodify(SotfIDs.SKILL_TICKTOCK);
			} else {
				duration -= amount;
				if (duration < 0f) {
					duration = 0f;
				}
				if (duration <= 0f) {
					effectLevel -= amount / TIMEFLOW_RAMP;
					if (effectLevel <= 0f) {
						active = false;
						effectLevel = 0f;
						return;
					}
				} else {
					effectLevel += amount / TIMEFLOW_RAMP;
				}
				if (effectLevel > 1f) {
					effectLevel = 1f;
				}
				float shipTimeMult = 1f + (TIMEFLOW_MULT - 1f) * effectLevel;
				float perceptionMult = shipTimeMult;
				if (player) {
					perceptionMult = 1f + ((TIMEFLOW_MULT - 1f) * 0.65f) * effectLevel;
				}
				ship.getMutableStats().getTimeMult().modifyMult(SotfIDs.SKILL_TICKTOCK, shipTimeMult);
				if (player) {
					Global.getCombatEngine().getTimeMult().modifyMult(SotfIDs.SKILL_TICKTOCK, 1f / perceptionMult);
				}
			}
		}

		private boolean completelyInactive() {
			return duration <= 0f && effectLevel <= 0f;
		}
	}

}
