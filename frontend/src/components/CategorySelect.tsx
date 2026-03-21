import { cn } from '@/lib/utils'
import type { Category } from '@/types/listing'

interface CategorySelectProps {
  value?: number
  onChange: (categoryId: number | undefined) => void
  categories: Category[]
  placeholder?: string
  disabled?: boolean
  className?: string
}

/**
 * Recursively flattens category tree with indentation
 */
function flattenCategories(
  categories: Category[],
  level = 0
): { id: number; name: string; level: number }[] {
  const result: { id: number; name: string; level: number }[] = []

  for (const category of categories) {
    result.push({
      id: category.id,
      name: category.name,
      level,
    })

    if (category.children && category.children.length > 0) {
      result.push(...flattenCategories(category.children, level + 1))
    }
  }

  return result
}

/**
 * Get indentation prefix based on level
 */
function getIndentPrefix(level: number): string {
  return '—'.repeat(level * 2) + (level > 0 ? ' ' : '')
}

export function CategorySelect({
  value,
  onChange,
  categories,
  placeholder = 'Select a category',
  disabled = false,
  className,
}: CategorySelectProps) {
  const flattenedCategories = flattenCategories(categories)

  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newValue = e.target.value
    if (newValue === '') {
      onChange(undefined)
    } else {
      onChange(parseInt(newValue, 10))
    }
  }

  return (
    <select
      value={value ?? ''}
      onChange={handleChange}
      disabled={disabled}
      className={cn(
        'flex h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none',
        'focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50',
        'disabled:cursor-not-allowed disabled:opacity-50',
        'md:text-sm',
        className
      )}
    >
      <option value="">{placeholder}</option>
      {flattenedCategories.map((category) => (
        <option key={category.id} value={category.id}>
          {getIndentPrefix(category.level)}
          {category.name}
        </option>
      ))}
    </select>
  )
}