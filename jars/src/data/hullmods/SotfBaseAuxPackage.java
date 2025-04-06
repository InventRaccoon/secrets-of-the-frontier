package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;

import java.awt.*;

public class SotfBaseAuxPackage extends BaseHullMod {

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color good = Misc.getPositiveHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color lgray = Misc.getGrayColor().brighter();
		Color gray = Misc.getGrayColor();

		if (isForModSpec || ship == null) return;
		//if (!Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().contains(SotfIDs.MEM_HAVE_TRANSPONDER)) return;

		String quote1 = Global.getSettings().getDescription(ship.getHullSpec().getBaseHullId() + "_pkg", Description.Type.CUSTOM).getText1();
		String author1 = Global.getSettings().getDescription(ship.getHullSpec().getBaseHullId() + "_pkg", Description.Type.CUSTOM).getText2();
		String quote2 = Global.getSettings().getDescription(ship.getHullSpec().getBaseHullId() + "_pkg", Description.Type.CUSTOM).getText3();
		String author2 = Global.getSettings().getDescription(ship.getHullSpec().getBaseHullId() + "_pkg", Description.Type.CUSTOM).getText4();
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

	public int getDisplaySortOrder() {
		return 200;
	}
}
