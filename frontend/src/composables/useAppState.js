import { ref, computed } from 'vue'

// Global configuration
export const API_BASE = 'http://localhost:8080'
export const globalUserId = ref('')
export const globalCurrency = ref('usd')
export const exchangeRateEur = ref(0.92)

// Global Toast logic
export const uiMessage = ref(null)
export const showMessage = (text, type = 'error') => {
  uiMessage.value = { text, type }
  setTimeout(() => { uiMessage.value = null }, 4000)
}

// Currency formatters
export const currencySymbol = computed(() => globalCurrency.value === 'eur' ? '€' : '$')

export const formatPrice = (usdPrice) => {
  if (usdPrice === null || usdPrice === undefined) return '-'
  if (globalCurrency.value === 'eur') {
    return (usdPrice * exchangeRateEur.value).toFixed(2) + ' €'
  }
  return usdPrice.toFixed(2) + ' $'
}

export const toDisplayValue = (usdPrice) => {
  if (usdPrice === null || usdPrice === undefined) return ''
  if (globalCurrency.value === 'eur') {
    return Number((usdPrice * exchangeRateEur.value).toFixed(2))
  }
  return Number(usdPrice.toFixed(2))
}

export const toUsdValue = (displayPrice) => {
  if (displayPrice === null || displayPrice === undefined || displayPrice === '') return null
  if (globalCurrency.value === 'eur') {
    return Number((parseFloat(displayPrice) / exchangeRateEur.value).toFixed(2))
  }
  return parseFloat(displayPrice)
}