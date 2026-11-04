import { http } from './http';

export interface Goal {
    id: number; name: string;
    targetAmount: number; currentAmount: number;
    percentage: number; targetDate: string | null; completed: boolean;
}

export interface CreateGoal {
    name: string; targetAmount: number; targetDate?: string;
}

export const goalsApi = {
    list: () => http.get<Goal[]>('/goals').then((r) => r.data),
    create: (p: CreateGoal) => http.post<Goal>('/goals', p).then((r) => r.data),
    contribute: (id: number, amount: number) =>
        http.post<Goal>(`/goals/${id}/contribute`, { amount }).then((r) => r.data),
    remove: (id: number) => http.delete(`/goals/${id}`).then((r) => r.data),
};