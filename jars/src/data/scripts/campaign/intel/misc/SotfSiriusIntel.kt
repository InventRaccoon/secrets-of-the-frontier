package data.scripts.campaign.intel.misc

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.skills.CombatEndurance
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import data.hullmods.SotfNaniteSynthesized
import data.scripts.campaign.ids.SotfIDs
import data.scripts.campaign.ids.SotfPeople
import data.scripts.campaign.intel.misc.elements.AptitudeBackgroundElement
import data.scripts.campaign.intel.misc.elements.SkillGapElement
import data.scripts.campaign.intel.misc.elements.SkillSeperatorElement
import data.scripts.campaign.intel.misc.elements.SkillWidgetElement
import data.scripts.campaign.skills.SotfAdvancedCountermeasures
import data.scripts.campaign.skills.SotfPolarizedNanorepair
import data.scripts.combat.special.SotfInvokeHerBlessingPlugin
import data.subsystems.SotfInvokeHerBlessingSubsystem
import lunalib.lunaUI.elements.LunaElement
import java.awt.Color
import kotlin.math.roundToInt

class SotfSiriusIntel : BaseIntelPlugin() {

    class SiriusSkillSection(var isTierActive: Boolean, var soundId: String) {
        var skills = ArrayList<SiriusSkillData>()

        var skillElements = ArrayList<SkillWidgetElement>()
    }

    data class SiriusSkillData(var skillId: String, var skillName: String, var iconPath: String, var isActive: Boolean) {

    }

