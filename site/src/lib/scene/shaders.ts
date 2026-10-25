/** The panel: shot a wiped left-to-right into shot b, with a bright edge while the wipe runs. */
export const screenVert = /* glsl */ `
varying vec2 vUv;
void main() {
  vUv = uv;
  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
}`;

export const screenFrag = /* glsl */ `
uniform sampler2D mapA;
uniform sampler2D mapB;
uniform float wipe;
uniform float power;
varying vec2 vUv;
void main() {
  vec3 col = mix(texture2D(mapA, vUv).rgb, texture2D(mapB, vUv).rgb, step(vUv.x, wipe));
  float running = step(0.0005, wipe) * step(wipe, 0.9995);
  float d = abs(vUv.x - wipe);
  col += running * (vec3(2.4) * exp(-d * 1400.0) + vec3(0.55, 0.65, 1.0) * exp(-d * 45.0) * 0.28);
  // faint glass sheen across the top-left of the panel
  col += vec3(0.035) * smoothstep(0.35, 0.0, abs(vUv.x * 0.7 - vUv.y * 0.9 + 0.35));
  gl_FragColor = vec4(col * power, 1.0);
  #include <tonemapping_fragment>
  #include <colorspace_fragment>
}`;

/** The light behind the panel: the shot's tiny blurred copy, spread out and faded at the rim. */
export const glowFrag = /* glsl */ `
uniform sampler2D mapA;
uniform sampler2D mapB;
uniform float wipe;
uniform float strength;
uniform vec2 spread;
varying vec2 vUv;
void main() {
  vec2 p = (vUv - 0.5) * spread + 0.5;
  vec2 q = clamp(p, 0.0, 1.0);
  vec3 col = mix(texture2D(mapA, q).rgb, texture2D(mapB, q).rgb, smoothstep(wipe + 0.15, wipe - 0.15, q.x));
  vec2 e = (vUv - 0.5) * 2.0;
  float fall = pow(clamp(1.0 - length(e * vec2(0.92, 1.0)), 0.0, 1.0), 1.6);
  gl_FragColor = vec4(col * fall * strength, 1.0);
  #include <colorspace_fragment>
}`;

/** Dust drifting through the light. */
export const dustVert = /* glsl */ `
uniform float time;
uniform float scale;
attribute float seed;
varying float vFade;
void main() {
  vec3 p = position;
  p.y += mod(time * 0.035 * (0.5 + seed) + seed * 3.0, 3.0) - 1.5;
  p.x += sin(time * 0.2 + seed * 12.0) * 0.08;
  vec4 mv = modelViewMatrix * vec4(p, 1.0);
  gl_Position = projectionMatrix * mv;
  gl_PointSize = scale * (0.6 + seed) / -mv.z;
  // motes catch the light only near the screen
  float near = smoothstep(2.6, 0.4, length(p.xy * vec2(0.7, 1.0)));
  vFade = near * (0.4 + 0.6 * (sin(time * 0.6 + seed * 20.0) * 0.5 + 0.5));
}`;

export const dustFrag = /* glsl */ `
uniform vec3 color;
varying float vFade;
void main() {
  float d = length(gl_PointCoord - 0.5);
  float a = smoothstep(0.5, 0.0, d) * vFade * 0.3;
  gl_FragColor = vec4(color * a, 1.0);
}`;
