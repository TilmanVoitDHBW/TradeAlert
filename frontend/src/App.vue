<script setup>
import { ref, watch, computed, onUnmounted, onMounted } from 'vue'

const API_BASE = 'http://localhost:8080'

// --- Globale Variablen ---
const globalUserId = ref('')
const globalCurrency = ref('usd') 

const tempUserId = ref('')
const tempCurrency = ref('usd')

const exchangeRateEur = ref(0.92) 
let refreshInterval = null;

// --- Modal State für Löschen ---
const stockToDelete = ref(null)

onMounted(async () => {
  try {
    const res = await fetch(`${API_BASE}/exchange-rate`)
    const data = await res.json()
    if (data.convertedAmount) exchangeRateEur.value = data.convertedAmount
  } catch (err) { console.warn("Konnte Wechselkurs nicht laden.") }
})

const hasSettingsChanges = computed(() => {
  return tempUserId.value !== globalUserId.value || tempCurrency.value !== globalCurrency.value
})

const currencySymbol = computed(() => globalCurrency.value === 'eur' ? '€' : '$')

const applySettings = () => {
  globalUserId.value = tempUserId.value
  globalCurrency.value = tempCurrency.value
  
  if (!globalUserId.value) {
    userSubscriptions.value = []
    alertsList.value = []
  }
  
  showMessage('Einstellungen gespeichert', 'success')
}

const formatPrice = (usdPrice) => {
  if (usdPrice === null || usdPrice === undefined) return '-'
  if (globalCurrency.value === 'eur') {
    return (usdPrice * exchangeRateEur.value).toFixed(2) + ' €'
  }
  return usdPrice.toFixed(2) + ' $'
}

const toDisplayValue = (usdPrice) => {
  if (usdPrice === null || usdPrice === undefined) return ''
  if (globalCurrency.value === 'eur') {
    return Number((usdPrice * exchangeRateEur.value).toFixed(2))
  }
  return Number(usdPrice.toFixed(2))
}

const toUsdValue = (displayPrice) => {
  if (displayPrice === null || displayPrice === undefined || displayPrice === '') return null
  if (globalCurrency.value === 'eur') {
    return Number((parseFloat(displayPrice) / exchangeRateEur.value).toFixed(2))
  }
  return parseFloat(displayPrice)
}

const uiMessage = ref(null)
const showMessage = (text, type = 'error') => {
  uiMessage.value = { text, type }
  setTimeout(() => { uiMessage.value = null }, 4000)
}

watch(globalUserId, () => {
  if (globalUserId.value) {
    loadSubscriptions()
    getAlerts()
    
    if (refreshInterval) clearInterval(refreshInterval)
    refreshInterval = setInterval(() => { 
      loadSubscriptions()
      getAlerts()
    }, 60000)
  }
})

watch(globalCurrency, () => {
  if (globalUserId.value) {
    loadSubscriptions()
  }
})

onUnmounted(() => { if (refreshInterval) clearInterval(refreshInterval) })

const quoteQuery = ref('')
const quoteResult = ref(null)

const getQuote = async () => {
  quoteResult.value = null;
  if(!quoteQuery.value) return;
  try {
    const res = await fetch(`${API_BASE}/quote?query=${encodeURIComponent(quoteQuery.value)}`)
    const data = await res.json()
    if (data.error || data.NotFoundException) throw new Error("Aktie nicht gefunden")
    quoteResult.value = data
  } catch (err) { showMessage(err.message, 'error') }
}

const subscribeFromQuote = async (symbol) => {
  if (!globalUserId.value) { showMessage("Bitte logge dich rechts in der Sidebar ein.", "warning"); return; }
  try {
    const res = await fetch(`${API_BASE}/subscribe`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: globalUserId.value, query: symbol, upperThresholdUsd: null, lowerThresholdUsd: null })
    })
    const data = await res.json()
    if (data.error) throw new Error(data.error)
    showMessage(`${symbol.toUpperCase()} erfolgreich abonniert`, 'success')
    quoteQuery.value = ''
    quoteResult.value = null
    await loadSubscriptions() 
  } catch (err) { showMessage(err.message, 'error') }
}

const alertsList = ref([])

