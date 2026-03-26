export type Condition = 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'POOR'

export type ListingStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD'

export interface Category {
  id: number
  name: string
  slug: string
  description?: string
  parentId?: number
  children?: Category[]
  displayOrder: number
}

export interface ListingImage {
  id: number
  imageUrl: string
  isPrimary: boolean
  displayOrder: number
}

export interface Listing {
  id: number
  title: string
  price: number
  condition: Condition
  status: ListingStatus
  city?: string
  region?: string
  primaryImageUrl?: string
  categoryId: number
  categoryName: string
  createdAt: string
}

export interface ListingDetail extends Listing {
  description: string
  latitude?: number
  longitude?: number
  images: ListingImage[]
  seller: {
    id: number
    displayName: string
    avatarUrl?: string
    memberSince: string
    listingCount: number
  }
  updatedAt?: string
}

export interface CreateListingRequest {
  title: string
  description: string
  price: number
  categoryId: number
  condition: Condition
  city?: string
  region?: string
  latitude?: number
  longitude?: number
}

export interface UpdateListingRequest {
  title?: string
  description?: string
  price?: number
  categoryId?: number
  condition?: Condition
  city?: string
  region?: string
  latitude?: number
  longitude?: number
}

export interface ListingSearchRequest {
  query?: string
  categoryId?: number
  collection?: string
  minPrice?: number
  maxPrice?: number
  conditions?: Condition[]
  city?: string
  region?: string
  page?: number
  size?: number
  sort?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}
