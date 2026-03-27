import { useMemo, useState } from 'react'
import { ChevronRight } from 'lucide-react'
import type { Category } from '@/types/listing'

interface BrowseCategoryDisclosureProps {
  categories: Category[]
  onCommitCategory: (categoryId: number) => void
}

export function BrowseCategoryDisclosure({
  categories,
  onCommitCategory,
}: BrowseCategoryDisclosureProps) {
  const orderedCategories = useMemo(
    () => [...categories].sort((left, right) => left.displayOrder - right.displayOrder),
    [categories]
  )
  const [previewedCategoryId, setPreviewedCategoryId] = useState<number | null>(null)

  const previewedCategory =
    orderedCategories.find((category) => category.id === previewedCategoryId) ?? null

  const previewChildren = [...(previewedCategory?.children ?? [])].sort(
    (left, right) => left.displayOrder - right.displayOrder
  )

  const commitCategory = (categoryId: number) => {
    onCommitCategory(categoryId)
    setPreviewedCategoryId(categoryId)
  }

  const previewImageMap = {
    electronics: 'https://source.unsplash.com/19YCOjHosDk/900x700',
    'phones-tablets': 'https://images.pexels.com/photos/2520829/pexels-photo-2520829.jpeg?auto=compress&cs=tinysrgb&w=900',
    'computers-laptops': 'https://images.pexels.com/photos/614710/pexels-photo-614710.jpeg?auto=compress&cs=tinysrgb&w=900',
    'cameras-photography': 'https://images.pexels.com/photos/34201639/pexels-photo-34201639.jpeg?auto=compress&cs=tinysrgb&w=900',
    gaming: 'https://images.pexels.com/photos/2520829/pexels-photo-2520829.jpeg?auto=compress&cs=tinysrgb&w=900',
    networking: 'https://source.unsplash.com/19YCOjHosDk/900x700',
    'audio-video': 'https://source.unsplash.com/19YCOjHosDk/900x700',
  } as const

  const previewTiles = orderedCategories.slice(0, 4).map((category) => ({
    id: category.id,
    name: category.name,
    imageUrl:
      previewImageMap[category.slug as keyof typeof previewImageMap] ??
      previewImageMap.electronics,
  }))

  return (
    <section className="rounded-[1.75rem] border border-slate-200/70 bg-white/88 p-5 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Browse categories</h2>
          <p className="mt-1 text-sm text-slate-600">
            Preview groups on hover or focus, then explicitly apply a category when you&apos;re ready.
          </p>
        </div>
      </div>

      <div
        className="mt-5 grid gap-4 lg:grid-cols-[240px_1fr]"
        data-testid="browse-category-disclosure"
        onMouseLeave={() => setPreviewedCategoryId(null)}
        onBlurCapture={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget as Node)) {
            setPreviewedCategoryId(null)
          }
        }}
      >
        <div className="space-y-2">
          {orderedCategories.map((category) => (
            <button
              key={category.id}
              className="flex w-full items-center justify-between rounded-2xl border border-slate-200/70 bg-slate-50/90 px-4 py-3 text-left text-sm font-medium text-slate-900 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500"
              onFocus={() => setPreviewedCategoryId(category.id)}
              onMouseEnter={() => setPreviewedCategoryId(category.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  commitCategory(category.id)
                }
              }}
              type="button"
            >
              <span>{category.name}</span>
              <ChevronRight className="h-4 w-4 text-slate-500" />
            </button>
          ))}
        </div>

        <div className="relative overflow-hidden rounded-[1.5rem] border border-slate-200/70 bg-slate-50/85 p-5">
          <div className="grid gap-3 md:grid-cols-2">
            {previewTiles.map((tile) => (
              <div
                key={tile.id}
                className="group relative overflow-hidden rounded-2xl border border-slate-200/60 bg-white/70"
              >
                <img
                  src={tile.imageUrl}
                  alt={tile.name}
                  className="h-32 w-full object-cover transition duration-500 group-hover:scale-105"
                  loading="lazy"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-slate-950/50 via-transparent to-transparent" />
                <div className="absolute bottom-3 left-3 text-sm font-semibold text-white">
                  {tile.name}
                </div>
              </div>
            ))}
          </div>

          {previewedCategory && (
            <div className="absolute inset-0 z-10 rounded-[1.5rem] border border-white/70 bg-white/90 p-5 shadow-[0_18px_60px_rgba(15,23,42,0.12)] backdrop-blur-lg">
              <h3 className="text-base font-semibold text-slate-900">
                {previewedCategory?.name ?? 'Categories'}
              </h3>
              <p className="mt-2 text-sm text-slate-600">
                Keyboard focus and hover stay local here until you click a link or press Enter on a category button.
              </p>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                {previewChildren.map((child) => (
                  <div key={child.id} className="rounded-2xl border border-slate-200/70 bg-white p-4">
                    <h4 className="text-base font-semibold text-slate-900">
                      {child.name}
                    </h4>
                    <p className="mt-2 text-sm text-slate-600">
                      Commit this category filter only when you explicitly choose it.
                    </p>
                    <div className="mt-4">
                      <button
                        className="inline-flex items-center rounded-full border border-slate-300/80 bg-white px-4 py-2 text-sm font-medium text-slate-900 transition hover:bg-slate-100"
                        onClick={() => {
                          commitCategory(child.id)
                        }}
                        type="button"
                      >
                        Browse {child.name}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
