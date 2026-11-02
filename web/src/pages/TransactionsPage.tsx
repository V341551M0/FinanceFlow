import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDownCircle, ArrowUpCircle, Receipt, Trash2 } from 'lucide-react';
import { transactionsApi } from '@/api/transactions';
import { formatCurrency, formatDate } from '@/lib/format';

export default function TransactionsPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const size = 20;

  const { data, isLoading } = useQuery({
    queryKey: ['transactions', { page, q }],
    queryFn: () => transactionsApi.list({ page, size, q: q || undefined }),
  });

  const remove = useMutation({
    mutationFn: transactionsApi.remove,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['transactions'] }),
  });

  const rows = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">Transações</h1>
        <p className="mt-1 text-sm text-slate-400">Suas movimentações financeiras</p>
      </div>

      <input
        className="input-base max-w-sm"
        placeholder="Buscar por descrição..."
        value={q}
        onChange={(e) => { setQ(e.target.value); setPage(0); }}
      />

      {isLoading ? (
        <p className="text-slate-400">Carregando...</p>
      ) : rows.length === 0 ? (
        <div className="card flex flex-col items-center justify-center py-12 text-center">
          <Receipt className="mb-3 text-slate-500" size={32} />
          <p className="text-slate-300">Nenhuma transação encontrada</p>
        </div>
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full text-sm">
            <thead className="border-b border-surface-700 bg-surface-800/50">
              <tr className="text-left text-xs uppercase text-slate-500">
                <th className="px-4 py-3">Data</th>
                <th className="px-4 py-3">Descrição</th>
                <th className="px-4 py-3">Conta</th>
                <th className="px-4 py-3">Categoria</th>
                <th className="px-4 py-3 text-right">Valor</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => (
                <tr key={t.id} className="border-b border-surface-800 hover:bg-surface-800/30">
                  <td className="px-4 py-3 text-slate-400">{formatDate(t.transactionDate)}</td>
                  <td className="px-4 py-3 text-slate-100">{t.description}</td>
                  <td className="px-4 py-3 text-slate-400">{t.accountName}</td>
                  <td className="px-4 py-3 text-slate-400">{t.categoryName ?? '—'}</td>
                  <td className="px-4 py-3 text-right">
                    <span className={`inline-flex items-center gap-1 ${
                      t.direction === 'CREDIT' ? 'text-positive' : 'text-negative'
                    }`}>
                      {t.direction === 'CREDIT'
                        ? <ArrowUpCircle size={14} />
                        : <ArrowDownCircle size={14} />}
                      {formatCurrency(t.amount)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => remove.mutate(t.id)}
                      className="rounded p-1.5 text-slate-500 hover:bg-red-500/10 hover:text-red-400"
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-slate-400">
          <span>{data.totalElements} transações</span>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="btn-primary">
              Anterior
            </button>
            <span className="self-center">Página {page + 1} / {data.totalPages}</span>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="btn-primary"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  );
}