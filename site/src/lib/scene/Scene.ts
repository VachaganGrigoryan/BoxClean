import {
  AdditiveBlending,
  BufferAttribute,
  BufferGeometry,
  CanvasTexture,
  Color,
  Fog,
  Mesh,
  MeshStandardMaterial,
  NeutralToneMapping,
  PerspectiveCamera,
  PlaneGeometry,
  Points,
  RectAreaLight,
  Scene as ThreeScene,
  ShaderMaterial,
  SRGBColorSpace,
  type Texture,
  TextureLoader,
  Vector2,
  Vector3,
  VideoTexture,
  WebGLRenderer,
  HemisphereLight,
  MeshBasicMaterial,
} from "three";
import { RoundedBoxGeometry } from "three/addons/geometries/RoundedBoxGeometry.js";
import { RectAreaLightUniformsLib } from "three/addons/lights/RectAreaLightUniformsLib.js";
import { EffectComposer } from "three/addons/postprocessing/EffectComposer.js";
import { OutputPass } from "three/addons/postprocessing/OutputPass.js";
import { RenderPass } from "three/addons/postprocessing/RenderPass.js";
import { UnrealBloomPass } from "three/addons/postprocessing/UnrealBloomPass.js";
import type { Frame, Pose, Screen } from "../timeline";
import { ClockDream, DisableRun, GitLog, InstallStep } from "./canvases";
import { dustFrag, dustVert, glowFrag, screenFrag, screenVert } from "./shaders";

const TV_W = 3.2;
const TV_H = 1.8;
const FLOOR_Y = -1.5;
const ROOM = new Color("#0B1020");
const SHOTS = ["stock", "v1", "v2", "bento", "ribbon", "spotlight", "editorial", "rail", "classic"];
const WIPE_MS = 750;

export interface SceneOptions {
  mobile: boolean;
  reduced: boolean;
  media: string;
}

type ScreenKeys = { a: string; b: string; mix: number };

/** The one TV the whole page is told on: a floating panel, its light on the room, a Mi Box on the floor. */
export class Scene {
  private renderer: WebGLRenderer;
  private scene = new ThreeScene();
  private camera = new PerspectiveCamera(30, 1, 0.1, 80);
  private composer?: EffectComposer;
  private screenMat: ShaderMaterial;
  private glowMat: ShaderMaterial;
  private dust?: Points<BufferGeometry, ShaderMaterial>;
  private floorLight: RectAreaLight;
  private led: MeshBasicMaterial;
  private boxMats: (MeshStandardMaterial | MeshBasicMaterial)[] = [];
  private boxFade = 1;

  private textures = new Map<string, Texture>();
  private glows = new Map<string, Texture>();
  private colours = new Map<string, Color>();
  private video: HTMLVideoElement;
  private disable = new DisableRun();
  private gitlog = new GitLog();
  private install = new InstallStep();
  private clock = new ClockDream();

  private target?: Frame;
  private pose?: Pose;
  private shown: ScreenKeys = { a: "video", b: "video", mix: 0 };
  private anim?: { from: string; to: string; start: number };
  private pending?: Screen;
  private pointer = new Vector2();
  private pointerSmooth = new Vector2();
  private raf = 0;
  private last = performance.now();
  private size = new Vector2();
  private disposed = false;

