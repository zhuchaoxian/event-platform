<template>
  <div class="bubble" :class="msg.type === 'USER' ? 'right' : 'left'">
    <div class="content">{{ msg.text || (msg.data && msg.data.answer) || '' }}</div>
  </div>
</template>

<script>
export default {
  name: 'AiMessageBubble',
  props: { msg: { type: Object, required: true } }
}
</script>

<style scoped>
.bubble { margin: 8px 0; }
.right { text-align: right; }
.left { text-align: left; }
.content { display: inline-block; padding: 8px 14px; border-radius: 8px; font-size: 14px; }
.right .content { background: #409eff; color: #fff; }
.left .content { background: #fff; color: #333; }
</style>
