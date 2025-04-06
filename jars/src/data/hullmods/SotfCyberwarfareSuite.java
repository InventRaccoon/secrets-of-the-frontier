// Cyberwarfare Suite for Dustkeeper flagships
package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfCyberwarfareSuite extends SotfBaseAuxPackage {

	public static float CWAR_COOLDOWN_MULT = 0.75f;
	public static float CWAR_PENETRATION_PERCENT = 30f;

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getStat(SotfIDs.STAT_CYBERWARFARE_COOLDOWN_MULT).modifyMult(id, CWAR_COOLDOWN_MULT);
		stats.getDynamic().getStat(SotfIDs.STAT_CYBERWARFARE_PENETRATION_MULT).modifyPercent(id, CWAR_PENETRATION_PERCENT);
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		return null;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color good = Misc.getPositiveHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color lgray = Misc.getGrayColor().brighter();
		Color gray = Misc.getGrayColor();

		SkillSpecAPI cyberwarfare = Global.getSettings().getSkillSpec(SotfIDs.SKILL_CYBERWARFARE);
		tooltip.addPara("When this ship is captained by a Dustkeeper warmind with the %s skill, the cooldown of their cyberwarfare intrusions is reduced by %s, " +
				"and their cyberwarfare attacks against ships ignore %s of the target's ECM and ECCM bonuses.",
				opad,
				new Color[]{cyberwarfare.getGoverningAptitudeColor().brighter(), h, h},
				cyberwarfare.getName(),
				"" + Math.round((1 - CWAR_COOLDOWN_MULT) * 100) + "%",
				"" + (int) CWAR_PENETRATION_PERCENT + "%");

		if (isForModSpec || ship == null) return;
		//if (!Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().contains(SotfIDs.MEM_HAVE_TRANSPONDER)) return;

		String id = ship.getHullSpec().getBaseHullId() + "_cwar";
		if (ship.getVariant().hasTag("sotf_morrowshield")) {
			id = "sotf_morrowshield_cwar";
		}

		String quote1 = Global.getSettings().getDescription(id, Description.Type.CUSTOM).getText1();
		String author1 = Global.getSettings().getDescription(id, Description.Type.CUSTOM).getText2();
		String quote2 = Global.getSettings().getDescription(id, Description.Type.CUSTOM).getText3();
		String author2 = Global.getSettings().getDescription(id, Description.Type.CUSTOM).getText4();
		if (quote1 == null) return;
		tooltip.addSpacer(5f);
		LabelAPI label = tooltip.addPara("\"" + quote1 + "\"", lgray, opad);
		label.italicize();
		tooltip.addPara("   - " + author1, gray, pad);
		if (quote2 != null && !quote2.equals("No description... yet")) {
			label = tooltip.addPara("\"" + quote2 + "\"", lgray, opad);
			label.italicize();
			tooltip.addPara("   - " + author2, gray, pad);
		}
	}
}
