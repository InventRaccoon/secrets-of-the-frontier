package data.scripts.campaign.misc;

import com.fs.starfarer.api.EveryFrameScript;

public class PausedTimeAdvancer implements EveryFrameScript {

    public static float time = 0f;

    @Override
    public boolean isDone() {
        return false;
    }


    @Override
    public boolean runWhilePaused() {
        return true;
    }


    @Override
    public void advance(float amount) {
        time += 1 * amount;
    }
}
