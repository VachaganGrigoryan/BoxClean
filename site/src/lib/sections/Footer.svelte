<script lang="ts">
  import { CLEANYOURTV, REPO } from "../data";
  import Cta from "./Cta.svelte";
</script>

<footer class="wrap" data-scene="footer">
  <div class="end">
    <h2>Give the box its quiet back.</h2>
    <Cta />
    <p class="small credits">
      BoxClean is Apache-2.0. Parts adapted from <a href={CLEANYOURTV}>CleanYourTV</a> stay MIT, and the launcher's
      Outfit font is under the SIL Open Font License. Not affiliated with Xiaomi or Google.
      <a href={REPO}>github.com/VachaganGrigoryan/BoxClean</a>
    </p>
  </div>
</footer>

<style>
  footer {
    min-height: 100svh;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    padding-block: 0 48px;
  }
  .end {
    text-align: center;
    display: grid;
    justify-items: center;
    gap: 18px;
    max-width: 44rem;
  }
  h2 {
    margin: 0;
  }
  .credits {
    margin: 8px 0 0;
    max-width: 40em;
  }
  @media (max-width: 860px) {
    footer {
      padding-top: 50svh;
    }
  }
</style>
