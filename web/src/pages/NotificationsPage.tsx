import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, Check, CheckCheck, Trash2 } from 'lucide-react';
import { notificationsApi, type AppNotification } from '@/api/notifications';

const typeColors: Record<string, string> = {
    BUDGET_WARNING: 'border-yellow-500/40 bg-yellow-500/5',
    BUDGET_EXCEEDED: 'border-red-500/40 bg-red-500/5',
    INVOICE_DUE: 'border-brand-500/40 bg-brand-500/5',
    GOAL_REACHED: 'border-positive/40 bg-positive/5',
    SYSTEM: 'border-surface-700',
};

export default function NotificationsPage() {
    const qc = useQueryClient();
    const { data: list = [], isLoading } = useQuery({
        queryKey: ['notifications'], queryFn: notificationsApi.list,
    });

    const markRead = useMutation({
        mutationFn: notificationsApi.markRead,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
    });

    const markAll = useMutation({
        mutationFn: notificationsApi.markAllRead,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
    });

    const remove = useMutation({
        mutationFn: notificationsApi.remove,
        onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
    });

    const unread = list.filter((n) => !n.read).length;

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Notificações</h1>
                    <p className="mt-1 text-sm text-slate-400">
                        {unread > 0 ? `${unread} não lida(s)` : 'Tudo em dia'}
                    </p>
                </div>
                {unread > 0 && (
                    <button onClick={() => markAll.mutate()} className="btn-primary">
                        <CheckCheck size={16} /> Marcar todas como lidas
                    </button>
                )}
            </div>

            {isLoading ? (
                <p className="text-slate-400">Carregando...</p>
            ) : list.length === 0 ? (
                <div className="card flex flex-col items-center justify-center py-12 text-center">
                    <Bell className="mb-3 text-slate-500" size={32} />
                    <p className="text-slate-300">Nenhuma notificação</p>
                    <p className="mt-1 text-xs text-slate-500">Alertas aparecerão aqui.</p>
                </div>
            ) : (
                <div className="space-y-2">
                    {list.map((n) => (
                        <NotificationCard key={n.id} n={n}
                                          onMarkRead={() => markRead.mutate(n.id)}
                                          onDelete={() => remove.mutate(n.id)} />
                    ))}
                </div>
            )}
        </div>
    );
}

function NotificationCard({ n, onMarkRead, onDelete }: {
    n: AppNotification; onMarkRead: () => void; onDelete: () => void;
}) {
    const border = typeColors[n.type] ?? 'border-surface-700';
    return (
        <div className={`card flex items-start gap-3 border ${border} ${n.read ? 'opacity-60' : ''}`}>
            <Bell className={`mt-0.5 ${n.read ? 'text-slate-500' : 'text-brand-400'}`} size={18} />
            <div className="flex-1">
                <div className="flex items-center justify-between">
                    <p className={`font-medium ${n.read ? 'text-slate-300' : 'text-white'}`}>{n.title}</p>
                    <span className="text-xs text-slate-500">
            {new Date(n.createdAt).toLocaleString('pt-BR')}
          </span>
                </div>
                <p className="mt-1 text-sm text-slate-400">{n.message}</p>
            </div>
            <div className="flex gap-1">
                {!n.read && (
                    <button onClick={onMarkRead}
                            className="rounded p-1.5 text-slate-500 hover:bg-surface-800 hover:text-positive"
                            title="Marcar como lida">
                        <Check size={14} />
                    </button>
                )}
                <button onClick={onDelete}
                        className="rounded p-1.5 text-slate-500 hover:bg-surface-800 hover:text-negative"
                        title="Excluir">
                    <Trash2 size={14} />
                </button>
            </div>
        </div>
    );
}