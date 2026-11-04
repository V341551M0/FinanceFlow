import type { ReactNode } from 'react';

interface Props {
    title: string;
    subtitle?: string;
    children: ReactNode;
    action?: ReactNode;
    className?: string;
}

export function ChartCard({ title, subtitle, children, action, className = '' }: Props) {
    return (
        <div className={`card ${className}`}>
            <div className="mb-4 flex items-start justify-between">
                <div>
                    <h3 className="text-sm font-medium text-white">{title}</h3>
                    {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
                </div>
                {action}
            </div>
            {children}
        </div>
    );
}