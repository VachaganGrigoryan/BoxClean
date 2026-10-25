<script lang="ts">
  import { LAYOUTS } from "../data";
  import { ui } from "../ui.svelte";

  let tabs: HTMLButtonElement[] = $state([]);
  const current = $derived(LAYOUTS.find((l) => l.id === ui.layout)!);

  function pick(i: number, focus = false) {
    ui.layout = LAYOUTS[i].id;
    if (focus) tabs[i].focus();
  }

  function onKey(e: KeyboardEvent) {
    const i = LAYOUTS.findIndex((l) => l.id === ui.layout);
    const n = LAYOUTS.length;
    const next = { ArrowRight: i + 1, ArrowDown: i + 1, ArrowLeft: i - 1, ArrowUp: i - 1, Home: 0, End: n - 1 }[e.key];
    if (next === undefined) return;
    e.preventDefault();
    pick((next + n) % n, true);
  }
</script>

<section id="layouts" data-scene="layouts">
  <div class="sticky wrap">
    <div class="head">
      <h2>Six ways to see your apps</h2>
      <p class="soft">Pick one the way you would on the TV. Every layout is plain Android views, so focus moves instantly on the box's small GPU.</p>
    </div>
    <div class="foot">
      <div class="picker" role="tablist" aria-label="Layouts" tabindex="-1" onkeydown={onKey}>
        {#each LAYOUTS as l, i}
          <button
            bind:this={tabs[i]}
            role="tab"
            aria-selected={l.id === ui.layout}
            tabindex={l.id === ui.layout ? 0 : -1}
            onclick={() => pick(i)}
          >
            <img src="/media/{l.id}.jpg" alt="" width="1280" height="720" loading="lazy" />
            {l.name}
          </button>
        {/each}
      </div>
      <p class="desc soft" aria-live="polite"><b>{current.name}.</b> {current.desc}</p>
    </div>
  </div>
</section>

<style>
  section {
    height: 210svh;
  }
  .sticky {
    position: sticky;
    top: 0;
    height: 100svh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding-block: 88px 28px;
  }
  .head {
    max-width: 40rem;
  }
  .head p {
    max-width: 34em;
  }
  .picker {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    max-width: 900px;
    margin: 0 auto;
  }
  button {
    all: unset;
    cursor: pointer;
    display: grid;
    gap: 6px;
    font-size: 0.9rem;
    color: var(--muted);
    text-align: center;
  }
  img {
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
    border-radius: 7px;
    outline: 2px solid transparent;
    outline-offset: 3px;
    opacity: 0.5;
    transition: opacity 0.25s, outline-color 0.25s;
  }
  button:hover img {
    opacity: 0.85;
  }
  button[aria-selected="true"] {
    color: var(--text);
  }
  button[aria-selected="true"] img,
  button:focus-visible img {
    opacity: 1;
    outline-color: #fff;
  }
  .desc {
    max-width: 44em;
    margin: 14px auto 0;
    text-align: center;
    min-height: 3.2em;
    font-size: 0.98rem;
  }
  .desc b {
    color: var(--text);
    font-weight: 600;
  }
  @media (max-width: 860px) {
    section {
      height: auto;
    }
    .sticky {
      position: static;
      height: auto;
      padding-top: 50svh;
      gap: 24px;
    }
    .picker {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }
</style>
