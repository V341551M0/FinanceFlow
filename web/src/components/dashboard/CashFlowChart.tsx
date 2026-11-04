import ReactECharts from 'echarts-for-react';
import type { TimeSeriesPoint } from '@/api/dashboard';
import { palette, chartBase } from '@/components/charts/theme';
import { formatCurrency } from '@/lib/format';

export function CashFlowChart({ data }: { data: TimeSeriesPoint[] }) {
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
            { name: 'Receita', type: 'bar', data: data.map((p) => p.income),
                itemStyle: { color: palette.positive, borderRadius: [3, 3, 0, 0] } },
            { name: 'Despesa', type: 'bar', data: data.map((p) => p.expense),
                itemStyle: { color: palette.negative, borderRadius: [3, 3, 0, 0] } },
        ],
    };
    return <ReactECharts option={option} style={{ height: 280 }} notMerge />;
}