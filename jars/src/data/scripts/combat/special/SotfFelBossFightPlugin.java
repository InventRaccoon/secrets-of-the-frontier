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
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ONE;

/**
 *	BOUND BY VENGEANCE, BOUND TO SUFFER TOGETHER
 */

public class SotfFelBossFightPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;

    protected float fadeIn = 0f;
    protected float fadeOut = 1f;
    protected float fadeBounce = 0f;
    protected boolean bounceUp = true;

    protected float expireFade = 1f;
    protected boolean expiring = false;

    protected SpriteAPI iconSprite = Global.getSettings().getSprite("ui", "sotf_fel_pointer");

    private static LazyFont.DrawableString TODRAW14;
    private static LazyFont.DrawableString TODRAW10;

    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;


    }

    public void renderInUICoords(ViewportAPI viewport) {
    }
}