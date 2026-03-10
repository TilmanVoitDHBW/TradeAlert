<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { API_BASE, exchangeRateEur, globalUserId, globalCurrency, uiMessage } from '@/composables/useAppState'
import { loadSubscriptions, userSubscriptions } from '@/composables/useSubscriptions'
import { getAlerts, alertsList } from '@/composables/useAlerts'
import { quoteRefreshInterval } from '@/composables/useQuote'

// Components
import StockSearch from '@/components/StockSearch.vue'
import PortfolioTable from '@/components/PortfolioTable.vue'
import AlertsFeed from '@/components/AlertsFeed.vue'
import SettingsPanel from '@/components/SettingsPanel.vue'
import DeleteModal from '@/components/DeleteModal.vue'

let refreshInterval = null;

// Initial Setup
onMounted(async () => {
  try {
    const res = await fetch(`${API_BASE}/exchange-rate`)
    const data = await res.json()
    if (data.convertedAmount) exchangeRateEur.value = data.convertedAmount
  } catch (err) { console.warn("Konnte Wechselkurs nicht laden.") }
})

const startAutoRefresh = () => {
  if (refreshInterval) clearInterval(refreshInterval)
  refreshInterval = setInterval(() => { 
    loadSubscriptions()
    getAlerts()
  }, 1000)
}

const stopAutoRefresh = () => {
  if (refreshInterval) clearInterval(refreshInterval)
}

// Global Watchers
watch(globalUserId, () => {
  if (globalUserId.value) {
    loadSubscriptions()
    getAlerts()
    startAutoRefresh()
  } else {
    userSubscriptions.value = []
    alertsList.value = []
    stopAutoRefresh()
  }
})

watch(globalCurrency, () => {
  if (globalUserId.value) loadSubscriptions()
})

onUnmounted(() => {
  stopAutoRefresh()
  if (quoteRefreshInterval) clearInterval(quoteRefreshInterval)
})
</script>

<template>
  <div class="app-wrapper">
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    
    <div v-if="uiMessage" :class="['toast-notification', `toast-${uiMessage.type}`]">
      {{ uiMessage.text }}
    </div>

    <DeleteModal />

    <main class="main-content">
      <div class="main-inner-container">
        
        <header class="main-header">
          <h1>{{ globalUserId ? `Portfolio von ${globalUserId}` : 'Marktübersicht & Portfolio' }}</h1>
          <p class="header-subtitle">Beobachte deine Aktien in Echtzeit und setze smarte Limits.</p>
        </header>

        <StockSearch />
        <PortfolioTable />
        
      </div>
    </main>

    <aside class="sidebar-right">
      <SettingsPanel />
      <AlertsFeed />
    </aside>
  </div>
</template>

<style>

</style>