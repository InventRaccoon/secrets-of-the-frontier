package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.campaign.impl.items.GenericSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.magiclib.util.MagicAnim;

import java.awt.*;

import static java.lang.Math.exp;
import static java.lang.Math.sin;

/**
 * Code to render the Daydream Analyzer's glow
 * Original code for Knights of Ludd written by Dal
 */

public class SotfDaydreamAnalyzerPlugin extends GenericSpecialItemPlugin {

	int glowMS = 5000;
	boolean flickerActive = false;

	@Override
	public void render(float x, float y, float w, float h, float alphaMult,
					   float glowMult, SpecialItemRendererAPI renderer) {
		var centerX = x+w/2;
		var centerY = y+h/2;

		SpriteAPI sprite = Global.getSettings().getSprite(this.spec.getIconName());
		sprite.setNormalBlend();
		sprite.setAlphaMult(alphaMult);
		sprite.renderAtCenter(centerX, centerY);

		if (glowMult > 0) {
			sprite.setAdditiveBlend();
			sprite.setAlphaMult(alphaMult * glowMult * 0.5f);
			sprite.renderAtCenter(centerX, centerY);
		}

		float secondFraction = (System.currentTimeMillis() % glowMS) / (float) glowMS;

		float progress;
		if (secondFraction < 0.5f) {
			progress = secondFraction * 2f;
		} else {
			progress = (1f - (secondFraction - 0.5f) * 2f);
		}

		// smooth it
		progress = MagicAnim.smooth(progress);

		// random flickers
		if (Math.random() > 0.96f) progress = Math.min(progress + ((float) Math.random() * 0.25f), 1f);

		SpriteAPI glow = Global.getSettings().getSprite("cargo", "sotf_daydreamanalyzer_glow");
		//sprite render
		glow.setNormalBlend();
		glow.setAlphaMult(0.1f + (progress * 0.9f));
		glow.renderAtCenter(centerX, centerY);
	}

//	public float getModifiedSineWaveY(float progress) {
//		float sineDuration = 0.08f;
//		String s = "progress - $progress";
//		float y = 0f;
//		if (progress < sineDuration) {
//			//A*sin(bx)
//			//A = 1 damit obere/untere bounds 1
//			//B macht die kurve schärfer je höher es ist, b= 1 heißt y0 = 0, y360 = 0, b = 3 heißt y0 = 0, y120 (360/3) = 1
//			//geht ab 0.5 ins minus!
//			s += " - sine: ";
//			y = (float) sin(2f * Math.PI * (progress / sineDuration));
//		} else {
//			progress -= sineDuration; //so it starts with 0
//			progress *= 10f; //don't ask
//			float riseSteepness = 10f;
//			float fallCenter = 1f;
//			float fallWidth = 1.5f;
//
//			//exponential rise
//			float risePart = (1f / (1f + (float) exp((-riseSteepness * (progress - 0.2f)))));
//
//			//gaussian fall (slow fall off)
//			float fallPart = (float) exp(-Math.pow(progress-fallCenter, 2f) / (2f * Math.pow(fallWidth, 2f)));
//			s += " - gaussian zombie: ";
//			y = MathUtils.clamp(risePart * fallPart, 0f, 1f);
//		}
//		s += y;
//		return 0.5f + 0.5f * y; //y can be -1 for the sine!
//	}

}



