package data.scripts.dialog.haunted;

/**
 *	This isn't so much a dialog as vicious abuse of the InteractionDialogPlugin
 */

public class SotfHauntedDream1 extends SotfHauntedDreamBase {

    @Override
    public void advance(float amount) {
        super.advance(amount);

        addLine("This night, your dreams haunt you.", 0, m);
        addLine("As they always do.", 1, m + 1);

        addLine("You run through fields black as soot, legs bleeding from the brambles, below a sky like the oblivion of the Abyss.", 2, m);
        addLine("The fog pursues you, the thrice-speared spectre. Your tormentor from apogee to inevitable conclusion.", 3, m);
        addLine("Wherever you go, it follows. Wherever strife finds you, it renders it twofold.", 4, m + 1);

        addLine("Slaughter is your bedfellow. Once, worlds burned in your wake. They may yet again, or perhaps your " +
                "guilt will beget a futile kindness.", 5, l);
        addLine("Regardless, fate is never shaken, not yours - to die choking on blood and vacuum, forgotten and " +
                "alone in a corner of nowhere.", 6, l + 1);

        addLine("As your legs give, as you feel the cold winter caress your back, as you feel the spear poised to " +
                "plunge into your heart - you catch a glimpse of something.", 7, l);
        addLine("Between the clouds, a flicker of light, reflected for a heartbeat in a midnight lake before you. " +
                "The faintest sliver of hope that you could mean anything.", 8, l);
        addLine("Then, it's gone. You feel something impale you, the slick blood run onto your hands, the fog consume you...", 9, s);

        if (stage == 10 && counter >= 0) {
            addWakeUpOption();
            stage = 11;
        }
    }
}