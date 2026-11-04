import { http } from './http';

export interface CreditCard {
    id: number; name: string; brand: string; lastDigits: string | null;
    creditLimit: number; closingDay: number; dueDay: number;
    usedLimit: number; availableLimit: number; active: boolean;
}

export interface Invoice {
    id: number; cardId: number; referenceMonth: string;
    closingDate: string; dueDate: string; totalAmount: number;
    status: 'OPEN' | 'CLOSED' | 'PAID';
}

export interface CreateCard {
    name: string; brand: string; lastDigits?: string;
    creditLimit: number; closingDay: number; dueDay: number;
}

export const creditCardsApi = {
    list: () => http.get<CreditCard[]>('/credit-cards').then((r) => r.data),
    create: (p: CreateCard) => http.post<CreditCard>('/credit-cards', p).then((r) => r.data),
    remove: (id: number) => http.delete(`/credit-cards/${id}`).then((r) => r.data),
    invoices: (id: number) => http.get<Invoice[]>(`/credit-cards/${id}/invoices`).then((r) => r.data),
    purchase: (id: number, p: {
        categoryId?: number; description: string; amount: number;
        transactionDate: string; installments: number;
    }) => http.post(`/credit-cards/${id}/purchases`, p).then((r) => r.data),
    payInvoice: (invoiceId: number, accountId: number) =>
        http.post(`/credit-cards/invoices/${invoiceId}/pay?accountId=${accountId}`).then((r) => r.data),
};