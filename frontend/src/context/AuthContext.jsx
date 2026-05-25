import React, { createContext, useState, useEffect, useContext } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [loading, setLoading] = useState(true);

  const API_URL = 'http://localhost:8080/api';

  useEffect(() => {
    const fetchMe = async () => {
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const response = await fetch(`${API_URL}/auth/me`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response.ok) {
          const data = await response.json();
          setUser({ email: data.email, name: data.name, role: data.role });
        } else {
          logout();
        }
      } catch (error) {
        console.error('Auth error', error);
      } finally {
        setLoading(false);
      }
    };

    fetchMe();
  }, [token]);

  const login = async (email, password) => {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(errText || 'Invalid credentials');
    }

    const data = await response.json();
    localStorage.setItem('token', data.token);
    setToken(data.token);
    setUser({ email: data.email, name: data.name, role: data.role });
    return data;
  };

  const register = async (name, email, password, role) => {
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name, email, password, role })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(errText || 'Registration failed');
    }

    const data = await response.json();
    localStorage.setItem('token', data.token);
    setToken(data.token);
    setUser({ email: data.email, name: data.name, role: data.role });
    return data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken('');
    setUser(null);
  };

  const apiFetch = async (endpoint, options = {}) => {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const res = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers
    });

    if (res.status === 401) {
      logout();
      throw new Error('Session expired');
    }

    return res;
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, apiFetch }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
