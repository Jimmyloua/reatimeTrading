import { Link } from 'react-router-dom'
import type { HomepageModule, HomepageModuleItem } from '@/types/content'

interface HomepageModuleRendererProps {
  modules: HomepageModule[]
}

function getDestination(item: HomepageModuleItem) {
  switch (item.linkType) {
    case 'category':
      return `/listings?categoryId=${item.linkValue}`
    case 'collection':
      return `/listings?collection=${item.linkValue}`
    case 'listing':
      return `/listings/${item.linkValue}`
    case 'route':
    default:
      return item.linkValue
  }
}

function ModuleCallToAction({ item }: { item: HomepageModuleItem }) {
  return (
    <Link
      className="inline-flex items-center rounded-full border border-slate-300/80 bg-white/80 px-4 py-2 text-sm font-medium text-slate-900 transition hover:bg-white"
      to={getDestination(item)}
    >
      {item.headline}
    </Link>
  )
}

export function HomepageModuleRenderer({ modules }: HomepageModuleRendererProps) {
  return (
    <div className="space-y-6">
      {modules.map((module) => {
        if (module.moduleType === 'hero') {
          const heroItem = module.items[0]
          return (
            <section key={module.slug} className="rounded-[2rem] border border-slate-200/70 bg-white/90 p-8 shadow-[0_18px_60px_rgba(15,23,42,0.08)]">
              <p className="text-sm font-medium uppercase tracking-[0.24em] text-sky-700">
                {heroItem?.accentLabel ?? 'Featured'}
              </p>
              <h2 className="mt-4 text-3xl font-semibold text-slate-900">{module.title}</h2>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">{module.subtitle}</p>
              {heroItem ? (
                <div className="mt-6">
                  <ModuleCallToAction item={heroItem} />
                </div>
              ) : null}
            </section>
          )
        }

        return (
          <section key={module.slug} className="space-y-4 rounded-[1.75rem] border border-slate-200/70 bg-white/88 p-6 shadow-[0_18px_60px_rgba(15,23,42,0.08)]">
            <div className="flex items-end justify-between gap-4">
              <div>
                <h2 className="text-2xl font-semibold text-slate-900">{module.title}</h2>
                <p className="mt-2 text-sm text-slate-600">{module.subtitle}</p>
              </div>
            </div>
            <div className={`grid gap-4 ${module.moduleType === 'collection_row' ? 'md:grid-cols-3' : 'md:grid-cols-2'}`}>
              {module.items.map((item) => (
                <article key={`${module.slug}-${item.displayOrder}`} className="overflow-hidden rounded-[1.5rem] border border-slate-200/70 bg-slate-50/85">
                  <div
                    className="h-40 bg-cover bg-center"
                    style={{ backgroundImage: `url(${item.imageUrl})` }}
                  />
                  <div className="space-y-3 p-5">
                    {item.accentLabel ? (
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">
                        {item.accentLabel}
                      </p>
                    ) : null}
                    <h3 className="text-xl font-semibold text-slate-900">{item.headline}</h3>
                    <p className="text-sm text-slate-600">{item.subheadline}</p>
                    <ModuleCallToAction item={item} />
                  </div>
                </article>
              ))}
            </div>
          </section>
        )
      })}
    </div>
  )
}
