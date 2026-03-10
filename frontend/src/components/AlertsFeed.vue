<script setup>
import { alertsList, clearAlerts, deleteSingleAlert } from '@/composables/useAlerts'
</script>

<template>
  <div class="sidebar-widget alerts-widget glass-card">
    <div class="widget-header">
      <h3 class="widget-title">Alerts</h3>
      <button v-if="alertsList.length > 0" @click="clearAlerts" class="btn-clear-alerts">Alle löschen</button>
    </div>
    
    <div class="alerts-feed" v-if="alertsList.length > 0">
      <div v-for="(alert, index) in alertsList" :key="index" 
           class="feed-card" 
           :class="{'border-green': alert.message.includes('oberen'), 'border-red': alert.message.includes('unteren')}">
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
    <div v-else class="empty-feed">Aktuell keine neuen Systemmeldungen.</div>
  </div>
</template>