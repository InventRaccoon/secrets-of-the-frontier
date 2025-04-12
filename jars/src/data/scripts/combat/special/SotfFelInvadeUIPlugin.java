// draws a big circle around a ship, very useful for AoE auras and the like
package data.scripts.combat.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.JitterUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.combat.SotfNeutrinoLockVisualScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

import static org.lwjgl.opengl.GL11.GL_ONE;

public class SotfFelInvadeUIPlugin extends BaseCombatLayeredRenderingPlugin {


    protected SpriteAPI atmosphereTex;
    protected ShipAPI ship;
    protected int segments;
    protected float fadeIn = 0f;
    protected float fadeOut = 1f;
    protected float fadeBounce = 0f;
    protected float progress = 0f;

    protected boolean bounceUp = true;
    protected SpriteAPI expiringSprite = Global.getSettings().getSprite("ui", "sotf_mimic_expiring");
    protected SpriteAPI vigorSprite = Global.getSettings().getSprite("ui", "sotf_mimic_vigor");

    protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_fel_pointer");

    protected JitterUtil jitter = new JitterUtil();
    protected float prop = 1f;

    private static LazyFont.DrawableString TODRAW14;
    private static LazyFont.DrawableString TODRAW10;

    public String threatDetected;
    public String skillsText;

    public SotfFelInvadeUIPlugin(ShipAPI ship, String warningString) {
        this.ship = ship;
        this.threatDetected = warningString;

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();
            TODRAW14.setBlendSrc(GL_ONE);

            fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
            TODRAW10 = fontdraw.createText();
            TODRAW10.setBlendSrc(GL_ONE);

        } catch (FontException ignored) {
        }

        jitter.setUseCircularJitter(true);
        jitter.setSetSeedOnRender(false);
        iconSprite.setSize(40, 35f);
        iconSprite.setColor(Misc.setAlpha(Misc.getNegativeHighlightColor(), 155));

        PersonAPI fel = SotfPeople.getPerson(SotfPeople.FEL);
        skillsText = "";

