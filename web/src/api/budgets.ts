import { http } from './http';

export interface Budget {
    id: number; categoryId: number | null; categoryName: string;
    color: string | null; limitAmount: number; spent: number;
    remaining: number; percentageUsed: number; exceeded: boolean;
}

export interface CreateBudget {
    categoryId: number | null; referenceMonth: string; limitAmount: number;
}

export const budgetsApi = {
    list: (month?: string) =>
        http.get<Budget[]>('/budgets', { params: month ? { month } : {} }).then((r) => r.data),
    create: (p: CreateBudget) => http.post<Budget>('/budgets', p).then((r) => r.data),
    remove: (id: number) => http.delete(`/budgets/${id}`).then((r) => r.data),
};