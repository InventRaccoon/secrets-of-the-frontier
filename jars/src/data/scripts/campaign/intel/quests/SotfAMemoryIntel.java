package data.scripts.campaign.intel.quests;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Set;

/**
 *	A Memory's intel report
 */

public class SotfAMemoryIntel extends BaseIntelPlugin {

	public enum AMemoryStage {
		GHOSTS,
		FIGHT,
		REFIGHT,
		DONE,
	}

	public AMemoryStage stage;

	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;
		
		float initPad = pad;
		if (mode == ListInfoMode.IN_DESC) initPad = opad;

		Color sc = SotfMisc.getSierraColor();
		
		bullet(info);
		boolean isUpdate = getListInfoParam() != null;

		if (stage == AMemoryStage.FIGHT) {
			if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMHarmonicTuning")) {
				info.addPara("Achieve Victory By Concord", initPad, sc, "Concord");
			} else {
				info.addPara("Achieve Victory By Concord Alone", initPad, sc, "Concord");
			}
		} else if (stage == AMemoryStage.REFIGHT) {
			info.addPara("Return to the ISS Athena when ready", initPad, h, "Tia'Taxet");
		}
		
		unindent(info);
	}
	
	
	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.setParaSmallInsignia();
		info.addPara(getName(), c, 0f);
		info.setParaFontDefault();
		addBulletPoints(info, mode);
	}
	
	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		Color sc = SotfMisc.getSierraColor();
		float pad = 3f;
		float opad = 10f;
		
		addBulletPoints(info, ListInfoMode.IN_DESC);

		StarSystemAPI system = null;
		if (Global.getSector().getStarSystem("tia") != null) {
			system = Global.getSector().getStarSystem("tia");
		} else if (Global.getSector().getMemoryWithoutUpdate().contains("sotf_athenaWreck")) {
			SectorEntityToken athena = (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get("sotf_athenaWreck");
			system = athena.getStarSystem();
		}
		String sysname = "null";
		if (system != null) {
			sysname = system.getName();
		}

		if (stage == AMemoryStage.GHOSTS) {
			info.addPara("Sierra has pointed out the presence of unusual sensor ghost activity around the " + sysname + ".", opad, h, sysname);
		} else if (stage == AMemoryStage.FIGHT) {
			info.addPara("Achieve Victory By Concord Alone", opad, sc, "Concord");
		} else if (stage == AMemoryStage.REFIGHT) {
			info.addPara("Thrust into phase-space and faced with anomalies in another dimension, you were defeated in battle. " +
					"Return to the wreck of the ISS Athena in " + sysname + " when ready and achieve victory.", opad, h, sysname);
			info.addPara("As you have discovered, ships without a Phase Concord are crippled by the sustained interdimensional travel - only Sierra's " +
					"ship can fight effectively.", opad, sc, "Phase Concord", "Sierra's");
			if (Global.getSector().getMemoryWithoutUpdate().contains("$sotf_AMHarmonicTuning")) {
				info.addPara("However, Sierra has been inspired to employ a so-called \"harmonic tuning\" technique that will " +
						"also allow the safe deployment of a single phase frigate alongside it.", opad, h, "single phase frigate");
			}
		} else {
			info.addPara("After encountering the wreckage of an Aurora-class cruiser, you and Sierra tunneled into phase-space " +
					"in order to join the memory of a battle long past. She appears satisfied with the outcome, despite " +
					"the seemingly immaterial rewards and very material threat of Hegemony weapons batteries.", opad);
		}
	}
	
	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("intel", "sotf_amemory");
	}
	
	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_EXPLORATION);
		tags.add(Tags.INTEL_STORY);
		return tags;
	}
	
	public String getSortString() {
		return "A Memory";
	}
	
	public String getName() {
		if (isEnded() || isEnding()) {
			return "A Memory - Completed";
		}
		return "A Memory";
	}
	
	@Override
	public FactionAPI getFactionForUIColors() {
		return super.getFactionForUIColors();
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (Global.getSector().getStarSystem("tia") != null) {
			return Global.getSector().getStarSystem("tia").getStar();
		} else if (Global.getSector().getMemoryWithoutUpdate().contains("sotf_athenaWreck")) {
			return (SectorEntityToken) Global.getSector().getMemoryWithoutUpdate().get("sotf_athenaWreck");
		}
		return null;
	}
	
	@Override
	public boolean shouldRemoveIntel() {
		return false;
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}

	public AMemoryStage getStage() {
		return stage;
	}

	public void setStage(AMemoryStage newStage) {
		stage = newStage;
	}
	
}







