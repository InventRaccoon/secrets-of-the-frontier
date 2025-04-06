package data.scripts.dialog;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.ids.SotfIDs;
import data.scripts.campaign.ids.SotfPeople;
import data.scripts.campaign.intel.quests.SotfWaywardStarIntel;
import data.scripts.utils.SotfMisc;

import java.awt.*;
import java.util.Map;

public class SotfWSWinPlugin implements InteractionDialogPlugin {

    public static enum OptionId {
        INIT,
        WHAT,
        FINE,
        GRUMBLE,
        DRAGGED,
        CHAT,
        YES,
        NO,
        LATER,
        REPORT,
        CONT,
        ;
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;

    protected CampaignFleetAPI playerFleet;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        playerFleet = Global.getSector().getPlayerFleet();

        optionSelected(null, OptionId.INIT);
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void backFromEngagement(EngagementResultAPI result) {

    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        OptionId option = (OptionId) optionData;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        Color sc = Global.getSector().getFaction(SotfIDs.SIERRA_FACTION).getBaseUIColor();

        switch (option) {
            case INIT:
                textPanel.addParagraph("Everything is a blur. There's little you can do but stare at the indistinct shapes " +
                        "that shift around in front of you against a blank backdrop of dull grey.");

                textPanel.addParagraph("A cold embrace becomes clear to you, pinprick points of pain and broad sores " +
                        "alike across your back. Voices, but you can't make out the words. One question clearly needs to be " +
                        "asked.");

                options.clearOptions();
                options.addOption("\"What just happened?\"", OptionId.WHAT, null);
                break;
            case WHAT:
                textPanel.addParagraph("Someone grabs you by the arm and pulls, throwing the world around you up into the air. " +
                        "It becomes obvious that you've spent the last while lying on the bridge floor.");
                textPanel.addParagraph("The voices grow louder. Some of the sounds start making sense, about the time that " +
                        "the shapes begin to solidify into the forms of your concerned crew.");
                textPanel.addParagraph("A junior officer shakes you. \"Commander? " + Global.getSector().getPlayerPerson().getNameString() + "? " +
                        "Can you hear me?\"");

                options.clearOptions();
                options.addOption("\"I'm fine.\"", OptionId.FINE, null);
                options.addOption("Grumble", OptionId.GRUMBLE, null);
                break;
            case FINE:
                textPanel.addParagraph("\"You were out for a while,\" he says worriedly.");
            case GRUMBLE:
                textPanel.addParagraph("An engineer, part-way through a repair job on an overloaded conduit, calls from a corner. \"Sure the chief will walk it off.\" " +
                        "He laughs heartily. \"Not so sure about the beauty sleep, though!\" Few appear to share his mood.");
                textPanel.addParagraph("The junior officer turns back to you after having given the engineer a withering glare. \"Let's get you to your quarters, " +
                        "fighting phase ghosts earns you some alone time.\"");
                textPanel.addParagraph("You aren't exactly given a chance to argue.");
                options.clearOptions();
                options.addOption("Get dragged to your quarters", OptionId.DRAGGED, null);
                break;
            case DRAGGED:
                textPanel.addParagraph("Time proves the greatest healer of all, and the pounding headache eventually relents, leaving you " +
                        "alone with your thoughts, if still nauseous.");

                if (SotfMisc.playerHasSierra() && !Global.getSector().getMemoryWithoutUpdate().contains("$sotf_WSSecondTime")) {
                    textPanel.addParagraph("That is, until a small chime arrives at your datapad. Somehow, you already know who it is before " +
                            "reading the message. An invitation to chat.");
                    options.clearOptions();
                    options.addOption("Chat with Sierra", OptionId.CHAT, null);
                    options.addOption("Insist on discussing matters later", OptionId.LATER, null);
                    break;
                } else if (!SotfMisc.playerHasSierra()) {
                    textPanel.addParagraph("You feel like Sierra would have something to say if her ship wasn't more scrap metal than functioning " +
                            "warship. Hopefully her core's still in there, somewhere.");
                    Global.getSector().getMemoryWithoutUpdate().set("$sierraWantsWSConv", true);
                    // another chance
                    SectorEntityToken token = playerFleet.getContainingLocation().createToken(playerFleet.getLocation().x, playerFleet.getLocation().y);
                    SotfMisc.addDerelict(playerFleet.getStarSystem(), token, SotfMisc.getSierraVariant(), ShipRecoverySpecial.ShipCondition.WRECKED, 0f, true);
                    textPanel.addParagraph("It is admittedly nice to get time to yourself after the experience you just went through.");
                }
            case REPORT:
                //visual.hideCore();
                textPanel.addParagraph("A report is soon delivered to your quarters.");
                textPanel.addParagraph("Fleet-wide damage: exactly as expected from the last encounter. Seemingly " +
                        "otherworldly opponents. Extremely material weapons fire and no salvage to patch it up with.");
                textPanel.addParagraph("Or so you think, as you reach the next paragraph. Two vessels spotted on " +
                        "sensors just nearby. Derelict. Scans are a near but not exact match to those of your assailants.");
                textPanel.addParagraph("The cruiser, and one of the carriers. Partially intact.");

                SectorEntityToken token = playerFleet.getContainingLocation().createToken(playerFleet.getLocation().x, playerFleet.getLocation().y);
                SectorEntityToken vow = SotfMisc.addStoryDerelictWithName(playerFleet.getStarSystem(), token, "sotf_vow_Base", ShipRecoverySpecial.ShipCondition.PRISTINE, 150f, true, "Voidwitch");
                vow.getMemoryWithoutUpdate().set("$sotf_WSvow", true);

                ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
                data.notNowOptionExits = true;
                data.noDescriptionText = true;
                data.storyPointRecovery = false;
                DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) vow.getCustomPlugin();
                ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
                copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
                copy.variantId = null;
                data.addShip(copy);
                Misc.setSalvageSpecial(vow, data);

                SectorEntityToken wispmother = SotfMisc.addStoryDerelictWithName(playerFleet.getStarSystem(), token, "sotf_wispmother_WS", ShipRecoverySpecial.ShipCondition.WRECKED, 100f, false, "Silent Whisper");
                wispmother.getMemoryWithoutUpdate().set("$sotf_WSwispmother", true);
                DerelictShipEntityPlugin dsep2 = (DerelictShipEntityPlugin) wispmother.getCustomPlugin();

                // the veil falters
                SotfWaywardStarIntel.addReveriesMeal(playerFleet.getStarSystem());

                if (Global.getSector().getIntelManager().hasIntelOfClass(SotfWaywardStarIntel.class)) {
                    SotfWaywardStarIntel ws = (SotfWaywardStarIntel) Global.getSector().getIntelManager().getFirstIntel(SotfWaywardStarIntel.class);
                    ws.endAfterDelay();
                }

                options.clearOptions();
                options.addOption("Return to your duties", OptionId.CONT, null);
                break;
            case CHAT:
                visual.showPersonInfo(SotfPeople.getPerson(SotfPeople.SIERRA), true);
                textPanel.addParagraph("Sierra's avatar appears on your datapad. \"Hey. Are you okay?\" she asks. " +
                        "\"I heard that... whatever happened had a rough effect on you. I don't really know what that's like.\"");
                textPanel.highlightInLastPara(sc, "\"Hey. Are you okay?\"",
                        "\"I heard that... whatever happened had a rough effect on you. I don't really know what that's like.\"");
                textPanel.addParagraph("\"Did you hear that voice, right before we were attacked by those... ghosts?\" She pauses. \"I feel really silly saying that.\"");
                textPanel.highlightInLastPara(sc, "\"Did you hear that voice, right before we were attacked by those... ghosts?\"", "\"I feel really silly saying that.\"");

                options.clearOptions();
                options.addOption("\"Yes. Almost sounded like you.\"", OptionId.YES, null);
                options.addOption("\"No.\"", OptionId.NO, null);
                break;
            case YES:
                textPanel.addParagraph("She sighs. \"It did, didn't it? Consider me severely creeped out. What did it want from us?\" she wonders aloud. \"Besides the obvious.\"");
                textPanel.highlightInLastPara(sc, "\"It did, didn't it? Consider me severely creeped out. What did it want from us?\"", "\"Besides the obvious.\"");
                textPanel.addParagraph("Her avatar spins. \"I'm going to think about it for a bit. Take care. I'll, um, tell you if I get any more signals.\"");
                textPanel.highlightInLastPara(sc, "\"I'm going to think about it for a bit. Take care. I'll, um, tell you if I get any more signals.\"");
                textPanel.addParagraph("She disconnects. A report is soon delivered to your quarters.");

                Global.getSector().getMemoryWithoutUpdate().set("$sotf_kindred", "kindred");
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_Kindred", "Kindred");

                CoreReputationPlugin.CustomRepImpact impact = new CoreReputationPlugin.CustomRepImpact();
                impact.limit = RepLevel.COOPERATIVE;
                impact.delta = 0.05f;
                Global.getSector().adjustPlayerReputation(
                        new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact,
                                null, dialog.getTextPanel(), true), SotfPeople.getPerson(SotfPeople.SIERRA));

