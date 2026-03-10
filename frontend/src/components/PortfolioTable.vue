<script setup>
import { searchQuery, filteredSubscriptions } from '@/composables/useSubscriptions'
import PortfolioRow from './PortfolioRow.vue'
</script>

<template>
  <section class="portfolio-section glass-card">
    <div class="section-top-bar">
      <h2>Deine Positionen</h2>
      <input v-model="searchQuery" placeholder="Liste filtern..." class="filter-input" />
    </div>

    <div class="table-container">
      <table class="fullwidth-table">
        <thead>
          <tr>
            <th style="width: 22%;">Aktie</th>
            <th style="width: 13%;">Live-Kurs</th>
            <th style="width: 12%;">Rendite</th>
            <th style="width: 14%;">Limit Hoch (↑)</th>
            <th style="width: 14%;">Limit Tief (↓)</th>
            <th style="width: 10%;">Alerts aktiv</th>
            <th style="width: 15%;"></th>
          </tr>
        </thead>
        <tbody v-if="filteredSubscriptions.length > 0">
          <PortfolioRow v-for="sub in filteredSubscriptions" :key="sub.symbol" :sub="sub" />
        </tbody>
        <tbody v-else>
          <tr>
            <td colspan="7" class="empty-state">
              <div class="empty-content">
                <p>Dein Portfolio ist noch leer.<br>Suche oben nach einer Aktie, um zu starten.</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>