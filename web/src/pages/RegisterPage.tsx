import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const setTokens = useAuthStore((s) => s.setTokens);
  const setUser = useAuthStore((s) => s.setUser);
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: async (data) => {
      setTokens(data.accessToken, data.refreshToken);
      const user = await authApi.me();
      setUser(user);
      navigate('/dashboard');
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate({ name, email, password });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-950 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600">
            <span className="text-xl font-bold text-white">FF</span>
          </div>
          <h1 className="text-2xl font-semibold text-white">Criar conta</h1>
          <p className="mt-1 text-sm text-slate-400">Comece a organizar suas finanças</p>
        </div>

        <form onSubmit={submit} className="card space-y-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-400">Nome</label>
            <input
              required value={name} onChange={(e) => setName(e.target.value)}
              className="input-base" placeholder="Seu nome"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-400">E-mail</label>
            <input
              type="email" required value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input-base" placeholder="voce@exemplo.com"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-400">Senha</label>
            <input
              type="password" required minLength={8} value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="input-base" placeholder="Mínimo 8 caracteres"
            />
          </div>

          {mutation.isError && (
            <p className="rounded-lg bg-red-500/10 px-3 py-2 text-xs text-red-400">
              Não foi possível cadastrar. Verifique os dados.
            </p>
          )}

          <button type="submit" disabled={mutation.isPending} className="btn-primary w-full">
            {mutation.isPending ? 'Criando...' : 'Criar conta'}
          </button>

          <p className="text-center text-xs text-slate-400">
            Já tem conta?{' '}
            <Link to="/login" className="text-brand-400 hover:underline">
              Entrar
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}