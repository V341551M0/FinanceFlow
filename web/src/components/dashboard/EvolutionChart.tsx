import ReactECharts from 'echarts-for-react';
import type { TimeSeriesPoint } from '@/api/dashboard';
import { palette, chartBase } from '@/components/charts/theme';
import { formatCurrency } from '@/lib/format';

export function EvolutionChart({ data }: { data: TimeSeriesPoint[] }) {
    const option = {
        ...chartBase,
        tooltip: {
            trigger: 'axis',
            backgroundColor: '#0f172a',
            borderColor: '#1e293b',
            textStyle: { color: '#e2e8f0', fontSize: 12 },
            formatter: (params: any[]) =>
                `${params[0].axisValue}<br/>` +
                params.map((p) => `${p.marker} ${p.seriesName}: ${formatCurrency(p.value)}`).join('<br/>'),
        },
        legend: {
            textStyle: { color: '#94a3b8', fontSize: 11 },
            top: 0, right: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10,
        },
        xAxis: {
            type: 'category',
            data: data.map((p) => p.date),
            axisLine: { lineStyle: { color: '#334155' } },
            axisLabel: { color: '#64748b', fontSize: 10 },
        },
        yAxis: {
            type: 'value',
            splitLine: { lineStyle: { color: '#1e293b' } },
            axisLabel: { color: '#64748b', fontSize: 10,
                formatter: (v: number) => `R$${(v / 1000).toFixed(0)}k` },
        },
        series: [
            {
                name: 'Saldo',
                type: 'line',
                data: data.map((p) => p.balance),
                smooth: true,
                lineStyle: { width: 2, color: palette.brand },
                areaStyle: {
                    color: {
                        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                        colorStops: [
                            { offset: 0, color: 'rgba(59,130,246,0.25)' },
                            { offset: 1, color: 'rgba(59,130,246,0)' },
                        ],
                    },
                },
                symbol: 'none',
            },
        ],
    };

    return <ReactECharts option={option} style={{ height: 280 }} notMerge />;
}