

uniform sampler2D tex;
uniform sampler2D noiseTex1;

vec2 texCoord = gl_TexCoord[0].xy;

uniform float level;
uniform float iTime;
uniform vec3 colorMult;

void main() {


	vec4 col = texture2D(tex, texCoord);
	vec4 noiseCol1 = texture2D(noiseTex1, vec2(texCoord.x - iTime * 0.45, texCoord.y - iTime * 0.45));
	vec4 noiseCol2 = texture2D(noiseTex1, vec2(texCoord.x - iTime, texCoord.y - iTime));

	//col.g = texture2D(tex, texCoord + offset).g;
	//col.r = texture2D(tex, texCoord).r;
	//col.b = texture2D(tex, texCoord - offset).b;

	float brightness = (col.r + col.g + col.b) / 3;
	vec4 brightCol = vec4(brightness, brightness, brightness, col.a);

	float noiseBrightness1 = (noiseCol1.r + noiseCol1.g + noiseCol1.b) / 3;
	float noiseBrightness2 = (noiseCol1.r + noiseCol1.g + noiseCol1.b) / 3;

	float noiseBright = (noiseBrightness1 + noiseBrightness2) / 2.0;

	col.b *= 1.0 + (1.5 * level);
	col.b += 0.2 * level;

	col.g *= 1.0 + (1 * level);
	col.g += 0.1 * level;

	col.b += 0.4 * noiseBright * level;
	col.g += 0.4 * noiseBright * level;
	col.b *= 1 + (0.5 * noiseBright * level);
	col.g *= 1 + (0.2 * noiseBright * level);

	col.r *= colorMult.r;
	col.g *= colorMult.g;
	col.b *= colorMult.b; 

	col = mix(col, vec4(brightCol), 0 + (0.80*level));

	gl_FragColor = col;

}