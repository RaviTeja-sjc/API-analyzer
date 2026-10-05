import React, { useState } from 'react';
import './MigrationReviewView.css';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';

interface PatchProposal {
  id: string;
  fileName: string;
  affectedSymbol: string;
  originalContentHash: string;
  sourceVersion: string;
  patchVersion: string;
  diffContent: string;
  explanation: string;
  confidence: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'APPLIED';
  reviewer: string | null;
  reviewedAt: string | null;
}

const mockProposals: PatchProposal[] = [
  {
    id: "patch-101",
    fileName: "src/main/java/com/payments/PaymentDto.java",
    affectedSymbol: "PaymentDto.transactionId",
    originalContentHash: "a1b2c3d4e5f6",
    sourceVersion: "v1.2.0",
    patchVersion: "v2.0.0",
    diffContent: "<<<<<< ORIGINAL\n    private String transactionId;\n    \n    public String getTransactionId() {\n        return transactionId;\n    }\n====== PATCHED\n\n>>>>>> END",
    explanation: "The JSON property 'transactionId' was removed from the API spec. Safely removing this field from the DTO to prevent Jackson deserialization warnings.",
    confidence: 0.95,
    status: 'PENDING',
    reviewer: null,
    reviewedAt: null
  },
  {
    id: "patch-102",
    fileName: "src/main/java/com/payments/PaymentClient.java",
    affectedSymbol: "PaymentClient.createPayment",
    originalContentHash: "9f8e7d6c5b4a",
    sourceVersion: "v1.2.0",
    patchVersion: "v2.0.0",
    diffContent: "<<<<<< ORIGINAL\n    @PostMapping(\"/api/v1/payments\")\n    PaymentResponse createPayment(@RequestBody PaymentDto dto);\n====== PATCHED\n    @PostMapping(\"/api/v2/payments\")\n    PaymentResponse createPayment(@RequestBody PaymentDto dto);\n>>>>>> END",
    explanation: "Endpoint /api/v1/payments was removed. Migrating FeignClient to the new /api/v2/payments endpoint.",
    confidence: 1.0,
    status: 'APPROVED',
    reviewer: "alice@apianalyzer.com",
    reviewedAt: "2026-10-05T12:00:00Z"
  }
];

export const MigrationReviewView: React.FC = () => {
  const [proposals, setProposals] = useState<PatchProposal[]>(mockProposals);

  const handleAction = (id: string, action: 'APPROVED' | 'REJECTED') => {
    setProposals(prev => prev.map(p => {
      if (p.id === id) {
        return {
          ...p,
          status: action,
          reviewer: 'current.user@example.com',
          reviewedAt: new Date().toISOString()
        };
      }
      return p;
    }));
  };

  return (
    <div className="review-container">
      <header className="review-header">
        <div>
          <h1 className="gradient-text">Migration Patch Review</h1>
          <p>Review and approve high-confidence automated code transformations.</p>
        </div>
      </header>

      <div className="proposals-list">
        {proposals.map(proposal => (
          <Card key={proposal.id} className="proposal-card">
            <div className="proposal-header">
              <div>
                <h3 className="file-name">{proposal.fileName}</h3>
                <p className="symbol-name">Symbol: {proposal.affectedSymbol}</p>
              </div>
              <div className="meta-info">
                <span className={`status-badge status-${proposal.status.toLowerCase()}`}>{proposal.status}</span>
                <span className="confidence-badge">Confidence: {(proposal.confidence * 100).toFixed(0)}%</span>
              </div>
            </div>

            <div className="proposal-body">
              <div className="explanation-box">
                <strong>Why?</strong> {proposal.explanation}
              </div>
              
              <div className="diff-viewer">
                <div className="diff-header">
                  <span>Source Hash: {proposal.originalContentHash}</span>
                  <span>{proposal.sourceVersion} &rarr; {proposal.patchVersion}</span>
                </div>
                <pre className="diff-content">{proposal.diffContent}</pre>
              </div>
            </div>

            <div className="proposal-footer">
              {proposal.status === 'PENDING' ? (
                <div className="action-buttons">
                  <Button variant="danger" onClick={() => handleAction(proposal.id, 'REJECTED')}>Reject Patch</Button>
                  <Button variant="primary" onClick={() => handleAction(proposal.id, 'APPROVED')}>Approve & Apply</Button>
                </div>
              ) : (
                <div className="audit-trail">
                  <span>{proposal.status === 'APPROVED' ? '✅ Approved' : '❌ Rejected'} by <strong>{proposal.reviewer}</strong></span>
                  <span> at {new Date(proposal.reviewedAt!).toLocaleString()}</span>
                </div>
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};
