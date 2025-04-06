package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.impl.campaign.skills.FieldModulation;
import com.fs.starfarer.api.impl.hullmods.ShieldShunt;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SotfModPlugin;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.utils.SotfMisc;
import data.shipsystems.scripts.SotfConcordShiftStats;
import org.lazywizard.lazylib.combat.DefenseUtils;

import java.awt.*;

public class SotfLifedrinker extends SotfBaseConcordAugment {

    public static float MINIMUM_ARMOR_BONUS = 0.05f;
    public static float ARMOR_BONUS = 30f;
    public static float REFLECTION_MULT = 0.5f;
    public static float REPAIR_PER_SECOND = 50f;

    public static class SotfLifedrinkerArmorRepairScript implements AdvanceableListener {
        public ShipAPI ship;
        public SotfLifedrinkerArmorRepairScript(ShipAPI ship) {
            this.ship = ship;
        }

        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused()) return;
            if (DefenseUtils.getMostDamagedArmorCell(ship) == null) return;
            SotfMisc.repairMostDamaged(ship, REPAIR_PER_SECOND * amount);
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //if (SotfModPlugin.LIFEDRINKER_HULLS.contains(ship.getHullSpec().getHullId() + "_lifedrinker")) {
        //    ship.getVariant().setHullSpecAPI(Global.getSettings().getHullSpec(ship.getHullSpec().getHullId() + "_lifedrinker"));
        //}
        ship.setShield(ShieldAPI.ShieldType.NONE, 0f, 1f, 1f);

        ship.addListener(new SotfLifedrinkerArmorRepairScript(ship));
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBeamDamageTakenMult().modifyMult(id, 1f - REFLECTION_MULT);
        stats.getEmpDamageTakenMult().modifyMult(id, 1f - REFLECTION_MULT);

        //stats.getMinArmorFraction().modifyFlat(id, MINIMUM_ARMOR_BONUS);
        stats.getArmorBonus().modifyPercent(id, ARMOR_BONUS);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) REPAIR_PER_SECOND;
        if (index == 1) return "" + (int) Math.round(ARMOR_BONUS) + "%";
        //if (index == 1) return "" + (int) Math.round(MINIMUM_ARMOR_BONUS * 100f) + "%";
        if (index == 2) return "" + (int) Math.round(REFLECTION_MULT * 100f) + "%";
        return null;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (!shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord")) {
            return "Requires a Phase Concord";
        }
        //if (!SotfModPlugin.LIFEDRINKER_HULLS.contains(ship.getHullSpec().getHullId() + "_lifedrinker")) {
        //    return "Cannot be installed on " + ship.getHullSpec().getHullNameWithDashClass();
        //}
        return super.getUnapplicableReason(ship);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord") && SotfModPlugin.LIFEDRINKER_HULLS.contains(ship.getHullSpec().getHullId() + "_lifedrinker");
        return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord");
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color good = Misc.getPositiveHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        Color gray = Misc.getGrayColor();

        if (Global.getSettings().getCurrentState() == GameState.TITLE) return;
        if (isForModSpec || ship == null) return;

        //tooltip.addPara("Also moves Concord Shift into the ship's defensive system slot, and adjusts Sierra's skill loadout to favor short-range shieldless brawling.", opad);
        tooltip.addPara("Also adjusts Sierra's skill loadout to favor short-range shieldless brawling and heightened durability.", opad);

        PersonAPI sierra = SotfPeople.getPerson(SotfPeople.SIERRA);

        float colW = width * 0.4f;
        tooltip.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                20f, true, true,
                new Object [] {"Skill", colW, "Exchanged for...", colW});

        tooltip.addRow(Alignment.MID,
                Global.getSettings().getSkillSpec(Skills.GUNNERY_IMPLANTS).getGoverningAptitudeColor().brighter(),
                Global.getSettings().getSkillSpec(Skills.GUNNERY_IMPLANTS).getName(),
                Alignment.MID,
                Global.getSettings().getSkillSpec(Skills.IMPACT_MITIGATION).getGoverningAptitudeColor().brighter(),
                Global.getSettings().getSkillSpec(Skills.IMPACT_MITIGATION).getName());

        tooltip.addRow(Alignment.MID,
                Global.getSettings().getSkillSpec(Skills.ORDNANCE_EXPERTISE).getGoverningAptitudeColor().brighter(),
                Global.getSettings().getSkillSpec(Skills.ORDNANCE_EXPERTISE).getName(),
                Alignment.MID,
                Global.getSettings().getSkillSpec(Skills.COMBAT_ENDURANCE).getGoverningAptitudeColor().brighter(),
                Global.getSettings().getSkillSpec(Skills.COMBAT_ENDURANCE).getName());

        if (sierra.getStats().getLevel() >= 7) {
            tooltip.addRow(Alignment.MID,
                    Global.getSettings().getSkillSpec(Skills.TARGET_ANALYSIS).getGoverningAptitudeColor().brighter(),
                    Global.getSettings().getSkillSpec(Skills.TARGET_ANALYSIS).getName(),
                    Alignment.MID,
                    Global.getSettings().getSkillSpec(Skills.DAMAGE_CONTROL).getGoverningAptitudeColor().brighter(),
                    Global.getSettings().getSkillSpec(Skills.DAMAGE_CONTROL).getName());
        }

        tooltip.addTable("", 0, opad);

//        LabelAPI label = tooltip.addPara("\"A warding sigil, a song to the void? No. I survive by tempered spirit and a witch's grit.\"", SotfMisc.getSierraColor().darker(), opad);
//        label.italicize();
//        tooltip.addPara("   - Sierra-Nought-Bravo", gray, opad);

        LabelAPI label = tooltip.addPara(
                " \"Blood flows from your lashes~\n" +
                "   My tormentor, so sweet~\n" +
                "     Salt of tears like honey~\n" +
                "       Soak a lifedrinker's\n" +
                "         Lips and feet~\""
                ,
                SotfMisc.getSierraColor().darker(), opad);
        label.italicize();
        tooltip.addPara("   - \"Lifedrinker\" lyrical excerpt submitted to Epiphany inquisitor", gray, opad);
    }

}
