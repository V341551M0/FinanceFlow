import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Star, Trash2 } from 'lucide-react';
import { marketApi, type WatchlistEntry } from '@/api/market';
import type { AssetType } from '@/api/investments';
import { CandlestickChart } from '@/components/market/CandlestickChart';
import { formatCurrency } from '@/lib/format';

const intervals = [
    { key: '1m', label: '1m' }, { key: '5m', label: '5m' },
    { key: '15m', label: '15m' }, { key: '1h', label: '1h' },
    { key: '1d', label: '1D' }, { key: '1wk', label: '1S' },
];

export default function MarketPage() {
    const qc = useQueryClient();
    const [symbol, setSymbol] = useState('PETR4');
    const [type, setType] = useState<AssetType>('STOCK');
    const [interval, setInterval] = useState('1d');
    const [addSymbol, setAddSymbol] = useState('');
    const [addType, setAddType] = useState<AssetType>('STOCK');

    const { data: watchlist = [] } = useQuery({
        queryKey: ['watchlist'], queryFn: marketApi.watchlist,
        refetchInterval: 60_000,
    });

    const { data: quote } = useQuery({
        queryKey: ['quote', symbol, type],
        queryFn: () => marketApi.quote(symbol, type, type === 'STOCK' ? 'B3' : undefined),
        enabled: !!symbol,
        refetchInterval: 60_000,
    });

    const { data: candles = [], isLoading: loadingCandles } = useQuery({
        queryKey: ['candles', symbol, type, interval],
        queryFn: () => marketApi.candles(symbol, type, interval, 120),
        enabled: !!symbol,
    });

    const add = useMutation({
        mutationFn: () => marketApi.addToWatchlist(addSymbol, addType, addType === 'STOCK' ? 'B3' : undefined),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['watchlist'] });
            setAddSymbol('');
        },
    });

    const remove = useMutation({
        mutationFn: marketApi.removeFromWatchlist,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['watchlist'] }),
    });

    const select = (entry: WatchlistEntry) => {
        setSymbol(entry.symbol);
        setType(entry.type);
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-semibold text-white">Mercado</h1>
                <p className="mt-1 text-sm text-slate-400">
                    Cotações e gráficos. Dados podem estar atrasados conforme o provedor.
                </p>
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-4">
                <aside className="space-y-4 lg:col-span-1">
                    <div className="card space-y-2 p-4">
                        <p className="text-xs uppercase text-slate-500">Watchlist</p>
                        <div className="flex gap-2">
                            <input placeholder="PETR4" className="input-base" value={addSymbol}
                                   onChange={(e) => setAddSymbol(e.target.value.toUpperCase())} />
                            <select className="input-base" value={addType}
                                    onChange={(e) => setAddType(e.target.value as AssetType)}>
                                <option value="STOCK">Ação</option>
                                <option value="ETF">ETF</option>
                                <option value="FII">FII</option>
                                <option value="CRYPTO">Cripto</option>
                            </select>
                            <button onClick={() => add.mutate()} disabled={!addSymbol}
                                    className="btn-primary px-2">
                                <Plus size={14} />
                            </button>
                        </div>
                        <div className="space-y-1">
                            {watchlist.map((w) => (
                                <div key={w.id}
                                     className={`group flex items-center justify-between rounded-lg px-2 py-1.5 text-sm ${
                                         w.symbol === symbol ? 'bg-brand-600/10 text-brand-400' : 'text-slate-300 hover:bg-surface-800'
                                     }`}>
                                    <button onClick={() => select(w)} className="flex flex-1 items-center gap-2 text-left">
                                        <Star size={12} className={w.symbol === symbol ? 'fill-current' : ''} />
                                        <span className="font-medium">{w.symbol}</span>
                                        {w.price != null && (
                                            <span className={w.changePct24h != null && w.changePct24h < 0 ? 'text-negative' : 'text-positive'}>
                        {w.changePct24h != null ? `${w.changePct24h.toFixed(2)}%` : ''}
                      </span>
                                        )}
                                    </button>
                                    <button onClick={() => remove.mutate(w.id)}
                                            className="opacity-0 transition group-hover:opacity-100">
                                        <Trash2 size={12} className="text-slate-500 hover:text-red-400" />
                                    </button>
                                </div>
                            ))}
                            {watchlist.length === 0 && (
                                <p className="text-xs text-slate-500">Nenhum ativo na watchlist.</p>
                            )}
                        </div>
                    </div>
                </aside>

                <section className="space-y-4 lg:col-span-3">
                    <div className="card">
                        <div className="flex items-center justify-between">
                            <div>
                                <h2 className="text-lg font-semibold text-white">{symbol}</h2>
                                {quote && (
                                    <p className="text-sm text-slate-400">
                                        {quote.price != null ? formatCurrency(quote.price) : 'Cotação indisponível'}
                                        {quote.changePct24h != null && (
                                            <span className={quote.changePct24h < 0 ? 'ml-2 text-negative' : 'ml-2 text-positive'}>
                        {quote.changePct24h > 0 ? '+' : ''}{quote.changePct24h.toFixed(2)}%
                      </span>
                                        )}
                                        <span className="ml-2 text-xs text-slate-600">via {quote.source}</span>
                                    </p>
                                )}
                            </div>
                            <div className="flex gap-1">
                                {intervals.map((i) => (
                                    <button key={i.key} onClick={() => setInterval(i.key)}
                                            className={`rounded px-2 py-1 text-xs ${
                                                interval === i.key ? 'bg-brand-600 text-white' : 'text-slate-400 hover:bg-surface-800'
                                            }`}>{i.label}</button>
                                ))}
                            </div>
                        </div>

                        <div className="mt-4">
                            {loadingCandles ? (
                                <p className="py-16 text-center text-slate-500">Carregando gráfico...</p>
                            ) : candles.length === 0 ? (
                                <p className="py-16 text-center text-sm text-slate-500">
                                    Dados históricos indisponíveis para este ativo/intervalo.
                                </p>
                            ) : (
                                <CandlestickChart candles={candles} />
                            )}
                        </div>
                    </div>
                </section>
            </div>
        </div>
    );
}