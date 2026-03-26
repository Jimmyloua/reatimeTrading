import apiClient from './client'
import type { CuratedCollectionResponse, HomepageResponse } from '@/types/content'

export const contentApi = {
  async getHomepage(): Promise<HomepageResponse> {
    const response = await apiClient.get<HomepageResponse>('/api/content/homepage')
    return response.data
  },

  async getCollection(slug: string): Promise<CuratedCollectionResponse> {
    const response = await apiClient.get<CuratedCollectionResponse>(`/api/content/collections/${slug}`)
    return response.data
  }
}
