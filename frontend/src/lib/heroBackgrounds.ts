import type { CSSProperties } from 'react'

export const HERO_IMAGES = {
  home: 'https://images.unsplash.com/photo-1756705533779-105bf34e0722?auto=format&fit=crop&q=80&w=2000',
  dashboard: 'https://images.unsplash.com/photo-1555664424-778a1e5e1b48?auto=format&fit=crop&q=80&w=2000',
  browse: 'https://images.unsplash.com/photo-1555664424-778a1e5e1b48?auto=format&fit=crop&q=80&w=2000',
  transactions: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=2000',
  transactionDetail: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=2000',
  auth: 'https://images.unsplash.com/photo-1517430816045-df4b7de11d1d?auto=format&fit=crop&q=80&w=2000',
  listingDetail: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&q=80&w=2000',
  profile: 'https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&q=80&w=2000',
} as const

export function buildHeroBackground(imageUrl: string, tone: 'dark' | 'light' = 'dark'): CSSProperties {
  const overlay =
    tone === 'dark'
      ? 'linear-gradient(110deg, rgba(8, 17, 30, 0.88), rgba(23, 70, 111, 0.7) 52%, rgba(255, 194, 86, 0.16))'
      : 'linear-gradient(110deg, rgba(255,255,255,0.82), rgba(240,247,255,0.7) 48%, rgba(207,229,248,0.72))'

  return {
    backgroundImage: `${overlay}, url(${imageUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat',
  }
}
