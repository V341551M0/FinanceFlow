import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Target, Trash2 } from 'lucide-react';
import { budgetsApi, type CreateBudget } from '@/api/budgets';
import { categoriesApi } from '@/api/categories';
import { formatCurrency } from '@/lib/format';

export default function BudgetsPage() {
    const qc = useQueryClient();
    const today = new Date();
    const defaultMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`;

    const [month, setMonth] = useState(defaultMonth);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<CreateBudget>({
        categoryId: null, referenceMonth: defaultMonth, limitAmount: 0,
    });

    const { data: budgets = [], isLoading } = useQuery({
        queryKey: ['budgets', month], queryFn: () => budgetsApi.list(month),
    });
    const { data: categories = [] } = useQuery({
        queryKey: ['categories'], queryFn: categoriesApi.list,
    });

    const create = useMutation({
        mutationFn: budgetsApi.create,
        onSuccess: () => { qc.invalidateQueries({ queryKey: ['budgets'] }); setShowForm(false); },
    });
    const remove = useMutation({
        mutationFn: budgetsApi.remove,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['budgets'] }),
    });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Orçamentos</h1>
                    <p className="mt-1 text-sm text-slate-400">Controle seus limites por categoria</p>
                </div>
                <button onClick={() => setShowForm((v) => !v)} className="btn-primary">
                    <Plus size={16} /> Novo orçamento
                </button>
            </div>

            <input type="date" className="input-base max-w-[200px]" value={month}
                   onChange={(e) => setMonth(e.target.value)} />

            {showForm && (
                <form onSubmit={(e) => { e.preventDefault(); create.mutate({ ...form, referenceMonth: month }); }}
                      className="card grid grid-cols-1 gap-3 md:grid-cols-3">
                    <select className="input-base" value={form.categoryId ?? ''}
                            onChange={(e) => setForm({ ...form, categoryId: e.target.value ? Number(e.target.value) : null })}>
                        <option value="">Geral (todas as despesas)</option>
                        {categories.filter((c) => c.type === 'EXPENSE').map((c) =>
                            <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                    <input required type="number" step="0.01" placeholder="Limite" className="input-base"
                           value={form.limitAmount || ''}
                           onChange={(e) => setForm({ ...form, limitAmount: Number(e.target.value) })} />
                    <button type="submit" disabled={create.isPending} className="btn-primary">
                        {create.isPending ? 'Salvando...' : 'Salvar'}
                    </button>
                </form>
            )}

            {isLoading ? (
                <p className="text-slate-400">Carregando...</p>
            ) : budgets.length === 0 ? (
                <div className="card flex flex-col items-center justify-center py-12 text-center">
                    <Target className="mb-3 text-slate-500" size={32} />
                    <p className="text-slate-300">Nenhum orçamento para este mês</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {budgets.map((b) => (
                        <div key={b.id} className="card">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-xs uppercase text-slate-500">{b.categoryName}</p>
                                    <p className="mt-1 text-lg font-medium text-white">
                                        {formatCurrency(b.spent)} <span className="text-sm text-slate-500">
                      / {formatCurrency(b.limitAmount)}
                    </span>
                                    </p>
                                </div>
                                <button onClick={() => remove.mutate(b.id)}
                                        className="rounded p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400">
                                    <Trash2 size={14} />
                                </button>
                            </div>
                            <div className="mt-3 h-2 overflow-hidden rounded-full bg-surface-800">
                                <div className={`h-full ${b.exceeded ? 'bg-negative' : b.percentageUsed > 80 ? 'bg-yellow-500' : 'bg-brand-500'}`}
                                     style={{ width: `${Math.min(100, b.percentageUsed)}%` }} />
                            </div>
                            <p className={`mt-1 text-xs ${b.exceeded ? 'text-negative' : 'text-slate-500'}`}>
                                {b.percentageUsed.toFixed(1)}% usado · Resta {formatCurrency(b.remaining)}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}