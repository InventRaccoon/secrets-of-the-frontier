package data.scripts.dialog.haunted;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfHauntedDream3 extends SotfHauntedDreamBase {

    @Override
    public void advance(float amount) {
        super.advance(amount);

        addLine("This night is wracked by nightmares as a storm does a starship.", 0, m + 1);

        addLine("That white star hangs high in the black sky, dangling before you. A glimmer of promise - or an " +
                "angler's light.", 1, m);
        addLine("You run raggedly through the thicket. It's as if the fog pursues you more doggedly now.", 2, m);
        addLine("This is no longer an execution. It's a hunt, and your hope only fuels the spectre's hatred.", 3, m);

        addLine("You scramble, and run, and claw for every step towards the shore of the lake. Rarely do you feel " +
                "like you've gotten closer.", 4, l);
        addLine("And never do you feel you'll make it before it catches you. Each step bleeds you a little drier, " +
                "until you can run no longer.", 5, l);
        addLine("And you accept its judgement.", 6, s);

        if (stage == 7 && counter >= 0) {
            addWakeUpOption();
            stage = 8;
        }
    }

    @Override
    public String getEndingTrigger() {
        return "sotfHauntedDream3End";
    }
}