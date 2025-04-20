// TRUE TERROR IS IN THE SILENCE. Periodic Quantum Disruptor effect on ALL enemy ships
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
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.SotfRingTimerVisualScript;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class SotfPerfectStorm {

	public static float TIME_BETWEEN_STRIKES = 35f;
	public static float OVERLOAD_TIME = 2.5f;
	public static final String TIMER_KEY = "sotf_perfectstormcdvisual";

	public static final String REACTED_KEY = "sotf_perfectstorm_reacted";
	public static final String WILL_REACT_KEY = "sotf_perfectstorm_willreact";
	public static final String REACTION_SPEED_KEY = "sotf_perfectstorm_reactionspeed";

	public static Color COLOR = new Color(100,75,155,255);
	public static final Color OVERLOAD_COLOR = new Color(255,155,255,255);

	public static class Stormbringer extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.addListener(new SotfSilenceBetweenStrikesScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			ship.removeListenerOfClass(SotfSilenceBetweenStrikesScript.class);
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

			String overloadDuration = new DecimalFormat("#.##").format(OVERLOAD_TIME);
			String seconds = "seconds";
			if (OVERLOAD_TIME == 1f) {
				seconds = "second";
			}

			info.addPara("Piloted ship creates an extreme quantum disruption across the entire battlespace every %s seconds, causing all hostile " +
							"vessels and fighters to overload for %s %s", 0f, hc, hc,
					"" + (int) TIME_BETWEEN_STRIKES, overloadDuration, seconds);
			info.addPara("Ships that are already overloaded instead have their overload extended by %s %s", 0f, hc, hc, overloadDuration, seconds);
			info.addPara("Ships are not affected by the disruption if they are actively venting or currently phased", hc, 0f);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfSilenceBetweenStrikesScript implements AdvanceableListener {
		protected ShipAPI ship;

		protected float timer = TIME_BETWEEN_STRIKES;
		protected float reaction_timer = 0.25f;
		public SotfSilenceBetweenStrikesScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!ship.isAlive()) {
				Global.getCombatEngine().getListenerManager().removeListener(this);
				return;
			}

			// one instance: note that AdvanceableListeners don't actually *do* anything when assigned to the engine
			if (!Global.getCombatEngine().getListenerManager().hasListenerOfClass(SotfSilenceBetweenStrikesScript.class)) {
				Global.getCombatEngine().getListenerManager().addListener(this);
			}

			// create timer visual
			if (!ship.getCustomData().containsKey(TIMER_KEY)) {
				SotfRingTimerVisualScript.AuraParams p = new SotfRingTimerVisualScript.AuraParams();
				p.color = Misc.setAlpha(OVERLOAD_COLOR, 200);
				p.ship = ship;
				p.radius = 350f;
				p.renderDarkerCopy = true;
				p.darkerColorAlpha = 0.25f;
				p.followsPlayerShip = true;
				SotfRingTimerVisualScript plugin = new SotfRingTimerVisualScript(p);
				Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
				ship.setCustomData(TIMER_KEY, plugin);
			} else {
				SotfRingTimerVisualScript visual = (SotfRingTimerVisualScript) ship.getCustomData().get(TIMER_KEY);
				visual.p.totalArc = 1 - (timer / TIME_BETWEEN_STRIKES);
			}

			String status = "Disruption incoming - ";
			status += "" + Math.round(timer) + " seconds";
			Global.getCombatEngine().maintainStatusForPlayerShip(TIMER_KEY, "graphics/icons/hullsys/emp_emitter.png",
					"Perfect Storm", status, true);

			float timeMult = Global.getCombatEngine().getTimeMult().getModifiedValue() / ship.getMutableStats().getTimeMult().getModifiedValue();
			timer -= amount * timeMult;
			if (timer < 0) {
				timer = 0;
			}

			// bzzzzzzzzzz
			if (timer <= 3) {
				float prog = (3 - timer) / 3;
				Global.getSoundPlayer().playUILoop("sotf_perfectstorm_loop", 0.75f + (0.5f * prog), 0.5f + (1.25f * prog));

				reaction_timer -= amount * timeMult;

				// ships at risk of being hit by the disruption may try and avoid it by phasing or venting
				// skilled captains are more likely to do so and start backing off earlier
				if (reaction_timer < 0) {
					for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
						if (!otherShip.isAlive() || otherShip.getOwner() == ship.getOwner() ||
								Global.getCombatEngine().getPlayerShip().equals(otherShip) || otherShip.getCustomData().containsKey(REACTED_KEY)) {
							continue;
						}
						PersonAPI captain = otherShip.getCaptain();
						// note: in combat layer, officerless ships do not have a null officer (have a hidden default one instead)
						if (captain == null) {
							continue;
						}
						boolean ai = otherShip.getVariant().hasHullMod(HullMods.AUTOMATED) || captain.isAICore();
						float reactionTime = 0.5f;
						if (otherShip.getCustomData().containsKey(REACTION_SPEED_KEY)) {
							reactionTime = (float) otherShip.getCustomData().get(REACTION_SPEED_KEY);
						} else {
							reactionTime = 0.5f + ((float) Math.random() * 0.25f);
							if (!captain.isDefault()) {
								reactionTime -= captain.getStats().getLevel() * 0.05f;
							}
							if (reactionTime < 0.1f) {
								reactionTime = 0.1f;
							}
							otherShip.setCustomData(REACTION_SPEED_KEY, reactionTime);
						}

						boolean willReact = false;
						if (otherShip.getCustomData().containsKey(WILL_REACT_KEY)) {
							willReact = (boolean) otherShip.getCustomData().get(WILL_REACT_KEY);
						} else {
							float reactionChance = 10f;
							if (captain.isDefault() && ai) {
								reactionChance += 0.3f;
							} else if (ai) {
								// A: 100%, B: 75%, G: 45%
								reactionChance += captain.getStats().getLevel() * 0.15f;
							} else {
								// 1: 12.5%, 7: 87.5%
								reactionChance += captain.getStats().getLevel() * 0.125f;
							}
							willReact = Math.random() < reactionChance;
							otherShip.setCustomData(WILL_REACT_KEY, willReact);
						}

						boolean systemPhase = false;
						if (otherShip.getSystem() != null) {
							if (otherShip.getSystem().getSpecAPI().isPhaseCloak()) {
								systemPhase = true;
							}
						}

						boolean phaseAvailable = false;
						if (otherShip.getPhaseCloak() != null) {
							if (otherShip.getPhaseCloak().getSpecAPI().isPhaseCloak()) {
								phaseAvailable = true;
							}
						} else if (systemPhase) {
							phaseAvailable = true;
						}

						if (willReact && !phaseAvailable) {
							otherShip.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 0.3f);
						}

						if (timer > reactionTime) continue;

						if (willReact) {
							if (otherShip.getPhaseCloak() != null) {
								if (otherShip.getPhaseCloak().getSpecAPI().isPhaseCloak() && !otherShip.isPhased() && ship.getHardFluxLevel() < 0.9f) {
									otherShip.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
								}
								otherShip.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.STAY_PHASED, timer + 0.25f);
							} else if (systemPhase) {
								otherShip.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
								otherShip.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.STAY_PHASED, timer + 0.25f);
							} else if (otherShip.getFluxTracker().getFluxLevel() > 0.25f &&
									otherShip.getMutableStats().getVentRateMult().getModifiedValue() > 0.5f) {
								otherShip.giveCommand(ShipCommand.VENT_FLUX, null, 0);
							}

							otherShip.setCustomData(REACTED_KEY, true);
						} else {
							otherShip.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, timer + 0.25f);
						}
					}
					reaction_timer = 0.2f;
				}
			}

			if (timer > 0) {
				return;
			}

			for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
				if (!otherShip.isAlive() || otherShip.getOwner() == ship.getOwner()) {
					continue;
				}
				otherShip.getCustomData().remove(REACTED_KEY);
				if (otherShip.isPhased()) {
					otherShip.getFluxTracker().showOverloadFloatyIfNeeded("Disruption dodged!", OVERLOAD_COLOR, 4f, true);
					continue;
				}