const getAlerts = async () => {
  if (!globalUserId.value) return
  try {
    const res = await fetch(`${API_BASE}/alerts?userId=${encodeURIComponent(globalUserId.value)}`)
    const data = await res.json()
    alertsList.value = Array.isArray(data) ? data : []
  } catch (err) {
  console.error(err)
  showMessage("Alerts konnten nicht geladen werden", "error")
  }
}

const clearAlerts = async () => {
  if (!globalUserId.value) return;
  try {
    await fetch(`${API_BASE}/alerts?userId=${encodeURIComponent(globalUserId.value)}&clear=true`)
    alertsList.value = []
    showMessage("Verlauf gelöscht", "success")
  } catch (err) { showMessage(err.message, "error") }
}

const deleteSingleAlert = async (alert, index) => {
  if (!globalUserId.value) return;
  try {
    await fetch(`${API_BASE}/alerts/${encodeURIComponent(globalUserId.value)}/single?createdAt=${encodeURIComponent(alert.createdAt)}`, { 
      method: 'DELETE' 
    })
    alertsList.value.splice(index, 1)
  } catch (err) { 
    showMessage("Konnte Alert nicht löschen", "error") 
  }
}

const userSubscriptions = ref([])
const searchQuery = ref('')

const loadSubscriptions = async () => {
  if (!globalUserId.value) return
  try {
    const res = await fetch(`${API_BASE}/subscriptions/${globalUserId.value}`)
    const data = await res.json()
    
    let entries = (data.entries || []).map(sub => {
      const displayUpper = toDisplayValue(sub.upperThresholdUsd)
      const displayLower = toDisplayValue(sub.lowerThresholdUsd)
      const isActive = (sub.upperThresholdUsd != null || sub.lowerThresholdUsd != null)
      
      return {
        ...sub,
        editUpper: displayUpper,
        editLower: displayLower,
        alertsActive: isActive,
        originalUpper: displayUpper,
        originalLower: displayLower,
        originalActive: isActive
      }
    })

    // NEU: Liste streng alphabetisch nach dem Symbol (A-Z) sortieren
    entries.sort((a, b) => a.symbol.localeCompare(b.symbol))

    userSubscriptions.value = entries
  } catch (err) { 
  console.error(err)
  showMessage("Portfolio konnte nicht geladen werden", "error")
  }
}

const hasUnsavedChanges = (sub) => {
  return String(sub.editUpper || '') !== String(sub.originalUpper || '') ||
         String(sub.editLower || '') !== String(sub.originalLower || '') ||
         sub.alertsActive !== sub.originalActive;
}

