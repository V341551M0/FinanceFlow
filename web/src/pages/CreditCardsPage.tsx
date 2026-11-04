import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CreditCard as CardIcon, Plus, Trash2 } from 'lucide-react';
import { creditCardsApi, type CreateCard } from '@/api/creditCards';
import { formatCurrency } from '@/lib/format';

const empty: CreateCard = {
    name: '', brand: 'VISA', lastDigits: '', creditLimit: 0, closingDay: 5, dueDay: 12,
};

export default function CreditCardsPage() {
    const qc = useQueryClient();
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState<CreateCard>(empty);

    const { data: cards = [], isLoading } = useQuery({
        queryKey: ['cards'], queryFn: creditCardsApi.list,
    });

    const create = useMutation({
        mutationFn: creditCardsApi.create,
        onSuccess: () => { qc.invalidateQueries({ queryKey: ['cards'] }); setForm(empty); setShowForm(false); },
    });

    const remove = useMutation({
        mutationFn: creditCardsApi.remove,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['cards'] }),
    });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Cartões de Crédito</h1>
                    <p className="mt-1 text-sm text-slate-400">Limites, faturas e parcelamentos</p>
                </div>
                <button onClick={() => setShowForm((v) => !v)} className="btn-primary">
                    <Plus size={16} /> Novo cartão
                </button>
            </div>

            {showForm && (
                <form onSubmit={(e) => { e.preventDefault(); create.mutate(form); }}
                      className="card grid grid-cols-1 gap-3 md:grid-cols-3">
                    <input required placeholder="Nome" className="input-base" value={form.name}
                           onChange={(e) => setForm({ ...form, name: e.target.value })} />
                    <input required placeholder="Bandeira (VISA, Master...)" className="input-base" value={form.brand}
                           onChange={(e) => setForm({ ...form, brand: e.target.value })} />
                    <input maxLength={4} placeholder="4 últimos dígitos" className="input-base" value={form.lastDigits ?? ''}
                           onChange={(e) => setForm({ ...form, lastDigits: e.target.value })} />
                    <input required type="number" step="0.01" placeholder="Limite" className="input-base"
                           value={form.creditLimit || ''}
                           onChange={(e) => setForm({ ...form, creditLimit: Number(e.target.value) })} />
                    <input required type="number" min={1} max={28} placeholder="Fechamento (dia)" className="input-base"
                           value={form.closingDay} onChange={(e) => setForm({ ...form, closingDay: Number(e.target.value) })} />
                    <input required type="number" min={1} max={28} placeholder="Vencimento (dia)" className="input-base"
                           value={form.dueDay} onChange={(e) => setForm({ ...form, dueDay: Number(e.target.value) })} />
                    <button type="submit" disabled={create.isPending} className="btn-primary md:col-span-3">
                        {create.isPending ? 'Salvando...' : 'Salvar'}
                    </button>
                </form>
            )}

            {isLoading ? (
                <p className="text-slate-400">Carregando...</p>
            ) : cards.length === 0 ? (
                <div className="card flex flex-col items-center justify-center py-12 text-center">
                    <CardIcon className="mb-3 text-slate-500" size={32} />
                    <p className="text-slate-300">Nenhum cartão cadastrado</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                    {cards.map((c) => (
                        <div key={c.id} className="card">
                            <div className="flex items-start justify-between">
                                <div>
                                    <p className="text-xs uppercase text-slate-500">{c.brand}{c.lastDigits ? ` ••${c.lastDigits}` : ''}</p>
                                    <p className="mt-1 text-lg font-medium text-white">{c.name}</p>
                                </div>
                                <button onClick={() => remove.mutate(c.id)}
                                        className="rounded p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400">
                                    <Trash2 size={14} />
                                </button>
                            </div>
                            <div className="mt-3 space-y-1 text-sm">
                                <Row label="Limite" value={formatCurrency(c.creditLimit)} />
                                <Row label="Usado" value={formatCurrency(c.usedLimit)} className="text-negative" />
                                <Row label="Disponível" value={formatCurrency(c.availableLimit)} className="text-positive" />
                            </div>
                            <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-surface-800">
                                <div className="h-full bg-brand-500"
                                     style={{ width: `${Math.min(100, (c.usedLimit / c.creditLimit) * 100)}%` }} />
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

function Row({ label, value, className = 'text-white' }: { label: string; value: string; className?: string }) {
    return (
        <div className="flex justify-between">
            <span className="text-slate-400">{label}</span>
            <span className={className}>{value}</span>
        </div>
    );
}