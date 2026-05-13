import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

var app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

for (var key in ElementPlusIconsVue) {
  app.component(key, ElementPlusIconsVue[key])
}

app.mount('#app')
