import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './api-states.css'
import './design-details.css'
import './landing.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
