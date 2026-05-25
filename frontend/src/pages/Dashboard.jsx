import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { 
  ShieldAlert, ShieldCheck, Play, Folder, Activity, Zap, 
  ChevronRight, Sparkles, Clock, AlertTriangle, GitBranch
} from 'lucide-react';
import { 
  ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line, 
  XAxis, YAxis, CartesianGrid, Tooltip, BarChart, Bar, Legend 
} from 'recharts';

const Dashboard = () => {
  const { apiFetch } = useAuth();
  const [stats, setStats] = useState({
    reposCount: 0,
    scansCount: 0,
    totalVulnerabilities: 0,
    avgMaintainability: 100,
    avgComplexity: 1,
    techDebtMins: 0,
    critical: 0,
    high: 0,
    medium: 0,
    low: 0
  });
  const [recentScans, setRecentScans] = useState([]);
  const [repositories, setRepositories] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        // Fetch repositories
        const reposRes = await apiFetch('/repositories');
        const repos = await reposRes.json();
        setRepositories(repos);

        // Fetch scans
        const scansRes = await apiFetch('/scans/user');
        const scans = await scansRes.json();
        setRecentScans(scans);

        // Compute stats
        if (scans.length > 0) {
          let critical = 0, high = 0, medium = 0, low = 0;
          let sumMaintainability = 0;
          let sumComplexity = 0;
          let sumTechDebt = 0;
          let completedCount = 0;

          scans.forEach(s => {
            critical += s.criticalCount || 0;
            high += s.highCount || 0;
            medium += s.mediumCount || 0;
            low += s.lowCount || 0;

            if (s.status === 'COMPLETED') {
              sumMaintainability += s.maintainabilityIndex || 0;
              sumComplexity += s.cyclomaticComplexity || 0;
              sumTechDebt += s.technicalDebtMinutes || 0;
              completedCount++;
            }
          });

          setStats({
            reposCount: repos.length,
            scansCount: scans.length,
            totalVulnerabilities: critical + high + medium + low,
            avgMaintainability: completedCount > 0 ? Math.round(sumMaintainability / completedCount) : 100,
            avgComplexity: completedCount > 0 ? Math.round((sumComplexity / completedCount) * 10) / 10 : 1,
            techDebtMins: sumTechDebt,
            critical,
            high,
            medium,
            low
          });
        } else {
          setStats({
            reposCount: repos.length,
            scansCount: 0,
            totalVulnerabilities: 0,
            avgMaintainability: 100,
            avgComplexity: 1,
            techDebtMins: 0,
            critical: 0,
            high: 0,
            medium: 0,
            low: 0
          });
        }
      } catch (err) {
        console.error("Dashboard load failure", err);
      } finally {
        setLoading(false);
      }
    };

    loadDashboardData();
  }, []);

  const getSecurityGrade = (score) => {
    if (score >= 90) return { grade: 'A', color: 'text-cyber-green border-cyber-green/20 bg-cyber-green/5' };
    if (score >= 80) return { grade: 'B', color: 'text-cyber-blue border-cyber-blue/20 bg-cyber-blue/5' };
    if (score >= 70) return { grade: 'C', color: 'text-cyber-orange border-cyber-orange/20 bg-cyber-orange/5' };
    if (score >= 60) return { grade: 'D', color: 'text-amber-500 border-amber-500/20 bg-amber-500/5' };
    return { grade: 'F', color: 'text-cyber-red border-cyber-red/20 bg-cyber-red/5' };
  };

  const gradeInfo = getSecurityGrade(stats.avgMaintainability);

  // Chart data
  const severityData = [
    { name: 'Critical', value: stats.critical, color: '#ef4444' },
    { name: 'High', value: stats.high, color: '#f59e0b' },
    { name: 'Medium', value: stats.medium, color: '#3b82f6' },
    { name: 'Low', value: stats.low, color: '#10b981' }
  ].filter(d => d.value > 0);

  // Safe fallback if all severe counts are zero
  const finalSeverityData = severityData.length > 0 ? severityData : [
    { name: 'Secure', value: 1, color: '#10b981' }
  ];

  // Trends
  const trendData = [...recentScans]
    .reverse()
    .filter(s => s.status === 'COMPLETED')
    .map((s, index) => ({
      name: `Scan #${index + 1}`,
      Vulnerabilities: (s.criticalCount || 0) + (s.highCount || 0) + (s.mediumCount || 0) + (s.lowCount || 0),
      Debt: s.technicalDebtMinutes || 0
    }));

  if (loading) {
    return (
      <div className="p-8 max-w-7xl mx-auto space-y-6">
        <div className="h-8 w-48 shimmer rounded-lg" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map(n => <div key={n} className="h-28 shimmer rounded-2xl" />)}
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map(n => <div key={n} className="h-64 shimmer rounded-2xl" />)}
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8 bg-dark-950 min-h-screen text-slate-100">
      
      {/* Welcome header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">Security Analytics</h1>
          <p className="text-slate-400 text-sm mt-1">
            Analyze scanning pipelines, quality standards, and threat remediations.
          </p>
        </div>
        <Link 
          to="/repositories"
          className="bg-primary-600 hover:bg-primary-700 text-white text-xs font-semibold px-4 py-2.5 rounded-xl transition-all shadow-md shadow-primary-600/10 flex items-center space-x-2 border border-primary-500/20 cursor-pointer"
        >
          <Play className="h-3.5 w-3.5 fill-white" />
          <span>Start Scan Run</span>
        </Link>
      </div>

      {/* Grade and numbers cards */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Security Rating Card */}
        <div className="glass lg:col-span-2 rounded-2xl p-6 border border-dark-700 flex items-center justify-between">
          <div className="space-y-2">
            <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold block">
              Global Security Rating
            </span>
            <h3 className="text-2xl font-bold">Maintainability Score</h3>
            <p className="text-slate-400 text-xs">
              Derived from cyclomatic density, duplicates, and findings.
            </p>
          </div>
          <div className={`h-24 w-24 shrink-0 rounded-2xl flex flex-col items-center justify-center border font-extrabold text-5xl tracking-tighter ${gradeInfo.color} cyber-glow`}>
            {gradeInfo.grade}
            <span className="text-[10px] tracking-normal font-semibold mt-1">
              {stats.avgMaintainability}%
            </span>
          </div>
        </div>

        {/* Counter cards */}
        <div className="grid grid-cols-2 lg:col-span-3 gap-6">
          <div className="glass rounded-2xl p-5 border border-dark-700 flex items-center space-x-4">
            <div className="p-3.5 bg-cyber-red/10 border border-cyber-red/20 text-cyber-red rounded-xl">
              <ShieldAlert className="h-6 w-6" />
            </div>
            <div>
              <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold block">
                Total Vulnerabilities
              </span>
              <span className="text-2xl font-bold block mt-0.5">{stats.totalVulnerabilities}</span>
            </div>
          </div>

          <div className="glass rounded-2xl p-5 border border-dark-700 flex items-center space-x-4">
            <div className="p-3.5 bg-cyber-orange/10 border border-cyber-orange/20 text-cyber-orange rounded-xl">
              <Clock className="h-6 w-6" />
            </div>
            <div>
              <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold block">
                Technical Debt
              </span>
              <span className="text-2xl font-bold block mt-0.5">
                {Math.round((stats.techDebtMins / 60) * 10) / 10} hrs
              </span>
            </div>
          </div>

          <div className="glass rounded-2xl p-5 border border-dark-700 flex items-center space-x-4">
            <div className="p-3.5 bg-primary-600/10 border border-primary-500/20 text-primary-500 rounded-xl">
              <Folder className="h-6 w-6" />
            </div>
            <div>
              <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold block">
                Tracked Repositories
              </span>
              <span className="text-2xl font-bold block mt-0.5">{stats.reposCount}</span>
            </div>
          </div>

          <div className="glass rounded-2xl p-5 border border-dark-700 flex items-center space-x-4">
            <div className="p-3.5 bg-cyber-blue/10 border border-cyber-blue/20 text-cyber-blue rounded-xl">
              <Activity className="h-6 w-6" />
            </div>
            <div>
              <span className="text-[10px] uppercase tracking-wider text-slate-400 font-bold block">
                Completed Scans
              </span>
              <span className="text-2xl font-bold block mt-0.5">{stats.scansCount}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Visual Analytics Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Severity distribution */}
        <div className="glass rounded-2xl p-6 border border-dark-700 flex flex-col justify-between">
          <div>
            <h3 className="text-base font-bold">Severity Distribution</h3>
            <p className="text-slate-400 text-xs mt-1">Breakdown of vulnerabilities by impact level.</p>
          </div>
          <div className="h-44 mt-4 flex items-center justify-center">
            {stats.totalVulnerabilities === 0 ? (
              <div className="text-center space-y-2">
                <ShieldCheck className="h-10 w-10 text-cyber-green mx-auto" />
                <span className="text-xs text-slate-400 block font-semibold">Codebase fully secure</span>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={finalSeverityData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={75}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {finalSeverityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0b0f19', border: '1px solid #1f2937', borderRadius: '8px', color: '#fff', fontSize: '12px' }} 
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs mt-4">
            {finalSeverityData.map((entry, idx) => (
              <div key={idx} className="flex items-center space-x-2">
                <span className="h-2 w-2 rounded-full shrink-0" style={{ backgroundColor: entry.color }} />
                <span className="text-slate-400 truncate">{entry.name}:</span>
                <span className="font-semibold text-white">{entry.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Security Trends Line Chart */}
        <div className="glass lg:col-span-2 rounded-2xl p-6 border border-dark-700 flex flex-col justify-between">
          <div>
            <h3 className="text-base font-bold">Vulnerability & Debt Trend</h3>
            <p className="text-slate-400 text-xs mt-1">Historical changes of findings over consecutive scan operations.</p>
          </div>
          <div className="h-48 mt-6">
            {trendData.length === 0 ? (
              <div className="h-full flex items-center justify-center">
                <span className="text-xs text-slate-500 italic">Insufficient scan history. Run a scan to visualize trends.</span>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trendData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                  <XAxis dataKey="name" stroke="#64748b" fontSize={10} />
                  <YAxis stroke="#64748b" fontSize={10} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0b0f19', border: '1px solid #1f2937', borderRadius: '8px', color: '#fff', fontSize: '12px' }}
                  />
                  <Legend verticalAlign="top" height={36} iconType="circle" wrapperStyle={{ fontSize: '10px' }} />
                  <Line type="monotone" dataKey="Vulnerabilities" stroke="#ef4444" strokeWidth={2} dot={{ r: 3 }} />
                  <Line type="monotone" dataKey="Debt" stroke="#6366f1" strokeWidth={2} dot={{ r: 3 }} />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* bottom list section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Connected Repositories summary */}
        <div className="glass rounded-2xl p-6 border border-dark-700 space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-base font-bold">Monitored Codebases</h3>
            <Link to="/repositories" className="text-xs text-primary-500 hover:text-primary-600 font-semibold flex items-center">
              <span>View All</span>
              <ChevronRight className="h-3 w-3 mt-0.5 ml-0.5" />
            </Link>
          </div>
          <div className="divide-y divide-dark-700 max-h-64 overflow-y-auto">
            {repositories.length === 0 ? (
              <div className="text-center py-8">
                <span className="text-slate-500 text-xs italic block">No repositories added yet.</span>
              </div>
            ) : (
              repositories.map(repo => (
                <div key={repo.id} className="py-3 flex justify-between items-center first:pt-0 last:pb-0">
                  <div className="flex items-center space-x-3 truncate">
                    <div className="bg-dark-850 p-2 rounded-xl text-slate-400">
                      <Folder className="h-4 w-4" />
                    </div>
                    <div className="text-left truncate">
                      <span className="text-xs font-semibold text-white block">{repo.name}</span>
                      <span className="text-[10px] text-slate-500 block truncate max-w-sm">{repo.gitUrl}</span>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2 shrink-0">
                    <span className="text-[9px] font-bold uppercase tracking-wider text-slate-400 px-2 py-1 bg-dark-800 rounded-md border border-dark-750 flex items-center space-x-1">
                      <GitBranch className="h-2.5 w-2.5 mr-0.5" />
                      {repo.branch}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent Scan Runs */}
        <div className="glass rounded-2xl p-6 border border-dark-700 space-y-4">
          <h3 className="text-base font-bold">Recent Scan Operations</h3>
          <div className="divide-y divide-dark-700 max-h-64 overflow-y-auto">
            {recentScans.length === 0 ? (
              <div className="text-center py-8">
                <span className="text-slate-500 text-xs italic block">No scan executions logged.</span>
              </div>
            ) : (
              recentScans.map(scan => {
                const totalV = (scan.criticalCount || 0) + (scan.highCount || 0) + (scan.mediumCount || 0) + (scan.lowCount || 0);
                return (
                  <Link 
                    key={scan.id} 
                    to={`/scans/${scan.id}`} 
                    className="py-3 flex justify-between items-center hover:bg-dark-800/10 px-2 -mx-2 rounded-xl transition-colors first:pt-0 last:pb-0 block"
                  >
                    <div className="text-left">
                      <span className="text-xs font-semibold text-white block">
                        {scan.repository.name}
                      </span>
                      <span className="text-[9px] text-slate-500 block">
                        ID: #{scan.id} • {new Date(scan.startedAt).toLocaleString()}
                      </span>
                    </div>

                    <div className="flex items-center space-x-4 shrink-0">
                      {scan.status === 'COMPLETED' ? (
                        <div className="flex items-center space-x-3 text-right">
                          <div>
                            <span className="text-[10px] font-bold text-white block">
                              {totalV} issues
                            </span>
                            <span className="text-[9px] text-slate-500 block">
                              Score: {scan.maintainabilityIndex}%
                            </span>
                          </div>
                          <span className="text-[9px] font-bold px-2.5 py-1 bg-cyber-green/10 border border-cyber-green/20 text-cyber-green rounded-lg uppercase tracking-wider">
                            Success
                          </span>
                        </div>
                      ) : scan.status === 'RUNNING' ? (
                        <div className="flex items-center space-x-3">
                          <span className="text-[9px] font-bold px-2.5 py-1 bg-cyber-blue/10 border border-cyber-blue/20 text-cyber-blue rounded-lg uppercase tracking-wider animate-pulse">
                            Scanning
                          </span>
                        </div>
                      ) : scan.status === 'FAILED' ? (
                        <div className="flex items-center space-x-3">
                          <span className="text-[9px] font-bold px-2.5 py-1 bg-cyber-red/10 border border-cyber-red/20 text-cyber-red rounded-lg uppercase tracking-wider">
                            Failed
                          </span>
                        </div>
                      ) : (
                        <div className="flex items-center space-x-3">
                          <span className="text-[9px] font-bold px-2.5 py-1 bg-slate-800 border border-slate-700 text-slate-400 rounded-lg uppercase tracking-wider">
                            Queued
                          </span>
                        </div>
                      )}
                      <ChevronRight className="h-4 w-4 text-slate-500" />
                    </div>
                  </Link>
                );
              })
            )}
          </div>
        </div>

      </div>

    </div>
  );
};

export default Dashboard;
