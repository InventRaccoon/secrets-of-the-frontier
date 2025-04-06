package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

import java.awt.*;

public class SotfBaseConcordAugment extends BaseHullMod {

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord");
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (!shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord")) {
			return "Requires a Phase Concord";
		}
		return super.getUnapplicableReason(ship);
	}

	public Color getNameColor() {
		return SotfMisc.getSierraColor();
	}

	@Override
	public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
		return shipHasOtherModInCategory(ship, spec.getId(), "sotf_concord");
	}
}
