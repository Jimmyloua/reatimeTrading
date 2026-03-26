import type { Listing } from '@/types/listing'

export type HomepageModuleType = 'hero' | 'image_tiles' | 'collection_row' | 'category_spotlight'
export type ContentLinkType = 'category' | 'listing' | 'collection' | 'route'

export interface HomepageModuleItem {
  imageUrl: string
  headline: string
  subheadline: string
  linkType: ContentLinkType
  linkValue: string
  accentLabel: string
  displayOrder: number
}

export interface HomepageModule {
  slug: string
  moduleType: HomepageModuleType
  title: string
  subtitle: string
  displayOrder: number
  items: HomepageModuleItem[]
}

export interface HomepageResponse {
  modules: HomepageModule[]
}

export interface CuratedCollectionResponse {
  slug: string
  title: string
  subtitle: string
  description: string
  coverImageUrl: string
  targetType: ContentLinkType
  targetValue: string
  displayOrder: number
  items: Listing[]
}
