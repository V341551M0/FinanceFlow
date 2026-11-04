import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Wallet } from 'lucide-react';
import { accountsApi } from '@/api/accounts';
import type { AccountType, CreateAccountPayload } from '@/types';
import { formatCurrency } from '@/lib/format';

const emptyForm: CreateAccountPayload = {
  name: '', institution: '', type: 'CHECKING', currency: 'BRL', initialBalance: 0,
};

export default function AccountsPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CreateAccountPayload>(emptyForm);

  const { data: accounts = [], isLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: accountsApi.list,
  });

  const create = useMutation({
    mutationFn: accountsApi.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] });
      setForm(emptyForm);
      setShowForm(false);
    },
  });

  const remove = useMutation({
    mutationFn: accountsApi.remove,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['accounts'] }),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-white">Contas</h1>
          <p className="mt-1 text-sm text-slate-400">Suas contas bancárias e carteiras</p>
        </div>
        <button onClick={() => setShowForm((v) => !v)} className="btn-primary">
          <Plus size={16} /> Nova conta
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={(e) => { e.preventDefault(); create.mutate(form); }}
          className="card grid grid-cols-1 gap-4 md:grid-cols-5"
        >
          <input required placeholder="Nome" className="input-base"
            value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input required placeholder="Instituição" className="input-base"
            value={form.institution} onChange={(e) => setForm({ ...form, institution: e.target.value })} />
          <select className="input-base" value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as AccountType })}>
            <option value="CHECKING">Conta Corrente</option>
            <option value="SAVINGS">Poupança</option>
            <option value="PAYMENT">Pagamento</option>
            <option value="WALLET">Carteira</option>
            <option value="INVESTMENT">Investimento</option>
          </select>
          <input type="number" step="0.01" placeholder="Saldo inicial" className="input-base"
            value={form.initialBalance}
            onChange={(e) => setForm({ ...form, initialBalance: Number(e.target.value) })} />
          <button type="submit" disabled={create.isPending} className="btn-primary">
            {create.isPending ? 'Salvando...' : 'Salvar'}
          </button>
        </form>
      )}

      {isLoading ? (
        <p className="text-slate-400">Carregando...</p>
      ) : accounts.length === 0 ? (
        <div className="card flex flex-col items-center justify-center py-12 text-center">
          <Wallet className="mb-3 text-slate-500" size={32} />
          <p className="text-slate-300">Nenhuma conta cadastrada</p>
          <p className="mt-1 text-xs text-slate-500">Clique em "Nova conta" para começar</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {accounts.map((acc) => (
            <div key={acc.id} className="card flex flex-col justify-between">
              <div>
                <p className="text-xs uppercase text-slate-500">{acc.institution}</p>
                <p className="mt-1 text-lg font-medium text-white">{acc.name}</p>
                <p className="mt-3 text-2xl font-semibold text-white">
                  {formatCurrency(acc.currentBalance, acc.currency)}
                </p>
              </div>
              <div className="mt-4 flex items-center justify-between">
                <span className="rounded-full bg-surface-800 px-2 py-0.5 text-xs text-slate-400">
                  {acc.type}
                </span>
                <button
                  onClick={() => remove.mutate(acc.id)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}