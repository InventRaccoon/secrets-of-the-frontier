package data.scripts.campaign.missions.dkcontact;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

/**
 * Contacts that can't be dismissed or prioritized and don't count towards the limit.
 * CODE MOSTLY FROM Nexerelin BY Histidine
 */
public class SotfDKContactIntel extends ContactIntel {

    public SotfDKContactIntel(PersonAPI person, MarketAPI market) {
        super(person, market);
        state = ContactState.SUSPENDED;	// so they don't count towards limit
    }

    // don't lose importance when relocating
    @Override
    public void relocateToMarket(MarketAPI other, boolean withIntelUpdate) {
        super.relocateToMarket(other, withIntelUpdate);
        person.setImportance(person.getImportance().next());
    }

    @Override
    public void doPeriodicCheck() {
        super.doPeriodicCheck();
        person.getMemoryWithoutUpdate().set(BaseMissionHub.CONTACT_SUSPENDED, false);
    }

    public String getName() {
        if (state == ContactState.LOST_CONTACT_DECIV) {
            return "Lost Contact: " + person.getNameString();
        } else if (state == ContactState.LOST_CONTACT) {
            return "Lost Contact: " + person.getNameString();
        }
        return "Contact: " + person.getNameString();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        //info.addImage(person.getPortraitSprite(), width, 128, opad);

        FactionAPI faction = person.getFaction();
        info.addImages(width, 128, opad, opad, person.getPortraitSprite(), faction.getCrest());

        float relBarWidth = 128f * 2f + 10f;
        float importanceBarWidth = relBarWidth;

        float indent = 25;
        info.addSpacer(0).getPosition().setXAlignOffset(indent);

        //info.addRelationshipBar(person, relBarWidth, opad);

        relBarWidth = (relBarWidth - 10f) / 2f;
        info.addRelationshipBar(person, relBarWidth, opad);
        float barHeight = info.getPrev().getPosition().getHeight();
        info.addRelationshipBar(person.getFaction(), relBarWidth, 0f);
        UIComponentAPI prev = info.getPrev();
        prev.getPosition().setYAlignOffset(barHeight);
        prev.getPosition().setXAlignOffset(relBarWidth + 10f);
        info.addSpacer(0f);
        info.getPrev().getPosition().setXAlignOffset(-(relBarWidth + 10f));

        info.addImportanceIndicator(person.getImportance(), importanceBarWidth, opad);
        addImportanceTooltip(info);
        info.addSpacer(0).getPosition().setXAlignOffset(-indent);

        if (market != null && state == ContactState.LOST_CONTACT_DECIV) {
            info.addPara(person.getNameString() + " was " +
                            person.getPostArticle() + " " + person.getPost().toLowerCase() +
                            " " + market.getOnOrAt() + " " + market.getName() +
                            ", a colony controlled by " + marketFaction.getDisplayNameWithArticle() + ".",
                    opad, marketFaction.getBaseUIColor(),
                    Misc.ucFirst(marketFaction.getDisplayNameWithArticleWithoutArticle()));
            info.addPara("This colony has decivilized, and you've since lost contact with " + person.getHimOrHer() + ".", opad);
        } else {
            if (market != null) {
                LabelAPI label = info.addPara(person.getNameString() + " is " +
                                person.getPostArticle() + " " + person.getPost().toLowerCase() +
                                " and can be found " + market.getOnOrAt() + " " + market.getName() +
                                ", a size %s colony controlled by " + market.getFaction().getDisplayNameWithArticle() + ".",
                        opad, market.getFaction().getBaseUIColor(),
                        "" + (int)market.getSize(), market.getFaction().getDisplayNameWithArticleWithoutArticle());
                label.setHighlightColors(h, market.getFaction().getBaseUIColor());
            }
        }

        addBulletPoints(info, ListInfoMode.IN_DESC);


        if (state == ContactState.PRIORITY || state == ContactState.NON_PRIORITY || state == ContactState.SUSPENDED) {
            long ts = BaseMissionHub.getLastOpenedTimestamp(person);
            if (ts <= Long.MIN_VALUE) {
                //info.addPara("Never visited.", opad);
            } else {
                info.addPara("Last visited: %s.", opad, h, Misc.getDetailedAgoString(ts));
            }
        }

        info.addPara("Dustkeeper contacts don't count towards the contact limit and cannot be " +
                "prioritized, suspended or deleted.", opad);
    }

    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
        if ((isEnding() || isEnded()) && state != ContactState.LOST_CONTACT_DECIV) return;
        super.addBulletPoints(info, mode);
    }
}
