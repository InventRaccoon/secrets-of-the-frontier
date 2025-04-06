package data.scripts.campaign.ghosts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseGhostBehavior;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.SotfMisc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SotfGBShowMessage extends BaseGhostBehavior {

	protected SectorEntityToken from;
	protected String string;
	protected Color color;
	protected float messageDuration;

	public SotfGBShowMessage(String string, Color color, float messageDuration) {
		super(100f);
		this.string = string;
		this.color = color;
	}

	@Override
	public void advance(float amount, SensorGhost ghost) {
		//if (Global.getSector().getPlayerFleet() != null) {
		//	Global.getSector().getPlayerFleet().addFloatingText(string, color, messageDuration, true);
		//}
		Global.getSector().getCampaignUI().addMessage(string, color);
		end();
	}

}