  constructor(
    private canvas: HTMLCanvasElement,
    private opts: SceneOptions,
  ) {
    this.renderer = new WebGLRenderer({ canvas, antialias: true, powerPreference: "high-performance" });
    this.renderer.setPixelRatio(Math.min(devicePixelRatio, opts.mobile ? 1.5 : 1.75));
    this.renderer.toneMapping = NeutralToneMapping;
    this.renderer.outputColorSpace = SRGBColorSpace;
    this.scene.background = ROOM;
    this.scene.fog = new Fog(ROOM, 9, 26);
    RectAreaLightUniformsLib.init();

    this.loadTextures();
    this.video = this.makeVideo();

    const uniforms = () => ({
      mapA: { value: this.tex("video") },
      mapB: { value: this.tex("video") },
      wipe: { value: 0 },
    });
    this.screenMat = new ShaderMaterial({
      uniforms: { ...uniforms(), power: { value: 0.9 } },
      vertexShader: screenVert,
      fragmentShader: screenFrag,
    });
    this.glowMat = new ShaderMaterial({
      uniforms: {
        ...uniforms(),
        mapA: { value: this.glow("video") },
        mapB: { value: this.glow("video") },
        strength: { value: opts.mobile ? 0.7 : 0.85 },
        spread: { value: new Vector2(1.9, 1.9) },
      },
      vertexShader: screenVert,
      fragmentShader: glowFrag,
      blending: AdditiveBlending,
      depthWrite: false,
      transparent: true,
    });

    // the panel: a satin slab with the screen just proud of its face, and its light spilling behind
    const slab = new Mesh(
      new RoundedBoxGeometry(TV_W + 0.03, TV_H + 0.03, 0.05, 4, 0.018),
      new MeshStandardMaterial({ color: "#10131D", roughness: 0.35, metalness: 0.7 }),
    );
    slab.position.z = -0.03;
    const screen = new Mesh(new PlaneGeometry(TV_W, TV_H), this.screenMat);
    const glow = new Mesh(new PlaneGeometry(TV_W * 2.6, TV_H * 3), this.glowMat);
    glow.position.z = -0.4;
    this.scene.add(glow, slab, screen);

    // the room: a dark satin floor lit by the screen itself
    const floor = new Mesh(
      new PlaneGeometry(80, 80),
      // emissive = the room colour, so the unlit floor melts into the background instead of showing a horizon
      new MeshStandardMaterial({ color: "#0D1326", emissive: ROOM, roughness: 0.42, metalness: 0.15 }),
    );
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = FLOOR_Y;
    this.floorLight = new RectAreaLight("#ffffff", 7, TV_W, TV_H);
    this.floorLight.position.set(0, 0, 0.02);
    this.floorLight.lookAt(0, 0, 5);
    this.scene.add(floor, this.floorLight, new HemisphereLight("#8090c0", "#000000", 0.25));

    // the Mi Box S: a small rounded puck in front of the panel, with its white LED
    const boxMat = new MeshStandardMaterial({ color: "#0A0A0C", roughness: 0.55, metalness: 0.2, transparent: true });
    const box = new Mesh(new RoundedBoxGeometry(0.34, 0.06, 0.34, 4, 0.028), boxMat);
    box.position.set(-0.7, FLOOR_Y + 0.03, 1.1);
    box.rotation.y = 0.35;
    this.led = new MeshBasicMaterial({ color: "#ffffff", transparent: true });
    const led = new Mesh(new PlaneGeometry(0.012, 0.006), this.led);
    led.position.set(0, 0.0, 0.1705);
    box.add(led);
    const shadowMat = new MeshBasicMaterial({ map: radialShadow(), transparent: true, depthWrite: false });
    const shadow = new Mesh(new PlaneGeometry(0.9, 0.9), shadowMat);
    this.boxMats = [boxMat, this.led, shadowMat];
    shadow.rotation.x = -Math.PI / 2;
    shadow.position.set(-0.7, FLOOR_Y + 0.002, 1.1);
    this.scene.add(shadow, box);

    if (!opts.mobile && !opts.reduced) this.dust = this.makeDust();

    if (!opts.mobile) {
      this.composer = new EffectComposer(this.renderer);
      this.composer.addPass(new RenderPass(this.scene, this.camera));
      this.composer.addPass(new UnrealBloomPass(new Vector2(256, 256), 0.32, 0.55, 0.9));
      this.composer.addPass(new OutputPass());
    }

    addEventListener("pointermove", this.onPointer, { passive: true });
    document.fonts?.ready.then(() => [this.disable, this.gitlog, this.install].forEach((t) => t.invalidate()));
    this.resize();
    this.raf = requestAnimationFrame(this.tick);
  }

  setFrame(f: Frame) {
    this.target = f;
  }

  resize() {
    const w = this.canvas.clientWidth;
    const h = this.canvas.clientHeight;
    if (!w || !h) return;
    this.size.set(w, h);
    this.renderer.setSize(w, h, false);
    this.composer?.setSize(w, h);
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
  }

  dispose() {
    this.disposed = true;
    cancelAnimationFrame(this.raf);
    removeEventListener("pointermove", this.onPointer);
    this.video.pause();
    this.video.removeAttribute("src");
    this.renderer.dispose();
  }

  private onPointer = (e: PointerEvent) => {
    this.pointer.set((e.clientX / innerWidth) * 2 - 1, (e.clientY / innerHeight) * 2 - 1);
  };

