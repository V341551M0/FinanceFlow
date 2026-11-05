import { http } from './http';
import type { AssetType } from './investments';

export interface Quote {
    price: number | null;
    changePct24h: number | null;
    source: string;
    asOf: string | null;
}

export interface Candle {
    time: number;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
}

export interface WatchlistEntry {
    id: number;
    symbol: string;
    name: string;
    type: AssetType;
    exchange: string | null;
    price: number | null;
    changePct24h: number | null;
    source: string;
    asOf: string | null;
}

export const marketApi = {
    quote: (symbol: string, type: AssetType, exchange?: string) =>
        http.get<Quote>('/market/quote', { params: { symbol, type, exchange } }).then((r) => r.data),
    candles: (symbol: string, type: AssetType, interval = '1d', limit = 120, exchange?: string) =>
        http.get<Candle[]>('/market/candles', { params: { symbol, type, interval, limit, exchange } })
            .then((r) => r.data),
    watchlist: () => http.get<WatchlistEntry[]>('/market/watchlist').then((r) => r.data),
    addToWatchlist: (symbol: string, type: AssetType, exchange?: string) =>
        http.post('/market/watchlist', null, { params: { symbol, type, exchange } }).then((r) => r.data),
    removeFromWatchlist: (id: number) =>
        http.delete(`/market/watchlist/${id}`).then((r) => r.data),
};