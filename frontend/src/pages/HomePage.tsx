import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { contentApi } from '@/api/contentApi'
import { HomepageModuleRenderer } from '@/components/home/HomepageModuleRenderer'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/stores/authStore'
import { HERO_IMAGES, buildHeroBackground } from '@/lib/heroBackgrounds'

export default function HomePage() {
  const { isAuthenticated } = useAuthStore()
  const { data, isLoading } = useQuery({
    queryKey: ['homepage-content'],
    queryFn: () => contentApi.getHomepage(),
  })

  return (
    <div className="space-y-8">
      <section
        className="relative overflow-hidden rounded-[2.25rem] border border-white/40 px-8 py-14 shadow-[0_30px_90px_rgba(15,23,42,0.22)]"
        style={buildHeroBackground(isAuthenticated ? HERO_IMAGES.dashboard : HERO_IMAGES.home)}
      >
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.2),transparent_28%),linear-gradient(180deg,transparent,rgba(2,6,23,0.1))]" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl text-white">
            <p className="text-sm font-medium uppercase tracking-[0.32em] text-amber-200/80">
              Inspired by modern resale marketplaces
            </p>
            <h1 className="mt-5 text-4xl font-semibold leading-tight md:text-6xl">
              Discover curated electronics moments from one server-driven home.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-slate-200 md:text-lg">
              Move from merchandising modules into sharable browse URLs without losing the platform&apos;s
              clean trading workflow.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link to="/listings">
              <Button className="h-11 px-6">Browse listings</Button>
            </Link>
            {!isAuthenticated ? (
              <Link to="/register">
                <Button variant="outline" className="h-11 border-white/40 bg-white/10 px-6 text-white hover:bg-white/15">
                  Create account
                </Button>
              </Link>
            ) : (
              <Link to="/listings/create">
                <Button variant="outline" className="h-11 border-white/40 bg-white/10 px-6 text-white hover:bg-white/15">
                  Create listing
                </Button>
              </Link>
            )}
          </div>
        </div>
      </section>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-40 rounded-[1.75rem]" />
          ))}
        </div>
      ) : (
        <HomepageModuleRenderer modules={data?.modules ?? []} />
      )}
    </div>
  )
}
