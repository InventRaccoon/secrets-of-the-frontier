// PK detonation upon messing with Mayfly
package data.scripts.campaign.plugins.mayfly;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfAskoniaProbeExplosionPlugin implements EveryFrameScript {

    protected boolean done = false;
    protected SectorEntityToken explosion = null;
    protected float delay = 0.5f;
    protected float delay2 = 1f;

    protected SectorEntityToken entity;

    public SotfAskoniaProbeExplosionPlugin(SectorEntityToken entity) {
        this.entity = entity;
        delay = 0.5f; // plus approximately 2 seconds from how long plugin.jitter() takes to build up
    }


    public void advance(float amount) {
        if (done) return;

        delay -= amount;

        if (delay <= 0 && explosion == null) {
            Misc.fadeAndExpire(entity);

            LocationAPI cl = entity.getContainingLocation();
            Vector2f loc = entity.getLocation();
            Vector2f vel = entity.getVelocity();


            float size = entity.getRadius() + 500f;
            Color color = new Color(255, 165, 100);
            //color = new Color(255, 155, 255);
            //ExplosionParams params = new ExplosionParams(color, cl, loc, size, 1f);
            ExplosionEntityPlugin.ExplosionParams params = new ExplosionEntityPlugin.ExplosionParams(color, cl, loc, size, 1f);
            params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.MEDIUM;

            explosion = cl.addCustomEntity(Misc.genUID(), "PK Explosion",
                    Entities.EXPLOSION, Factions.NEUTRAL, params);
            explosion.setLocation(loc.x, loc.y);
        }

        if (explosion != null) {
            delay2 -= amount;
            if (!explosion.isAlive() || delay2 <= 0) {
                done = true;
            }
        }
    }

    public boolean isDone() {
        return done;
    }

    public boolean runWhilePaused() {
        return false;
    }
}
