import { useState } from 'react';
import { Download, FileText } from 'lucide-react';
import { reportsApi, type ReportFormat } from '@/api/reports';

const firstOfMonth = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
};
const today = () => new Date().toISOString().slice(0, 10);

export default function ReportsPage() {
    const [from, setFrom] = useState(firstOfMonth());
    const [to, setTo] = useState(today());
    const [loading, setLoading] = useState<ReportFormat | null>(null);
    const [error, setError] = useState<string | null>(null);

    const download = async (format: ReportFormat) => {
        setError(null);
        setLoading(format);
        try {
            await reportsApi.download(format, from, to);
        } catch (e) {
            setError('Não foi possível gerar o relatório. Tente novamente.');
        } finally {
            setLoading(null);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-semibold text-white">Relatórios</h1>
                <p className="mt-1 text-sm text-slate-400">
                    Exporte suas transações em CSV, XLSX ou PDF.
                </p>
            </div>

            <div className="card space-y-4">
                <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                    <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">Data inicial</label>
                        <input type="date" className="input-base" value={from}
                               onChange={(e) => setFrom(e.target.value)} />
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-medium text-slate-400">Data final</label>
                        <input type="date" className="input-base" value={to}
                               onChange={(e) => setTo(e.target.value)} />
                    </div>
                </div>

                {error && (
                    <p className="rounded-lg bg-red-500/10 px-3 py-2 text-xs text-negative">{error}</p>
                )}

                <div>
                    <p className="mb-2 text-xs font-medium text-slate-400">Formato</p>
                    <div className="flex flex-wrap gap-2">
                        <FormatButton format="CSV" label="CSV" hint="Planilha simples" loading={loading} onClick={download} />
                        <FormatButton format="XLSX" label="XLSX" hint="Excel" loading={loading} onClick={download} />
                        <FormatButton format="PDF" label="PDF" hint="Documento formatado" loading={loading} onClick={download} />
                    </div>
                </div>
            </div>

            <div className="card flex items-start gap-3 border-brand-500/30 bg-brand-500/5">
                <FileText className="mt-0.5 text-brand-400" size={18} />
                <div className="text-sm text-slate-300">
                    <p className="font-medium text-white">O que está incluído</p>
                    <ul className="mt-1 list-inside list-disc text-xs text-slate-400">
                        <li>Transações do período (exceto ignoradas)</li>
                        <li>Totais de receitas, despesas e resultado líquido</li>
                        <li>Descrição, categoria, conta, tipo e direção</li>
                    </ul>
                </div>
            </div>
        </div>
    );
}

function FormatButton({ format, label, hint, loading, onClick }: {
    format: ReportFormat; label: string; hint: string;
    loading: ReportFormat | null; onClick: (f: ReportFormat) => void;
}) {
    const isLoading = loading === format;
    const disabled = loading !== null;
    return (
        <button onClick={() => onClick(format)} disabled={disabled}
                className="group flex flex-1 flex-col items-start rounded-lg border border-surface-700 bg-surface-800/50 p-3 text-left transition hover:border-brand-500 hover:bg-surface-800 disabled:opacity-50">
            <div className="flex items-center gap-2">
                <Download size={16} className="text-brand-400" />
                <span className="font-medium text-white">{label}</span>
            </div>
            <span className="mt-0.5 text-xs text-slate-500">{hint}</span>
            {isLoading && <span className="mt-1 text-xs text-brand-400">Baixando...</span>}
        </button>
    );
}