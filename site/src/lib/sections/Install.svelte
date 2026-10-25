<script lang="ts">
  import { REPO, STEPS } from "../data";
  import { ui } from "../ui.svelte";

  let copied = $state(-1);

  async function copy(i: number, cmd: string) {
    try {
      await navigator.clipboard.writeText(cmd);
      copied = i;
      setTimeout(() => (copied = -1), 1600);
    } catch {
      copied = -2;
    }
  }
</script>

<section id="install" class="wrap">
  <header class="panel">
    <h2>Set it up</h2>
    <p class="soft">
      You need a Mac or Linux machine on the same network as the box, <a href="https://docs.astral.sh/uv/">uv</a> and
      Android's <code>adb</code>. Replace <code>&lt;ip&gt;</code> with your box's address.
    </p>
  </header>
  <ol class="panel wide" data-scene="install">
    {#each STEPS as s, i}
      <li class:active={ui.section === "install" && ui.step === i}>
        <span class="n">{i + 1}</span>
        <div>
          <h3>{s.title}</h3>
          <p class="soft">{s.body}</p>
          {#if s.cmd}
            <div class="cmd">
              <pre>{s.cmd}</pre>
              <button type="button" onclick={() => copy(i, s.cmd!)}>{copied === i ? "Copied" : "Copy"}</button>
            </div>
          {/if}
        </div>
      </li>
    {/each}
  </ol>
  <p class="panel more soft">
    Changed your mind? <code>restore lists/&lt;list&gt;.txt</code> puts it all back. The
    <a href="{REPO}#readme">README</a> has every detail.
  </p>
</section>

<style>
  section {
    padding-block: 20svh 10svh;
  }
  ol {
    list-style: none;
    margin: 40px 0 0;
    padding: 0;
  }
  li {
    min-height: 55svh;
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    align-content: center;
    opacity: 0.35;
    transition: opacity 0.4s ease;
  }
  li.active {
    opacity: 1;
  }
  .n {
    font-size: 2rem;
    line-height: 1;
    font-weight: 300;
    color: var(--muted);
    font-variant-numeric: tabular-nums;
  }
  .cmd {
    position: relative;
    background: rgb(20 26 46 / 0.85);
    border: 1px solid var(--line);
    border-radius: 10px;
    overflow-x: auto;
  }
  pre {
    margin: 0;
    padding: 14px 84px 14px 16px;
    font: 400 0.8rem/1.7 var(--mono);
    color: #d6dae6;
  }
  button {
    position: absolute;
    top: 8px;
    right: 8px;
    font: 500 0.8rem var(--sans);
    color: var(--text);
    background: var(--room);
    border: 1px solid var(--line);
    border-radius: 999px;
    padding: 5px 12px;
    cursor: pointer;
  }
  button:hover {
    border-color: #4a5480;
  }
  .more {
    margin-top: 24px;
  }
  @media (max-width: 860px) {
    section {
      padding-top: 50svh;
    }
    li {
      min-height: 60svh;
      align-content: start;
      padding-top: 6svh;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    li {
      transition: none;
    }
  }
</style>
