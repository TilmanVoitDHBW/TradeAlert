<script setup>
import { ref, computed } from 'vue'
import { globalUserId, globalCurrency, showMessage } from '@/composables/useAppState'

const tempUserId = ref(globalUserId.value)
const tempCurrency = ref(globalCurrency.value)

const hasSettingsChanges = computed(() => {
  return tempUserId.value !== globalUserId.value || tempCurrency.value !== globalCurrency.value
})

const applySettings = () => {
  globalUserId.value = tempUserId.value
  globalCurrency.value = tempCurrency.value
  showMessage('Einstellungen gespeichert', 'success')
}
</script>

<template>
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
      <button class="btn-primary btn-full" :class="{'is-active': hasSettingsChanges}" :disabled="!hasSettingsChanges" @click="applySettings">
        Speichern
      </button>
    </div>
  </div>
</template>