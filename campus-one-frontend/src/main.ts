import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { User, Lock, Expand, Fold } from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/index.css'

const app = createApp(App)

// Only icons referenced by string/dynamic name need global registration.
app.component('User', User)
app.component('Lock', Lock)
app.component('Expand', Expand)
app.component('Fold', Fold)

app.use(createPinia())
app.use(router)

app.config.errorHandler = (err, _instance, info) => {
  console.error('Global error:', err)
  console.error('Error info:', info)
}

app.mount('#app')
