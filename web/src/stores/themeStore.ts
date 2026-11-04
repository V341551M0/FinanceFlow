import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type ThemeMode = 'dark' | 'light';

interface ThemeState {
  mode: ThemeMode;
  toggle: () => void;
  apply: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      mode: 'dark',
      toggle: () => {
        const next: ThemeMode = get().mode === 'dark' ? 'light' : 'dark';
        set({ mode: next });
        document.documentElement.classList.toggle('light', next === 'light');
      },
      apply: () => {
        document.documentElement.classList.toggle('light', get().mode === 'light');
      },
    }),
    { name: 'financeflow-theme' }
  )
);