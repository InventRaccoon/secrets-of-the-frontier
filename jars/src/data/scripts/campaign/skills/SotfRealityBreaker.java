// ERROR 404: COMMENT NOT FOUND. Starts a timer, then crashes the game via NPE
package data.scripts.campaign.skills;

import com.fs.starfarer.api.EveryFrameScript;
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
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;

import static data.scripts.utils.SotfMisc.getAllowedRealityBreaker;

public class SotfRealityBreaker {

	public static final float TIME_LIMIT = 300f;

	public static class LevelMinus1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return; // Keel gets all skills
			ship.addListener(new SotfRealityBreakerScript(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship.isStationModule()) return;
			if (ship.isFighter()) return;
			if (ship.getHullSpec().getHullId().contains("higgs")) return; // Keel gets all skills
			ship.removeListenerOfClass(SotfRealityBreakerScript.class);
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
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class SotfRealityBreakerScript extends BaseEveryFrameCombatPlugin implements AdvanceableListener {
		protected ShipAPI ship;
		protected float timer;
		protected boolean showStatus = true;
		private static LazyFont.DrawableString TODRAW14;
		public SotfRealityBreakerScript(ShipAPI ship) {
			this.ship = ship;

			try {
				LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
				TODRAW14 = fontdraw.createText();
				TODRAW14.setFontSize(28);
			} catch (FontException ignored) {
			}

			Global.getCombatEngine().addPlugin(this);
		}

		public void advance(float amount) {
			if (!ship.isAlive()) {
				Global.getCombatEngine().removePlugin(this);
				return;
			}
			timer += amount / ship.getMutableStats().getTimeMult().getModifiedValue();
			float left = TIME_LIMIT - timer;

			String status = "Detected spacial destabilization";
			String subtext = "Eliminate source within " + Math.round(left) + " seconds";

			if (left < (TIME_LIMIT * 0.65f)) {
				status = "Reality breaker arming";
			}

			if (left < (TIME_LIMIT * 0.35f)) {
				status = "Reality breaker almost ready";
				showStatus = !showStatus;
			}

			if (left < 5f) {
				status = "Reality breaker deploying";
				subtext = "Too late";
				showStatus = true;
				ship.setJitter(this, Color.WHITE, 1 - (left / 5), 500, 200);
			}

			if (left < 1f) {
				status = "title";
				subtext = "data";
			}

			if (showStatus) {
				Global.getCombatEngine().maintainStatusForPlayerShip("sotf_realitybreaker" + ship.getId(), null, status, subtext, true);
			}
			if (timer < TIME_LIMIT) {
				return;
			}

			if (getAllowedRealityBreaker()) {
				// make sure you can't disarm it by adding a new weapon to the files
				String weaponId = "sotf_realitybreaker";
				if (Global.getSettings().getWeaponSpec(weaponId) != null) {
					weaponId += "" + Misc.random.nextInt(9999);
				}
				// crash the game via NullPointerException
				Global.getCombatEngine().spawnProjectile(ship, null, weaponId, ship.getLocation(), 360f, ship.getVelocity());
			} else {
				Global.getCombatEngine().endCombat(0f);
				Global.getSector().addScript(new EveryFrameScript() {
					@Override
					public boolean isDone() {
						return false;
					}

					@Override
					public boolean runWhilePaused() {
						return true;
					}

					@Override
					public void advance(float amount) {
						if (Global.getSector().getCampaignUI().isShowingDialog()) {
							Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
						}
						Global.getSector().getCampaignUI().cmdExitWithoutSaving();
					}
				});
			}
		}

		@Override
		public void renderInWorldCoords(ViewportAPI viewport) {
			CombatEngineAPI engine = Global.getCombatEngine();
			if (engine.getPlayerShip() == null) return;
			if (!engine.isUIShowingHUD()) return;
			if (!ship.isAlive()) return;
			if (timer == 0) return;
			ShipAPI player = engine.getPlayerShip();

			LazyFont.DrawableString toUse = TODRAW14;
			if (toUse != null) {
				float glitchChance = 0.005f + (0.04f * (timer / TIME_LIMIT));
				float left = TIME_LIMIT - timer;

				String timeLeft = "" + Math.round(left);
				String status = "REALITY_BREAKER";

//				if (left < (TIME_LIMIT * 0.65f)) {
//					status = "Reality breaker arming";
//				}
//				if (left < (TIME_LIMIT * 0.35f)) {
//					status = "Reality breaker almost ready";
//				}
				if (left < 5f) {
					//status = "Reality breaker deploying";
					timeLeft = "TOO_LATE";
				}
				if (left < 1f) {
					status = "ERROR";
				}

				String timerString = status + " - " + timeLeft;

				toUse.setText(SotfMisc.glitchify(timerString, glitchChance));
				toUse.setAnchor(LazyFont.TextAnchor.CENTER);
				//toUse.setAlignment(LazyFont.TextAlignment.CENTER);
				toUse.setAlignment(LazyFont.TextAlignment.CENTER);
				Vector2f pos = new Vector2f(
						player.getLocation().x - 28,
						player.getLocation().y + 350
				);
				toUse.setBaseColor(Color.BLACK);
				toUse.draw(pos.x + 1, pos.y - 1);
				toUse.setBaseColor(Color.WHITE);
				toUse.draw(pos);
				//toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);
			}
		}
	}

}
