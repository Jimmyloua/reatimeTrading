import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import axios from 'axios'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { authApi } from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { HERO_IMAGES, buildHeroBackground } from '@/lib/heroBackgrounds'

const registerSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required').min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type RegisterFormData = z.infer<typeof registerSchema>

export default function RegisterPage() {
  const navigate = useNavigate()
  const { setTokens, setUser } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true)

    try {
      const response = await authApi.register(data.email, data.password)

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
      const message = axios.isAxiosError(err)
        ? err.response?.data?.message || err.response?.data?.errors?.email || 'Registration failed. Please try again.'
        : 'Registration failed. Please try again.'

      toast.error(message)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <section
      className="relative overflow-hidden rounded-[2.25rem] border border-white/40 px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.18)]"
      style={buildHeroBackground(HERO_IMAGES.auth)}
    >
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.2),transparent_30%)]" />
      <div className="relative grid items-center gap-8 lg:grid-cols-[0.95fr_0.8fr]">
        <div className="max-w-xl text-white">
          <p className="text-sm font-medium uppercase tracking-[0.28em] text-amber-200/85">Marketplace join</p>
          <h1 className="mt-4 text-3xl font-semibold md:text-5xl">Create your seller-ready account.</h1>
          <p className="mt-4 text-sm leading-7 text-slate-200 md:text-base">
            Start listing devices, chatting with buyers, and managing transactions in the same polished workspace.
          </p>
        </div>

        <Card className="w-full max-w-md border-white/60 bg-white/92 shadow-[0_18px_60px_rgba(15,23,42,0.12)] backdrop-blur-sm">
          <CardHeader className="text-center">
            <CardTitle className="text-2xl">Create account</CardTitle>
            <CardDescription>Join Trading Platform today</CardDescription>
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
                    autoComplete="new-password"
                    placeholder="At least 8 characters"
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

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm password</Label>
                <div className="relative">
                  <Input
                    {...register('confirmPassword')}
                    type={showConfirmPassword ? 'text' : 'password'}
                    id="confirmPassword"
                    autoComplete="new-password"
                    placeholder="Confirm your password"
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
                )}
              </div>
            </CardContent>
            <CardFooter className="flex-col gap-4">
              <Button type="submit" disabled={isLoading} className="w-full">
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Creating account...
                  </>
                ) : (
                  'Create account'
                )}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                Already have an account?{' '}
                <Link to="/login" className="text-primary hover:underline">
                  Sign in
                </Link>
              </p>
            </CardFooter>
          </form>
        </Card>
      </div>
    </section>
  )
}
