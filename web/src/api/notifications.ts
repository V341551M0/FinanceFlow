import { http } from './http';

export type NotificationType =
    | 'BUDGET_WARNING' | 'BUDGET_EXCEEDED'
    | 'INVOICE_DUE' | 'GOAL_REACHED' | 'SYSTEM';

export interface AppNotification {
    id: number;
    type: NotificationType;
    title: string;
    message: string;
    read: boolean;
    createdAt: string;
}

export const notificationsApi = {
    list: () => http.get<AppNotification[]>('/notifications').then((r) => r.data),
    unreadCount: () => http.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count),
    markRead: (id: number) => http.post(`/notifications/${id}/read`).then((r) => r.data),
    markAllRead: () => http.post('/notifications/read-all').then((r) => r.data),
    remove: (id: number) => http.delete(`/notifications/${id}`).then((r) => r.data),
};