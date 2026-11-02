import { Navigate, Route, Routes } from 'react-router-dom';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<div className="p-8">Bootstrap OK</div>} />
    </Routes>
  );
}