import { ref, computed } from 'vue'
import { API_BASE, globalUserId, showMessage, toDisplayValue, toUsdValue } from './useAppState'

export const userSubscriptions = ref([])
export const searchQuery = ref('')
export const stockToDelete = ref(null)

export const loadSubscriptions = async () => {
  if (!globalUserId.value) return
  try {
    const res = await fetch(`${API_BASE}/subscriptions/${globalUserId.value}`)
    const data = await res.json()
    
    let entries = (data.entries || []).map(newSub => {
      const displayUpper = toDisplayValue(newSub.upperThresholdUsd)
      const displayLower = toDisplayValue(newSub.lowerThresholdUsd)
      const isActive = (newSub.upperThresholdUsd != null || newSub.lowerThresholdUsd != null)
      
      const existingSub = userSubscriptions.value.find(s => s.symbol === newSub.symbol)
      
      // Keep unsaved inputs intact while pulling new backend values
      if (existingSub && existingSub.isModified) {
        const upperBreached = (existingSub.originalUpper !== '' && displayUpper === '');
        const lowerBreached = (existingSub.originalLower !== '' && displayLower === '');

        return {
          ...newSub,
          editUpper: upperBreached ? '' : existingSub.editUpper,       
          editLower: lowerBreached ? '' : existingSub.editLower,       
          alertsActive: existingSub.alertsActive, 
          originalUpper: displayUpper,            
          originalLower: displayLower,
          originalActive: isActive,
          isModified: true
        }
      }
      
      return {
        ...newSub,
        editUpper: displayUpper,
        editLower: displayLower,
        alertsActive: isActive,
        originalUpper: displayUpper,
        originalLower: displayLower,
        originalActive: isActive,
        isModified: false
      }
    })

    entries.sort((a, b) => a.symbol.localeCompare(b.symbol))
    userSubscriptions.value = entries
  } catch (err) { console.error(err) }
}

export const updateThresholds = async (sub) => {
  if (!sub.isModified) return;
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
        if (errorData.message) errorMsg = errorData.message; 
      } catch (jsonErr) {}
      throw new Error(errorMsg);
    }

    showMessage(`Limits & Status für ${sub.symbol} gespeichert`, 'success')
    sub.originalUpper = sub.editUpper;
    sub.originalLower = sub.editLower;
    sub.originalActive = sub.alertsActive;
    sub.isModified = false; 
  } catch (err) { showMessage(err.message, "error") }
}

export const requestDelete = (symbol) => { stockToDelete.value = symbol; }

export const executeDelete = async () => {
  const symbol = stockToDelete.value;
  if (!symbol) return;
  
  try {
    await fetch(`${API_BASE}/subscriptions/${globalUserId.value}/${symbol}`, { method: 'DELETE' })
    showMessage(`${symbol} deabonniert`, 'success')
    stockToDelete.value = null;
    userSubscriptions.value = userSubscriptions.value.filter(s => s.symbol !== symbol);
  } catch (err) { 
    showMessage(err.message, "error") 
    stockToDelete.value = null;
  }
}

export const filteredSubscriptions = computed(() => {
  if (!searchQuery.value) return userSubscriptions.value
  const query = searchQuery.value.toLowerCase()
  return userSubscriptions.value.filter(sub => sub.symbol.toLowerCase().includes(query))
})