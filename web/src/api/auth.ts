import { http } from './http';
import type { AuthResponse, LoginPayload, RegisterPayload, User } from '@/types';

export const authApi = {
  register: (payload: RegisterPayload) =>
    http.post<AuthResponse>('/auth/register', payload).then((r) => r.data),
  login: (payload: LoginPayload) =>
    http.post<AuthResponse>('/auth/login', payload).then((r) => r.data),
  me: () => http.get<User>('/me').then((r) => r.data),
};