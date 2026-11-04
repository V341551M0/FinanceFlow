import { TrendingUp, TrendingDown, Wallet, PiggyBank } from 'lucide-react';
import type { DashboardSummary } from '@/api/dashboard';
import { formatCurrency } from '@/lib/format';

export function SummaryCards({ summary }: { summary: DashboardSummary }) {
    const cards = [
        {
            icon: Wallet,
            label: 'Patrimônio atual',
            value: formatCurrency(summary.totalPatrimony),
            color: 'text-white',
            sub: null,
        },
        {
            icon: TrendingUp,
            label: 'Receita líquida',
            value: formatCurrency(summary.netIncome),
            color: 'text-positive',
            sub: `Bruta: ${formatCurrency(summary.grossIncome)}`,
        },
        {
            icon: TrendingDown,
            label: 'Despesa líquida',
            value: formatCurrency(summary.netExpense),
            color: 'text-negative',
            sub: summary.netExpenseChangePct !== 0
                ? `${summary.netExpenseChangePct > 0 ? '+' : ''}${summary.netExpenseChangePct.toFixed(1)}% vs período anterior`
                : null,
        },
        {
            icon: PiggyBank,
            label: 'Resultado líquido',
            value: formatCurrency(summary.netResult),
            color: summary.netResult.sign && summary.netResult.sign() < 0 ? 'text-negative' : 'text-positive',
            sub: `Taxa de poupança: ${summary.savingsRate.toFixed(1)}%`,
        },
    ];

    return (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
            {cards.map((c) => (
                <div key={c.label} className="card">
                    <div className="flex items-center gap-2 text-slate-400">
                        <c.icon size={16} />
                        <span className="text-xs uppercase">{c.label}</span>
                    </div>
                    <p className={`mt-3 text-2xl font-semibold ${c.color}`}>{c.value}</p>
                    {c.sub && <p className="mt-1 text-xs text-slate-500">{c.sub}</p>}
                </div>
            ))}
        </div>
    );
}