    companion object {

        fun createTooltipForUpgrade(tooltip: TooltipMakerAPI, id: String?) {
            val h = Misc.getHighlightColor()
            val gray = Misc.getGrayColor()
            val story = Misc.getStoryOptionColor();
            val pad = 3f
            val opad = 10f
            when (id) {
                // TIER 1

                SotfIDs.COTL_PERFECTREPLICATION -> {
                    tooltip.addPara(
                        "Sharply increases the ship quality of all mimics, removing d-mods and improving weapon selection.",
                        0f, h, "ship quality"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Mimics typically spawn with 2-3 d-mods. Some later upgrades spawn special " +
                            "\"reflections\" with improved quality - this upgrade grants them 1-3 s-mods instead.",
                        gray, 0f)
                }

                SotfIDs.COTL_MULTIFACETED -> tooltip.addPara("Increases mimic capacity by an additional %s.",
                    0f,
                    h,
                    "" + (SotfInvokeHerBlessingPlugin.MULTIFACTED_MULT * 100).roundToInt() + "%")

                // TIER 2

                SotfIDs.COTL_ENHANCEDCOUNTERMEASURES -> {
                    tooltip.addPara("Mimics' %s skill is upgraded to Elite level:",
                        0f,
                        arrayOf(Misc.getHighlightColor(), Misc.getStoryOptionColor()),
                        "Advanced Countermeasures",
                        "Elite")
//                    tooltip.addPara("    - Deploys up to %s/%s/%s/%s orbiting drones to protect the ship, depending on size",
//                        0f,
//                        Misc.getHighlightColor(),
//                        "" + SotfNaniteDronesSubsystem.DRONES_FRIGATE,
//                        "" + SotfNaniteDronesSubsystem.DRONES_DESTROYER,
//                        "" + SotfNaniteDronesSubsystem.DRONES_CRUISER,
//                        "" + SotfNaniteDronesSubsystem.DRONES_CAPITAL
//                    )
                    tooltip.addPara("    - Enables the ship's %s to automatically fire energy beams at nearby threats",
                        0f,
                        Misc.getHighlightColor(),
                        "orbiting defense swarm"
                    )
                    tooltip.addPara("    - %s reduced penalty to weapon range due to superior enemy Electronic Warfare",
                        0f,
                        Misc.getHighlightColor(),
                        "" + (SotfAdvancedCountermeasures.EW_PENALTY_MULT * 100f).roundToInt() + "%"
                    )
                }

                SotfIDs.COTL_SHRIEKOFTHEDAMNED -> {
                    tooltip.addPara(
                        "When Sirius creates an echo, he also releases a %s against all nearby hostile ships, " +
                                "fighters and missiles within %s units. Hits %s and %s. Deals %s damage and %s, " +
                                "heightened versus smaller targets. Low-hull fighters that are struck %s into non-existence.",
                        0f, h, "gravitic strike", "" + SotfInvokeHerBlessingPlugin.SHRIEK_RANGE.roundToInt(),
                        "through shields", "across dimensions", "fragmentation", "damps velocity", "collapse"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Hovering over an echo shows the range of the gravitic strike.",
                        gray, 0f)
                }

                SotfIDs.COTL_SERVICEBEYONDDEATH -> {
                    val frigWing = Global.getSettings().getFighterWingSpec("sotf_sbd_frig_wing")
                    val desWing = Global.getSettings().getFighterWingSpec("sotf_sbd_des_wing")
                    val cruWing = Global.getSettings().getFighterWingSpec("sotf_sbd_cru_wing")
                    val capWing = Global.getSettings().getFighterWingSpec("sotf_sbd_cap_wing")
                    tooltip.addPara("When a mimic is %s, it splits into a wing of %s/%s/%s/%s %s, each armed with an " +
                            "energy-based autolance that slows targets on hit. The wing fights autonomously until destroyed.",
                        0f, h, "destroyed", frigWing.numFighters.toString(), desWing.numFighters.toString(),
                        cruWing.numFighters.toString(), capWing.numFighters.toString(), "Nettle-class interceptors")
                }

                // TIER 3

                SotfIDs.COTL_EVERYROSEITSTHORNS -> {
//                    tooltip.addPara(
//                        "Spawn two Rosethorn-class frigate reflections* at the start of combat, light interdictors " +
//                                "equipped with a Resonance Catalyst system that severely impairs high-grade shields and phase cloaks.",
//                        0f, h, "Rosethorn-class", "reflections*", "Resonance Catalyst"
//                    )
                    tooltip.addPara(
                        "Spawn two Scarab-class frigate reflections* at the start of combat.",
                        0f, h, "Scarab-class", "reflections*"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara(
                        "Reflections created by this upgrade do not count against Sirius' mimic capacity.",
                        0f, h, "do not"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Reflections are special mimics that have no lifespan limit, no d-mods, and all of their " +
                            "skills are elite. If \"Perfect Replication\" is active, they spawn with 1-3 s-mods.",
                        0f, gray, Misc.getStoryDarkBrighterColor(), "elite", "s-mods")
                }

                SotfIDs.COTL_UNLIVINGVIGOR -> {
                    tooltip.addPara(
                        "For %s seconds after being created, mimics deal %s more damage, have %s increased speed, and take %s less damage.",
                        0f, h,"" + SotfInvokeHerBlessingPlugin.VIGOR_DURATION.roundToInt(), "" + (SotfInvokeHerBlessingPlugin.VIGOR_DAMAGE * 100f).roundToInt() + "%", "" +
                                (SotfInvokeHerBlessingPlugin.VIGOR_SPEED * 100f).roundToInt() + "%",
                        "" + (SotfInvokeHerBlessingPlugin.VIGOR_RESIST * 100f).roundToInt() + "%"
                    )
                }

                SotfIDs.COTL_DEATHTHROES -> {
                    tooltip.addPara("When they start expiring, mimics begin lashing nearby hostiles with gravitic " +
                            "strikes that deal energy damage and damp targets' velocity.",
                        0f, h, "expiring", "gravitic strikes", "energy", "damp targets' velocity")
                }

                // TIER 4

                SotfIDs.COTL_RECONSTITUTION -> {
                    tooltip.addPara("Mimics' %s skill is upgraded to Elite level:",
                        0f,
                        arrayOf(Misc.getHighlightColor(), Misc.getStoryOptionColor()),
                        "Polarized Nanorepair",
                        "Elite")
                    tooltip.addPara("    - When below %s hull, repair %s per second; maximum total repair is " +
                            "the higher of %s points or %s of maximum hull",
                        0f,
                        Misc.getHighlightColor(),
                        "" + (CombatEndurance.MAX_REGEN_LEVEL * 100f).roundToInt() + "%",
                        "" + Misc.getRoundedValueMaxOneAfterDecimal(CombatEndurance.REGEN_RATE * 100f) + "%",
                        "" + CombatEndurance.TOTAL_REGEN_MAX_POINTS.roundToInt() + "",
                        "" + (CombatEndurance.TOTAL_REGEN_MAX_HULL_FRACTION * 100f).roundToInt() + "%")
                    tooltip.addPara("    - Repair up to %s of armor per second; maximum total repair is " +
                            "the higher of %s armor points or %s of maximum armor",
                        0f,
                        Misc.getHighlightColor(),
                        "" + Misc.getRoundedValueMaxOneAfterDecimal(SotfPolarizedNanorepair.ARMOR_REGEN_RATE * 100f) + "%",
                        "" + SotfPolarizedNanorepair.TOTAL_ARMOR_REGEN_MAX_POINTS.roundToInt() + "",
                        "" + (SotfPolarizedNanorepair.TOTAL_ARMOR_REGEN_MAX_FRACTION * 100f).roundToInt()
                    )
                }

                SotfIDs.COTL_HULLSIPHON -> {
                    tooltip.addPara(
                        "While there are mimics within %s units of the flagship, reduce the damage it takes to hull and armor by %s " +
                                "and redirect %s of the damage reduced to mimics within range. Only redirects to mimics " +
                                "created by Invoke Her Blessing.",
                        0f, h, "" + SotfInvokeHerBlessingPlugin.SIPHON_RANGE.roundToInt(),
                        "" + (SotfInvokeHerBlessingPlugin.SIPHON_PERCENT * 100f).roundToInt() + "%",
                        "" + (SotfInvokeHerBlessingPlugin.SIPHON_MIMIC_DR * 100f).roundToInt()
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara(
                        "Prioritizes %s mimics, then those with the %s, then reflections created by %s.",
                        0f, h, "expiring", "lowest remaining lifespan", "\"Blessing of the Lake\""
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Reflected damage ignores the target mimic's shields.",
                        gray, 0f)
                }

                // TIER 5

                SotfIDs.COTL_BLESSINGOFTHEDAYDREAM -> {
                    tooltip.addPara(
                        "Mimics with a base deployment point cost of %s or higher spawn as a reflection*. " +
                                "This upgrade can't create a second reflection until the first is destroyed or is " +
                                "manually set to expire.",
                        0f, h, "" + SotfInvokeHerBlessingPlugin.BLESSING_DP_GATE.roundToInt(), "reflection*"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Reflections are special mimics that have no lifespan limit, no d-mods, and all of their " +
                            "skills are elite. If \"Perfect Replication\" is active, they spawn with 1-3 s-mods.",
                        0f, gray, Misc.getStoryDarkBrighterColor(), "elite", "s-mods")
                }

                SotfIDs.COTL_DREAMEATER -> {
                    tooltip.addPara(
                        "Prematurely expiring a mimic also repairs %s of the flagship's hull and armor based on " +
                                "the mimic's size. Repair is reduced proportional to the mimic's spent lifespan, to " +
                                "a minimum of %s of the base value.",
                        0f, h, "" + (SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_FRIGATE * 100f).roundToInt() + "%/" +
                            (SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_DESTROYER * 100f).roundToInt() + "%/" +
                                (SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_CRUISER * 100f).roundToInt() + "%/" +
                                (SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_CAPITAL * 100f).roundToInt() + "%",
                        "" + (SotfInvokeHerBlessingPlugin.DREAMEATER_REPAIR_MINIMUM * 100f).roundToInt() + "%"
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara(
                        "Also gain a \"Dream Eater\" subsystem that can be used on ship echoes to consume them and " +
                                "repair the flagship as if they were a full-lifespan mimic.",
                        0f, h, "\"Dream Eater\""
                    )
                    tooltip.addSpacer(opad)
                    tooltip.addPara("Consuming an echo also triggers \"Shriek of the Damned\" and \"Service Beyond Death\" if active.", gray,
                        0f)
                }
            }
        }
    }

    init {
        isImportant = true
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advanceImpl(amount: Float) {}

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: ListInfoMode) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f
        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad
        val tc = getBulletColorForMode(mode)
        bullet(info)
        val isUpdate = getListInfoParam() != null
        unindent(info)
    }

    override fun createLargeDescription(panel: CustomPanelAPI, width: Float, height: Float) {
        val opad = 10f

        val main = panel.createUIElement(width, height, true)
        main.setTitleOrbitronVeryLarge()
        main.addTitle(name, factionForUIColors.baseUIColor)
        main.addSpacer(opad)
        main.addSpacer(opad)
        val c = factionForUIColors.baseUIColor
        val bg = factionForUIColors.darkUIColor
        val b = factionForUIColors.brightUIColor
        val h = Misc.getHighlightColor()

        val sirius = SotfPeople.getPerson(SotfPeople.SIRIUS_MIMIC)

        val skills = main.beginSubTooltip(width * 0.45f)

        skills.addSectionHeading("Mimic Skills", c, bg, Alignment.MID, opad)
        skills.addImages(skills.widthSoFar, 128f, opad, opad, sirius.portraitSprite)
        skills.addPara("Sirius' mimics use the skillset below:", opad, Misc.getHighlightColor(), "Sirius")
        skills.addSkillPanel(sirius, opad)

        main.endSubTooltip()

        val capacity = main.beginSubTooltip(width * 0.5f)

        capacity.addSectionHeading("Invoke Her Blessing", c, bg, Alignment.MID, opad).position.setXAlignOffset(0f)
        capacity.addSpacer(10f)
        capacity.addPara("- Gain the %s subsystem in combat. Allied and enemy ships destroyed within %s range " +
                "of the flagship leave an %s for %s seconds. Using the subsystem on an echo orders Sirius to create " +
                "a %s that fights for your fleet.", 0f, h,
            "\"Invoke Her Blessing\"",
            "" + SotfInvokeHerBlessingPlugin.ECHO_CREATION_RANGE.roundToInt(),
            "echo",
            "" + SotfInvokeHerBlessingPlugin.ECHO_LIFETIME.roundToInt(),
            "mimic"
        )
        capacity.addSpacer(10f)
        capacity.addPara("- Mimics have a lifespan of %s/%s/%s/%s seconds based on their hull size, after " +
                "which they lose %s of their maximum hull per second.", 0f, h,
            "" + SotfInvokeHerBlessingPlugin.LIFESPAN_FRIGATE.roundToInt(),
            "" + SotfInvokeHerBlessingPlugin.LIFESPAN_DESTROYER.roundToInt(),
            "" + SotfInvokeHerBlessingPlugin.LIFESPAN_CRUISER.roundToInt(),
            "" + SotfInvokeHerBlessingPlugin.LIFESPAN_CAPITAL.roundToInt(),
            "" + (SotfInvokeHerBlessingPlugin.MIMIC_EXPIRE_RATE * 100f).roundToInt() + "%"
        )
        capacity.addSpacer(10f)
        capacity.addPara("- Mimics created with Invoke Her Blessing do not count against the fleet's deployment " +
                "point limit.", 0f
        )
        capacity.addSpacer(10f)
        capacity.addPara("- Sirius has a maximum mimic capacity of %s deployment points, increased to %s with " +
                "the %s upgrade (base value of %s; increased by %s for each level you gain)", 0f, h,
            "" + SotfInvokeHerBlessingSubsystem.getMimicCapacityTheoretical(false),
            "" + SotfInvokeHerBlessingSubsystem.getMimicCapacityTheoretical(true),
            "\"Multifaceted\"",
            "" + SotfInvokeHerBlessingPlugin.BASE_DP,
            "" + SotfInvokeHerBlessingPlugin.DP_PER_LEVEL
        )
        capacity.addSpacer(10f)
        capacity.addPara("- Expiring mimics and those created by means other than Invoke Her Blessing do not count " +
                "against Sirius' capacity.", 0f
        )
        capacity.addSpacer(10f)
        capacity.addPara("- Exceeding Sirius' capacity causes mimics to decay proportionally more quickly (at %s " +
                "the limit, mimics decay %s as quickly - minimum %s multiplier while overclocked)", 0f, h,
            "2x", "2x", "" + Misc.getRoundedValueMaxOneAfterDecimal(SotfInvokeHerBlessingPlugin.OVERCLOCK_MIN_RATE) + "x"
        )
        capacity.addSpacer(10f)
        capacity.addPara("Sirius' safety interlocks have been disabled and he can replicate all ship designs with very few exceptions.",
            Misc.getGrayColor(), 0f
        )
        capacity.heightSoFar = skills.heightSoFar

        main.endSubTooltip()

        main.addCustom(capacity, opad)
        main.addCustomDoNotSetPosition(skills).position.rightOfTop(capacity, opad)
        main.addSpacer(capacity.heightSoFar)

        val upgrades = main.beginSubTooltip(width * 0.95f)
        upgrades.addSpacer(10f)
        upgrades.addSectionHeading("Her Boons For The Worthy", c, bg, Alignment.MID, opad).position.setXAlignOffset(0f)
        upgrades.addPara("- Unlock a new tier of upgrades when you reach levels 3/6/9/12/15.", opad, h, "3", "6", "9", "12", "15")
        upgrades.addPara("- Can only pick 1 upgrade from each tier, but they can be reassigned freely.", opad, h, "1", "reassigned")
        //Widget Preview
        upgrades.addSpacer(10f)
        addSkillSection(upgrades, SotfNaniteSynthesized.COLOR_STRONGER)
        main.endSubTooltip()

        main.addCustomDoNotSetPosition(upgrades).position.belowLeft(capacity, opad * 2f)

        panel.addUIElement(main).inTL(0f, 0f)
    }

    fun getSkillSections() : ArrayList<SiriusSkillSection> {
        var playerLevel = Global.getSector().playerPerson.stats.level

        var sections = ArrayList<SiriusSkillSection>()
        var s1 = SiriusSkillSection(playerLevel >= 3, "technology1")
        addToSection(s1, SotfIDs.COTL_PERFECTREPLICATION, "Perfect Replication")
        addToSection(s1, SotfIDs.COTL_MULTIFACETED, "Multifaceted")
        sections.add(s1)

        var s2 = SiriusSkillSection(playerLevel >= 6, "technology2")
        addToSection(s2, SotfIDs.COTL_ENHANCEDCOUNTERMEASURES, "Enhanced Countermeasures")
        addToSection(s2, SotfIDs.COTL_SHRIEKOFTHEDAMNED, "Shriek of the Damned")
        addToSection(s2, SotfIDs.COTL_SERVICEBEYONDDEATH, "Service Beyond Death")
        sections.add(s2)

        var s3 = SiriusSkillSection(playerLevel >= 9, "technology3")
        addToSection(s3, SotfIDs.COTL_EVERYROSEITSTHORNS, "Every Rose Its Thorns")
        addToSection(s3, SotfIDs.COTL_UNLIVINGVIGOR, "Unliving Vigor")
        addToSection(s3, SotfIDs.COTL_DEATHTHROES, "Death Throes")
        sections.add(s3)

        var s4 = SiriusSkillSection(playerLevel >= 12, "technology3")
        addToSection(s4, SotfIDs.COTL_RECONSTITUTION, "Reconstitution")
        addToSection(s4, SotfIDs.COTL_HULLSIPHON, "Hull Siphon")
        sections.add(s4)

        var s5 = SiriusSkillSection(playerLevel >= 15, "technology4")
        addToSection(s5, SotfIDs.COTL_BLESSINGOFTHEDAYDREAM, "Blessing of the Daydream")
        addToSection(s5, SotfIDs.COTL_DREAMEATER, "Dream Eater")
        sections.add(s5)

        return sections
    }

    fun addToSection(section: SiriusSkillSection, id: String, name: String) {
        section.skills.add(SiriusSkillData(id,
            name,
            Global.getSettings().getSpriteName("skills", id.substring(1)),
            haveUpgrade(id)
        ))
    }

    //Gets called whenever Sirius skills have been changed from a click.
    fun onSkillsChanged(enabledSkill: SiriusSkillData, disabledSkills: List<SiriusSkillData>) {
        setUpgrade(enabledSkill.skillId, true)
        for (disabledSkill in disabledSkills) {
            setUpgrade(disabledSkill.skillId, false)
        }
        syncSiriusSkills()
    }

    fun addSkillSection(main: TooltipMakerAPI, color: Color) {

        var sections = getSkillSections()

        //Setup UI
        var w = 800f
        var h = 84f

        var backgroundWidth = 950f

        var skillPanel = Global.getSettings().createCustom(w, h, null)
        main.addCustom(skillPanel, 0f)

        var skillElement = skillPanel.createUIElement(w, h, false)
        skillPanel.addUIElement(skillElement)

        var background = AptitudeBackgroundElement(color, backgroundWidth, skillElement)
        background.position.inTL(0f, h/2)

        //Dumb positioning fix
        var placeholder = LunaElement(skillElement, 0f, 0f).position.inTL(5f, 12f)

        //Add every section to the UI
        var previous: CustomPanelAPI? = null
        for (section in sections) {

            var isFirstSection = sections.first() == section
            var isLastSection = sections.last() == section

            //Add every skill from every section
            for (skill in section.skills) {

                var isFirst = section.skills.last() == skill
                var isLast = section.skills.last() == skill

                var skillDisplay = SkillWidgetElement(skill.skillId, skill.isActive, section.isTierActive, skill.iconPath, color, skillElement, 60f, 60f)
                section.skillElements.add(skillDisplay)
                skillElement.addTooltipToPrevious(SiriusUpgradeTooltipCreator(skill.skillId, skill.skillName, section.isTierActive), TooltipMakerAPI.TooltipLocation.BELOW)

                skillDisplay.onClick {

                    if (!section.isTierActive) {
                        skillDisplay.playSound("ui_char_can_not_increase_skill_or_aptitude", 1f, 1f)
                        return@onClick
                    }

                    var disabledSkills = section.skills.filter { it.skillId != skill.skillId }
                    var enabledSkill = section.skills.find { it.skillId == skill.skillId }

                    for (other in section.skillElements) {
                        if (other.id == skillDisplay.id) {
                            skillDisplay.playSound("${section.soundId}")
                            other.activated = true
                        } else {
                            other.activated = false
                        }
                    }

                    onSkillsChanged(enabledSkill!!, disabledSkills)
                }


                if (previous != null) {
                    var gap = 3f
                    skillDisplay.position.rightOfTop(previous, gap)
                }

                previous = skillDisplay.elementPanel

                if (!isLast) {
                    var seperator = SkillSeperatorElement(color, skillElement)
                    seperator.elementPanel.position.rightOfTop(skillDisplay.elementPanel, 3f)
                    previous = seperator.elementPanel
                }

            }

            if (!isLastSection) {
                var gap = SkillGapElement(color, skillElement)
                gap.elementPanel.position.rightOfTop(previous, 0f)
                previous = gap.elementPanel
            }
        }



        /* val widget1 = SkillWidgetElement("perfect_replication", false, false, iconPath, color, skillElement, 72f, 72f)
         val widget2 = SkillWidgetElement("perfect_replication", false, true, iconPath, color, skillElement, 72f, 72f)
         widget2.elementPanel.position.rightOfTop(widget1.elementPanel, 10f)
         val widget3 = SkillWidgetElement("perfect_replication", true, true, iconPath, color, skillElement, 72f, 72f)
         widget3.elementPanel.position.rightOfTop(widget2.elementPanel, 10f)*/
    }

    class SiriusUpgradeTooltipCreator(var id: String, var name: String?, var isTierUnlocked: Boolean) : TooltipCreator {
        override fun isTooltipExpandable(tooltipParam: Any): Boolean {
            return false
        }

        override fun getTooltipWidth(tooltipParam: Any): Float {
            return 600f
        }

        override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
            val pad = 3f
            val opad = 10f
            tooltip.addPara(name, Misc.getHighlightColor(), 0f)
            tooltip.addSpacer(opad)
            createTooltipForUpgrade(tooltip, id)

            if (!isTierUnlocked) {
                tooltip.addSpacer(opad)
                tooltip.addPara("This tier is not unlocked yet.", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())
            }
        }
    }

    private fun haveUpgrade(id: String): Boolean {
        return Global.getSector().memoryWithoutUpdate.getBoolean(id)
    }

    private fun setUpgrade(id: String, new: Boolean) {
        Global.getSector().memoryWithoutUpdate.set(id, new)
    }

    private fun syncSiriusSkills() {
        var sirius = SotfPeople.getPerson(SotfPeople.SIRIUS_MIMIC)
        var countermeasures = 1f
        if (haveUpgrade(SotfIDs.COTL_ENHANCEDCOUNTERMEASURES)) countermeasures = 2f
        sirius.stats.setSkillLevel(SotfIDs.SKILL_ADVANCEDCOUNTERMEASURES, countermeasures)
        var nanorepair = 1f
        if (haveUpgrade(SotfIDs.COTL_RECONSTITUTION)) nanorepair = 2f
        sirius.stats.setSkillLevel(SotfIDs.SKILL_POLARIZEDNANOREPAIR, nanorepair)
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode) {
        val c = getTitleColor(mode)
        info.addPara(name, c, 0f)
        addBulletPoints(info, mode)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
    }

    override fun getIcon(): String? {
        return Global.getSettings().getSpriteName("sotf_characters", "sirius")
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_IMPORTANT)
        tags.add(Tags.INTEL_STORY)
        tags.add(SotfIDs.DREAMING_GESTALT)
        return tags
    }

    override fun getSortString(): String? {
        return "Boons of the Daydream"
    }

    override fun getName(): String? {
        return "Boons of the Daydream"
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(SotfIDs.DREAMING_GESTALT)
    }

    override fun getSmallDescriptionTitle(): String? {
        return name
    }

    override fun hasSmallDescription(): Boolean {
        return false
    }

    override fun hasLargeDescription(): Boolean {
        return true
    }

    override fun shouldRemoveIntel(): Boolean {
        return false
    }

    // don't show unless a Concord ship is in our fleet
    override fun isHidden(): Boolean {
        return !Global.getSector().memoryWithoutUpdate.contains(SotfIDs.MEM_COTL_START)
    }

    override fun getCommMessageSound(): String? {
        return getSoundMajorPosting()
    }
}