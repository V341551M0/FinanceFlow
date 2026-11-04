import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Target, Trash2, Check } from 'lucide-react';
import { goalsApi, type CreateGoal } from '@/api/goals';
import { formatCurrency } from '@/lib/format';

export default function GoalsPage() {
    const qc = useQueryClient();
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<CreateGoal>({ name: '', targetAmount: 0 });
    const [contributeId, setContributeId] = useState<number | null>(null);
    const [contribAmount, setContribAmount] = useState(0);

    const { data: goals = [], isLoading } = useQuery({
        queryKey: ['goals'], queryFn: goalsApi.list,
    });

    const create = useMutation({
        mutationFn: goalsApi.create,
        onSuccess: () => { qc.invalidateQueries({ queryKey: ['goals'] }); setShowForm(false); setForm({ name: '', targetAmount: 0 }); },
    });
    const contribute = useMutation({
        mutationFn: ({ id, amount }: { id: number; amount: number }) => goalsApi.contribute(id, amount),
        onSuccess: () => { qc.invalidateQueries({ queryKey: ['goals'] }); setContributeId(null); setContribAmount(0); },
    });
    const remove = useMutation({
        mutationFn: goalsApi.remove,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['goals'] }),
    });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Metas financeiras</h1>
                    <p className="mt-1 text-sm text-slate-400">Seus objetivos de longo prazo</p>
                </div>
                <button onClick={() => setShowForm((v) => !v)} className="btn-primary">
                    <Plus size={16} /> Nova meta
                </button>
            </div>

            {showForm && (
                <form onSubmit={(e) => { e.preventDefault(); create.mutate(form); }}
                      className="card grid grid-cols-1 gap-3 md:grid-cols-3">
                    <input required placeholder="Nome" className="input-base" value={form.name}
                           onChange={(e) => setForm({ ...form, name: e.target.value })} />
                    <input required type="number" step="0.01" placeholder="Valor alvo" className="input-base"
                           value={form.targetAmount || ''}
                           onChange={(e) => setForm({ ...form, targetAmount: Number(e.target.value) })} />
                    <input type="date" className="input-base" value={form.targetDate ?? ''}
                           onChange={(e) => setForm({ ...form, targetDate: e.target.value || undefined })} />
                    <button type="submit" disabled={create.isPending} className="btn-primary md:col-span-3">
                        {create.isPending ? 'Salvando...' : 'Salvar'}
                    </button>
                </form>
            )}

            {isLoading ? (
                <p className="text-slate-400">Carregando...</p>
            ) : goals.length === 0 ? (
                <div className="card flex flex-col items-center justify-center py-12 text-center">
                    <Target className="mb-3 text-slate-500" size={32} />
                    <p className="text-slate-300">Nenhuma meta cadastrada</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                    {goals.map((g) => (
                        <div key={g.id} className={`card ${g.completed ? 'border-positive/40' : ''}`}>
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-lg font-medium text-white">{g.name}</p>
                                    {g.targetDate && <p className="text-xs text-slate-500">Até {g.targetDate}</p>}
                                </div>
                                <div className="flex items-center gap-1">
                                    {g.completed
                                        ? <span className="rounded-full bg-positive/10 px-2 py-0.5 text-xs text-positive">Concluída</span>
                                        : <button onClick={() => remove.mutate(g.id)}
                                                  className="rounded p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400">
                                            <Trash2 size={14} />
                                        </button>}
                                </div>
                            </div>
                            <p className="mt-3 text-xl font-semibold text-white">
                                {formatCurrency(g.currentAmount)}{' '}
                                <span className="text-sm text-slate-500">/ {formatCurrency(g.targetAmount)}</span>
                            </p>
                            <div className="mt-2 h-2 overflow-hidden rounded-full bg-surface-800">
                                <div className={`h-full ${g.completed ? 'bg-positive' : 'bg-brand-500'}`}
                                     style={{ width: `${Math.min(100, g.percentage)}%` }} />
                            </div>
                            <p className="mt-1 text-xs text-slate-500">{g.percentage.toFixed(1)}% concluído</p>

                            {!g.completed && (
                                contributeId === g.id ? (
                                    <div className="mt-3 flex gap-2">
                                        <input type="number" step="0.01" className="input-base" value={contribAmount || ''}
                                               onChange={(e) => setContribAmount(Number(e.target.value))} />
                                        <button onClick={() => contribute.mutate({ id: g.id, amount: contribAmount })}
                                                className="btn-primary" disabled={contribAmount <= 0}>
                                            <Check size={14} /> Confirmar
                                        </button>
                                    </div>
                                ) : (
                                    <button onClick={() => setContributeId(g.id)}
                                            className="btn-primary mt-3 w-full">
                                        Contribuir
                                    </button>
                                )
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}