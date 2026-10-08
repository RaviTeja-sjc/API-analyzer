import React, { useState, useEffect } from 'react';
import './AnalysisReportView.css';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { ImpactGraphView } from './ImpactGraphView';
import { useParams } from 'react-router-dom';
import { apiClient } from '../../api/client';
import { AlertTriangle, Lightbulb, FileCode } from 'lucide-react'; // Added icons

export const AnalysisReportView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [report, setReport] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isTriggering, setIsTriggering] = useState(false);
  const [activeTab, setActiveTab] = useState<'API' | 'PROJECT'>('API');

  // Filters for project issues
  const [filterSeverity, setFilterSeverity] = useState('ALL');
  const [filterCategory, setFilterCategory] = useState('ALL');

  const fetchReport = async () => {
    try {
      setIsLoading(true);
      const projectId = id || '00000000-0000-0000-0000-000000000000';
      const response = await apiClient.get(`/analysis/${projectId}`);
      setReport(response.data);
    } catch (error) {
      console.error("Failed to fetch analysis report:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [id]);

  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (report && (report.status === 'PENDING' || report.status === 'RUNNING')) {
      interval = setInterval(() => {
        fetchReport();
      }, 1000); // poll every 1s
    }
    return () => clearInterval(interval);
  }, [report, id]);

  const triggerAnalysis = async () => {
    try {
      setIsTriggering(true);
      const projectId = id || '00000000-0000-0000-0000-000000000000';
      await apiClient.post(`/analysis/${projectId}/repository-analysis`);
      fetchReport();
    } catch (error) {
      console.error("Failed to trigger analysis:", error);
    } finally {
      setIsTriggering(false);
    }
  };

  if (isLoading && !report) return <div style={{ padding: '4rem', textAlign: 'center' }}>Loading Analysis...</div>;
  if (!report) return <div style={{ padding: '4rem', textAlign: 'center' }}>Report not found.</div>;

  if (report.status === 'PENDING' || report.status === 'RUNNING') {
    return (
      <div className="report-container" style={{ textAlign: 'center', paddingTop: '4rem' }}>
        <h2>Analysis is running...</h2>
        <div style={{ marginTop: '2rem', fontSize: '1.2rem' }}>Progress: {report.progress}%</div>
        <pre style={{ marginTop: '1rem', background: '#f4f4f4', padding: '1rem', borderRadius: '8px', textAlign: 'left', display: 'inline-block', maxWidth: '600px', whiteSpace: 'pre-wrap' }}>
          {report.logs}
        </pre>
      </div>
    );
  }

  const apiAnalysis = report.apiAnalysis;
  const projectAnalysis = report.projectAnalysis;

  const filteredIssues = projectAnalysis?.issues?.filter((issue: any) => {
    if (filterSeverity !== 'ALL' && issue.severity !== filterSeverity) return false;
    if (filterCategory !== 'ALL' && issue.category !== filterCategory) return false;
    return true;
  }) || [];

  return (
    <div className="report-container">
      <header className="report-header">
        <div>
          <h1>Unified Engineering Review</h1>
          <p>Diffing {apiAnalysis?.baseVersion || 'HEAD~1'} &rarr; {apiAnalysis?.headVersion || 'HEAD'}</p>
        </div>
        <div style={{ display: 'flex', gap: '2rem', alignItems: 'center' }}>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '0.85rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '1px' }}>Security Score</div>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: report.scoring?.securityScore >= 80 ? '#10b981' : report.scoring?.securityScore >= 50 ? '#f59e0b' : '#ef4444' }}>
              {report.scoring?.securityScore !== undefined && report.scoring?.securityScore !== null ? `${report.scoring.securityScore} / 100` : 'N/A'}
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '0.85rem', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '1px' }}>API Health</div>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: report.scoring?.apiHealthScore >= 80 ? '#10b981' : report.scoring?.apiHealthScore >= 50 ? '#f59e0b' : (report.scoring?.apiHealthScore !== undefined && report.scoring?.apiHealthScore !== null ? '#ef4444' : '#94a3b8') }}>
              {report.scoring?.apiHealthScore !== undefined && report.scoring?.apiHealthScore !== null ? `${report.scoring.apiHealthScore} / 100` : 'N/A'}
            </div>
          </div>
          <div style={{ display: 'flex', gap: '1rem', marginLeft: '2rem' }}>
            <Button variant="primary" onClick={triggerAnalysis} disabled={isTriggering}>
              {isTriggering ? 'Analyzing...' : 'Analyze Repository'}
            </Button>
            <Button variant="secondary">Download PDF</Button>
          </div>
        </div>
      </header>

      {/* Tabs */}
      <div className="tabs-container">
        <button 
          className={`tab-button ${activeTab === 'API' ? 'active' : ''}`}
          onClick={() => setActiveTab('API')}
        >
          API ANALYZER
        </button>
        <button 
          className={`tab-button ${activeTab === 'PROJECT' ? 'active' : ''}`}
          onClick={() => setActiveTab('PROJECT')}
        >
          ENTIRE PROJECT ANALYZER
        </button>
      </div>

      {activeTab === 'API' && apiAnalysis && (
        <div>
          <div className="summary-grid">
            <Card className="stat-card">
              <h3>Total Changes</h3>
              <div className="stat-value">{apiAnalysis.summary?.totalChanges || 0}</div>
            </Card>
            <Card className="stat-card error-border">
              <h3>Breaking</h3>
              <div className="stat-value error-text">{apiAnalysis.summary?.breakingChanges || 0}</div>
            </Card>
            <Card className="stat-card warning-border">
              <h3>Potential Breaking</h3>
              <div className="stat-value warning-text">{apiAnalysis.summary?.potentiallyBreakingChanges || 0}</div>
            </Card>
            <Card className="stat-card info-border">
              <h3>Consumers Impacted</h3>
              <div className="stat-value info-text">{apiAnalysis.summary?.totalImpactedConsumers || 0}</div>
            </Card>
          </div>

          <h2 className="section-title">Impact Blast Radius</h2>
          <p className="section-subtitle">
            Interactive graph showing how breaking changes propagate from the API endpoint through your internal services.
          </p>
          <ImpactGraphView graphData={apiAnalysis.dependencyGraph} />

          <h2 className="section-title">Detailed Changes</h2>
          <div className="changes-list">
            {apiAnalysis.changes?.map((change: any, idx: number) => (
              <Card key={idx} className="change-card">
                <div className="change-header">
                  <div className="change-title">
                    <span className={`method-badge ${change.endpointMethod?.toLowerCase() || 'get'}`}>{change.endpointMethod || 'GET'}</span>
                    <span className="path-text">{change.endpointPath || '/'}</span>
                  </div>
                  <div className={`severity-badge severity-${change.severityLevel?.toLowerCase() || 'info'}`}>
                    {change.severityLevel} ({change.impactScore})
                  </div>
                </div>

                <div className="change-body">
                  <p className="change-desc"><strong>{change.changeType}</strong>: {change.description}</p>
                  {change.oldValue && change.newValue && (
                    <div className="diff-box">
                      <div className="diff-old">- {change.oldValue}</div>
                      <div className="diff-new">+ {change.newValue}</div>
                    </div>
                  )}

                  {change.warnings?.length > 0 && (
                    <div className="alert warning-alert">
                      <strong>Warnings:</strong>
                      <ul>{change.warnings.map((w: string, i: number) => <li key={i}>{w}</li>)}</ul>
                    </div>
                  )}
                  
                  {report?.recommendations?.filter((r: any) => r.category === 'API' && r.filePath === change.endpointPath && r.ruleId === 'API-' + change.changeType).map((rec: any, idx: number) => (
                    <div key={idx} className="alert success-alert" style={{ marginTop: '1rem' }}>
                      <strong>{rec.title}</strong>
                      <p style={{ margin: '0.5rem 0' }}>{rec.remediation}</p>
                    </div>
                  ))}
                </div>
              </Card>
            ))}
            {(!apiAnalysis.changes || apiAnalysis.changes.length === 0) && (
              <div style={{ padding: '3rem', textAlign: 'center', color: '#94a3b8', border: '1px dashed rgba(255,255,255,0.2)', borderRadius: '12px' }}>
                No API changes or commits found for this repository.
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'PROJECT' && projectAnalysis && (
        <div>
          {/* Executive Summary */}
          <div className="summary-grid" style={{ marginBottom: '2rem' }}>
            <div style={{ gridColumn: 'span 2', textAlign: 'left', background: 'linear-gradient(135deg, rgba(30,30,45,0.8), rgba(15,23,42,0.9))', borderRadius: '16px' }}>
            <Card className="stat-card">
              <h2 style={{ marginBottom: '1.5rem', color: '#fff', fontSize: '1.8rem', letterSpacing: '1px' }}>PROJECT HEALTH</h2>
              <div style={{ fontSize: '1.4rem', color: '#cbd5e1', marginBottom: '1rem' }}>Overall Risk: <strong style={{ color: '#fff', background: 'rgba(255,255,255,0.1)', padding: '0.2rem 1rem', borderRadius: '20px' }}>{projectAnalysis.summary?.overallRisk || 'UNKNOWN'}</strong></div>
              <div style={{ display: 'flex', gap: '2rem', marginTop: '1.5rem' }}>
                <span style={{ color: '#ef4444', fontWeight: 'bold', fontSize: '1.2rem' }}>Critical: {projectAnalysis.summary?.critical || 0}</span>
                <span style={{ color: '#f97316', fontWeight: 'bold', fontSize: '1.2rem' }}>High: {projectAnalysis.summary?.high || 0}</span>
                <span style={{ color: '#eab308', fontWeight: 'bold', fontSize: '1.2rem' }}>Medium: {projectAnalysis.summary?.medium || 0}</span>
                <span style={{ color: '#60a5fa', fontWeight: 'bold', fontSize: '1.2rem' }}>Low: {projectAnalysis.summary?.low || 0}</span>
              </div>
            </Card>
            </div>
            
            {report?.coverage && (
              <div style={{ gridColumn: 'span 2' }}>
                <Card className="stat-card">
                  <h3 style={{ marginBottom: '1rem', color: '#fff' }}>Repository Coverage</h3>
                  <div style={{ display: 'flex', gap: '2rem' }}>
                    <div>
                      <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Total Files</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#fff' }}>{report.coverage.totalFiles}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Analyzed</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#10b981' }}>{report.coverage.analyzedFiles}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Skipped / Unsupported</div>
                      <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#f59e0b' }}>{report.coverage.skippedFiles + report.coverage.unsupportedFiles}</div>
                    </div>
                  </div>
                  {report.coverage.detectedLanguages && Object.keys(report.coverage.detectedLanguages).length > 0 && (
                    <div style={{ marginTop: '1rem', fontSize: '0.85rem', color: '#94a3b8' }}>
                      Detected Languages: {Object.entries(report.coverage.detectedLanguages).map(([lang, count]) => `${lang} (${count})`).join(', ')}
                    </div>
                  )}
                </Card>
              </div>
            )}
          </div>

          <div style={{ display: 'flex', gap: '1rem', marginBottom: '3rem' }}>
            <select value={filterSeverity} onChange={(e) => setFilterSeverity(e.target.value)} style={{ padding: '0.75rem 1.5rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(0,0,0,0.5)', color: '#fff', fontSize: '1rem' }}>
              <option value="ALL">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
            <select value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)} style={{ padding: '0.75rem 1.5rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.1)', background: 'rgba(0,0,0,0.5)', color: '#fff', fontSize: '1rem' }}>
              <option value="ALL">All Categories</option>
              <option value="SECURITY">Security</option>
              <option value="ARCHITECTURE">Architecture</option>
              <option value="CODE_QUALITY">Code Quality</option>
              <option value="PERFORMANCE">Performance</option>
            </select>
          </div>

          <div className="changes-list">
            {filteredIssues.map((issue: any, idx: number) => (
              <Card key={idx} className="change-card">
                <div className="change-header">
                  <div className="change-title">
                    <span style={{ background: 'rgba(255,255,255,0.1)', padding: '0.3rem 0.8rem', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 'bold', color: '#cbd5e1' }}>{issue.category}</span>
                    <span className="path-text" style={{ fontWeight: '600' }}>{issue.title}</span>
                  </div>
                  <div className={`severity-badge severity-${issue.severity.toLowerCase()}`}>
                    {issue.severity}
                  </div>
                </div>

                <div className="change-body">
                  <div className="problem-identifier">
                    <div className="problem-header">
                      <AlertTriangle size={20} />
                      Real-Time Problem Identifier
                    </div>
                    <p style={{ margin: 0, fontSize: '1.05rem', color: '#e2e8f0', lineHeight: 1.5 }}>
                      {issue.problemDescription}
                    </p>
                    <div style={{ marginTop: '0.5rem', fontSize: '0.9rem', color: '#fca5a5' }}>
                      <strong>Impact:</strong> {issue.impact}
                    </div>
                  </div>

                  {report?.recommendations?.find((r: any) => r.projectIssueId === issue.id) && (
                    <div className="solution-block">
                      <div className="solution-header">
                        <Lightbulb size={20} />
                        Recommendation: {report.recommendations.find((r: any) => r.projectIssueId === issue.id).title}
                      </div>
                      <p style={{ margin: 0, fontSize: '1.05rem', color: '#e2e8f0', lineHeight: 1.5 }}>
                        {report.recommendations.find((r: any) => r.projectIssueId === issue.id).remediation}
                      </p>
                    </div>
                  )}

                  <div className="evidence-block">
                    <div className="evidence-location">
                      <FileCode size={16} />
                      {issue.filePath}
                    </div>
                    <pre className="evidence-code">{issue.evidence}</pre>
                  </div>
                  
                  <div style={{ marginTop: '1.5rem', fontSize: '0.9rem', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', background: issue.confidence === 'HIGH' ? '#10b981' : '#f59e0b' }}></span>
                    Confidence Score: <strong>{issue.confidence}</strong>
                  </div>
                </div>
              </Card>
            ))}
            {filteredIssues.length === 0 && (
              <div style={{ padding: '3rem', textAlign: 'center', color: '#94a3b8', border: '1px dashed rgba(255,255,255,0.2)', borderRadius: '12px' }}>
                No issues found matching the current filters.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

