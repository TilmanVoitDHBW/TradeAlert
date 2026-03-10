<script setup>
import { quoteQuery, quoteResult, getQuote, subscribeFromQuote } from '@/composables/useQuote'
import { formatPrice } from '@/composables/useAppState'
</script>

<template>
  <section class="search-section glass-card">
    <form @submit.prevent="getQuote" class="hero-search-form">
      <input 
        v-model="quoteQuery" 
        placeholder="Aktie suchen (z.B. AAPL)..." 
        class="hero-search-input"
        @input="quoteResult = null" 
        autocomplete="off"
      />
      <button type="submit" class="hero-search-btn" :class="{'is-active': quoteQuery}" :disabled="!quoteQuery">
        →
      </button>
    </form>

    <div v-if="quoteResult" class="hero-result-row">
      <div class="hero-result-info">
        <h2 class="hero-symbol">{{ quoteResult.symbol }}</h2>
        <p class="hero-price">{{ formatPrice(quoteResult.priceUsd) }}</p>
      </div>
      <button class="btn-primary" @click="subscribeFromQuote(quoteResult.symbol)">Abonnieren</button>
    </div>
  </section>
</template>