//				if (otherShip.getFluxTracker().isOverloaded()) {
//					otherShip.getFluxTracker().setOverloadDuration(otherShip.getFluxTracker().getOverloadTimeRemaining() + OVERLOAD_TIME);
//					continue;
//				}

				if (otherShip.getFluxTracker().isVenting()) {
					if (otherShip.getFluxTracker().showFloaty() ||
							otherShip == Global.getCombatEngine().getPlayerShip()) {
						otherShip.getFluxTracker().showOverloadFloatyIfNeeded("Disruption negated!", OVERLOAD_COLOR, 4f, true);
					}
					continue;
				}

				disrupt(otherShip);
				otherShip.getCustomData().remove(REACTED_KEY);
				otherShip.getCustomData().remove(WILL_REACT_KEY);
			}
			Global.getSoundPlayer().playUISound("sotf_perfectstorm_blast", 1f, 1f);
			timer = TIME_BETWEEN_STRIKES;
		}

		protected void disrupt(final ShipAPI target) {
			target.setOverloadColor(OVERLOAD_COLOR);
			if (target.getFluxTracker().isOverloaded()) {
				target.getFluxTracker().setOverloadDuration(target.getFluxTracker().getOverloadTimeRemaining() + OVERLOAD_TIME);
			} else {
				target.getFluxTracker().beginOverloadWithTotalBaseDuration(OVERLOAD_TIME);
			}
			if (target.getFluxTracker().showFloaty() ||
					target == Global.getCombatEngine().getPlayerShip()) {
				target.getFluxTracker().playOverloadSound();
				target.getFluxTracker().showOverloadFloatyIfNeeded("System disruption!", OVERLOAD_COLOR, 4f, true);
			}

			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (!target.getFluxTracker().isOverloadedOrVenting()) {
						target.resetOverloadColor();
						Global.getCombatEngine().removePlugin(this);
					}
				}
			});
		}
	}

}
