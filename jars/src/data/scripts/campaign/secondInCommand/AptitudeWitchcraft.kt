package data.scripts.campaign.secondInCommand

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import second_in_command.SCData
import second_in_command.specs.SCAptitudeSection
import second_in_command.specs.SCBaseAptitudePlugin

class AptitudeWitchcraft : SCBaseAptitudePlugin() {

    override fun addCodexDescription(tooltip: TooltipMakerAPI) {
        tooltip.addPara("The Witchcraft aptitude is added by the Secrets of the Frontiers mod. " +
                "The aptitude allows Sierra to pilot multiple Concord-equipped vessels and provides several exotic boons for high-tech fleets specializing in fast-attack or strike tactics. " +
                "Acquired by completing the Wayward Star quest, interacting with both Vow and Wispmother wrecks, and then starting a dialog with Sierra through the Contact in the intel screen.",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "Witchcraft", "Acquired")
    }

    override fun getOriginSkillId(): String {
        return "sotf_dance_between_realms"
    }

    override fun createSections() {

        var section1 = SCAptitudeSection(true, 0, "technology1")
        section1.addSkill("sotf_voidwalking")
        section1.addSkill("sotf_accelerando")
        section1.addSkill("sotf_the_withering_hex")
        section1.addSkill("sotf_seal_of_abjuration")
        section1.addSkill("sotf_dance_with_me")
        addSection(section1)

        var section2 = SCAptitudeSection(false, 3, "technology2")
        section2.addSkill("sotf_tend_to_our_garden")
        section2.addSkill("sotf_agony_encore")
        addSection(section2)

        var section3 = SCAptitudeSection(false, 4, "technology4")
        section3.addSkill("sotf_spirit_ward")
        section3.addSkill("sotf_one_witch_waltz")
        addSection(section3)

    }

    override fun getNPCFleetSpawnWeight(data: SCData?, fleet: CampaignFleetAPI?): Float {
        return 0f
    }


}