        if (fel.getStats().hasSkill(SotfIDs.SKILL_HATREDBEYONDDEATH)) {
            skillsText += "\nOFF - HATRED BEYOND DEATH - UNDYING RAGE";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_LEVIATHANSBANE)) {
            skillsText += "\nOFF - LEVIATHAN'S BANE - ANTI-CAPITAL CANNON";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_JUBILANTSIREN)) {
            skillsText += "\nOFF - JUBILANT TECH-SIREN - SHIP/FIGHTER HACKER";
        }

        if (fel.getStats().hasSkill(SotfIDs.SKILL_ATRICKSTERSCALLING)) {
            skillsText += "\nHYB - A TRICKSTER'S CALLING - MISSILE THIEF";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_DEARDOTTY)) {
            skillsText += "\nHYB - DEAR DOTTY - ESCORT FIGMENT";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_GROVETENDER)) {
            skillsText += "\nHYB - WISPERING GROVETENDER - WISP SUMMONER";
        }

        if (fel.getStats().hasSkill(SotfIDs.SKILL_HELLIONSHELLHIDE)) {
            skillsText += "\nDEF - HELLION'S HELLHIDE - SKINSHIELD";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_INSACRIFICEMEANING)) {
            skillsText += "\nDEF - IN SACRIFICE, MEANING - FLUX TRANSFER";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_MANTLEOFTHORNS)) {
            skillsText += "\nDEF - MANTLE OF THORNS - VENGEFUL SHIELDS";
        }

        if (fel.getStats().hasSkill(SotfIDs.SKILL_ELEGYOFOPIS)) {
            skillsText += "\nSUP - ELEGY OF OPIS - UNDYING AURA";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_HANDSOFTHEDROWNED)) {
            skillsText += "\nSUP - HANDS OF THE DROWNED - GRAVITIC MINES";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_REALITYBREAKER)) {
            skillsText += "\nERROR: description(\"reality_breaker\") not found";
        }
        if (fel.getStats().hasSkill(SotfIDs.SKILL_PERFECTSTORM)) {
            skillsText += "\nSUP - THE PERFECT STORM - PERIODIC OVERLOAD";
        }

    }

    public float getRenderRadius() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getPlayerShip() == null) return 4000f;
        ShipAPI player = engine.getPlayerShip();
        return player.getShieldRadiusEvenIfNoShield() + 100f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null) return;
        if (engine.getPlayerShip() == null) return;
        ShipAPI player = engine.getPlayerShip();

        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            float angle = Misc.getAngleInDegrees(player.getLocation(), ship.getLocation());
            iconSprite.setAlphaMult(fadeIn);
            iconSprite.setAngle(angle - 90f);
            Vector2f pointLoc = MathUtils.getPointOnCircumference(player.getLocation(), player.getShieldRadiusEvenIfNoShield() * 1.2f + 100f, angle);
            iconSprite.renderAtCenter(pointLoc.x, pointLoc.y);

            LazyFont.DrawableString toUse = TODRAW10;
            if (toUse != null) {
                float glitchChance = 0.005f;
                toUse.setFontSize(20);
                int alpha = Math.round(255 * fadeIn * fadeOut * (1f - (fadeBounce * 0.3f)));

                toUse.setBaseColor(Misc.setBrightness(Misc.getNegativeHighlightColor(), alpha));
                toUse.setText(glitchify(threatDetected, glitchChance));
                toUse.setAnchor(LazyFont.TextAnchor.CENTER);
                //toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                toUse.setAlignment(LazyFont.TextAlignment.CENTER);
                toUse.draw(player.getLocation().x, player.getLocation().y + player.getShieldRadiusEvenIfNoShield() * 1.25f);
                //toUse.draw(loc.x, loc.y - shieldRadius * 0.6f);

                String text = glitchify("ASSESSING TRAITS", glitchChance);
                text += ":" + skillsText;
                toUse.setText(text);
                toUse.setAnchor(LazyFont.TextAnchor.CENTER_LEFT);
                toUse.setAlignment(LazyFont.TextAlignment.LEFT);
                toUse.draw(player.getLocation().x + player.getShieldRadiusEvenIfNoShield() * 1.25f, player.getLocation().y);

                text = glitchify("HOST SHIP", glitchChance);
                text += ":\n" + ship.getHullSpec().getNameWithDesignationWithDashClass();
                toUse.setText(text);
                toUse.setAnchor(LazyFont.TextAnchor.CENTER_RIGHT);
                toUse.setAlignment(LazyFont.TextAlignment.RIGHT);
                toUse.draw(player.getLocation().x - player.getShieldRadiusEvenIfNoShield() * 1.25f, player.getLocation().y);
            }
        }
    }

    public String glitchify(String string, float glitchChance) {
        StringBuilder text = new StringBuilder();
        for (char character : string.toCharArray()) {
            if (character != ' ' && Misc.random.nextFloat() < glitchChance) {
                text.append("#");
            } else {
                text.append(character);
            }
        }
        return text.toString();
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);

//        atmosphereTex = Global.getSettings().getSprite("combat", "corona_soft");
//
//        float perSegment = 2f;
//        segments = (int) ((ship.getShieldRadiusEvenIfNoShield() * 2f * 3.14f) / perSegment);
//        if (segments < 8) segments = 8;
    }

    public void advance(float amount) {
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

        if (progress > 10f) {
            fadeOut -= amount;
            if (fadeOut < 0f) {
                fadeOut = 0f;
            }
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getPlayerShip() == null) return;
        ShipAPI player = engine.getPlayerShip();

        progress += amount;

        entity.getLocation().set(player.getLocation().x, player.getLocation().y);
    }

    public boolean isExpired() {
        if (ship == null) return true;
        return !ship.isAlive() || fadeOut <= 0f;
    }

}
