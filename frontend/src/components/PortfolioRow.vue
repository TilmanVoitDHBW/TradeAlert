<script setup>
import { currencySymbol, formatPrice } from '@/composables/useAppState'
import { updateThresholds, requestDelete } from '@/composables/useSubscriptions'

defineProps({
  sub: { type: Object, required: true }
})

// Calculate percentage gain/loss
const getPercentChange = (entry, current) => {
  if (!entry || !current || entry === 0) return { val: '0.00', color: 'gray' }
  const change = ((current - entry) / entry) * 100
  if (change > 0) return { val: '+' + change.toFixed(2), color: 'green' }
  if (change < 0) return { val: change.toFixed(2), color: 'red' }
  return { val: '0.00', color: 'gray' }
}
</script>

<template>
  <tr class="table-row">
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
         <input type="number" step="0.01" v-model="sub.editUpper" 
           @input="sub.alertsActive = true; sub.isModified = true"
           placeholder="Kein Limit" class="invisible-input" />
         <span class="input-symbol" v-show="sub.editUpper !== null && sub.editUpper !== ''">{{ currencySymbol }}</span>
      </div>
    </td>
    <td class="text-center">
      <div class="input-wrapper">
         <input type="number" step="0.01" v-model="sub.editLower" 
           @input="sub.alertsActive = true; sub.isModified = true"
           placeholder="Kein Limit" class="invisible-input" />
         <span class="input-symbol" v-show="sub.editLower !== null && sub.editLower !== ''">{{ currencySymbol }}</span>
      </div>
    </td>
    <td class="text-center">
      <label class="custom-checkbox" :class="{'disabled': !sub.editUpper && !sub.editLower}" title="Mindestens ein Schwellenwert erforderlich">
        <input type="checkbox" v-model="sub.alertsActive" :disabled="!sub.editUpper && !sub.editLower" @change="sub.isModified = true">
        <span class="checkmark"></span>
      </label>
    </td>
    <td class="text-center">
      <div class="action-buttons">
        <button class="action-btn save-btn" :class="{ 'is-dirty': sub.isModified }" :disabled="!sub.isModified" @click="updateThresholds(sub)" title="Änderungen speichern">Speichern</button>
        <button class="action-btn delete-btn" @click="requestDelete(sub.symbol)" title="Aktie entfernen">Löschen</button>
      </div>
    </td>
  </tr>
</template>