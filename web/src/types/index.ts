export interface User {
  id: number;
  name: string;
  email: string;
  currency: string;
  locale: string;
  timezone: string;
  theme: 'DARK' | 'LIGHT';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}

export type AccountType = 'CHECKING' | 'SAVINGS' | 'PAYMENT' | 'WALLET' | 'INVESTMENT';

export interface FinancialAccount {
  id: number;
  name: string;
  institution: string;
  type: AccountType;
  currency: string;
  initialBalance: number;
  currentBalance: number;
  active: boolean;
  createdAt: string;
}

export interface CreateAccountPayload {
  name: string;
  institution: string;
  type: AccountType;
  currency: string;
  initialBalance: number;
}