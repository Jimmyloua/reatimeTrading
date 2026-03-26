import { Link } from 'react-router-dom'
import type { CuratedCollectionResponse } from '@/types/content'

interface CuratedCollectionRailProps {
  collection: CuratedCollectionResponse
}

export function CuratedCollectionRail({ collection }: CuratedCollectionRailProps) {
  return (
    <section className="rounded-[1.75rem] border border-slate-200/70 bg-white/88 p-6 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur-sm">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-700">
            Curated collection
          </p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900">{collection.title}</h2>
          <p className="mt-2 max-w-2xl text-sm text-slate-600">{collection.description}</p>
        </div>
        <Link
          className="text-sm font-medium text-sky-700 transition hover:text-sky-900"
          to={`/listings?collection=${collection.slug}`}
        >
          Share this collection
        </Link>
      </div>

      <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {collection.items.map((item) => (
          <Link
            key={item.id}
            className="overflow-hidden rounded-[1.5rem] border border-slate-200/70 bg-slate-50/85 transition hover:-translate-y-0.5 hover:shadow-lg"
            to={`/listings/${item.id}`}
          >
            <div
              className="h-40 bg-cover bg-center"
              style={{ backgroundImage: `url(${item.primaryImageUrl ?? collection.coverImageUrl})` }}
            />
            <div className="space-y-2 p-4">
              <div className="text-sm font-medium uppercase tracking-[0.18em] text-slate-500">
                {item.categoryName ?? collection.title}
              </div>
              <h3 className="text-lg font-semibold text-slate-900">{item.title}</h3>
              <p className="text-sm text-slate-600">
                {item.city ? `${item.city}${item.region ? `, ${item.region}` : ''}` : 'Available now'}
              </p>
              <div className="text-base font-semibold text-slate-900">${item.price}</div>
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
