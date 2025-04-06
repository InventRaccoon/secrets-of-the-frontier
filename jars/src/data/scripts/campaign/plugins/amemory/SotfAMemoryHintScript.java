// A Memory: spawn sensor ghosts that guide the player
package data.scripts.campaign.plugins.amemory;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.GBDartAround;
import com.fs.starfarer.api.impl.campaign.ghosts.GBLeadPlayerTo;
import com.fs.starfarer.api.util.Misc;
import data.scripts.dialog.SotfAMemoryHintPlugin;
import data.scripts.utils.SotfMisc;

import java.util.ArrayList;
import java.util.List;

public class SotfAMemoryHintScript implements EveryFrameScript {

    private float ghostCounter = 0;
    private float dancerCheckCounter = 0;
    private float sierraCounter = 0;
    private static float MAX_DIST_FROM_TIA = 6000f;
    private List<BaseSensorGhost> dancingGhosts = new ArrayList<>();

    public void advance(float amount) {
        if (!Global.getCurrentState().equals(GameState.CAMPAIGN) || Global.getSector().getPlayerFleet() == null) {
            return;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatStarted") || Global.getSector().getMemoryWithoutUpdate().contains("sotf_AMemoryFoundAthena")) {
            cleanUpGhosts();
            Global.getSector().removeScript(this);
            return;
        }
        StarSystemAPI tia = Global.getSector().getStarSystem("tia");
        if (tia == null && Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AthenaWreck")) {
            if (Global.getSector().getMemoryWithoutUpdate().get("$sotf_AthenaWreck") == null) return;
            SectorEntityToken athena = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get("$sotf_AthenaWreck");
            if (athena != null) {
                tia = athena.getStarSystem();
            }
        }
        if (!Global.getSector().getPlayerFleet().getContainingLocation().isHyperspace() || tia == null || !SotfMisc.playerHasSierra()) {
            return;
        }
        ghostCounter += Global.getSector().getClock().convertToDays(amount);
        dancerCheckCounter += Global.getSector().getClock().convertToDays(amount);
        float dist_from_tia = Misc.getDistance(Global.getSector().getPlayerFleet().getLocation(), tia.getLocation());
        // spawn dancing ghosts around Tia if the player is nearby
        if (dancerCheckCounter >= 1f) {
            if (Global.getSector().getHyperspace().getEntitiesWithTag("sotf_AMDancingGhost").isEmpty() && dist_from_tia < 8000) {
                spawnDancingGhosts(tia);
            }
            dancerCheckCounter = 0f;
        }
        // spawn a guiding ghost if they're some distance from Tia
        if (ghostCounter >= 8f && dist_from_tia >= 6000 && dist_from_tia <= 20000) {
            spawnLeadingGhost(tia);
            ghostCounter = 0f;
        }
        // eventually Sierra just mentions it herself
        if (dist_from_tia <= 2400 && !Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMemorySierraChat")) {
            sierraCounter += amount;
            if (sierraCounter >= 6f) {
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_AMemorySierraChat", true);
                Global.getSector().getCampaignUI().showInteractionDialog(new SotfAMemoryHintPlugin(), null);
            }
        }
        // suppress music when near Tia
        float vol = 1f - (Misc.getDistance(Global.getSector().getPlayerFleet().getLocation(), tia.getLocation()) / MAX_DIST_FROM_TIA);
        if (vol > 0) {
            Global.getSector().getCampaignUI().suppressMusic(vol);
        }
    }

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return false;
    }

    // Guidance ghosts that spawn near the player and insist on leading them to Tia'Taxet
    private void spawnLeadingGhost(StarSystemAPI tia) {
        BaseSensorGhost g = new BaseSensorGhost(null, 0);
        g.initEntity(g.genHugeSensorProfile(), g.genLargeRadius(), 0, Global.getSector().getHyperspace());
        g.addBehavior(new GBLeadPlayerTo(20f, tia.getHyperspaceAnchor(), 800f, 12));
        g.setDespawnRange(0f);
        g.placeNearPlayer(800f, 1000f);
        g.getEntity().addScript(new SotfLeadingGhostScript(g.getEntity()));
        Global.getSector().getHyperspace().addScript(g);
        g.getEntity().addFloatingText("!!!", SotfMisc.getEidolonColor(), 2f, true);
        Global.getSector().addPing(g.getEntity(), "sotf_ping_sensorghost", SotfMisc.getEidolonColor());
    }

    // Dancing ghosts that dart around Tia'Taxet in hyperspace
    private void spawnDancingGhosts(StarSystemAPI tia) {
        cleanUpGhosts();
        for (int i = 0; i < 12; i++) {
            BaseSensorGhost g = new BaseSensorGhost(null, 0);
            g.initEntity(g.genHugeSensorProfile(), g.genSmallRadius(), 0, Global.getSector().getHyperspace());
            g.addBehavior(new GBDartAround(tia.getHyperspaceAnchor(), 30f, 16 + Misc.random.nextInt(4), 800, 3200));
            g.setDespawnRange(0f);
            g.getEntity().addTag("sotf_AMDancingGhost");
            g.setLoc(Misc.getPointAtRadius(tia.getLocation(), 800f));
            //g.placeNearEntity(tia.getHyperspaceAnchor(), 800, 3200);
            Global.getSector().getHyperspace().addScript(g);
        }
    }

    private void cleanUpGhosts() {
        for (SectorEntityToken ghost : Global.getSector().getHyperspace().getEntitiesWithTag("sotf_AMDancingGhost")) {
            Misc.fadeAndExpire(ghost, 1f);
        }
    }

}