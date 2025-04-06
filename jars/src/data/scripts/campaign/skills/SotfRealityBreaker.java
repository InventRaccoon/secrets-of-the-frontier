// ERROR 404: COMMENT NOT FOUND. Starts a timer, then crashes the game via NPE
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
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;

public class SotfRealityBreaker {

	public static final float TIME_LIMIT = 360f;

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

	public static class SotfRealityBreakerScript implements AdvanceableListener {
		protected ShipAPI ship;
		protected float timer;
		protected boolean showStatus = true;
		public SotfRealityBreakerScript(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (!ship.isAlive() || ship.isFighter()) {
				return;
			}
			// timer goes slower while player is phased: does not go faster if only Fel is phased
			timer += amount * Math.min(Global.getCombatEngine().getTimeMult().getModifiedValue(), 1f);
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
				breakRendering(ship, Color.WHITE, ship.getLocation());
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
			// make sure you can't disarm it by adding a new weapon to the files
			String weaponId = "sotf_realitybreaker";
			if (Global.getSettings().getWeaponSpec(weaponId) != null) {
				weaponId += "" + Misc.random.nextInt(9999);
			}
			// crash the game via NPE
			Global.getCombatEngine().spawnProjectile(ship, null, weaponId, ship.getLocation(), 360f, ship.getVelocity());
		}

		private static LazyFont.DrawableString TODRAW14;
		private static LazyFont.DrawableString TODRAW10;
		private static final float UIscaling = Global.getSettings().getScreenScaleMult();

		static {
			try {
				LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
				TODRAW14 = fontdraw.createText();

				fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
				TODRAW10 = fontdraw.createText();

			} catch (FontException ignored) {
			}
		}

		/**
		 * This was supposed to render ASB text but it ended up just breaking the game's rendering
		 * ... so, uh, don't copy this code. It doesn't work. That's the point.
		 */

		private static void breakRendering(ShipAPI ship, Color textColor, Vector2f screenPos) {
			Color borderCol = textColor == null ? MagicUI.GREENCOLOR : textColor;
			if (!ship.isAlive()) {
				borderCol = MagicUI.BLUCOLOR;
			}
			float alpha = 1f;
			Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
					1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
			Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
					alpha * (borderCol.getAlpha() / 255f)
							* (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));

			final Vector2f boxLoc = new Vector2f(screenPos.getX(), screenPos.getY());
			final Vector2f shadowLoc = new Vector2f(screenPos.getX() + 1f, screenPos.getY() - 1f);
			if (UIscaling !=1 ) {
				boxLoc.scale(UIscaling);
				shadowLoc.scale(UIscaling);
				TODRAW10.setFontSize(10*UIscaling);
			}

			// Global.getCombatEngine().getViewport().
			openGL11ForText();
			// TODRAW10.setText(text);
			// TODRAW10.setMaxHeight(26);
			TODRAW10.setText("ERROR");
			TODRAW10.setBaseColor(shadowcolor);
			TODRAW10.draw(shadowLoc);
			TODRAW10.setBaseColor(color);
			TODRAW10.draw(boxLoc);
			closeGL11ForText();
		}

		private static void openGL11ForText() {
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glPushMatrix();
			GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
			GL11.glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}

		private static void closeGL11ForText() {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}
	}

}
