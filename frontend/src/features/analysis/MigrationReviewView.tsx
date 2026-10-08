import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import './MigrationReviewView.css';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { apiClient } from '../../api/client';

interface MigrationProposal {
  id: string;
  analysisJobId: string;
  projectIssueId: string;
  ruleId: string;
  category: string;
  severity: string;
  title: string;
  description: string;
  filePath: string;
  startLine: number | null;
  endLine: number | null;
  originalContent: string;
  proposedContent: string;
  unifiedDiff: string;
  rationale: string;
  validationStatus: string;
  applyStatus: string;
  autoFixSupported: boolean;
  createdAt: string;
}

export const MigrationReviewView: React.FC = () => {
  const { id } = useParams<{ id: string }>(); // analysisJobId
  const [proposals, setProposals] = useState<MigrationProposal[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProposals = async () => {
      try {
        setIsLoading(true);
        // Assuming the ID passed is analysisJobId. If it's projectId, the backend would need an endpoint to get latest job proposals.
        // For now, we query by the id from the URL directly.
        const response = await apiClient.get(`/migration/analysis/${id}/proposals`);
        setProposals(response.data);
        setError(null);
      } catch (err: any) {
        console.error("Failed to fetch migration proposals", err);
        setError("Failed to fetch migration proposals. Please ensure the analysis job exists and has completed.");
      } finally {
        setIsLoading(false);
      }
    };
    
    if (id) {
      fetchProposals();
    }
  }, [id]);

  const handleAction = async (proposalId: string, action: 'approve' | 'reject') => {
    try {
      const response = await apiClient.post(`/migration/proposals/${proposalId}/${action}`);
      setProposals(prev => prev.map(p => {
        if (p.id === proposalId) {
          return response.data;
        }
        return p;
      }));
    } catch (err) {
      console.error(`Failed to ${action} proposal`, err);
      alert(`Failed to ${action} proposal`);
    }
  };

  if (isLoading) {
    return <div style={{ padding: '4rem', textAlign: 'center' }}>Loading Proposals...</div>;
  }

  if (error) {
    return <div style={{ padding: '4rem', textAlign: 'center', color: '#ef4444' }}>{error}</div>;
  }

  return (
    <div className="review-container">
      <header className="review-header">
        <div>
          <h1 className="gradient-text">Migration Patch Review</h1>
          <p>Review and approve high-confidence automated code transformations.</p>
        </div>
      </header>

      <div className="proposals-list">
        {proposals.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '4rem', color: '#94a3b8', border: '1px dashed rgba(255,255,255,0.2)', borderRadius: '12px', background: 'rgba(0,0,0,0.2)' }}>
            <h2>No migration proposals found.</h2>
            <p style={{ marginTop: '1rem' }}>No automatically remediable issues were detected, or analysis hasn't completed yet.</p>
          </div>
        ) : (
          proposals.map(proposal => (
            <Card key={proposal.id} className="proposal-card">
              <div className="proposal-header">
                <div>
                  <h3 className="file-name">{proposal.filePath} {proposal.startLine ? `(Line ${proposal.startLine})` : ''}</h3>
                  <p className="symbol-name">Rule: {proposal.ruleId} - {proposal.title}</p>
                </div>
                <div className="meta-info">
                  <span className={`status-badge status-${proposal.applyStatus.toLowerCase()}`}>{proposal.applyStatus}</span>
                  <span className={`severity-badge severity-${proposal.severity.toLowerCase()}`}>{proposal.severity}</span>
                </div>
              </div>

              <div className="proposal-body">
                <div className="explanation-box">
                  <strong>Description:</strong> {proposal.description}<br/>
                  <strong>Rationale:</strong> {proposal.rationale}
                </div>
                
                <div className="diff-viewer">
                  <div className="diff-header">
                    <span>Validation: {proposal.validationStatus}</span>
                    <span>{proposal.autoFixSupported ? 'Auto-Fix Available' : 'Manual Migration Required'}</span>
                  </div>
                  {proposal.unifiedDiff ? (
                    <pre className="diff-content">{proposal.unifiedDiff}</pre>
                  ) : (
                    <pre className="diff-content evidence-code">{proposal.originalContent}</pre>
                  )}
                </div>
              </div>

              <div className="proposal-footer">
                {proposal.applyStatus === 'GENERATED' || proposal.applyStatus === 'STALE' ? (
                  <div className="action-buttons">
                    <Button variant="danger" onClick={() => handleAction(proposal.id, 'reject')}>Reject</Button>
                    <Button variant="primary" onClick={() => handleAction(proposal.id, 'approve')}>Approve</Button>
                  </div>
                ) : (
                  <div className="audit-trail">
                    <span>Status: <strong>{proposal.applyStatus}</strong></span>
                    <span> at {new Date(proposal.createdAt).toLocaleString()}</span>
                  </div>
                )}
                
                <div style={{ marginTop: '1rem', width: '100%', textAlign: 'right', fontSize: '0.9rem', color: '#94a3b8' }}>
                  {proposal.autoFixSupported ? (
                    <span style={{ color: '#f59e0b' }}>Apply unavailable — write-back not supported.</span>
                  ) : (
                    <span>Manual migration required.</span>
                  )}
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};
