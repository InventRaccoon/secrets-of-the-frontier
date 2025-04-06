package data.scripts.dialog.haunted;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfHauntedDream2 extends SotfHauntedDreamBase {

    @Override
    public void advance(float amount) {
        super.advance(amount);

        addLine("This night, your dreams torment you.", 0, m);
        addLine("As they will until oblivion.", 1, m + 1);

        addLine("You see it now, the little white light peeking through the dark clouds. Glimpses of it spur your " +
                "steps as you struggle through the thicket, drowning in thorns.", 2, l);
        addLine("The revenant is never far behind you. Your pace is quickened but never do you make distance on it.", 3, m + 1);

        addLine("Then, you burst into the open, the thick choking air, and you see it high above you. A white " +
                "star, shining over a dark lake.", 4, l);
        addLine("Salvation, you can only pray. An escape from this hell.", 5, s);
        addLine("And in your wonder, you forget for the briefest moments. You look down to see the barbed spear " +
                "burst from your chest, then another, then-", 6, s);

        if (stage == 7 && counter >= 0) {
            addWakeUpOption();
            stage = 8;
        }
    }

    @Override
    public String getEndingTrigger() {
        return "sotfHauntedDream2End";
    }
}