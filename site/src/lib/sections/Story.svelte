<script lang="ts">
  import { CHAPTERS } from "../data";
  import { ui } from "../ui.svelte";
</script>

<section id="story" class="wrap" data-scene="story">
  {#each CHAPTERS as c, i}
    <article class="chapter panel" class:active={ui.section === "story" && ui.step === i}>
      {#if i === 0}<h2>What changes on your TV</h2>{/if}
      <div class="n">{i + 1} of {CHAPTERS.length}</div>
      <h3>{c.title}</h3>
      <p class="soft">{c.body}</p>
      <p class="small">On screen: {c.caption}, a real screenshot from the box.</p>
    </article>
  {/each}
</section>

<style>
  .chapter {
    min-height: 100svh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    opacity: 0.3;
    transition: opacity 0.5s ease;
  }
  .chapter.active {
    opacity: 1;
  }
  .chapter h2 {
    margin-bottom: 40px;
  }
  .n {
    font-size: 0.95rem;
    color: var(--muted);
    font-variant-numeric: tabular-nums;
    margin-bottom: 14px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .n::after {
    content: "";
    height: 1px;
    width: 48px;
    background: var(--line);
  }
  @media (max-width: 860px) {
    .chapter {
      min-height: 90svh;
      justify-content: flex-start;
      padding-top: 52svh;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .chapter {
      transition: none;
    }
  }
</style>
