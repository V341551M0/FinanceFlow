import { LogOut, User as UserIcon } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { ThemeToggle } from './ThemeToggle';

export function Header() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="flex h-14 items-center justify-between border-b border-surface-800 bg-surface-900 px-6">
      <div className="text-sm text-slate-400">
        Bem-vindo, <span className="text-slate-100">{user?.name ?? 'usuário'}</span>
      </div>

      <div className="flex items-center gap-2">
        <ThemeToggle />
        <div className="mx-2 h-6 w-px bg-surface-700" />
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-400 transition hover:bg-surface-800 hover:text-slate-200"
        >
          <UserIcon size={16} />
          <span className="hidden sm:inline">{user?.email}</span>
          <LogOut size={16} className="ml-2" />
        </button>
      </div>
    </header>
  );
}