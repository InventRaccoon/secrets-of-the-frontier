package data.scripts.campaign.secondInCommand.misc

import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import data.scripts.campaign.secondInCommand.AgonyEncore
import data.scripts.campaign.secondInCommand.TendToOurGarden
import data.scripts.utils.SotfMisc
import second_in_command.SCUtils
import kotlin.math.min

//Added to all ships in combat through the base origin skill
class WitchcraftKillingBlowHandler : DamageListener {

    var wasShipAlive = HashMap<ShipAPI, Boolean>()

    override fun reportDamageApplied(source: Any?, target: CombatEntityAPI?, result: ApplyDamageResultAPI?) {
        //if (!SotfMisc.isSecondInCommandEnabled()) return

        if (target !is ShipAPI || source !is ShipAPI) {
            return
        }

        if (source.owner != 0) return
        if (source.isStationModule) return
        if (target.isFighter) return

        //Add all ships encountered, set the default based on if the first ever damage was against a hulk.
        if (!wasShipAlive.contains(target)) {
            wasShipAlive.put(target, !target.isHulk)
        }

        if (target.hitpoints <= 0 && wasShipAlive.get(target) == true) {
            wasShipAlive.put(target, false)

            var data = SCUtils.getPlayerData()

            if (data.isSkillActive("sotf_agony_encore")) {

                var listener = source.getListeners(AgonyEncore.AgonyEncoreListener::class.java).firstOrNull()
                if (listener == null) {
                    source.addListener(AgonyEncore.AgonyEncoreListener(source))
                } else {
                    listener.duration = min(15f, listener.duration + 10f);
                }

            } else if (data.isSkillActive("sotf_tend_to_our_garden")) {
                var listener = source.getListeners(TendToOurGarden.TendToOurGardenListener::class.java).firstOrNull()

                var count = when(target.hullSize) {
                    ShipAPI.HullSize.FRIGATE -> 2
                    ShipAPI.HullSize.DESTROYER -> 2
                    ShipAPI.HullSize.CRUISER -> 3
                    ShipAPI.HullSize.CAPITAL_SHIP -> 4
                    else -> 0
                }

                listener?.spawnWisp(target, count)
            }

            //println("${target.name} killed by ${source.name}")
        }


    }
}