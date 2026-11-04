import type { PeriodPreset } from '@/api/dashboard';

interface Props {
    value: PeriodPreset;
    from: string;
    to: string;
    onChange: (preset: PeriodPreset, from?: string, to?: string) => void;
}

const presets: { key: PeriodPreset; label: string }[] = [
    { key: 'THIS_MONTH', label: 'Este mês' },
    { key: 'LAST_MONTH', label: 'Mês anterior' },
    { key: 'LAST_3_MONTHS', label: '3 meses' },
    { key: 'LAST_6_MONTHS', label: '6 meses' },
    { key: 'THIS_YEAR', label: 'Este ano' },
    { key: 'LAST_12_MONTHS', label: '12 meses' },
    { key: 'CUSTOM', label: 'Personalizado' },
];

export function PeriodSelector({ value, from, to, onChange }: Props) {
    return (
        <div className="space-y-2">
            <div className="flex flex-wrap gap-2">
                {presets.map((p) => (
                    <button
                        key={p.key}
                        onClick={() => onChange(p.key, from, to)}
                        className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                            value === p.key
                                ? 'bg-brand-600 text-white'
                                : 'bg-surface-800 text-slate-400 hover:bg-surface-700 hover:text-slate-200'
                        }`}
                    >
                        {p.label}
                    </button>
                ))}
            </div>
            {value === 'CUSTOM' && (
                <div className="flex flex-wrap gap-2">
                    <input type="date" className="input-base max-w-[180px]" value={from}
                           onChange={(e) => onChange('CUSTOM', e.target.value, to)} />
                    <input type="date" className="input-base max-w-[180px]" value={to}
                           onChange={(e) => onChange('CUSTOM', from, e.target.value)} />
                </div>
            )}
        </div>
    );
}