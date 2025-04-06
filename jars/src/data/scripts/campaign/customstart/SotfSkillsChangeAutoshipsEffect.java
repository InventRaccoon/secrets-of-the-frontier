package data.scripts.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.BaseSkillsChangeEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Triggers during respec while playing Banshee's Lost Thread
 * Allows player to pick a skill to replace with Automated Ships if they would not have it
 */

public class SotfSkillsChangeAutoshipsEffect extends BaseSkillsChangeEffect {

	public static class AutoshipsChangeData {
		static List<ButtonAPI> buttons = new ArrayList<ButtonAPI>();
	}

	@Override
	public boolean hasEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to) {
		return !to.hasSkill(Skills.AUTOMATED_SHIPS) && !Global.getSettings().getModManager().isModEnabled("second_in_command");
	}

	@Override
	public void printEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to, TooltipMakerAPI info, Map<String, Object> dataMap) {
		super.prepare();

		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color tech = Global.getSettings().getSkillSpec(Skills.AUTOMATED_SHIPS).getGoverningAptitudeColor().brighter();
		
		float initPad = 15f;
		info.addSectionHeading("Automated Ships", base, dark, Alignment.MID, initPad);
		initPad = opad;

		info.addPara("Years of jury-rigging AI-controlled vessels has left knowledge of their operation etched deep into your mind.", initPad);
		info.addPara("Choosing a skill will replace it with Automated Ships, even if you don't " +
				"have the prerequisite number of skill points spent in Technology.", opad, tech, "Automated Ships", "Technology");
		info.addPara("You decide to replace...", opad);

		float bw = 350;
		float bh = 25;
		float indent = 40f;

		ButtonAPI keep = info.addAreaCheckbox(
				"None of your skills", "none", base,
				dark,
				bright, bw, bh, opad, true);
		keep.setChecked(true);
		AutoshipsChangeData.buttons.add(keep);

		for (MutableCharacterStatsAPI.SkillLevelAPI skill : to.getSkillsCopy()) {
			if (skill.getLevel() == 0) continue;
			if (skill.getSkill().isAptitudeEffect()) continue;
			float p = opad;
			ButtonAPI b = info.addAreaCheckbox(
					skill.getSkill().getName(), skill.getSkill().getId(), skill.getSkill().getGoverningAptitudeColor(),
					skill.getSkill().getGoverningAptitudeColor().darker().darker(),
					skill.getSkill().getGoverningAptitudeColor().brighter(), bw, bh, p, true);
			AutoshipsChangeData.buttons.add(b);
		}
	}
	
	@Override
	public void infoButtonPressed(ButtonAPI button, Object param, Map<String, Object> dataMap) {
		for (ButtonAPI b : AutoshipsChangeData.buttons) {
			b.setChecked(b == button);
		}
	}
	
	@Override
	public void applyEffects(MutableCharacterStatsAPI from, MutableCharacterStatsAPI to, Map<String, Object> dataMap) {
		for (ButtonAPI b : AutoshipsChangeData.buttons) {
			if (b.isChecked() && to.hasSkill((String) b.getCustomData())) {
				to.setSkillLevel(Skills.AUTOMATED_SHIPS, 1);
				to.setSkillLevel((String) b.getCustomData(), 0);
			}
		}
	}
	
}