  private tick = (now: number) => {
    if (this.disposed) return;
    this.raf = requestAnimationFrame(this.tick);
    const dt = Math.min(0.1, (now - this.last) / 1000);
    this.last = now;
    const f = this.target;
    if (!f) return;

    // pose: damped towards the scroll target, plus a little parallax from the pointer
    const k = this.opts.reduced ? 1 : 1 - Math.exp(-dt * 3.2);
    this.pose = this.pose ? lerpPose(this.pose, f.pose, k) : { ...f.pose };
    this.pointerSmooth.lerp(this.pointer, this.opts.reduced || this.opts.mobile ? 0 : 1 - Math.exp(-dt * 2));
    this.place(this.pose);

    this.updateScreen(f.screen, now);
    const key = this.anim ? this.anim.to : dominant(this.shown);
    if (key === "video") {
      if (this.video.paused) this.video.play().catch(() => {});
    } else if (!this.video.paused) this.video.pause();
    for (const k of [this.shown.a, this.shown.b]) this.updateCanvas(k, f.screen.term, now);

    // the box steps aside where page text sits over the floor in front of the TV
    this.boxFade += ((f.section === "footer" || f.section === "layouts" ? 0 : 1) - this.boxFade) * k;
    for (const m of this.boxMats) m.opacity = this.boxFade;
    this.led.color.setScalar(f.section === "tools" && f.screen.term < 1 ? (Math.sin(now / 90) > 0 ? 1 : 0.15) : 0.9);

    if (this.dust) this.dust.material.uniforms.time.value = now / 1000;
    if (this.composer) this.composer.render();
    else this.renderer.render(this.scene, this.camera);
  };

  /** Orbit the focus point, frame the TV at the pose's size, then shift the lens to move it on screen. */
  private place(p: Pose) {
    const cam = this.camera;
    const fov = (cam.fov * Math.PI) / 180;
    const d = TV_W / (p.size * 2 * Math.tan(fov / 2) * cam.aspect);
    const yaw = p.yaw + this.pointerSmooth.x * 0.05;
    const pitch = p.pitch + this.pointerSmooth.y * 0.025;
    const focus = new Vector3(0, p.focusY, 0);
    cam.position.set(
      focus.x + d * Math.sin(yaw) * Math.cos(pitch),
      focus.y + d * Math.sin(pitch),
      focus.z + d * Math.cos(yaw) * Math.cos(pitch),
    );
    cam.lookAt(focus);
    const { x: w, y: h } = this.size;
    cam.setViewOffset(w, h, (-p.ox * w) / 2, (p.oy * h) / 2, w, h);
  }

  /**
   * Scroll-scrubbed wipes set the mix directly. Any other change of picture (a new section, a layout pick)
   * runs a timed wipe from what is showing to what should show.
   */
  private updateScreen(s: Screen, now: number) {
    if (this.anim) {
      const t = this.opts.reduced ? 1 : (now - this.anim.start) / WIPE_MS;
      this.apply(this.anim.from, this.anim.to, ease(t));
      if (t < 1) {
        this.pending = s;
        return;
      }
      this.shown = { a: this.anim.to, b: this.anim.to, mix: 0 };
      this.anim = undefined;
      s = this.pending ?? s;
    }
    if (s.a === this.shown.a && s.b === this.shown.b) {
      this.shown.mix = s.mix;
    } else {
      const from = dominant(this.shown);
      const to = dominant(s);
      if (from !== to) {
        this.anim = { from, to, start: now };
        this.apply(from, to, 0);
        return;
      }
      this.shown = { a: s.a, b: s.b, mix: s.mix };
    }
    this.apply(this.shown.a, this.shown.b, this.shown.mix);
  }

  private apply(a: string, b: string, mix: number) {
    const u = this.screenMat.uniforms;
    u.mapA.value = this.tex(a);
    u.mapB.value = this.tex(b);
    u.wipe.value = mix;
    const g = this.glowMat.uniforms;
    g.mapA.value = this.glow(a);
    g.mapB.value = this.glow(b);
    g.wipe.value = mix;
    this.floorLight.color.copy(this.colour(a)).lerp(this.colour(b), mix);
    if (this.dust) this.dust.material.uniforms.color.value.copy(this.floorLight.color).lerp(new Color("#ffffff"), 0.5);
  }

  private updateCanvas(key: string, term: number, now: number) {
    if (key === "term-tools") this.disable.update(term, now);
    else if (key === "term-git") this.gitlog.update(term, now);
    else if (key === "term-install") this.install.update(term, now);
    else if (key === "clock") this.clock.update(0, now);
  }

  private tex(key: string): Texture {
    if (key === "term-tools") return this.disable.texture;
    if (key === "term-git") return this.gitlog.texture;
    if (key === "term-install") return this.install.texture;
    if (key === "clock") return this.clock.texture;
    // the video shows the Bento shot until its first frame arrives
    if (key === "video" && this.video.readyState < 2) return this.textures.get("bento")!;
    return this.textures.get(key) ?? this.textures.get("bento")!;
  }

  private glow(key: string): Texture {
    return this.glows.get(key === "video" ? "bento" : key) ?? this.glows.get("term")!;
  }

