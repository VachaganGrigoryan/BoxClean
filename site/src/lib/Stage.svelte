<script lang="ts">
  import { onMount } from "svelte";
  import { CHAPTERS } from "./data";
  import type { Scene } from "./scene/Scene";
  import { frame, type SectionBox, type SectionId } from "./timeline";
  import { ui } from "./ui.svelte";

  let canvas: HTMLCanvasElement;
  let ready = $state(false);
  let fallback = $state(false);
  let update = () => {};

  // without WebGL the page still shows the right picture: the shot for what is in view
  const still = $derived(
    ui.section === "story" ? CHAPTERS[ui.step].shot : ui.section === "layouts" ? ui.layout : ui.section === "rebuilt" ? "classic" : "bento",
  );

  onMount(() => {
    const phone = matchMedia("(max-width: 860px)");
    const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
    let boxes: SectionBox[] = [];
    let scene: Scene | undefined;

    const measure = () => {
      boxes = [...document.querySelectorAll<HTMLElement>("[data-scene]")].map((el) => {
        const r = el.getBoundingClientRect();
        return { id: el.dataset.scene as SectionId, top: r.top + scrollY, height: r.height };
      });
    };
    update = () => {
      if (!boxes.length) return;
      const f = frame(boxes, scrollY, innerHeight, phone.matches, ui.layout);
      ui.section = f.section;
      ui.step = f.step;
      scene?.setFrame(f);
    };
    const start = async () => {
      scene?.dispose();
      scene = undefined;
      try {
        const { Scene } = await import("./scene/Scene");
        scene = new Scene(canvas, { mobile: phone.matches, reduced, media: "/media" });
        update();
        ready = true;
      } catch (e) {
        console.warn("3D stage unavailable, showing stills", e);
        fallback = true;
      }
    };

    const ro = new ResizeObserver(() => {
      measure();
      scene?.resize();
      update();
    });
    ro.observe(document.body);
    ro.observe(canvas);
    addEventListener("scroll", update, { passive: true });
    // the phone and desktop stages differ (canvas size, bloom, dust), so crossing the breakpoint rebuilds it
    phone.addEventListener("change", start);
    measure();
    update();
    if (document.createElement("canvas").getContext("webgl2")) start();
    else fallback = true;

    return () => {
      ro.disconnect();
      removeEventListener("scroll", update);
      phone.removeEventListener("change", start);
      scene?.dispose();
    };
  });

  $effect(() => {
    void ui.layout;
    update();
  });
</script>

<canvas bind:this={canvas} class:ready class:hidden={fallback} aria-hidden="true"></canvas>
{#if fallback}
  <img class="still" src="/media/{still}.jpg" alt="" />
{/if}

<style>
  canvas,
  .still {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100lvh;
    z-index: 0;
    pointer-events: none;
  }
  canvas {
    opacity: 0;
    transition: opacity 1.2s ease;
  }
  canvas.ready {
    opacity: 1;
  }
  canvas.hidden {
    display: none;
  }
  .still {
    inset: 50% 6vw auto auto;
    width: 46vw;
    height: auto;
    translate: 0 -50%;
    border-radius: 12px;
    box-shadow: 0 0 120px 20px rgb(120 140 220 / 0.18);
  }
  @media (max-width: 860px) {
    canvas {
      height: 46svh;
      z-index: 2;
      mask-image: linear-gradient(#000 82%, transparent);
    }
    .still {
      inset: 64px 16px auto;
      width: auto;
      translate: none;
      z-index: 2;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    canvas {
      transition: none;
    }
  }
</style>
