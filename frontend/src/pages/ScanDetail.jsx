import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Client } from '@stomp/stompjs';
import { 
  ShieldAlert, ShieldCheck, ChevronRight, Terminal, Loader2, 
  Download, FileCode, CheckCircle2, AlertTriangle, AlertCircle, 
  Sparkles, Split, BookOpen, ArrowRight, GitBranch, RefreshCw 
} from 'lucide-react';

const ScanDetail = () => {
  const { id } = useParams();
  const { apiFetch, token } = useAuth();
  
  const [scan, setScan] = useState(null);
  const [vulnerabilities, setVulnerabilities] = useState([]);
  const [selectedVuln, setSelectedVuln] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Real-time terminal log logs
  const [terminalLogs, setTerminalLogs] = useState([]);
  
  // AI fix states
  const [aiLoading, setAiLoading] = useState(false);
  const [aiFix, setAiFix] = useState(null);
  const [aiError, setAiError] = useState('');

  const stompClientRef = useRef(null);

  const loadScanDetails = async () => {
    try {
      const scanRes = await apiFetch(`/scans/${id}`);
      if (!scanRes.ok) throw new Error('Failed to load scan');
      const scanData = await scanRes.json();
      setScan(scanData);

      if (scanData.status === 'COMPLETED') {
        const vulnRes = await apiFetch(`/scans/${id}/vulnerabilities`);
        const vulnData = await vulnRes.json();
        setVulnerabilities(vulnData);
        if (vulnData.length > 0) {
          setSelectedVuln(vulnData[0]);
        }
      } else if (scanData.status === 'RUNNING' || scanData.status === 'PENDING') {
        setTerminalLogs([`Initializing security runner for Scan #${id}...`, `Status: ${scanData.status}`]);
        connectWebSocket();
      }
    } catch (err) {
      setError(err.message || 'Error loading scan details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadScanDetails();

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [id]);

  const connectWebSocket = () => {
    // Native WebSocket Stomp client setup
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: function (str) {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    client.onConnect = (frame) => {
      setTerminalLogs(prev => [...prev, '✓ Connected to scanning broker. Listening for pipeline updates...']);
      
      client.subscribe(`/topic/scans/${id}`, (message) => {
        const payload = JSON.parse(message.body);
        setTerminalLogs(prev => [...prev, `[${new Date(payload.timestamp).toLocaleTimeString()}] ${payload.status}: ${payload.step}`]);
        
        if (payload.status === 'COMPLETED' || payload.status === 'FAILED') {
          // Deactivate socket and reload
          client.deactivate();
          loadScanDetails();
        }
      });
    };

    client.onStompError = (frame) => {
      setTerminalLogs(prev => [...prev, '✗ Broker connection error. Retrying...']);
    };

    client.activate();
    stompClientRef.current = client;
  };

  const fetchAiFix = async (vulnId) => {
    setAiLoading(true);
    setAiFix(null);
    setAiError('');

    try {
      const res = await apiFetch(`/vulnerabilities/${vulnId}/ai-fix`);
      if (res.ok) {
        const data = await res.json();
        setAiFix(data);
      } else {
        setAiError('Could not retrieve remediation fix.');
      }
    } catch (err) {
      setAiError('Connection failure fetching fix suggestions.');
    } finally {
      setAiLoading(false);
    }
  };

  useEffect(() => {
    if (selectedVuln) {
      setAiFix(null);
      setAiError('');
    }
  }, [selectedVuln]);

  const getSeverityIcon = (sev) => {
    switch (sev.toUpperCase()) {
      case 'CRITICAL': return <AlertCircle className="h-4 w-4 text-cyber-red" />;
      case 'HIGH': return <AlertTriangle className="h-4 w-4 text-cyber-orange" />;
      case 'MEDIUM': return <AlertTriangle className="h-4 w-4 text-cyber-blue" />;
      default: return <Info className="h-4 w-4 text-cyber-green" />;
    }
  };

  const getSeverityBadgeClass = (sev) => {
    switch (sev.toUpperCase()) {
      case 'CRITICAL': return 'bg-cyber-red/10 border-cyber-red/20 text-cyber-red';
      case 'HIGH': return 'bg-cyber-orange/10 border-cyber-orange/20 text-cyber-orange';
      case 'MEDIUM': return 'bg-cyber-blue/10 border-cyber-blue/20 text-cyber-blue';
      default: return 'bg-cyber-green/10 border-cyber-green/20 text-cyber-green';
    }
  };

  // Helper to highlight vulnerable line inside context code block
  const renderCodeSnippet = (snippet, targetLine) => {
    if (!snippet) return <div className="text-slate-500 italic p-4">Snippet unavailable</div>;
    const lines = snippet.split('\n');
    return (
      <pre className="text-xs font-mono overflow-x-auto p-4 bg-dark-950 text-slate-300 select-text leading-relaxed">
        {lines.map((line, idx) => {
          const match = line.match(/^(\d+):(.*)$/);
          if (!match) return <div key={idx}>{line}</div>;
          
          const lineNum = match[1];
          const code = match[2];
          const isTarget = parseInt(lineNum) === targetLine;
          
          return (
            <div 
              key={idx} 
              className={`flex -mx-4 px-4 ${
                isTarget ? 'vulnerable-line-critical bg-cyber-red/10 font-medium' : ''
              }`}
            >
              <span className="w-10 text-right pr-4 text-slate-600 select-none border-r border-dark-700/50 mr-4">
                {lineNum}
              </span>
              <span>{code}</span>
            </div>
          );
        })}
      </pre>
    );
  };

  if (loading) {
    return (
      <div className="p-8 max-w-7xl mx-auto space-y-6">
        <div className="h-8 w-48 shimmer rounded-lg" />
        <div className="h-44 shimmer rounded-2xl" />
        <div className="h-96 shimmer rounded-2xl" />
      </div>
    );
  }

  if (error || !scan) {
    return (
      <div className="p-8 max-w-md mx-auto text-center space-y-4">
        <ShieldAlert className="h-12 w-12 text-cyber-red mx-auto" />
        <h3 className="text-lg font-bold">Failed to load Scan</h3>
        <p className="text-xs text-slate-500">{error || 'Scan details not found.'}</p>
        <Link to="/" className="text-xs text-primary-500 font-semibold block underline">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-[1600px] mx-auto space-y-6 bg-dark-950 min-h-screen text-slate-100">
      
      {/* Header bar */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 pb-4 border-b border-dark-700">
        <div>
          <div className="flex items-center space-x-2 text-xs text-slate-500">
            <Link to="/" className="hover:text-white transition-colors">Dashboard</Link>
            <ChevronRight className="h-3 w-3" />
            <Link to="/repositories" className="hover:text-white transition-colors">Repositories</Link>
            <ChevronRight className="h-3 w-3" />
            <span className="text-slate-400">Scan Details</span>
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight mt-1">
            Audit Run #{scan.id}
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Target: <span className="font-semibold text-slate-300">{scan.repository.name}</span> • Branch: <span className="font-semibold text-slate-300">{scan.repository.branch}</span>
          </p>
        </div>

        <div className="flex items-center space-x-3 shrink-0">
          {scan.status === 'COMPLETED' && (
            <>
              <a
                href={`http://localhost:8080/api/reports/download/${scan.id}/pdf`}
                target="_blank"
                rel="noreferrer"
                className="bg-dark-800 hover:bg-dark-700 border border-dark-700 px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all cursor-pointer"
              >
                <Download className="h-3.5 w-3.5" />
                <span>Download PDF</span>
              </a>
              <a
                href={`http://localhost:8080/api/reports/download/${scan.id}/csv`}
                target="_blank"
                rel="noreferrer"
                className="bg-dark-800 hover:bg-dark-700 border border-dark-700 px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all cursor-pointer"
              >
                <Download className="h-3.5 w-3.5" />
                <span>Export CSV</span>
              </a>
            </>
          )}
          <button 
            onClick={loadScanDetails}
            className="p-2 bg-dark-800 border border-dark-700 text-slate-400 hover:text-white rounded-xl transition-all cursor-pointer"
            title="Refresh Scan Status"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Runner Queue Terminal View */}
      {(scan.status === 'PENDING' || scan.status === 'RUNNING') && (
        <div className="glass rounded-2xl p-6 border border-dark-700 space-y-4">
          <div className="flex justify-between items-center">
            <div className="flex items-center space-x-2.5">
              <Loader2 className="h-5 w-5 text-primary-500 animate-spin" />
              <h3 className="font-bold text-sm">Async Scanner Active</h3>
            </div>
            <span className="text-[10px] font-bold uppercase px-2.5 py-1 rounded bg-primary-600/15 text-primary-500 border border-primary-500/10 animate-pulse">
              {scan.status}
            </span>
          </div>

          <div className="bg-black/90 rounded-xl p-4 border border-dark-800 font-mono text-xs text-emerald-400 space-y-2 h-72 overflow-y-auto cyber-glow">
            <div className="text-slate-500 pb-2 border-b border-dark-850 flex items-center space-x-1.5">
              <Terminal className="h-3.5 w-3.5" />
              <span>LOG STREAM CONSOLE</span>
            </div>
            {terminalLogs.map((log, idx) => (
              <div key={idx} className="whitespace-pre-wrap">{log}</div>
            ))}
          </div>
        </div>
      )}

      {/* Failure panel */}
      {scan.status === 'FAILED' && (
        <div className="bg-cyber-red/10 border border-cyber-red/20 rounded-2xl p-6 text-left space-y-3">
          <div className="flex items-center space-x-2.5 text-cyber-red">
            <AlertCircle className="h-6 w-6" />
            <h3 className="font-extrabold text-sm">Pipeline Scan Executing Error</h3>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            The scanning daemon encountered an unrecoverable exception during checkout or file assessment. Check repository access tokens, path validity, and git branch configuration.
          </p>
        </div>
      )}

      {/* Completed scan panels */}
      {scan.status === 'COMPLETED' && (
        <>
          {/* Top Quick Stats row */}
          <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
            <div className="glass rounded-xl p-4 border border-dark-700 text-center">
              <span className="text-[9px] uppercase tracking-wider text-slate-500 font-bold block">Security Score</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">{scan.maintainabilityIndex}%</span>
            </div>
            <div className="glass rounded-xl p-4 border border-dark-700 text-center">
              <span className="text-[9px] uppercase tracking-wider text-slate-500 font-bold block">Total Complexity</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">{scan.cyclomaticComplexity}</span>
            </div>
            <div className="glass rounded-xl p-4 border border-dark-700 text-center">
              <span className="text-[9px] uppercase tracking-wider text-slate-500 font-bold block">Duplication %</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">{scan.duplicateCodePercentage}%</span>
            </div>
            <div className="glass rounded-xl p-4 border border-dark-700 text-center border-l-4 border-l-cyber-red">
              <span className="text-[9px] uppercase tracking-wider text-cyber-red font-bold block">Critical</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">{scan.criticalCount}</span>
            </div>
            <div className="glass rounded-xl p-4 border border-dark-700 text-center border-l-4 border-l-cyber-orange">
              <span className="text-[9px] uppercase tracking-wider text-cyber-orange font-bold block">High</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">{scan.highCount}</span>
            </div>
            <div className="glass rounded-xl p-4 border border-dark-700 text-center border-l-4 border-l-cyber-blue">
              <span className="text-[9px] uppercase tracking-wider text-cyber-blue font-bold block">Medium / Low</span>
              <span className="text-xl font-extrabold text-white block mt-0.5">
                {(scan.mediumCount || 0) + (scan.lowCount || 0)}
              </span>
            </div>
          </div>

          {/* Interactive Code Viewer Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            
            {/* Left side: Vulnerable files list */}
            <div className="glass rounded-2xl border border-dark-700 flex flex-col h-[650px] overflow-hidden">
              <div className="p-4 border-b border-dark-700 bg-dark-900/50">
                <h3 className="font-bold text-xs">Vulnerability Explorer</h3>
                <span className="text-[10px] text-slate-400">{vulnerabilities.length} issues detected</span>
              </div>
              <div className="flex-1 overflow-y-auto divide-y divide-dark-700/50">
                {vulnerabilities.length === 0 ? (
                  <div className="p-8 text-center text-slate-500 text-xs italic">
                    No vulnerability logs present
                  </div>
                ) : (
                  vulnerabilities.map(v => (
                    <button
                      key={v.id}
                      onClick={() => setSelectedVuln(v)}
                      className={`w-full text-left p-3.5 transition-all text-xs flex flex-col space-y-2 hover:bg-dark-800/30 ${
                        selectedVuln?.id === v.id ? 'bg-primary-600/10 border-l-2 border-l-primary-500' : 'border-l-2 border-l-transparent'
                      }`}
                    >
                      <div className="flex justify-between items-center w-full">
                        <span className={`text-[9px] font-bold px-2 py-0.5 rounded border ${getSeverityBadgeClass(v.severity)}`}>
                          {v.severity}
                        </span>
                        <span className="text-[9px] text-slate-500 font-mono">Line {v.lineNumber}</span>
                      </div>
                      <div className="font-semibold text-slate-200 truncate w-full flex items-center space-x-1.5">
                        <FileCode className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                        <span className="truncate">{v.fileName.substring(v.fileName.lastIndexOf('/') + 1)}</span>
                      </div>
                      <p className="text-[10px] text-slate-400 line-clamp-1 truncate w-full">{v.ruleName}</p>
                    </button>
                  ))
                )}
              </div>
            </div>

            {/* Center + Right panels */}
            <div className="lg:col-span-3 flex flex-col space-y-6">
              
              {/* Code viewer console */}
              {selectedVuln && (
                <div className="glass rounded-2xl border border-dark-700 overflow-hidden flex flex-col h-[400px]">
                  <div className="px-4 py-3 bg-dark-900 border-b border-dark-700 flex justify-between items-center text-xs">
                    <span className="font-mono text-slate-300 font-semibold truncate flex items-center">
                      <FileCode className="h-4 w-4 mr-2 text-primary-500" />
                      {selectedVuln.fileName}
                    </span>
                    <span className="text-[10px] bg-dark-800 border border-dark-700 px-2 py-0.5 rounded text-slate-400">
                      Line {selectedVuln.lineNumber}
                    </span>
                  </div>
                  <div className="flex-1 overflow-auto bg-dark-950">
                    {renderCodeSnippet(selectedVuln.codeSnippet, selectedVuln.lineNumber)}
                  </div>
                </div>
              )}

              {/* Vulnerability details & AI Remediation panel */}
              {selectedVuln && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 min-h-[300px]">
                  
                  {/* Left: Detail description */}
                  <div className="glass rounded-2xl p-6 border border-dark-700 space-y-4">
                    <div className="flex items-center justify-between pb-3 border-b border-dark-700">
                      <div className="flex items-center space-x-2">
                        {getSeverityIcon(selectedVuln.severity)}
                        <h4 className="font-extrabold text-sm">{selectedVuln.ruleName}</h4>
                      </div>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${getSeverityBadgeClass(selectedVuln.severity)}`}>
                        {selectedVuln.severity}
                      </span>
                    </div>

                    <div className="space-y-3 text-xs leading-relaxed">
                      <div>
                        <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">Issue Details</span>
                        <p className="text-slate-300">{selectedVuln.description}</p>
                      </div>

                      <div>
                        <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">Default Remediation</span>
                        <code className="block bg-dark-900 p-3 rounded-lg text-slate-300 border border-dark-800 select-text overflow-x-auto whitespace-pre-wrap font-mono text-[10px]">
                          {selectedVuln.fixSuggestion}
                        </code>
                      </div>
                    </div>
                  </div>

                  {/* Right: AI fix container */}
                  <div className="glass rounded-2xl p-6 border border-dark-700 flex flex-col justify-between relative overflow-hidden">
                    <div className="flex items-center justify-between pb-3 border-b border-dark-700">
                      <div className="flex items-center space-x-2">
                        <Sparkles className="h-4.5 w-4.5 text-indigo-400" />
                        <h4 className="font-extrabold text-sm">AI Secure Fix Proposal</h4>
                      </div>
                      <span className="text-[8px] tracking-wider uppercase font-extrabold bg-indigo-500/10 text-indigo-400 px-2 py-0.5 rounded border border-indigo-500/15">
                        Gemini AI
                      </span>
                    </div>

                    <div className="flex-1 my-4 flex flex-col justify-center items-center">
                      {!aiLoading && !aiFix && !aiError && (
                        <div className="text-center space-y-4 max-w-xs">
                          <BookOpen className="h-10 w-10 text-slate-500 mx-auto" />
                          <p className="text-xs text-slate-400">
                            Request automated secure code refactoring for this vulnerability instance.
                          </p>
                          <button
                            onClick={() => fetchAiFix(selectedVuln.id)}
                            className="bg-primary-600 hover:bg-primary-700 text-white font-semibold text-xs px-4 py-2.5 rounded-xl transition-all shadow-md shadow-primary-600/10 flex items-center space-x-2 mx-auto cursor-pointer"
                          >
                            <Sparkles className="h-3.5 w-3.5 fill-white" />
                            <span>Remediate Code</span>
                          </button>
                        </div>
                      )}

                      {aiLoading && (
                        <div className="text-center space-y-3">
                          <Loader2 className="h-8 w-8 text-primary-500 animate-spin mx-auto" />
                          <p className="text-xs text-slate-400">Refactoring code securely with Gemini...</p>
                        </div>
                      )}

                      {aiError && (
                        <div className="text-center space-y-2 max-w-xs">
                          <AlertCircle className="h-8 w-8 text-cyber-red mx-auto" />
                          <p className="text-xs text-cyber-red font-semibold">{aiError}</p>
                          <button
                            onClick={() => fetchAiFix(selectedVuln.id)}
                            className="text-xs text-primary-500 underline font-semibold mt-2 cursor-pointer"
                          >
                            Retry Request
                          </button>
                        </div>
                      )}

                      {aiFix && (
                        <div className="w-full h-full overflow-y-auto space-y-4 text-left">
                          <div>
                            <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                              Refactored Secure Code
                            </span>
                            <pre className="bg-emerald-950/20 border border-emerald-500/20 text-emerald-400 p-3 rounded-lg text-[10px] font-mono select-text overflow-x-auto whitespace-pre leading-relaxed">
                              {aiFix.secureCode}
                            </pre>
                          </div>
                          <div>
                            <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">
                              Fix Rationale
                            </span>
                            <p className="text-xs text-slate-300 leading-relaxed bg-dark-900 p-3 rounded-lg border border-dark-800">
                              {aiFix.explanation}
                            </p>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>

                </div>
              )}

            </div>

          </div>
        </>
      )}

    </div>
  );
};

export default ScanDetail;
