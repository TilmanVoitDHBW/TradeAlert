<script setup>
import { stockToDelete, executeDelete } from '@/composables/useSubscriptions'
</script>

<template>
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
</template>