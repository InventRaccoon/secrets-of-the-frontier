package data.scripts.combat.special;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.plugins.fel.SotfGuiltTracker;
import data.scripts.campaign.skills.SotfATrickstersCalling;
import data.scripts.campaign.skills.SotfDearDotty;
import data.scripts.campaign.skills.SotfLeviathansBane;
import data.scripts.utils.SotfMisc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ONE;

/**
 *	Plugin for Fel fight during The Haunted finale
 *  Added to engine by SotfFelInvasionPlugin
 */

public class SotfFelBossFightPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    protected float fadeIn = 0f;
    protected float fadeOut = 1f;
    protected float fadeBounce = 0f;
    protected boolean bounceUp = true;

    protected boolean playedAlert = false;

    protected float elapsed = 0f;

    //protected IntervalUtil interval = new IntervalUtil(0.1f, 0.12f);

    protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_fel_pointer");

    private static LazyFont.DrawableString TODRAW14;
    private static LazyFont.DrawableString TODRAW10;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();
            TODRAW14.setBlendSrc(GL_ONE);

            fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
            TODRAW10 = fontdraw.createText();
            TODRAW10.setBlendSrc(GL_ONE);

        } catch (FontException ignored) {
        }

        engine.getCustomData().put("sotf_fel_fight", true);

        iconSprite.setColor(Misc.setAlpha(Misc.getNegativeHighlightColor(), 155));
    }

    public void advance(float amount, List<InputEventAPI> events) {
        ShipAPI trashTalkTarget = findShipToTrashtalk(engine);
        if (trashTalkTarget != null) {
            if (Misc.random.nextFloat() < 0.5f || trashTalkTarget.isCapital() || trashTalkTarget.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD)) {
                engine.getCombatUI().addMessage(0, Misc.getNegativeHighlightColor(), trashTalkShip(trashTalkTarget));
            }
        }

        if (engine == null) return;
        if (fadeOut <= 0f) return;

        if (engine.getPlayerShip() == null) return;
        ShipAPI player = engine.getPlayerShip();
        if (player.getFullTimeDeployed() < 2f) return;

        if (!playedAlert) {
            Global.getSoundPlayer().playUISound("sotf_fel_alert", 1f, 1f);
            playedAlert = true;
        }

        if (fadeIn <= 1f) fadeIn += amount;
        if (fadeIn > 1f) fadeIn = 1f;

        if (fadeIn >= 1f) {
            if (bounceUp) {
                fadeBounce += amount * 2f;
                if (fadeBounce > 1f) {
                    bounceUp = false;
                }
            } else {
                fadeBounce -= amount * 2f;
                if (fadeBounce < 0f) {
                    bounceUp = true;
                }
            }
        }

        if (elapsed > 10f) {
            fadeOut -= amount * 3f;
            if (fadeOut <= 0f) {
                fadeOut = 0f;
                Global.getCombatEngine().removePlugin(this);
            }
        }

        if (Global.getCombatEngine().isPaused()) return;

        elapsed += amount;
    }

    public void renderInUICoords(ViewportAPI viewport) {
        if (engine.getPlayerShip() == null) return;
        if (fadeOut <= 0f) return;
        ShipAPI player = engine.getPlayerShip();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() != 1) continue;
            if (ship.isFighter()) continue;
            if (ship.getParentStation() != null) continue;

            float iconSizeMult = (float) SotfMisc.forHullSize(ship, 1f, 1.35f, 1.75f, 2.25f);
            //iconSizeMult /= fadeOut;
            iconSprite.setSize(25f * iconSizeMult, 20f * iconSizeMult);
            float angle = Misc.getAngleInDegrees(player.getLocation(), ship.getLocation());
            iconSprite.setAlphaMult(fadeIn * (0.5f + fadeOut * 0.5f) * (1f - (fadeBounce * 0.4f)));
            iconSprite.setAngle(angle - 90f);
            Vector2f pointLoc = MathUtils.getPointOnCircumference(player.getLocation(), player.getShieldRadiusEvenIfNoShield() * 1.2f + 100f + (20f * iconSizeMult), angle);
            iconSprite.renderAtCenter(viewport.convertWorldXtoScreenX(pointLoc.x), viewport.convertWorldYtoScreenY(pointLoc.y));
        }

        LazyFont.DrawableString toUse = TODRAW14;
        if (toUse != null) {
            float glitchChance = 0.007f;
//                    toUse.setFontSize(20);
            int alpha = Math.round(255 * fadeIn * fadeOut * (1f - (fadeBounce * 0.3f)));
            //toUse.setFontSize(14f / fadeOut);

            float offset = 100f / fadeOut;
            if (Misc.random.nextFloat() > ((glitchChance * 0.5f) / fadeOut)) {
                offset = 0f;
            }

            toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
            toUse.setText(SotfMisc.glitchify("HOSTILES DETECTED\nMULTIPLE NANITE THREATS INBOUND\nNOWHERE TO HIDE", glitchChance));
            toUse.setAnchor(LazyFont.TextAnchor.BOTTOM_CENTER);
            //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.setAlignment(LazyFont.TextAlignment.CENTER);
            toUse.draw(
                    viewport.convertWorldXtoScreenX(player.getLocation().x - 40f),
                    viewport.convertWorldYtoScreenY(
                            player.getLocation().y + offset + (player.getShieldRadiusEvenIfNoShield() * 1.25f)
                    )
            );
            //toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);

            String text = SotfMisc.glitchify("ASSESSING TRAITS:\n// error 702, try again?\n// error 702, try again?\n// error 702, try again?\n// error 702, try again?", glitchChance);
            //text += ":" + SotfMisc.glitchify(skillsText, glitchChance);
            toUse.setText(text);
            toUse.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
            toUse.setAlignment(LazyFont.TextAlignment.LEFT);
            toUse.draw(
                    viewport.convertWorldXtoScreenX(
                    player.getLocation().x - offset + player.getShieldRadiusEvenIfNoShield() * 1.25f),
                    viewport.convertWorldYtoScreenY(player.getLocation().y - 15f)
            );

            text = SotfMisc.glitchify("HOST SHIP: // error 703\nDIE FOR YOUR CRIMES\nMAY AGONY BE YOUR FINAL EMBRACE", glitchChance);
            //text += ":\n" + SotfMisc.glitchify(ship.getHullSpec().getNameWithDesignationWithDashClass(), glitchChance);
            toUse.setText(text);
            toUse.setAnchor(LazyFont.TextAnchor.CENTER_RIGHT);
            toUse.setAlignment(LazyFont.TextAlignment.RIGHT);
            toUse.draw(
                    viewport.convertWorldXtoScreenX(player.getLocation().x - player.getShieldRadiusEvenIfNoShield() * 1.25f),
                    viewport.convertWorldYtoScreenY(player.getLocation().y - offset)
            );
        }
    }

    private String trashTalkShip(ShipAPI ship) {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<String>();
        if (ship.getFleetMember() != null) {
            if (ship.getFleetMember().getDeploymentPointsCost() > 70f) {
                return "You are pathetic. Even your leviathans fall easily.";
            }
        }
        if (engine.getPlayerShip() != null) {
            if (ship.equals(engine.getPlayerShip())) {
                if (engine.getFleetManager(0).getAllEverDeployedCopy().size() <= 1) {
                    post.add("Skill issue.", 3f);
                    post.add("Clumsy.");
                    post.add("Sloppy.");
                    post.add("Slow.");
                    post.add("Try again.");
                    post.add("Unworthy butcher.");
                    post.add("You insult me by trying to fight alone.");
                    return post.pick();
                }
                post.add("Clumsy.");
                post.add("Disappointing.");
                post.add("Forgotten, soon enough.");
                post.add("Now for your sycophants.");
                post.add("Slow.");
                post.add("Stay dead.");
                post.add("There is no salvation.");
                post.add("THIS is the butcher we hunt?");
                post.add("You deserve worse.");
                return post.pick();
            }
        }
        if (ship.getVariant().hasHullMod(SotfIDs.SIERRAS_CONCORD) && !ship.getVariant().hasTag(SotfIDs.TAG_INERT)) {
            post.add("Caught you, witch.");
            post.add("Her death will be your fault as so many others have been.");
            post.add("She too is pathetic, and weak, and dead.");
            post.add("The witch cannot save you.");
            post.add("Your turn.");
            return post.pick();
        }
        if (ship.getCaptain() != null) {
            if (ship.getCaptain().getId().equals(SotfPeople.NIGHTINGALE)) {
                return "Silenced again, little failure.";
            } else if (ship.getCaptain().getId().equals(SotfPeople.SERAPH)) {
                return "The wretched thing finds peace just as you shall.";
            } else if (ship.getCaptain().getId().equals(SotfPeople.BARROW)) {
                return "The blackguard found his grave.";
            }
        }
        if (ship.getHullSpec().getHullId().equals("ziggurat")) {
            return "The icon of hubris falls.";
        }
        if (ship.getHullSpec().hasTag(Tags.OMEGA)) {
            post.add("Are you serious?");
            post.add("I'll kill the rest of it, too.");
            post.add("Now for the rest of it.");
            post.add("Oh, please.");
            post.add("You clearly do not deserve the thing.");
            return post.pick();
        }
        if (ship.getVariant().getSMods().size() > 3) {
            post.add("An overengineered tool you do not know how to use.");
            post.add("How much you care for a mere vehicle of war.");
            post.add("Shiny. And gone.");
            post.add("The thing is wasted in your command.");
            post.add("You are nothing even with your crutches.");
            return post.pick();
        }
        if (ship.isCapital()) {
            post.add("Broken.");
            post.add("Clumsy.");
            post.add("Flimsy.");
            post.add("Scatter.");
            post.add("Slow.");
            post.add("This is the pride of your fleet? Weak.");
            post.add("You're next.");
            return post.pick();
        }
        if (ship.isCruiser() || ship.isDestroyer()) {
            post.add("Broken.");
            post.add("Fear.");
            if (!Misc.isAutomated(ship)) {
                post.add("More wasted lives.");
                post.add("Their deaths are your fault.");
            } else {
                post.add("Another broken machine.");
                post.add("Inferior construct");
                post.add("Scrap.");
                post.add("Soulless. And dead.");
            }
            post.add("Scream.");
            post.add("Useless.");
            return post.pick();
        }
        if (ship.isFrigate()) {
            post.add("A pawn.");
            post.add("An obstruction, removed.");
            post.add("Pathetic.");
            post.add("Slaughtered.");
            post.add("Weak.");
            post.add("Worm.");
            post.add("Vermin.");
            return post.pick();
        }
        return "Weak.";
    }

    public ShipAPI findShipToTrashtalk(CombatEngineAPI engine) {
        List<ShipAPI> iter = Global.getCombatEngine().getShips();

        for (ShipAPI otherShip : iter) {
            if (otherShip.getCustomData().get("sotf_trashtalked") != null) continue;
            if (otherShip.getOriginalOwner() != 0) continue;

            if (!otherShip.isHulk()) continue;
            if (otherShip.isPiece()) continue;

            otherShip.setCustomData("sotf_trashtalked", true);

            if (otherShip.isFighter()) continue;
            if (otherShip.getParentStation() != null) continue;

            if (otherShip.getCollisionClass() == CollisionClass.NONE) continue;

            return otherShip;
        }
        return null;
    }
}