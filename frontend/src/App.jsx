import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Repositories from './pages/Repositories';
import ScanDetail from './pages/ScanDetail';

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-dark-950 flex items-center justify-center text-slate-400">
        <div className="text-center space-y-2">
          <div className="h-6 w-6 border-2 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <span className="text-xs">Verifying credentials...</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

const AppContent = () => {
  const { user } = useAuth();
  return (
    <div className="min-h-screen bg-dark-950 flex flex-col">
      {user && <Navbar />}
      <main className="flex-1">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route 
            path="/" 
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/repositories" 
            element={
              <ProtectedRoute>
                <Repositories />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/scans/:id" 
            element={
              <ProtectedRoute>
                <ScanDetail />
              </ProtectedRoute>
            } 
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  );
}

export default App;
