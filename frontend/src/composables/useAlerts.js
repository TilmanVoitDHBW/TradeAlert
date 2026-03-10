import { ref } from 'vue'
import { API_BASE, globalUserId, showMessage } from './useAppState'

export const alertsList = ref([])

export const getAlerts = async () => {
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

export const clearAlerts = async () => {
  if (!globalUserId.value) return;
  try {
    await fetch(`${API_BASE}/alerts?userId=${encodeURIComponent(globalUserId.value)}&clear=true`)
    alertsList.value = []
    showMessage("Verlauf gelöscht", "success")
  } catch (err) { showMessage(err.message, "error") }
}

export const deleteSingleAlert = async (alert, index) => {
  if (!globalUserId.value) return;
  try {
    await fetch(`${API_BASE}/alerts/${encodeURIComponent(globalUserId.value)}/single?createdAt=${encodeURIComponent(alert.createdAt)}`, { 
      method: 'DELETE' 
    })
    alertsList.value.splice(index, 1)
  } catch (err) { showMessage("Konnte Alert nicht löschen", "error") }
}