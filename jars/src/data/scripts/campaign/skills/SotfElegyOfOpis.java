// FURY OF THE FALLEN. On destruction, ship enters an undying frenzy, then hurls itself at an enemy and detonates
package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.skills.SotfHatredBeyondDeath.SotfAnathemaUndyingScript;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE;

public class SotfElegyOfOpis {

    public static int ELEGY_CHARGE_COST = 3;
    public static float ELEGY_HEAL_PERCENT = 0.35f;
    public static float ELEGY_DECAY_PERCENT = 0.0075f;

    public static String ELEGY_DATA_KEY = "sotf_elegyofopis_elegy_key";

    public static class UndyingAnthem extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            ship.addListener(new SotfElegyOfOpisScript(ship));
        }

        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.isStationModule()) return;
            if (ship.isFighter()) return;
            if (ship.getHullSpec().getHullId().contains("higgs")) return;
            ship.removeListenerOfClass(SotfElegyOfOpisScript.class);
            if (Global.getCombatEngine() != null) {
                Global.getCombatEngine().getCustomData().remove(ELEGY_DATA_KEY);
            }
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            //stats.getBreakProb().modifyMult(id, 0f);
        }
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            //stats.getBreakProb().unmodify(id);
        }

        public String getEffectDescription(float level) {
            return null;
        }

        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);

            Color c = hc;
            info.addPara("Gain a charge for each allied ship destroyed",
                    0f, c, c, "" + ELEGY_DECAY_PERCENT * 100f + "%");
            info.addPara("At %s charges, the next allied ship that would be destroyed expends all charges to trigger a parasitic nanite swarm which " +
                            "instantly restores %s of maximum hull, cancels any ongoing overload and repairs all weapons and engines",
                    0f, c, c, "" + ELEGY_CHARGE_COST, "" + (int) (ELEGY_HEAL_PERCENT * 100f) + "%");
            info.addPara("Active nanites consume %s of max hull per second until destruction",
                    0f, c, c, "" + ELEGY_DECAY_PERCENT * 100f + "%");
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class SotfElegyOfOpisScript implements AdvanceableListener {
        protected ShipAPI ship;

        public static class SotfElegyData {
            ShipAPI source;
            int charges = 0;
        }

        public SotfElegyOfOpisScript(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (ship.isFighter() || ship.isStationModule()) return;
            if (!ship.isAlive()) {
                engine.getCustomData().remove(ELEGY_DATA_KEY);
                return;
            }

            SotfElegyData data = (SotfElegyData) engine.getCustomData().get(ELEGY_DATA_KEY);
            if (data == null) {
                data = new SotfElegyData();
                data.source = ship;
                engine.getCustomData().put(ELEGY_DATA_KEY, data);
                engine.addPlugin(new SotfElegyTallyScript());
            }

            if (data.source != ship) return;

            Global.getCombatEngine().maintainStatusForPlayerShip(ELEGY_DATA_KEY, Global.getSettings().getSpriteName("ui", "sotf_handsofthedrowned_deadlywaters"),
                    "Elegy of Opis", "Undying Gift - " + data.charges + "/" + ELEGY_CHARGE_COST + " charges", true);

            for (ShipAPI other : Global.getCombatEngine().getShips()) {
                if (allowedToGainChargeFromDeadShip(other) && data.charges < ELEGY_CHARGE_COST) {
                    data.charges++;
                    other.setCustomData(ELEGY_DATA_KEY + "_counted", true);
                }
                if (shouldAddUndyingScript(other) && !other.hasListenerOfClass(SotfAnathemaUndyingScript.class)) {
                    other.addListener(new SotfAnathemaUndyingScript(other, "Elegy of Opis", ELEGY_HEAL_PERCENT, ELEGY_DECAY_PERCENT, false, true));
                }
            }
        }

        private boolean allowedToGainChargeFromDeadShip(ShipAPI ship) {
            if (ship == this.ship) return false;
            if (!ship.isHulk() || ship.isPiece()) return false;
            if (ship.getOriginalOwner() != 1) return false;
            if (ship.getCustomData().containsKey(ELEGY_DATA_KEY + "_counted")) return false;
            if (ship.isFighter() || ship.isStationModule() || ship.isStation()) return false;
            if (ship.getCollisionClass() == CollisionClass.NONE) return false;
            return true;
        }

        private boolean shouldAddUndyingScript(ShipAPI ship) {
            if (ship == this.ship) return false;
            if (!ship.isAlive()) return false;
            if (ship.getOriginalOwner() != 1) return false;
            if (ship.isFighter() || ship.isStationModule() || ship.isStation()) return false;
            if (ship.getCollisionClass() == CollisionClass.NONE) return false;
            return true;
        }

        public static class SotfElegyTallyScript extends BaseEveryFrameCombatPlugin {

            protected float fadeIn = 0f;
            protected float fadeOut = 1f;
            protected float fadeBounce = 0f;
            protected boolean bounceUp = true;
            private static LazyFont.DrawableString TODRAW14;
            protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_skull_icon");

            public void init(CombatEngineAPI engine) {
                try {
                    LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
                    TODRAW14 = fontdraw.createText();
                    TODRAW14.setBlendSrc(GL_ONE);
                    TODRAW14.setFontSize(28);
                } catch (FontException ignored) {
                }

                iconSprite.setSize(70, 70);
                iconSprite.setColor(Misc.setAlpha(Misc.getNegativeHighlightColor(), 155));
            }

            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (fadeIn <= 1f) fadeIn += amount;
                if (fadeIn > 1f) fadeIn = 1f;

                if (fadeIn >= 1f) {
                    if (bounceUp) {
                        fadeBounce += amount;
                        if (fadeBounce > 1f) {
                            bounceUp = false;
                        }
                    } else {
                        fadeBounce -= amount;
                        if (fadeBounce < 0f) {
                            bounceUp = true;
                        }
                    }
                }
            }

            @Override
            public void renderInWorldCoords(ViewportAPI viewport) {
                CombatEngineAPI engine = Global.getCombatEngine();
                if (engine.getPlayerShip() == null) return;
                if (!engine.isUIShowingHUD()) return;
                ShipAPI player = engine.getPlayerShip();

                SotfElegyData data = (SotfElegyData) engine.getCustomData().get(ELEGY_DATA_KEY);
                if (data == null) {
                    engine.removePlugin(this);
                    return;
                }

                float iconPadding = 80f;

                float xoffset = 0f;
                if (ELEGY_CHARGE_COST % 2 == 0) {
                    xoffset = iconPadding;
                }
                xoffset -= (ELEGY_CHARGE_COST - 1f) / 2 * iconPadding;

                for (int i = 0; i < ELEGY_CHARGE_COST; i++) {
                    float alphaMult = 1f;
                    if (i >= data.charges) {
                        alphaMult = 0.35f;
                    }
                    iconSprite.setAlphaMult(fadeIn * (0.6f + fadeOut * 0.4f) * alphaMult);
                    Vector2f pos = new Vector2f(
                            player.getLocation().x + xoffset + (i * iconPadding),
                            player.getLocation().y + player.getShieldRadiusEvenIfNoShield() + 300 + (iconPadding * 0.35f)
                    );
                    iconSprite.renderAtCenter(pos.x, pos.y);
                }

                LazyFont.DrawableString toUse = TODRAW14;
                if (toUse != null) {
                    float glitchChance = 0.005f;
//                    toUse.setFontSize(20);
                    int alpha = Math.round(255 * fadeIn * fadeOut * (1f - (fadeBounce * 0.3f)));

                    String charging = "ELEGY CHARGING";
                    if (data.charges >= ELEGY_CHARGE_COST) {
                        charging = "ELEGY READY";
                    }
                    String chargeString = charging + ": " + data.charges + " / " + ELEGY_CHARGE_COST;


                    toUse.setText(SotfMisc.glitchify(chargeString, glitchChance));
                    toUse.setAnchor(LazyFont.TextAnchor.CENTER);
                    //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                    Vector2f pos = new Vector2f(
                            player.getLocation().x,
                            player.getLocation().y + 300
                    );
                    toUse.setBaseColor(Misc.setBrightness(Color.BLACK, alpha));
                    toUse.draw(pos.x + 1, pos.y - 1);
                    toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
                    toUse.draw(pos);
                    //toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);
                }
            }

        }
    }
}
