import ReactECharts from 'echarts-for-react';
import type { CategoryBreakdown } from '@/api/dashboard';
import { chartBase } from '@/components/charts/theme';
import { formatCurrency } from '@/lib/format';

const fallbackColors = [
    '#3b82f6', '#ef4444', '#22c55e', '#eab308',
    '#a855f7', '#06b6d4', '#f97316', '#64748b',
];

interface Props {
    data: CategoryBreakdown[];
    title: string;
}

export function CategoryPieChart({ data, title }: Props) {
    if (data.length === 0) {
        return <p className="py-12 text-center text-sm text-slate-500">Sem dados no período</p>;
    }

    const option = {
        ...chartBase,
        tooltip: {
            trigger: 'item',
            backgroundColor: '#0f172a',
            borderColor: '#1e293b',
            textStyle: { color: '#e2e8f0', fontSize: 12 },
            formatter: (p: any) => `${p.name}<br/>${formatCurrency(p.value)} (${p.percent}%)`,
        },
        legend: {
            orient: 'vertical',
            right: 0,
            top: 'center',
            textStyle: { color: '#94a3b8', fontSize: 11 },
            icon: 'roundRect', itemWidth: 8, itemHeight: 8,
        },
        series: [
            {
                name: title,
                type: 'pie',
                radius: ['55%', '80%'],
                center: ['35%', '50%'],
                avoidLabelOverlap: true,
                label: { show: false },
                data: data.map((c, i) => ({
                    name: c.categoryName,
                    value: c.amount,
                    itemStyle: { color: c.color || fallbackColors[i % fallbackColors.length] },
                })),
            },
        ],
    };

    return <ReactECharts option={option} style={{ height: 280 }} notMerge />;
}