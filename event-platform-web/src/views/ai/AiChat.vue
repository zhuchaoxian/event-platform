<template>
  <div class="ai-chat-container">
    <el-container>
      <el-aside width="200px" class="chat-sidebar">
        <el-button type="primary" style="width:100%;margin-bottom:12px;" @click="newChat">新建会话</el-button>
        <div style="font-size:12px;color:#999;margin-bottom:8px;">会话ID: {{ sessionId || '未创建' }}</div>
        <el-button type="text" @click="handleReindex">重建知识库索引</el-button>
      </el-aside>
      <el-main class="chat-main">
        <div class="chat-messages" ref="msgContainer" v-loading="loading">
          <div v-if="!messages.length" class="chat-empty">
            <el-empty description="开始新对话" :image-size="100" />
          </div>
          <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.type === 'USER' ? 'msg-user' : 'msg-ai'">
            <div class="msg-bubble">
              <div v-if="msg.type === 'USER'">{{ msg.text }}</div>
              <div v-else-if="msg.data">
                <div>{{ msg.data.answer }}</div>
                <div v-if="msg.data.toolCalls && msg.data.toolCalls.length" style="margin-top:8px;">
                  <el-collapse>
                    <el-collapse-item title="工具调用">
                      <div v-for="tc in msg.data.toolCalls" :key="tc.toolName" style="font-size:12px;color:#666;">
                        {{ tc.toolName }}: {{ tc.resultPreview }}
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
                <div v-if="msg.data.citations && msg.data.citations.length" style="margin-top:8px;font-size:12px;color:#999;">
                  来源: {{ msg.data.citations.map(function (c) { return c.title }).join(', ') }}
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" :disabled="loading" />
          <el-button type="primary" :loading="loading" style="margin-left:8px;" @click="handleSend">发送</el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { useAiChatStore } from '@/stores/aiChat'
import { ElMessage } from 'element-plus'

export default {
  name: 'AiChat',
  layout: 'AppLayout',
  data: function () { return { inputText: '' } },
  computed: {
    messages: function () { return useAiChatStore().messages },
    sessionId: function () { return useAiChatStore().sessionId },
    loading: function () { return useAiChatStore().loading }
  },
  mounted: function () {},
  watch: {
    'messages.length': function () {
      this.$nextTick(function () { this.scrollToBottom() })
    }
  },
  methods: {
    handleSend: function () {
      var text = this.inputText.trim()
      if (!text) return
      this.inputText = ''
      var self = this
      useAiChatStore().send(text).then(function () { self.scrollToBottom() })
    },
    newChat: function () { useAiChatStore().newSession() },
    handleReindex: function () {
      useAiChatStore().reindex().then(function () {
        ElMessage.success('知识库索引重建成功')
      })
    },
    scrollToBottom: function () {
      var el = this.$refs.msgContainer
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
.ai-chat-container { height: calc(100vh - 140px); }
.chat-sidebar { background: #fff; padding: 16px; border-right: 1px solid #e6e6e6; }
.chat-main { display: flex; flex-direction: column; background: #f5f5f5; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; }
.msg-row { display: flex; margin-bottom: 16px; }
.msg-user { justify-content: flex-end; }
.msg-ai { justify-content: flex-start; }
.msg-bubble { max-width: 70%; padding: 10px 16px; border-radius: 8px; font-size: 14px; line-height: 1.6; }
.msg-user .msg-bubble { background: #409eff; color: #fff; }
.msg-ai .msg-bubble { background: #fff; color: #333; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.chat-input { display: flex; padding: 12px; background: #fff; border-top: 1px solid #e6e6e6; }
</style>
