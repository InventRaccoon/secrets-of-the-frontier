// Fighter gains an officer with a few skills
package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class SotfElitePilots extends BaseHullMod {

    //private static final String ELITE_PILOT_KEY = "$sotf_elitePilot";

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive()) return;
        if (ship.getCaptain() == null) {
            FactionAPI league = null;
            PersonAPI pilot = null;
            if (Global.getCombatEngine().isMission()) {
                league = Global.getSettings().createBaseFaction(Factions.PERSEAN);
            } else {
                league = Global.getSector().getFaction(Factions.PERSEAN);
            }
            pilot = league.createRandomPerson();
            pilot.setRankId(Ranks.PILOT);
            pilot.setPostId(Ranks.POST_SPACER);
            pilot.getStats().setLevel(3);
            pilot.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            pilot.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 1);
            pilot.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
            // fun fact: portrait = captain appears on fighter's status UI
            pilot.setPortraitSprite("graphics/portraits/portrait_mercenary08.png");
            ship.setCaptain(pilot);
        }
    }
}