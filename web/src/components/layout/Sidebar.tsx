import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, Wallet, Receipt, Target,
  TrendingUp, PieChart, Settings, CreditCard,
} from 'lucide-react';

const items = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/accounts', icon: Wallet, label: 'Contas' },
  { to: '/transactions', icon: Receipt, label: 'Transações' },
  { to: '/credit-cards', icon: CreditCard, label: 'Cartões' },
  { to: '/budgets', icon: Target, label: 'Orçamentos' },
  { to: '/investments', icon: TrendingUp, label: 'Investimentos' },
  { to: '/reports', icon: PieChart, label: 'Relatórios' },
  { to: '/settings', icon: Settings, label: 'Configurações' },
];

export function Sidebar() {
  return (
    <aside className="hidden w-60 shrink-0 border-r border-surface-800 bg-surface-900 p-4 md:block">
      <div className="mb-8 flex items-center gap-2 px-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600">
          <span className="text-sm font-bold text-white">FF</span>
        </div>
        <span className="text-lg font-semibold text-white">FinanceFlow</span>
      </div>

      <nav className="space-y-1">
        {items.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${
                isActive
                  ? 'bg-brand-600/10 text-brand-400'
                  : 'text-slate-400 hover:bg-surface-800 hover:text-slate-200'
              }`
            }
          >
            <Icon size={18} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}