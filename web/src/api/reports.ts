import { http } from './http';

export type ReportFormat = 'CSV' | 'XLSX' | 'PDF';

export const reportsApi = {
  download: async (format: ReportFormat, from: string, to: string) => {
    const res = await http.get('/reports/transactions', {
      params: { format, from, to },
      responseType: 'blob',
    });
    const blob = new Blob([res.data]);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `financeflow-${from}-${to}.${format.toLowerCase()}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },
};