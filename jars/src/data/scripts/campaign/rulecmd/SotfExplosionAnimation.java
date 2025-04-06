// creates an explosion effect on the target, similar to when you bombard a planet
package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class SotfExplosionAnimation extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        entity = dialog.getInteractionTarget();
        addBombardVisual(entity);
        return true;
    }

    public static void addBombardVisual(SectorEntityToken target) {
        if (target != null && target.isInCurrentLocation()) {
            int num = (int) (target.getRadius() * target.getRadius() / 300f);
            num *= 2;
            if (num > 150) num = 150;
            if (num < 10) num = 4;
            target.addScript(new BombardmentAnimation(num, target));
        }
    }

    public static class BombardmentAnimation implements EveryFrameScript {
        public BombardmentAnimation(int num, SectorEntityToken target) {
            this.num = num;
            this.target = target;
        }
        int num = 0;
        SectorEntityToken target;
        int added = 0;
        float elapsed = 0;
        public boolean runWhilePaused() {
            return false;
        }
        public boolean isDone() {
            return added >= num;
        }
        public void advance(float amount) {
            elapsed += amount * (float) Math.random();
            if (elapsed < 0.03f) return;

            elapsed = 0f;

            int curr = (int) Math.round(Math.random() * 4);
            if (curr < 1) curr = 0;

            Color color = new Color(255, 165, 100, 255);

            Vector2f vel = new Vector2f();

            if (target.getOrbit() != null &&
                    target.getCircularOrbitRadius() > 0 &&
                    target.getCircularOrbitPeriod() > 0 &&
                    target.getOrbitFocus() != null) {
                float circumference = 2f * (float) Math.PI * target.getCircularOrbitRadius();
                float speed = circumference / target.getCircularOrbitPeriod();

                float dir = Misc.getAngleInDegrees(target.getLocation(), target.getOrbitFocus().getLocation()) + 90f;
                vel = Misc.getUnitVectorAtDegreeAngle(dir);
                vel.scale(speed / Global.getSector().getClock().getSecondsPerDay());
            }

            for (int i = 0; i < curr; i++) {
                float glowSize = 50f + 50f * (float) Math.random();
                float angle = (float) Math.random() * 360f;
                float dist = (float) Math.sqrt(Math.random()) * target.getRadius();

                float factor = 0.5f + 0.5f * (1f - (float)Math.sqrt(dist / target.getRadius()));;
                glowSize *= factor;
                Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
                loc.scale(dist);
                Vector2f.add(loc, target.getLocation(), loc);

                Color c2 = Misc.scaleColor(color, factor);
                //c2 = color;
                Misc.addHitGlow(target.getContainingLocation(), loc, vel, glowSize, c2);
                added++;

                if (i == 0) {
                    dist = Misc.getDistance(loc, Global.getSector().getPlayerFleet().getLocation());
                    if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
                        float volumeMult = 1f - (dist / HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE);
                        volumeMult = (float) Math.sqrt(volumeMult);
                        volumeMult *= 0.1f * factor;
                        if (volumeMult > 0) {
                            Global.getSoundPlayer().playSound("mine_explosion", 1f, 1f * volumeMult, loc, Misc.ZERO);
                        }
                    }
                }
            }
        }
    }
}
