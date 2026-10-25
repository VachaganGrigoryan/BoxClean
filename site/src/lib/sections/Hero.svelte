<script lang="ts">
  import Cta from "./Cta.svelte";
</script>

<section class="hero wrap" data-scene="hero">
  <div class="panel">
    <h1>Your Mi&nbsp;Box, quiet and yours.</h1>
    <p class="lead soft">
      BoxClean removes the ads, telemetry and auto-installs from a Xiaomi Mi Box S 2nd gen over network ADB. Then it
      replaces the Google TV home screen with a small offline launcher that has six layouts.
    </p>
    <Cta />
    <p class="small note">Free and open source under Apache-2.0. Every change can be undone.</p>
  </div>
</section>

<style>
  .hero {
    min-height: 100svh;
    display: flex;
    align-items: center;
    padding-block: 96px 64px;
  }
  .lead {
    font-size: clamp(1.1rem, 1.4vw, 1.22rem);
    margin-bottom: 30px;
  }
  .note {
    margin-top: 20px;
  }
  @media (max-width: 860px) {
    .hero {
      align-items: flex-start;
      padding-top: 47svh;
      min-height: auto;
      padding-bottom: 20svh;
    }
  }
</style>
