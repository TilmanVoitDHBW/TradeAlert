import { ref } from 'vue'
import { API_BASE, globalUserId, showMessage } from './useAppState'
import { loadSubscriptions } from './useSubscriptions'

export const quoteQuery = ref('')
export const quoteResult = ref(null)
export let quoteRefreshInterval = null

export const getQuote = async () => {
  quoteResult.value = null;
  if (quoteRefreshInterval) clearInterval(quoteRefreshInterval)
  if(!quoteQuery.value) return;
  try {
    const res = await fetch(`${API_BASE}/quote?query=${encodeURIComponent(quoteQuery.value)}`)
    const data = await res.json()
    if (data.error || data.NotFoundException) throw new Error("Aktie nicht gefunden")
    
    quoteResult.value = data
    
    // Auto refresh stock price while searching
    quoteRefreshInterval = setInterval(async () => {
      if (!quoteResult.value) { clearInterval(quoteRefreshInterval); return; }
      try {
        const r = await fetch(`${API_BASE}/quote?query=${encodeURIComponent(quoteResult.value.symbol)}`)
        const d = await r.json()
        if (d.priceUsd) quoteResult.value = d
      } catch {}
    }, 5000)
  } catch (err) { showMessage(err.message, 'error') }
}

export const subscribeFromQuote = async (symbol) => {
  if (!globalUserId.value) { 
    showMessage("Bitte logge dich rechts in der Sidebar ein.", "warning"); 
    return; 
  }
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
    if (quoteRefreshInterval) clearInterval(quoteRefreshInterval)
    
    await loadSubscriptions() 
  } catch (err) { showMessage(err.message, 'error') }
}