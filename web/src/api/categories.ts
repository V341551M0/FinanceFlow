import { http } from './http';

export interface Category {
    id: number; name: string; type: 'INCOME' | 'EXPENSE'; color: string | null; icon: string | null;
}

export const categoriesApi = {
    list: () => http.get<Category[]>('/categories').then((r) => r.data),
    create: (p: { name: string; type: string; color?: string }) =>
        http.post<Category>('/categories', p).then((r) => r.data),
    remove: (id: number) => http.delete(`/categories/${id}`).then((r) => r.data),
};