                options.clearOptions();
                options.addOption("Read the after-action report", OptionId.REPORT, null);
                break;
            case NO:
                // Gaslighting Sierra options feel like they're on par with not waving at Alviss, but gotta have something for the heartless monsters
                textPanel.addParagraph("\"Oh,\" she replies awkwardly. \"Right. Just me being me, I guess. Nevermind.\" She sighs. \"Actually, I think that was all. I'll leave you to it.\"");
                textPanel.highlightInLastPara(sc, "\"Oh,\"", "\"Right. Just me being me, I guess. Nevermind.\", \"Actually, I think that was all.  I'll leave you to it.\"");
                textPanel.addParagraph("She disconnects. A report is soon delivered to your quarters.");

                // consequences
                SotfMisc.addGuilt(0.5f);

                CoreReputationPlugin.CustomRepImpact impact2 = new CoreReputationPlugin.CustomRepImpact();
                impact2.limit = RepLevel.INHOSPITABLE;
                impact2.delta = -0.02f;
                Global.getSector().adjustPlayerReputation(
                        new CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact2,
                                null, dialog.getTextPanel(), true), SotfPeople.getPerson(SotfPeople.SIERRA));

                options.clearOptions();
                options.addOption("Read the after-action report", OptionId.REPORT, null);
                break;
            case LATER:
                textPanel.addParagraph("You send Sierra a message requesting you have your chat later, when you're not feeling like you've spent a few weeks " +
                        "in phase-space. For all you know, you have.");
                textPanel.addParagraph("She sends an understanding reply. Whenever you're ready, she's there.");
                textPanel.addParagraph("A report is soon delivered to your quarters.");
                Global.getSector().getMemoryWithoutUpdate().set("$sierraWantsWSConv", true);
                options.clearOptions();
                options.addOption("Read the after-action report", OptionId.REPORT, null);
                break;
            case CONT:
                Global.getSector().getMemoryWithoutUpdate().set("$sotf_WSSecondTime", true);
                Global.getSector().setPaused(false);
                dialog.dismiss();
                break;
        }
    }




    public void optionMousedOver(String optionText, Object optionData) {

    }

    public void advance(float amount) {

    }

    public Object getContext() {
        return null;
    }
}