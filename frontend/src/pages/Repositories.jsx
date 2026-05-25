import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { 
  FolderGit2, Plus, Play, Trash2, GitBranch, Link2, 
  Search, ShieldCheck, ShieldAlert, Terminal, Info 
} from 'lucide-react';

const Repositories = () => {
  const { apiFetch } = useAuth();
  const navigate = useNavigate();
  
  const [repos, setRepos] = useState([]);
  const [gitUrl, setGitUrl] = useState('');
  const [branch, setBranch] = useState('main');
  const [repoName, setRepoName] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const loadRepositories = async () => {
    try {
      const res = await apiFetch('/repositories');
      if (res.ok) {
        const data = await res.json();
        setRepos(data);
      }
    } catch (err) {
      setError('Could not load repositories.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRepositories();
  }, []);

  const handleAddRepo = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setActionLoading(true);

    if (!gitUrl) {
      setError('Git URL is required.');
      setActionLoading(false);
      return;
    }

    try {
      const res = await apiFetch('/repositories', {
        method: 'POST',
        body: JSON.stringify({
          name: repoName,
          gitUrl: gitUrl,
          branch: branch
        })
      });

      if (res.ok) {
        setSuccess('Repository connected successfully.');
        setGitUrl('');
        setRepoName('');
        setBranch('main');
        loadRepositories();
      } else {
        const msg = await res.text();
        setError(msg || 'Failed to connect repository.');
      }
    } catch (err) {
      setError('Network error connecting repository.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteRepo = async (id) => {
    if (!window.confirm('Are you sure you want to delete this repository and all of its scan history?')) {
      return;
    }

    setError('');
    setSuccess('');

    try {
      const res = await apiFetch(`/repositories/${id}`, {
        method: 'DELETE'
      });

      if (res.ok) {
        setSuccess('Repository deleted.');
        loadRepositories();
      } else {
        const msg = await res.text();
        setError(msg || 'Failed to delete.');
      }
    } catch (err) {
      setError('Network error deleting repository.');
    }
  };

  const handleStartScan = async (repoId) => {
    setError('');
    setSuccess('');
    
    try {
      const res = await apiFetch('/scans/start', {
        method: 'POST',
        body: JSON.stringify({ repoId })
      });

      if (res.ok) {
        const scan = await res.json();
        navigate(`/scans/${scan.id}`);
      } else {
        const msg = await res.text();
        setError(msg || 'Failed to start scan.');
      }
    } catch (err) {
      setError('Failed to trigger scan runner.');
    }
  };

  const filteredRepos = repos.filter(repo => 
    repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    repo.gitUrl.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="p-8 max-w-7xl mx-auto space-y-6">
        <div className="h-8 w-48 shimmer rounded-lg" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="h-96 shimmer rounded-2xl lg:col-span-1" />
          <div className="h-96 shimmer rounded-2xl lg:col-span-2" />
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 bg-dark-950 min-h-screen text-slate-100">
      
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Codebases</h1>
        <p className="text-slate-400 text-sm mt-1">
          Register git repositories, coordinate branches, and dispatch security audits.
        </p>
      </div>

      {error && (
        <div className="bg-cyber-red/10 border border-cyber-red/20 text-cyber-red rounded-xl p-3.5 text-xs flex items-start space-x-2.5">
          <Info className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="bg-cyber-green/10 border border-cyber-green/20 text-cyber-green rounded-xl p-3.5 text-xs flex items-start space-x-2.5">
          <ShieldCheck className="h-4 w-4 shrink-0 mt-0.5" />
          <span>{success}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Connection Form Card */}
        <div className="glass rounded-2xl p-6 border border-dark-700 h-fit space-y-5">
          <div className="flex items-center space-x-2.5 pb-3 border-b border-dark-700">
            <Plus className="h-5 w-5 text-primary-500" />
            <h3 className="font-bold text-sm">Connect Repository</h3>
          </div>

          <form onSubmit={handleAddRepo} className="space-y-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                Git URL (HTTPS)
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500">
                  <Link2 className="h-4 w-4" />
                </span>
                <input
                  type="text"
                  required
                  value={gitUrl}
                  onChange={(e) => setGitUrl(e.target.value)}
                  className="w-full bg-dark-950 border border-dark-700 rounded-xl py-2.5 pl-9 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500 transition-all"
                  placeholder="https://github.com/user/repo.git"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                Repository Name (Optional)
              </label>
              <input
                type="text"
                value={repoName}
                onChange={(e) => setRepoName(e.target.value)}
                className="w-full bg-dark-950 border border-dark-700 rounded-xl py-2.5 px-3.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500 transition-all"
                placeholder="Inferred if left blank"
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">
                Default Scan Branch
              </label>
              <div className="relative">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500">
                  <GitBranch className="h-4 w-4" />
                </span>
                <input
                  type="text"
                  required
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  className="w-full bg-dark-950 border border-dark-700 rounded-xl py-2.5 pl-9 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500 transition-all"
                  placeholder="main"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={actionLoading}
              className="w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-50 text-white rounded-xl py-2.5 font-semibold text-xs transition-all shadow-md shadow-primary-600/10 cursor-pointer"
            >
              {actionLoading ? 'Connecting...' : 'Connect Codebase'}
            </button>
          </form>
        </div>

        {/* Repositories List Grid */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex bg-dark-950/80 p-2 rounded-xl border border-dark-700 items-center justify-between">
            <div className="relative w-full max-w-xs">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-500">
                <Search className="h-4 w-4" />
              </span>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-dark-900 border border-dark-700 rounded-lg py-1.5 pl-9 pr-4 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-primary-500 transition-all"
                placeholder="Search connected repositories..."
              />
            </div>
            <span className="text-[10px] text-slate-500 font-medium pr-2">
              {filteredRepos.length} Total
            </span>
          </div>

          <div className="grid grid-cols-1 gap-4">
            {filteredRepos.length === 0 ? (
              <div className="glass rounded-2xl p-12 text-center border border-dark-700">
                <FolderGit2 className="h-12 w-12 text-slate-600 mx-auto mb-3" />
                <h4 className="text-sm font-bold text-slate-300">No matching codebases connected</h4>
                <p className="text-xs text-slate-500 mt-1">Connect your first Git repository to initiate audits.</p>
              </div>
            ) : (
              filteredRepos.map(repo => (
                <div key={repo.id} className="glass rounded-2xl p-5 border border-dark-700 hover:border-dark-600 transition-all flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                  <div className="space-y-2 truncate">
                    <div className="flex items-center space-x-2">
                      <h4 className="text-sm font-bold text-white truncate">{repo.name}</h4>
                      <span className="text-[9px] font-bold text-slate-400 px-2 py-0.5 bg-dark-800 rounded border border-dark-700 flex items-center">
                        <GitBranch className="h-2.5 w-2.5 mr-0.5 text-slate-500" />
                        {repo.branch}
                      </span>
                    </div>
                    <span className="text-xs text-slate-500 block truncate max-w-md">{repo.gitUrl}</span>
                    <span className="text-[10px] text-slate-600 block">
                      Added: {new Date(repo.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <div className="flex items-center space-x-3 shrink-0 w-full md:w-auto justify-end">
                    <button
                      onClick={() => handleStartScan(repo.id)}
                      className="bg-primary-600/10 border border-primary-500/20 text-primary-500 hover:bg-primary-600 hover:text-white transition-all text-xs font-semibold px-3 py-2 rounded-xl flex items-center space-x-1.5 cursor-pointer"
                    >
                      <Play className="h-3 w-3 fill-current" />
                      <span>Audit Now</span>
                    </button>
                    <button
                      onClick={() => handleDeleteRepo(repo.id)}
                      className="bg-dark-800 border border-dark-700 text-slate-400 hover:text-cyber-red hover:border-cyber-red/20 transition-all p-2 rounded-xl cursor-pointer"
                      title="Disconnect Repository"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

      </div>

    </div>
  );
};

export default Repositories;
