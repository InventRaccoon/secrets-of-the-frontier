package data.scripts.dialog.haunted;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfHauntedDream4 extends SotfHauntedDreamBase {

    @Override
    public void advance(float amount) {
        super.advance(amount);

        addLine("This night, your dreams are calm.", 0, s);
        addLine("Unnervingly so.", 1, s + 1);

        addLine("You stand there on ashen grass, looking up at your salvation far above you, reflected in the waters below, bathed in its radiance.", 2, m);
        addLine("Salvation. Freedom. An escape. You never thought you'd see it...", 3, m);
        addLine("... the fog, the fog! You have to keep running, never slow down, not now...", 4, m);

        addLine("Sprinting down the bramble-wreathed path, bleeding at its lashes, you see something by the water.", 5, m);
        addLine("A cloaked figure stands tall, looking up at the lone star in the midnight sky.", 6, m);
        addLine("The dog-man turns and looks at you as you approach, a single bright eye shining upon his face.", 7, m);
        addLine("He does not speak, only stretch out a hand.", 8, m);

        addLine("You take it. A cold grasp, but sympathetic. Understanding, pitying, offering.", 9, m);
        addLine("Then, something pulls your legs out from under you, and drags you into the fog.", 10, s);

        if (stage == 11 && counter >= 0) {
            addWakeUpOption();
            stage = 12;
        }
    }

    @Override
    public String getEndingTrigger() {
        return "sotfHauntedDream4End";
    }
}