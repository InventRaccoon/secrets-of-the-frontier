package data.scripts.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.SotfNaniteSynthesized;
import data.subsystems.SotfNaniteDronesSubsystem;
import data.subsystems.SotfScrapscreenDronesSubsystem;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static data.scripts.weapons.SotfLethargyOnFireEffect.lethargyFakeBeam;

public class SotfSalvorsScrapscreen {

	public static class AblativeArmor extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MagicSubsystemsManager.addSubsystemToShip(ship, new SotfScrapscreenDronesSubsystem(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MagicSubsystemsManager.removeSubsystemFromShip(ship, SotfScrapscreenDronesSubsystem.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
											TooltipMakerAPI info, float width) {
			initElite(stats, skill);

			info.addPara("Deploys a ring of heavily armored debris pieces to protect the ship", hc, 0f
			);
		}
	}
}
