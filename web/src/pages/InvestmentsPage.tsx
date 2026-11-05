import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, TrendingUp } from 'lucide-react';
import { investmentsApi, type AssetType, type CreateInvestment } from '@/api/investments';
import { formatCurrency } from '@/lib/format';

const empty: CreateInvestment = {
    symbol: '', name: '', type: 'STOCK', exchange: 'B3',
    currency: 'BRL', quantity: 0, price: 0,
    operationDate: new Date().toISOString().slice(0, 10),
};

export default function InvestmentsPage() {
    const qc = useQueryClient();
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<CreateInvestment>(empty);

    const { data: positions = [], isLoading } = useQuery({
        queryKey: ['investments'], queryFn: investmentsApi.list,
    });

    const create = useMutation({
        mutationFn: investmentsApi.create,
        onSuccess: () => { qc.invalidateQueries({ queryKey: ['investments'] }); setShowForm(false); },
    });

    const remove = useMutation({
        mutationFn: investmentsApi.remove,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['investments'] }),
    });

    const totals = positions.reduce(
        (acc, p) => ({
            invested: acc.invested + p.investedValue,
            market: acc.market + (p.marketValue ?? 0),
            pl: acc.pl + (p.profitLoss ?? 0),
        }),
        { invested: 0, market: 0, pl: 0 }
    );

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Investimentos</h1>
                    <p className="mt-1 text-sm text-slate-400">Sua carteira de ativos</p>
                </div>
                <button onClick={() => setShowForm((v) => !v)} className="btn-primary">
                    <Plus size={16} /> Nova posição
                </button>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <div className="card">
                    <p className="text-xs uppercase text-slate-500">Investido</p>
                    <p className="mt-2 text-2xl font-semibold text-white">{formatCurrency(totals.invested)}</p>
                </div>
                <div className="card">
                    <p className="text-xs uppercase text-slate-500">Valor de mercado</p>
                    <p className="mt-2 text-2xl font-semibold text-white">{formatCurrency(totals.market)}</p>
                </div>
                <div className="card">
                    <p className="text-xs uppercase text-slate-500">Resultado não realizado</p>
                    <p className={`mt-2 text-2xl font-semibold ${totals.pl >= 0 ? 'text-positive' : 'text-negative'}`}>
                        {formatCurrency(totals.pl)}
                    </p>
                </div>
            </div>

            {showForm && (
                <form onSubmit={(e) => { e.preventDefault(); create.mutate(form); }}
                      className="card grid grid-cols-1 gap-3 md:grid-cols-3">
                    <input required placeholder="Símbolo (PETR4)" className="input-base" value={form.symbol}
                           onChange={(e) => setForm({ ...form, symbol: e.target.value.toUpperCase() })} />
                    <input required placeholder="Nome" className="input-base" value={form.name}
                           onChange={(e) => setForm({ ...form, name: e.target.value })} />
                    <select className="input-base" value={form.type}
                            onChange={(e) => setForm({ ...form, type: e.target.value as AssetType })}>
                        <option value="STOCK">Ação</option>
                        <option value="ETF">ETF</option>
                        <option value="FII">FII</option>
                        <option value="BDR">BDR</option>
                        <option value="CRYPTO">Cripto</option>
                    </select>
                    <input required type="number" step="0.00000001" placeholder="Quantidade" className="input-base"
                           value={form.quantity || ''}
                           onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })} />
                    <input required type="number" step="0.01" placeholder="Preço unitário" className="input-base"
                           value={form.price || ''}
                           onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} />
                    <input required type="date" className="input-base" value={form.operationDate}
                           onChange={(e) => setForm({ ...form, operationDate: e.target.value })} />
                    <button type="submit" disabled={create.isPending} className="btn-primary md:col-span-3">
                        {create.isPending ? 'Salvando...' : 'Registrar compra'}
                    </button>
                </form>
            )}

            {isLoading ? (
                <p className="text-slate-400">Carregando...</p>
            ) : positions.length === 0 ? (
                <div className="card flex flex-col items-center justify-center py-12 text-center">
                    <TrendingUp className="mb-3 text-slate-500" size={32} />
                    <p className="text-slate-300">Nenhuma posição cadastrada</p>
                </div>
            ) : (
                <div className="card overflow-hidden p-0">
                    <table className="w-full text-sm">
                        <thead className="border-b border-surface-700 bg-surface-800/50 text-left text-xs uppercase text-slate-500">
                        <tr>
                            <th className="px-4 py-3">Ativo</th>
                            <th className="px-4 py-3 text-right">Qtd</th>
                            <th className="px-4 py-3 text-right">Preço médio</th>
                            <th className="px-4 py-3 text-right">Atual</th>
                            <th className="px-4 py-3 text-right">Mercado</th>
                            <th className="px-4 py-3 text-right">Resultado</th>
                            <th className="px-4 py-3" />
                        </tr>
                        </thead>
                        <tbody>
                        {positions.map((p) => (
                            <tr key={p.investmentId} className="border-b border-surface-800">
                                <td className="px-4 py-3">
                                    <p className="font-medium text-white">{p.symbol}</p>
                                    <p className="text-xs text-slate-500">{p.name}</p>
                                </td>
                                <td className="px-4 py-3 text-right text-slate-300">{p.quantity}</td>
                                <td className="px-4 py-3 text-right text-slate-300">{formatCurrency(p.averagePrice)}</td>
                                <td className="px-4 py-3 text-right text-slate-300">
                                    {p.currentPrice == null
                                        ? <span className="text-xs text-slate-500">indisponível</span>
                                        : formatCurrency(p.currentPrice)}
                                </td>
                                <td className="px-4 py-3 text-right text-slate-300">
                                    {p.marketValue == null ? '—' : formatCurrency(p.marketValue)}
                                </td>
                                <td className={`px-4 py-3 text-right ${p.profitLoss == null ? 'text-slate-500'
                                    : p.profitLoss >= 0 ? 'text-positive' : 'text-negative'}`}>
                                    {p.profitLoss == null ? '—' : formatCurrency(p.profitLoss)}
                                    {p.profitLossPct != null && (
                                        <span className="ml-1 text-xs">({p.profitLossPct.toFixed(1)}%)</span>
                                    )}
                                </td>
                                <td className="px-4 py-3 text-right">
                                    <button onClick={() => remove.mutate(p.investmentId)}
                                            className="rounded p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400">
                                        <Trash2 size={14} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            <p className="text-xs text-slate-500">
                Cotações via brapi.dev (B3) e CoinGecko (cripto). Podem estar atrasadas conforme o plano gratuito.
            </p>
        </div>
    );
}