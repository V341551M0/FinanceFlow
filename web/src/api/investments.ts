import { http } from './http';

export type AssetType = 'STOCK' | 'ETF' | 'FII' | 'BDR' | 'CRYPTO' | 'FIXED_INCOME' | 'FUND';

export interface Position {
    investmentId: number;
    assetId: number;
    symbol: string;
    name: string;
    type: AssetType;
    exchange: string | null;
    currency: string;
    quantity: number;
    averagePrice: number;
    currentPrice: number | null;
    investedValue: number;
    marketValue: number | null;
    profitLoss: number | null;
    profitLossPct: number | null;
    priceSource: string;
    priceUpdatedAt: string | null;
}

export interface CreateInvestment {
    symbol: string; name: string; type: AssetType;
    exchange?: string; currency: string;
    quantity: number; price: number;
    operationDate: string; fees?: number;
}

export const investmentsApi = {
    list: () => http.get<Position[]>('/investments').then((r) => r.data),
    create: (p: CreateInvestment) => http.post<Position>('/investments', p).then((r) => r.data),
    remove: (id: number) => http.delete(`/investments/${id}`).then((r) => r.data),
    addOperation: (id: number, p: {
        type: 'BUY' | 'SELL'; quantity: number; price: number;
        operationDate: string; fees?: number; notes?: string;
    }) => http.post<Position>(`/investments/${id}/operations`, p).then((r) => r.data),
};