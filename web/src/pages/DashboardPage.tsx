import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { dashboardApi, type PeriodPreset } from '@/api/dashboard';
import { PeriodSelector } from '@/components/dashboard/PeriodSelector';
import { SummaryCards } from '@/components/dashboard/SummaryCards';
import { formatDate } from '@/lib/format';

export default function DashboardPage() {
    const [preset, setPreset] = useState<PeriodPreset>('THIS_MONTH');
    const [from, setFrom] = useState('');
    const [to, setTo] = useState('');

    const { data, isLoading, isError } = useQuery({
        queryKey: ['dashboard', preset, from, to],
        queryFn: () =>
            dashboardApi.get({
                preset,
                from: preset === 'CUSTOM' ? from : undefined,
                to: preset === 'CUSTOM' ? to : undefined,
            }),
        enabled: preset !== 'CUSTOM' || (!!from && !!to),
    });

    const handlePeriod = (p: PeriodPreset, f?: string, t?: string) => {
        setPreset(p);
        if (f) setFrom(f);
        if (t) setTo(t);
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-white">Dashboard</h1>
                    {data && (
                        <p className="mt-1 text-sm text-slate-400">
                            Período analisado: {formatDate(data.summary.startDate)} até{' '}
                            {formatDate(data.summary.endDate)}
                        </p>
                    )}
                </div>
                <PeriodSelector value={preset} from={from} to={to} onChange={handlePeriod} />
            </div>

            {isLoading && <p className="text-slate-400">Carregando indicadores...</p>}
            {isError && <p className="text-negative">Não foi possível carregar o dashboard.</p>}

            {data && (
                <>
                    <SummaryCards summary={data.summary} />
                    {/* Gráficos entram no próximo commit */}
                </>
            )}
        </div>
    );
}