const updateThresholds = async (sub) => {
  if (!hasUnsavedChanges(sub)) return;

  const finalUpper = (sub.alertsActive && sub.editUpper) ? toUsdValue(sub.editUpper) : null;
  const finalLower = (sub.alertsActive && sub.editLower) ? toUsdValue(sub.editLower) : null;

  try {
    let livePrice = sub.currentPriceUsd;
    if (!livePrice) {
      const quoteRes = await fetch(`${API_BASE}/quote?query=${sub.symbol}`);
      if (quoteRes.ok) {
        const quoteData = await quoteRes.json();
        livePrice = quoteData.priceUsd;
      }
    }

    const response = await fetch(`${API_BASE}/subscriptions/${globalUserId.value}/${sub.symbol}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        upperThresholdUsd: finalUpper,
        lowerThresholdUsd: finalLower,
        currentPriceUsd: livePrice 
      })
    });
    
    if (!response.ok) {
      let errorMsg = "Fehler beim Speichern der Limits";
      try {
        const errorData = await response.json();
        if (errorData.message) {
           errorMsg = errorData.message; 
        }
      } catch (jsonErr) {
         console.error("Konnte Backend-Fehler nicht als JSON lesen");
      }
      throw new Error(errorMsg);
    }

    showMessage(`Limits & Status für ${sub.symbol} gespeichert`, 'success')
    await loadSubscriptions()
  } catch (err) { 
    showMessage(err.message, "error") 
  }
}

const requestDelete = (symbol) => {
  stockToDelete.value = symbol;
}

const executeDelete = async () => {
  const symbol = stockToDelete.value;
  if (!symbol) return;
  
  try {
    await fetch(`${API_BASE}/subscriptions/${globalUserId.value}/${symbol}`, { method: 'DELETE' })
    showMessage(`${symbol} deabonniert`, 'success')
    stockToDelete.value = null;
    await loadSubscriptions()
  } catch (err) { 
    showMessage(err.message, "error") 
    stockToDelete.value = null;
  }
}

const filteredSubscriptions = computed(() => {
  if (!searchQuery.value) return userSubscriptions.value
  const query = searchQuery.value.toLowerCase()
  return userSubscriptions.value.filter(sub => sub.symbol.toLowerCase().includes(query))
})

const getPercentChange = (entry, current) => {
  if (!entry || !current || entry === 0) return { val: '0.00', color: 'gray' }
  const change = ((current - entry) / entry) * 100
  if (change > 0) return { val: '+' + change.toFixed(2), color: 'green' }
  if (change < 0) return { val: change.toFixed(2), color: 'red' }
  return { val: '0.00', color: 'gray' }
}
</script>

<template>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap">
  
  <div class="app-wrapper">
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    
    <div v-if="uiMessage" :class="['toast-notification', `toast-${uiMessage.type}`]">
      {{ uiMessage.text }}
    </div>

    <div v-if="stockToDelete" class="modal-overlay">
      <div class="glass-card modal-content">
        <h3 class="modal-title">Aktie löschen</h3>
        <p class="modal-text">Möchtest du <strong>{{ stockToDelete }}</strong> wirklich aus deinem Portfolio entfernen?</p>
        <div class="modal-actions">
          <button class="action-btn" @click="stockToDelete = null">Abbrechen</button>
          <button class="action-btn delete-btn" @click="executeDelete">Ja, löschen</button>
        </div>
      </div>
    </div>

    <main class="main-content">
      <div class="main-inner-container">
        
        <header class="main-header">
          <h1>{{ globalUserId ? `Portfolio von ${globalUserId}` : 'Marktübersicht & Portfolio' }}</h1>
          <p class="header-subtitle">Beobachte deine Aktien in Echtzeit und setze smarte Limits.</p>
        </header>

        <section class="search-section glass-card">
          <form @submit.prevent="getQuote" class="hero-search-form">
            <input 
              v-model="quoteQuery" 
              placeholder="Aktie suchen (z.B. AAPL)..." 
              class="hero-search-input"
              @input="quoteResult = null" 
              autocomplete="off"
            />
            <button 
              type="submit" 
              class="hero-search-btn" 
              :class="{'is-active': quoteQuery}" 
              :disabled="!quoteQuery"
            >
              →
            </button>
          </form>

          <div v-if="quoteResult" class="hero-result-row">
            <div class="hero-result-info">
              <h2 class="hero-symbol">{{ quoteResult.symbol }}</h2>
              <p class="hero-price">{{ formatPrice(quoteResult.priceUsd) }}</p>
            </div>
            <button class="btn-primary" @click="subscribeFromQuote(quoteResult.symbol)">
              Abonnieren
            </button>
          </div>
        </section>

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
                <tr v-for="sub in filteredSubscriptions" :key="sub.symbol" class="table-row">
                  <td>
                    <div class="asset-cell">
                      <div class="asset-icon">{{ sub.symbol.substring(0, 1) }}</div>
                      <div class="asset-details">
                        <span class="asset-name">{{ sub.symbol }}</span>
                        <span class="asset-entry">Buy In: {{ formatPrice(sub.entryPriceUsd) }}</span>
                      </div>
                    </div>
                  </td>
                  
                  <td class="text-center">
                    <span v-if="sub.currentPriceUsd" class="asset-price">{{ formatPrice(sub.currentPriceUsd) }}</span>
                    <span v-else class="text-muted">--</span>
                  </td>

                  <td class="text-center">
                    <span :class="['trend-badge', 'bg-' + getPercentChange(sub.entryPriceUsd, sub.currentPriceUsd).color]">
                      {{ getPercentChange(sub.entryPriceUsd, sub.currentPriceUsd).val }} %
                    </span>
                  </td>

                  <td class="text-center">
                    <div class="input-wrapper">
                       <input 
                         type="number" 
                         step="0.01" 
                         v-model="sub.editUpper" 
                         @input="sub.alertsActive = true"
                         placeholder="Kein Limit" 
                         class="invisible-input" 
                       />
                       <span class="input-symbol" v-show="sub.editUpper !== null && sub.editUpper !== ''">{{ currencySymbol }}</span>
                    </div>
                  </td>
                  <td class="text-center">
                    <div class="input-wrapper">
                       <input 
                         type="number" 
                         step="0.01" 
                         v-model="sub.editLower" 
                         @input="sub.alertsActive = true"
                         placeholder="Kein Limit" 
                         class="invisible-input" 
                       />
                       <span class="input-symbol" v-show="sub.editLower !== null && sub.editLower !== ''">{{ currencySymbol }}</span>
                    </div>
                  </td>

                  <td class="text-center">
                    <label class="custom-checkbox" :class="{'disabled': !sub.editUpper && !sub.editLower}" title="Mindestens ein Schwellenwert erforderlich">
                      <input 
                        type="checkbox" 
                        v-model="sub.alertsActive" 
                        :disabled="!sub.editUpper && !sub.editLower"
                      >
                      <span class="checkmark"></span>
                    </label>
                  </td>

                  <td class="text-center">
                    <div class="action-buttons">
                      <button 
                        class="action-btn save-btn" 
                        :class="{ 'is-dirty': hasUnsavedChanges(sub) }"
                        :disabled="!hasUnsavedChanges(sub)"
                        @click="updateThresholds(sub)" 
                        title="Änderungen speichern"
                      >
                        Speichern
                      </button>
                      
                      <button 
                        class="action-btn delete-btn" 
                        @click="requestDelete(sub.symbol)" 
                        title="Aktie aus Portfolio entfernen"
                      >
                        Löschen
                      </button>
                    </div>
                  </td>
                </tr>
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
      </div>
    </main>

    <aside class="sidebar-right">
      <div class="sidebar-widget glass-card">
        <h3 class="widget-title">Einstellungen</h3>
        <div class="settings-group">
          <div class="input-group">
            <label>Aktiver Nutzer</label>
            <input v-model="tempUserId" placeholder="User-ID eingeben" class="sidebar-input large-input" />
          </div>
          <div class="input-group">
            <label>Anzeigewährung</label>
            <select v-model="tempCurrency" class="sidebar-select large-input">
              <option value="usd">US-Dollar ($)</option>
              <option value="eur">Euro (€)</option>
            </select>
          </div>
          <button 
            class="btn-primary btn-full" 
            :class="{'is-active': hasSettingsChanges}"
            :disabled="!hasSettingsChanges" 
            @click="applySettings"
          >
            Speichern
          </button>
        </div>
      </div>

      <div class="sidebar-widget alerts-widget glass-card">
        <div class="widget-header">
          <h3 class="widget-title">Alerts</h3>
          <button v-if="alertsList.length > 0" @click="clearAlerts" class="btn-clear-alerts">Alle löschen</button>
        </div>
        
        <div class="alerts-feed" v-if="alertsList.length > 0">
          <div v-for="(alert, index) in alertsList" :key="index" 
               class="feed-card" 
               :class="{
                 'border-green': alert.message.includes('oberen'), 
                 'border-red': alert.message.includes('unteren')
               }">
            <div class="feed-body">
              <div class="feed-top">
                <h4>{{ alert.symbol }}</h4>
                <span class="feed-time">{{ new Date(alert.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}</span>
              </div>
              <p>{{ alert.message }}</p>
            </div>
            <button class="btn-dismiss" @click="deleteSingleAlert(alert, index)" title="Diesen Alert löschen">✕</button>
          </div>
        </div>
        <div v-else class="empty-feed">
          Aktuell keine neuen Systemmeldungen.
        </div>
      </div>

    </aside>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

:root {
  --bg-base: #111827; 
  --bg-sidebar: rgba(31, 41, 55, 0.4); 
  
  --border-color: rgba(255, 255, 255, 0.1);
  --border-focus: rgba(255, 255, 255, 0.35);
  
  --text-primary: #ffffff;      
  --text-secondary: #9ca3af;    
  
  --accent-green: #10b981;
  --accent-red: #ef4444;
  --accent-blue: #3b82f6;
  --accent-hover: rgba(255, 255, 255, 0.08);
  
  --font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

html, body {
  width: 100%;
  height: 100%;
  background-color: var(--bg-base);
  color: var(--text-primary);
  font-family: var(--font-family);
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden; 
  font-size: 18px; 
}

.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  z-index: 0;
  opacity: 0.12; 
  pointer-events: none;
}
.glow-1 {
  width: 600px; height: 600px;
  background: #3b82f6;
  top: -100px; left: -100px;
}
.glow-2 {
  width: 500px; height: 500px;
  background: #8b5cf6;
  bottom: 10%; right: 30%;
}

.glass-card {
  background: rgba(255, 255, 255, 0.03); 
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 40px; 
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(16px);
  position: relative;
  z-index: 1;
}

.app-wrapper {
  display: flex;
  width: 100vw;
  min-height: 100vh;
  position: relative;
}

.main-content {
  flex: 1;
  padding: 3vw 2vw 3vw 3vw; 
  display: flex;
  justify-content: center;
  z-index: 1;
}

.main-inner-container {
  width: 100%;
  max-width: 1800px; 
  display: flex;
  flex-direction: column;
  gap: 3.5rem; 
}

.sidebar-right {
  width: 550px; 
  background-color: var(--bg-sidebar);
  border-left: 1px solid var(--border-color);
  backdrop-filter: blur(20px);
  padding: 50px 25px; 
  display: flex;
  flex-direction: column;
  gap: 3rem;
  z-index: 10;
}

.modal-overlay {
  position: fixed;
  top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}
.modal-content {
  width: 400px;
  padding: 30px;
  text-align: center;
}
.modal-title { margin-bottom: 15px; font-size: 1.5rem; }
.modal-text { color: var(--text-secondary); margin-bottom: 30px; line-height: 1.5; }
.modal-actions { display: flex; gap: 15px; justify-content: center; }

.main-header h1 {
  font-size: 3.5rem; 
  font-weight: 800;
  letter-spacing: -0.04em;
  margin-bottom: 0.5rem;
}
.header-subtitle {
  font-size: 1.3rem; 
  color: var(--text-secondary);
}

.search-section { width: 100%; }

.hero-search-form {
  display: flex;
  align-items: center;
  border-bottom: 2px solid var(--border-color);
  transition: border-color 0.3s;
}
.hero-search-form:focus-within {
  border-color: var(--text-primary);
}
.hero-search-input {
  width: 100%;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 1.5rem; 
  font-weight: 500;
  padding: 10px 0 12px 0; 
  outline: none;
}
.hero-search-input::placeholder { color: rgba(255, 255, 255, 0.3); }

.hero-search-btn {
  background: transparent;
  border: none;
  font-size: 1.8rem; 
  padding: 0 10px;
  color: var(--text-secondary);
  opacity: 0.3;
  cursor: not-allowed;
  transition: all 0.2s;
}
.hero-search-btn.is-active {
  color: var(--accent-blue);
  opacity: 1;
  cursor: pointer;
}
.hero-search-btn.is-active:hover {
  transform: translateX(4px);
}

.hero-result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 25px;
  animation: fadeIn 0.3s ease-out;
}
.hero-symbol { font-size: 2rem; font-weight: 700; margin-bottom: 2px;}
.hero-price { font-size: 1.2rem; color: var(--text-secondary); }

.btn-primary {
  background-color: var(--text-primary);
  color: #000;
  border: none;
  padding: 14px 28px;
  border-radius: 40px;
  font-weight: 700;
  font-size: 1.1rem;
  transition: all 0.2s;
}
.btn-primary:not(:disabled) {
  cursor: pointer;
  opacity: 1;
}
.btn-primary:not(:disabled):hover {
  opacity: 0.9; transform: translateY(-2px);
}
.btn-primary:disabled {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-secondary);
  cursor: not-allowed;
}

.btn-full {
  width: 100%;
  margin-top: 10px;
  padding: 16px;
  font-size: 1.2rem;
}

.portfolio-section { width: 100%; padding: 40px; } 

.section-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 35px;
}
.section-top-bar h2 { font-size: 2rem; font-weight: 700; margin: 0; }

.filter-input {
  background: rgba(0,0,0,0.2);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 10px 18px;
  border-radius: 12px;
  font-size: 1rem;
  outline: none;
  width: 280px;
  transition: border-color 0.3s;
}
.filter-input:focus { border-color: var(--text-primary); }

.table-container { width: 100%; overflow-x: auto; }

.fullwidth-table { 
  width: 100%; 
  border-collapse: collapse; 
  table-layout: fixed;
}

.fullwidth-table th {
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding-bottom: 20px;
  border-bottom: 2px solid var(--border-color);
  text-align: center;
}

.fullwidth-table td {
  padding: 25px 10px; 
  border-bottom: 1px solid var(--border-color);
  vertical-align: middle;
}
.table-row { transition: background-color 0.2s; }
.table-row:hover { background-color: rgba(255,255,255,0.03); }

.text-right { text-align: right !important; }
.text-center { text-align: center !important; }

.asset-cell { display: flex; align-items: center; gap: 20px; padding-left: 10px;}
.asset-icon {
  width: 50px; height: 50px; 
  border-radius: 50%;
  background-color: rgba(255,255,255,0.06);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 1.3rem; border: 1px solid var(--border-color);
}
.asset-details { display: flex; flex-direction: column; }
.asset-name { font-weight: 700; font-size: 1.3rem; }
.asset-entry { font-size: 0.95rem; color: var(--text-secondary); margin-top: 4px; }
.asset-price { font-weight: 600; font-size: 1.25rem; }

.trend-badge {
  padding: 8px 14px; border-radius: 8px; font-weight: 700; font-size: 1.1rem;
  display: inline-block; min-width: 90px; text-align: center; color: #fff;
}
.bg-green { background-color: rgba(16, 185, 129, 0.2); color: var(--accent-green); }
.bg-red { background-color: rgba(239, 68, 68, 0.2); color: var(--accent-red); }
.bg-gray { background-color: rgba(255, 255, 255, 0.1); color: var(--text-secondary); }

.input-wrapper {
  position: relative;
  display: inline-block;
}

.input-symbol {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
  pointer-events: none;
  font-size: 1rem;
  font-weight: 500;
}

.invisible-input {
  background: rgba(0,0,0,0.25); border: 1px solid var(--border-color); color: var(--text-primary);
  font-family: var(--font-family); font-size: 1.1rem; text-align: center;
  width: 120px; padding: 10px 24px 10px 10px; border-radius: 8px; transition: border-color 0.2s;
}
.invisible-input:focus { border-color: var(--text-primary); outline: none; }
.invisible-input::placeholder { color: rgba(255,255,255,0.3); font-size: 0.95rem; }

.custom-checkbox {
  display: inline-block;
  position: relative;
  cursor: pointer;
  width: 26px;
  height: 26px;
}
.custom-checkbox input {
  position: absolute; opacity: 0; cursor: pointer; height: 0; width: 0;
}
.checkmark {
  position: absolute; top: 0; left: 0; height: 26px; width: 26px;
  background-color: rgba(255,255,255,0.1);
  border: 2px solid var(--border-color);
  border-radius: 6px;
  transition: all 0.2s;
}
.custom-checkbox:hover input ~ .checkmark { background-color: rgba(255,255,255,0.2); }
.custom-checkbox input:checked ~ .checkmark { background-color: var(--text-primary); border-color: var(--text-primary); }
.checkmark:after {
  content: ""; position: absolute; display: none;
  left: 8px; top: 3px; width: 6px; height: 12px;
  border: solid #000; border-width: 0 3px 3px 0;
  transform: rotate(45deg);
}
.custom-checkbox input:checked ~ .checkmark:after { display: block; }
.custom-checkbox.disabled { cursor: not-allowed; opacity: 0.3; }
.custom-checkbox.disabled input ~ .checkmark { background-color: transparent; }

.action-buttons { 
  display: flex; 
  gap: 10px; 
  justify-content: center; 
  align-items: center; 
}

.action-btn {
  background: rgba(255,255,255,0.05); 
  border: 1px solid var(--border-color); 
  color: var(--text-secondary);
  border-radius: 8px; 
  padding: 10px 16px; 
  font-size: 0.95rem; 
  font-weight: 600; 
  transition: all 0.2s;
  min-width: 100px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1; 
}

.save-btn { opacity: 0.3; cursor: not-allowed; } 
.save-btn.is-dirty { 
  opacity: 1; 
  cursor: pointer;
  border-color: var(--accent-green); 
  color: var(--accent-green); 
  background: rgba(16, 185, 129, 0.1); 
}
.save-btn.is-dirty:hover { background: var(--accent-green); color: #000; }

.delete-btn { cursor: pointer; opacity: 0.8; }
.delete-btn:hover { opacity: 1; background: var(--accent-red); color: #fff; border-color: var(--accent-red); }

.empty-state { text-align: center !important; padding: 80px 0 !important; }
.empty-content { display: flex; flex-direction: column; align-items: center; gap: 20px; color: var(--text-secondary); font-size: 1.1rem;}

.sidebar-widget { padding: 35px; } 

.widget-title {
  margin: 0 0 20px 0; font-size: 1.05rem; text-transform: uppercase;
  letter-spacing: 0.1em; color: var(--text-secondary); font-weight: 700;
}

.settings-group { display: flex; flex-direction: column; gap: 20px; }
.input-group { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.input-group label { font-size: 0.95rem; color: var(--text-secondary); }

.large-input {
  background: rgba(0,0,0,0.3); border: 1px solid var(--border-color);
  color: var(--text-primary); padding: 15px 18px; border-radius: 10px;
  font-size: 1.1rem; width: 100%; outline: none; transition: border-color 0.2s;
  box-sizing: border-box; 
}
.large-input:focus { border-color: var(--text-primary); }

.widget-header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20px;}
.btn-clear-alerts { 
  background: rgba(239, 68, 68, 0.1); 
  border: 1px solid rgba(239, 68, 68, 0.3); 
  color: var(--accent-red); 
  cursor: pointer; 
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.85rem; 
  font-weight: 600; 
  transition: all 0.2s;
}
.btn-clear-alerts:hover { background: var(--accent-red); color: #fff; }

.alerts-feed { display: flex; flex-direction: column; gap: 15px; width: 100%; }

.feed-card {
  background: rgba(255,255,255,0.02); 
  border: 1px solid var(--border-color);
  border-left-width: 4px; 
  border-left-color: var(--border-color);
  padding: 20px 24px; 
  border-radius: 8px; 
  display: flex; 
  align-items: flex-start; 
  position: relative;
  width: 100%; 
  box-sizing: border-box; 
  transition: transform 0.2s;
}
.feed-card:hover { transform: translateX(2px); background: rgba(255,255,255,0.04); }

.border-green { border-left-color: var(--accent-green) !important; }
.border-red { border-left-color: var(--accent-red) !important; }
.border-blue { border-left-color: var(--accent-blue) !important; }

.feed-body { flex: 1; padding-right: 40px; }
.feed-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.feed-top h4 { margin: 0; font-size: 1.15rem; font-weight: 700; color: var(--text-primary); }
.feed-time { font-size: 0.9rem; color: var(--text-secondary); }
.feed-body p { margin: 0; font-size: 1.05rem; color: var(--text-secondary); line-height: 1.5; }

.btn-dismiss {
  background: transparent; border: none; color: var(--text-secondary);
  cursor: pointer; padding: 5px; font-size: 1.2rem; position: absolute; top: 15px; right: 15px;
  opacity: 0.5; transition: opacity 0.2s;
}
.btn-dismiss:hover { opacity: 1; color: var(--text-primary); }
.empty-feed { color: var(--text-secondary); font-size: 1rem; padding: 15px 0; }

.toast-notification {
  position: fixed; top: 30px; left: 50%; transform: translateX(-50%);
  padding: 14px 28px; border-radius: 40px; font-weight: 600; font-size: 1.05rem;
  z-index: 1000; animation: slideDown 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  color: #fff; box-shadow: 0 15px 40px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.1);
}
.toast-error { background-color: #2a0808; color: var(--accent-red); border-color: rgba(239,68,68,0.4); }
.toast-success { background-color: #062416; color: var(--accent-green); border-color: rgba(16,185,129,0.4); }

@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideDown { from { top: -60px; opacity: 0; } to { top: 30px; opacity: 1; } }

@media (max-width: 1400px) {
  .app-wrapper { flex-direction: column; }
  .sidebar-right { width: 100%; border-left: none; border-top: 1px solid var(--border-color); padding: 5vw; }
}
@media (max-width: 768px) {
  html, body { font-size: 15px; }
  .main-header h1 { font-size: 2.5rem; }
  .glass-card { padding: 25px; }
  .asset-icon { display: none; }
  .asset-cell { padding-left: 0; }
  .invisible-input { width: 90px; }
  .action-buttons { flex-direction: column; gap: 5px; }
}
</style>