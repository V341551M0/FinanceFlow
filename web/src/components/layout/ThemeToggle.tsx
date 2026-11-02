import { Moon, Sun } from 'lucide-react';
import { useThemeStore } from '@/stores/themeStore';

export function ThemeToggle() {
  const { mode, toggle } = useThemeStore();
  return (
    <button
      onClick={toggle}
      className="rounded-lg p-2 text-slate-400 transition hover:bg-surface-800 hover:text-slate-200"
      aria-label="Alternar tema"
    >
      {mode === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}