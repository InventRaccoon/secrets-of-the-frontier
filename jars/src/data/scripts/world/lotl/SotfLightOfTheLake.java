package data.scripts.world.lotl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager;

import java.awt.*;
import java.util.Random;

public class SotfLightOfTheLake {

	public void generate(SectorAPI sector) {
		
		StarSystemAPI system = sector.createStarSystem("Light of the Lake");
		system.setOptionalUniqueId("sotf_lotl");
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag("sotf_lotl");
		float w = Global.getSettings().getFloat("sectorWidth");
		float h = Global.getSettings().getFloat("sectorHeight");
		system.getLocation().set(0f, h/2f + 4500f);
		
		
		LocationAPI hyper = Global.getSector().getHyperspace();
		
		system.setBackgroundTextureFilename("graphics/backgrounds/wormhole_dest_black.jpg");
		
		
		// create the star and generate the hyperspace anchor for this system
		PlanetAPI star = system.initStar("sotf_lightofthelake", // unique id for this star
										    StarTypes.WHITE_DWARF,  // id in planets.json
										    600f, 		  // radius (in pixels at default zoom)
										    350f); // corona radius, from star edge
		
		
		system.setLightColor(new Color(155, 155, 155)); // light color in entire system, affects all entities
		
		Random random = StarSystemGenerator.random;
		
		PlanetAPI planet = system.addPlanet("sotf_elysium", star, "Elysium", "sotf_elysium", 215, 110, 2300, 75);
		planet.getMemoryWithoutUpdate().set("$sotf_elysium", true);
		//planet.setCustomDescriptionId("sotf_elysium");
		planet.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
		planet.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		planet.getMarket().addCondition(Conditions.ORE_RICH);
		planet.getMarket().addCondition(Conditions.RARE_ORE_RICH);
		planet.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);
		planet.getMarket().addCondition(Conditions.VERY_COLD);

		planet.getSpec().setGlowTexture(Global.getSettings().getSpriteName("hab_glows", "barren"));
		planet.getSpec().setGlowColor(new Color(205, 255, 255,255));
		planet.getSpec().setUseReverseLightForGlow(true);
		planet.applySpecChanges();

		StarSystemGenerator.addStableLocations(system, 1);

		for (SectorEntityToken curr : system.getEntitiesWithTag(Tags.STABLE_LOCATION)) {
			SpecialItemData item = WormholeManager.createWormholeAnchor("sotf_lotl", "sotf_acheron", "Acheron");
			JumpPointAPI wormhole = WormholeManager.get().addWormhole(item, curr, null);
			wormhole.getMemoryWithoutUpdate().unset(JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY);
			break;
		}

		system.autogenerateHyperspaceJumpPoints(true, true);

		AbyssalRogueStellarObjectEPEC.setAbyssalDetectedRanges(system);
	}
}










