import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { HERO_IMAGES, buildHeroBackground } from '@/lib/heroBackgrounds'

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormData = z.infer<typeof loginSchema>

export default function LoginPage() {
  const navigate = useNavigate()
  const { setTokens, setUser } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true)

    try {
      const response = await authApi.login(data.email, data.password)

      // Store tokens in Zustand store
      setTokens(response.accessToken, response.refreshToken)
      setUser({
        id: response.userId,
        email: data.email,
        displayName: null,
        avatarUrl: null,
        createdAt: new Date().toISOString(),
        listingCount: 0,
      })

      // Navigate to home
      navigate('/')
    } catch (err) {
      toast.error('Invalid email or password. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section
      className="relative overflow-hidden rounded-[2.25rem] border border-white/40 px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.18)]"
      style={buildHeroBackground(HERO_IMAGES.auth)}
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.2),transparent_30%)]" />
      <div className="relative grid items-center gap-8 lg:grid-cols-[0.95fr_0.8fr]">
        <div className="max-w-xl text-white">
          <p className="text-sm font-medium uppercase tracking-[0.28em] text-amber-200/85">Account access</p>
          <h1 className="mt-4 text-3xl font-semibold md:text-5xl">Return to your trading desk.</h1>
          <p className="mt-4 text-sm leading-7 text-slate-200 md:text-base">
            Sign in to manage listings, reply to buyers, and keep every transaction moving from one place.
          </p>
        </div>

        <Card className="w-full max-w-md border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.12)] backdrop-blur-sm">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl">Sign in</CardTitle>
            <CardDescription>Welcome back to Trading Platform</CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  {...register('email')}
                  type="email"
                  id="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                />
                {errors.email && (
                  <p className="text-sm text-destructive">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                  <Input
                    {...register('password')}
                    type={showPassword ? 'text' : 'password'}
                    id="password"
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-sm text-destructive">{errors.password.message}</p>
                )}
              </div>
            </CardContent>
            <CardFooter className="flex-col gap-4">
              <Button type="submit" disabled={isLoading} className="w-full">
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Signing in...
                  </>
                ) : (
                  'Sign in'
                )}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                Don't have an account?{' '}
                <Link to="/register" className="text-primary hover:underline">
                  Create account
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </section>
  )
}
