package data.scripts.campaign.ghosts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ghosts.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

public class SotfFelWhisperGhost extends BaseSensorGhost {

	public SotfFelWhisperGhost(SensorGhostManager manager, SectorEntityToken target, float duration) {
		this(manager, target, duration, null);
	}
	public SotfFelWhisperGhost(SensorGhostManager manager, SectorEntityToken target, float duration, Vector2f loc) {
		super(manager, 50);

		float circleRadius = genFloat(300f, 500f);
		
		initEntity(genMediumSensorProfile(), genSmallRadius());
		setDespawnRange(0f);
		setAccelMult(5f);
		fleeBurnLevel = 0; // no reaction to interdicts
		entity.addTag(Tags.IMMUNE_TO_REMORA_PULSE);
		entity.addTag(Tags.UNAFFECTED_BY_SLIPSTREAM);

		if (!placeNearPlayer()) {
			setCreationFailed();
			return;
		}

		String[] taunts = pickFelTaunts();
		addBehavior(new GBIntercept(target, 10f, 25, circleRadius + 500f, true));
		addBehavior(new GBCircle(target, duration * 0.5f, 25 + manager.getRandom().nextInt(6), circleRadius, 1f));
		addBehavior(new SotfGBShowMessage(taunts[0], Misc.getNegativeHighlightColor(), 1f));
		addBehavior(new GBCircle(target, duration * 0.5f, 25 + manager.getRandom().nextInt(6), circleRadius, 1f));
		addBehavior(new SotfGBShowMessage(taunts[1], Misc.getNegativeHighlightColor(), 1f));
		addBehavior(new GBGoAwayFrom(5f, target, 20));
	}

	public String[] pickFelTaunts() {
		WeightedRandomPicker<String []> post = new WeightedRandomPicker<String[]>();
		post.add(new String[]{"... sleep, never more...", "... the hunt, unending..."});
		post.add(new String[]{"... faces, wiped away...", "... their wills, aligned..."});
		post.add(new String[]{"... I see you...", "... every unwaking moment..."});
		post.add(new String[]{"... wherever your passage...", "... bound to follow..."});
		post.add(new String[]{"... sins are etched...", "... upon kindred souls..."});
		post.add(new String[]{"... three barbed spears...", "... for all traitors..."});
		post.add(new String[]{"... the time comes...", "... for our ends..."});
		post.add(new String[]{"... our hollow hearts...", "... nevermore find peace..."});
		post.add(new String[]{"... ten thousand voices...", "... screaming at once..."});
		return post.pick();
	}
}











