<template>
  <AppLayout v-if="showLayout">
    <router-view />
  </AppLayout>
  <router-view v-else />
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'

export default {
  name: 'App',
  components: { AppLayout: AppLayout },
  computed: {
    showLayout: function () {
      return this.$route.meta.layout !== 'none'
    }
  }
}
</script>
