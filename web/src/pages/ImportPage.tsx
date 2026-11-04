import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Upload, FileCheck, AlertTriangle, Check } from 'lucide-react';
import { accountsApi } from '@/api/accounts';
import { statementsApi, type PreviewResponse, type ImportResult } from '@/api/statements';
import { formatCurrency } from '@/lib/format';

export default function ImportPage() {
  const [accountId, setAccountId] = useState<number | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<PreviewResponse | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);

  const { data: accounts = [] } = useQuery({ queryKey: ['accounts'], queryFn: accountsApi.list });

  const previewMut = useMutation({
    mutationFn: ({ id, f }: { id: number; f: File }) => statementsApi.preview(id, f),
    onSuccess: setPreview,
  });

  const importMut = useMutation({
    mutationFn: ({ id, f }: { id: number; f: File }) => statementsApi.importFile(id, f),
    onSuccess: (r) => { setResult(r); setPreview(null); setFile(null); },
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-white">Importar extrato</h1>
        <p className="mt-1 text-sm text-slate-400">
          Envie um CSV do seu banco para importar movimentações em lote.
        </p>
      </div>

      <div className="card space-y-4">
        <div>
          <label className="mb-1 block text-xs font-medium text-slate-400">Conta destino</label>
          <select className="input-base" value={accountId ?? ''}
            onChange={(e) => setAccountId(Number(e.target.value) || null)}>
            <option value="">Selecione...</option>
            {accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-xs font-medium text-slate-400">Arquivo (.csv)</label>
          <input type="file" accept=".csv"
            onChange={(e) => { setFile(e.target.files?.[0] ?? null); setPreview(null); setResult(null); }}
            className="input-base" />
        </div>

        <button
          disabled={!accountId || !file || previewMut.isPending}
          onClick={() => previewMut.mutate({ id: accountId!, f: file! })}
          className="btn-primary"
        >
          <Upload size={16} /> Pré-visualizar
        </button>
      </div>

      {preview && (
        <div className="card space-y-4">
          <div className="flex items-center gap-3">
            <FileCheck className="text-brand-400" size={20} />
            <h2 className="text-lg font-semibold text-white">Pré-visualização</h2>
            {preview.reconciled ? (
              <span className="rounded-full bg-positive/10 px-2 py-0.5 text-xs text-positive">Reconciliado</span>
            ) : (
              <span className="rounded-full bg-negative/10 px-2 py-0.5 text-xs text-negative">Divergência</span>
            )}
          </div>

          <div className="grid grid-cols-4 gap-4 text-sm">
            <Stat label="Total" value={preview.totalRows} />
            <Stat label="Válidos" value={preview.validRows} color="text-positive" />
            <Stat label="Duplicados" value={preview.duplicateRows} color="text-yellow-400" />
            <Stat label="Inválidos" value={preview.invalidRows} color="text-negative" />
          </div>

          {preview.declaredFinalBalance !== null && (
            <div className="text-sm text-slate-400">
              Saldo final declarado: <span className="text-slate-100">
                {formatCurrency(preview.declaredFinalBalance)}
              </span> · Projetado: <span className="text-slate-100">
                {formatCurrency(preview.projectedBalance)}
              </span>
            </div>
          )}

          <div className="max-h-80 overflow-auto rounded-lg border border-surface-700">
            <table className="w-full text-sm">
              <thead className="bg-surface-800/50 text-left text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-3 py-2">Data</th>
                  <th className="px-3 py-2">Descrição</th>
                  <th className="px-3 py-2 text-right">Valor</th>
                  <th className="px-3 py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {preview.rows.map((r, i) => (
                  <tr key={i} className="border-t border-surface-800">
                    <td className="px-3 py-2 text-slate-400">{r.date ?? '—'}</td>
                    <td className="px-3 py-2 text-slate-100">{r.description}</td>
                    <td className="px-3 py-2 text-right text-slate-100">
                      {r.amount !== null ? formatCurrency(r.amount) : '—'}
                    </td>
                    <td className="px-3 py-2 text-xs">
                      {r.error ? <span className="text-negative">{r.error}</span>
                        : r.duplicate ? <span className="text-yellow-400">duplicado</span>
                        : <span className="text-positive">ok</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <button
            disabled={importMut.isPending || preview.validRows === 0}
            onClick={() => importMut.mutate({ id: accountId!, f: file! })}
            className="btn-primary"
          >
            Importar {preview.validRows} transações
          </button>
        </div>
      )}

      {result && (
        <div className="card flex items-start gap-3 border-positive/30 bg-positive/5">
          <Check className="mt-0.5 text-positive" size={20} />
          <div className="text-sm">
            <p className="font-medium text-white">Importação concluída</p>
            <p className="mt-1 text-slate-300">
              {result.imported} importadas · {result.skippedDuplicates} duplicadas ·{' '}
              {result.skippedInvalid} inválidas
            </p>
            <p className="mt-1 text-slate-400">
              Novo saldo da conta: <span className="text-slate-100">
                {formatCurrency(result.newAccountBalance)}
              </span>
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, color = 'text-white' }: { label: string; value: number; color?: string }) {
  return (
    <div>
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`text-xl font-semibold ${color}`}>{value}</p>
    </div>
  );
}