export const formatCurrency = (value: number, currency = 'BRL', locale = 'pt-BR') =>
  new Intl.NumberFormat(locale, { style: 'currency', currency }).format(value);

export const formatDate = (iso: string, locale = 'pt-BR') =>
  new Date(iso).toLocaleDateString(locale);