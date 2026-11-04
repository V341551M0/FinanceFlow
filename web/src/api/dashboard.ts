import { http } from './http';

export type PeriodPreset =
    | 'THIS_WEEK' | 'LAST_WEEK'
    | 'THIS_MONTH' | 'LAST_MONTH'
    | 'LAST_3_MONTHS' | 'LAST_6_MONTHS'
    | 'THIS_YEAR' | 'LAST_YEAR' | 'LAST_12_MONTHS'
    | 'CUSTOM';

export interface DashboardSummary {
    totalPatrimony: number;
    grossIncome: number;
    refunds: number;
    netIncome: number;
    grossExpense: number;
    netExpense: number;
    netResult: number;
    savingsRate: number;
    previousNetResult: number;
    netResultChangePct: number;
    previousNetExpense: number;
    netExpenseChangePct: number;
    avgDailyExpense: number;
    startDate: string;
    endDate: string;
}

export interface TimeSeriesPoint {
    date: string;
    income: number;
    expense: number;
    balance: number;
}

export interface CategoryBreakdown {
    categoryId: number;
    categoryName: string;
    color: string | null;
    amount: number;
    percentage: number;
}

export interface DashboardResponse {
    summary: DashboardSummary;
    evolution: TimeSeriesPoint[];
    expenseByCategory: CategoryBreakdown[];
    incomeByCategory: CategoryBreakdown[];
}

export interface DashboardParams {
    preset: PeriodPreset;
    from?: string;
    to?: string;
}

export const dashboardApi = {
    get: (params: DashboardParams) =>
        http.get<DashboardResponse>('/dashboard', { params }).then((r) => r.data),
};