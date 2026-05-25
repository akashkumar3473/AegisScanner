import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Shield, LayoutDashboard, FolderGit2, LogOut, User } from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (!user) return null;

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="glass sticky top-0 z-50 px-6 py-4 flex justify-between items-center border-b border-dark-700">
      <div className="flex items-center space-x-3">
        <div className="bg-primary-600 p-2 rounded-lg text-white cyber-glow">
          <Shield className="h-6 w-6" />
        </div>
        <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-indigo-400 bg-clip-text text-transparent">
          AegisScanner
        </span>
      </div>

      <div className="flex items-center space-x-6">
        <Link
          to="/"
          className={`flex items-center space-x-2 text-sm font-medium transition-colors hover:text-white ${
            isActive('/') ? 'text-primary-500 font-semibold' : 'text-slate-400'
          }`}
        >
          <LayoutDashboard className="h-4 w-4" />
          <span>Dashboard</span>
        </Link>
        <Link
          to="/repositories"
          className={`flex items-center space-x-2 text-sm font-medium transition-colors hover:text-white ${
            isActive('/repositories') ? 'text-primary-500 font-semibold' : 'text-slate-400'
          }`}
        >
          <FolderGit2 className="h-4 w-4" />
          <span>Repositories</span>
        </Link>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-3 border-r border-dark-700 pr-4">
          <div className="bg-dark-800 p-1.5 rounded-full text-slate-400">
            <User className="h-4 w-4" />
          </div>
          <div className="text-left">
            <div className="text-xs font-semibold text-white">{user.name}</div>
            <div className="text-[10px] uppercase tracking-wider text-primary-500 font-bold">
              {user.role}
            </div>
          </div>
        </div>

        <button
          onClick={logout}
          className="text-slate-400 hover:text-cyber-red transition-colors flex items-center space-x-1 text-sm font-medium"
        >
          <LogOut className="h-4 w-4" />
          <span className="hidden sm:inline">Logout</span>
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
