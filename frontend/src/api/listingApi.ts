import apiClient from './client'
import type {
  Listing, ListingDetail, Category, ListingImage,
  CreateListingRequest, UpdateListingRequest, ListingSearchRequest,
  PaginatedResponse, ListingStatus
} from '@/types/listing'

const API_BASE = '/api/listings'

export const listingApi = {
  /**
   * Create a new listing
   */
  async createListing(data: CreateListingRequest): Promise<Listing> {
    const response = await apiClient.post<Listing>(API_BASE, data)
    return response.data
  },

  /**
   * Get listing detail by ID
   */
  async getListingDetail(id: number): Promise<ListingDetail> {
    const response = await apiClient.get<ListingDetail>(`${API_BASE}/${id}`)
    return response.data
  },

  /**
   * Update an existing listing
   */
  async updateListing(id: number, data: UpdateListingRequest): Promise<Listing> {
    const response = await apiClient.put<Listing>(`${API_BASE}/${id}`, data)
    return response.data
  },

  /**
   * Delete a listing
   */
  async deleteListing(id: number): Promise<void> {
    await apiClient.delete(`${API_BASE}/${id}`)
  },

  /**
   * Update listing status
   */
  async updateStatus(id: number, status: ListingStatus): Promise<Listing> {
    const response = await apiClient.patch<Listing>(`${API_BASE}/${id}/status`, { status })
    return response.data
  },

  /**
   * Search/browse listings with filters
   */
  async searchListings(params: ListingSearchRequest): Promise<PaginatedResponse<Listing>> {
    const response = await apiClient.get<PaginatedResponse<Listing>>(API_BASE, { params })
    return response.data
  },

  /**
   * Get listings by user ID
   */
  async getUserListings(userId: number, page = 0, size = 20): Promise<PaginatedResponse<Listing>> {
    const response = await apiClient.get<PaginatedResponse<Listing>>(`${API_BASE}/user/${userId}`, {
      params: { page, size }
    })
    return response.data
  },

  /**
   * Upload images for a listing
   */
  async uploadImages(listingId: number, files: File[], primaryIndex = 0): Promise<ListingImage[]> {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    formData.append('primaryIndex', primaryIndex.toString())

    const response = await apiClient.post<ListingImage[]>(`${API_BASE}/${listingId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data
  },

  /**
   * Delete an image from a listing
   */
  async deleteImage(listingId: number, imageId: number): Promise<void> {
    await apiClient.delete(`${API_BASE}/${listingId}/images/${imageId}`)
  },

  /**
   * Set an image as primary for a listing
   */
  async setPrimaryImage(listingId: number, imageId: number): Promise<void> {
    await apiClient.patch(`${API_BASE}/${listingId}/images/${imageId}/primary`)
  },

  /**
   * Get all categories as a tree
   */
  async getCategories(): Promise<Category[]> {
    const response = await apiClient.get<Category[]>(`${API_BASE}/categories`)
    return response.data
  },

  /**
   * Get a single category by ID
   */
  async getCategory(id: number): Promise<Category> {
    const response = await apiClient.get<Category>(`${API_BASE}/categories/${id}`)
    return response.data
  }
}