  private colour(key: string): Color {
    return this.colours.get(key === "video" ? "bento" : key) ?? this.colours.get("term")!;
  }

  private loadTextures() {
    const loader = new TextureLoader();
    const aniso = this.renderer.capabilities.getMaxAnisotropy();
    for (const key of SHOTS) {
      const t = loader.load(`${this.opts.media}/${key}.jpg`);
      t.colorSpace = SRGBColorSpace;
      t.anisotropy = aniso;
      this.textures.set(key, t);
      const g = loader.load(`${this.opts.media}/glow-${key}.jpg`, (tex) => {
        this.colours.set(key, averageColour(tex.image as HTMLImageElement));
      });
      g.colorSpace = SRGBColorSpace;
      this.glows.set(key, g);
      this.colours.set(key, new Color("#30385a"));
    }
    this.glows.set("term", solid("#1c2850"));
    this.colours.set("term", new Color("#3a4c8a"));
  }

  private makeVideo() {
    const v = document.createElement("video");
    Object.assign(v, { src: `${this.opts.media}/demo.mp4`, muted: true, loop: true, playsInline: true, crossOrigin: "anonymous" });
    v.setAttribute("muted", "");
    const t = new VideoTexture(v);
    t.colorSpace = SRGBColorSpace;
    this.textures.set("video", t);
    return v;
  }

  private makeDust() {
    const n = 140;
    const pos = new Float32Array(n * 3);
    const seed = new Float32Array(n);
    for (let i = 0; i < n; i++) {
      pos.set([(Math.random() - 0.5) * 4.4, -1.1 + Math.random() * 2.4, 0.25 + Math.random() * 2], i * 3);
      seed[i] = Math.random();
    }
    const geo = new BufferGeometry();
    geo.setAttribute("position", new BufferAttribute(pos, 3));
    geo.setAttribute("seed", new BufferAttribute(seed, 1));
    const mat = new ShaderMaterial({
      uniforms: { time: { value: 0 }, scale: { value: 14 * this.renderer.getPixelRatio() }, color: { value: new Color("#c8d0ff") } },
      vertexShader: dustVert,
      fragmentShader: dustFrag,
      blending: AdditiveBlending,
      depthWrite: false,
      transparent: true,
    });
    const pts = new Points(geo, mat);
    this.scene.add(pts);
    return pts;
  }
}

const dominant = (s: { a: string; b: string; mix: number }) => (s.mix >= 0.5 ? s.b : s.a);
const ease = (t: number) => {
  const x = Math.min(1, Math.max(0, t));
  return x < 0.5 ? 4 * x * x * x : 1 - (-2 * x + 2) ** 3 / 2;
};

function lerpPose(a: Pose, b: Pose, k: number): Pose {
  const l = (x: number, y: number) => x + (y - x) * k;
  return { yaw: l(a.yaw, b.yaw), pitch: l(a.pitch, b.pitch), size: l(a.size, b.size), ox: l(a.ox, b.ox), oy: l(a.oy, b.oy), focusY: l(a.focusY, b.focusY) };
}

/** Average colour of a small image: draw it into one pixel and read that pixel back. */
function averageColour(img: HTMLImageElement): Color {
  const c = document.createElement("canvas");
  c.width = c.height = 1;
  const ctx = c.getContext("2d", { willReadFrequently: true })!;
  ctx.drawImage(img, 0, 0, 1, 1);
  const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
  // brighten: the light should carry the shot's hue, not its darkness
  const col = new Color().setRGB(r / 255, g / 255, b / 255, SRGBColorSpace);
  const hsl = { h: 0, s: 0, l: 0 };
  col.getHSL(hsl);
  return col.setHSL(hsl.h, Math.min(1, hsl.s * 1.3), Math.max(0.35, hsl.l));
}

function solid(hex: string): CanvasTexture {
  const c = document.createElement("canvas");
  c.width = c.height = 2;
  const ctx = c.getContext("2d")!;
  ctx.fillStyle = hex;
  ctx.fillRect(0, 0, 2, 2);
  const t = new CanvasTexture(c);
  t.colorSpace = SRGBColorSpace;
  return t;
}

function radialShadow(): CanvasTexture {
  const c = document.createElement("canvas");
  c.width = c.height = 128;
  const ctx = c.getContext("2d")!;
  const g = ctx.createRadialGradient(64, 64, 8, 64, 64, 64);
  g.addColorStop(0, "rgba(0,0,0,0.75)");
  g.addColorStop(1, "rgba(0,0,0,0)");
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, 128, 128);
  return new CanvasTexture(c);
}
