

uniform sampler2D tex;
uniform sampler2D noiseTex1;
uniform sampler2D noiseTex2;
uniform sampler2D damageTex;

uniform float iTime;
uniform float alphaMult;
uniform float shieldLevel;
uniform vec3 shieldColor;
uniform vec2 size;

vec2 texCoord = gl_TexCoord[0].xy;

float MAX_DAMAGE_INSTANCES = 16;
uniform vec4 damageInfo[16];

//float intensity = 1.25;
uniform float intensity;

void main() {

	vec2 uv = texCoord / size;

	float noiseX = texCoord.x - iTime;
	float noiseY = texCoord.y - iTime;
	vec2 offset = vec2(noiseX, noiseY);

	vec4 color = texture2D(tex, texCoord);
	vec4 noiseCol1 = texture2D(noiseTex1, vec2(texCoord.x - iTime, texCoord.y - iTime * 0.5));
	vec4 noiseCol2 = texture2D(noiseTex2, vec2(texCoord.x - iTime, texCoord.y - iTime));
	vec4 damCol = texture2D(damageTex, texCoord);

	if (color.a > 0.5) {
		float brigtness1 = (noiseCol1.r + noiseCol1.g + noiseCol1.b) / 3.0 ;
		float brigtness2 = (noiseCol2.r + noiseCol2.g + noiseCol2.b) / 3.0 ;

		float scaled1 = brigtness1 * brigtness1 * brigtness1  * brigtness1;
		float scaled2 = brigtness2 * brigtness2 * brigtness2 * brigtness2 * brigtness2;

		float mixed = (scaled1 + scaled2) / 2.0;

		color.r = shieldColor.r;
		color.g = shieldColor.g;
		color.b = shieldColor.b;

		//color.r += scaled2 * 1.0 * intensity;
		//color.g += scaled2 * 1.0 * level * intensity;
		//color.b += scaled2 * 1.0 * (1.0-level) * intensity;

		//color.a = (color.r + color.g + color.b);
		

		float level = (((brigtness1 + brigtness2) / 2.0) - 0.55) / (1.0 - 0.55);
		color.a = level * intensity;


		for(int i=0;i<MAX_DAMAGE_INSTANCES;++i)
		{

			float additionalAlpha = 0.0;

			//If the radius (z) is 0, then this element doesnt exit or is not worth iterating over
			if (damageInfo[i].z != 0) {
				vec2 pos = damageInfo[i].xy;
				float radius = damageInfo[i].z;
				float damageLevel = damageInfo[i].a;

				float dist = distance(pos, uv);

				float minimum = 0;
				float maximum = radius;

				float distLevel = (dist - minimum) / (maximum - minimum);
				distLevel = clamp(distLevel, 0.0, 1.0);
				distLevel = 1 - distLevel;

				//color.a *= distLevel;

				//color.r += 1 * distLevel;

  				//color.a += 0.1;

				additionalAlpha += 1 * distLevel * damageLevel;
				additionalAlpha = clamp(additionalAlpha, 0.0, 1.0);
			}

			if (additionalAlpha != 0) {
				float brightnessDem = (damCol.r + damCol.g + damCol.b) / 3.0;

				color.r += (0.3 + shieldColor.r) * brightnessDem * additionalAlpha * additionalAlpha * additionalAlpha * 1;
				color.g += (0.1 + shieldColor.g) * brightnessDem * additionalAlpha * additionalAlpha * additionalAlpha * 1;
				color.b += shieldColor.b * brightnessDem * additionalAlpha * brightnessDem * brightnessDem * 1;

				color.r += 2 * additionalAlpha;

				color.a += additionalAlpha * 0.5;
			}

		}
	} 
	else {
		color.a = 0.0;
	}

	color.a *= alphaMult;


	color.a *= shieldLevel;


	gl_FragColor = color;
	//gl_FragColor = damCol;

	//if (uv.x >= 0.5) {
	//	gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
	//} else {
	//	gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);
	//}

}