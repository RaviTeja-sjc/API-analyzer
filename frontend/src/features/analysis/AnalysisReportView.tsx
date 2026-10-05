import React from 'react';
import './AnalysisReportView.css';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';

// Mock data reflecting AnalysisReportDto
const mockReport = {
  baseVersion: "v1.2.0",
  headVersion: "v2.0.0",
  summary: {
    totalChanges: 14,
    breakingChanges: 2,
    potentiallyBreakingChanges: 1,
    totalImpactedConsumers: 3,
  },
  changes: [
    {
      endpointMethod: "GET",
      endpointPath: "/api/v1/payments",
      changeType: "PARAMETER_REMOVED",
      breakingStatus: "BREAKING",
      severityLevel: "CRITICAL",
      impactScore: 95,
      description: "Parameter removed: transactionId",
      oldValue: "Present",
      newValue: "Removed",
      impactReasons: ["Base +50: Change is strictly BREAKING.", "Consumers +30: High usage detected.", "Importance +20: Path is flagged as a core critical endpoint."],
      warnings: ["This change will break downstream clients."],
      recommendations: ["Consider versioning the API endpoint instead of modifying it directly.", "Update all FE clients to stop sending transactionId."],
      impactedConsumers: [
        {
          fileName: "PaymentService.java",
          className: "PaymentService",
          methodName: "fetchPaymentDetails",
          confidence: 0.95,
          evidence: "Feign client maps directly to changed endpoint path: /api/v1/payments",
          dependencyPath: ["PaymentController.getPayment", "PaymentService.fetchPaymentDetails"]
        }
      ]
    },
    {
      endpointMethod: "POST",
      endpointPath: "/api/v1/users",
      changeType: "SCHEMA_PROPERTY_ADDED",
      breakingStatus: "NON_BREAKING",
      severityLevel: "LOW",
      impactScore: 10,
      description: "Added optional property: age",
      oldValue: "Missing",
      newValue: "Integer",
      impactReasons: ["Base +0: Change is NON-BREAKING backwards compatible."],
      warnings: [],
      recommendations: [],
      impactedConsumers: []
    }
  ]
};

import { ImpactGraphView } from './ImpactGraphView';

export const AnalysisReportView: React.FC = () => {
  return (
    <div className="report-container">
      <header className="report-header">
        <div>
          <h1 className="gradient-text">Analysis Report</h1>
          <p>Diffing {mockReport.baseVersion} &rarr; {mockReport.headVersion}</p>
        </div>
        <Button variant="secondary">Download PDF</Button>
      </header>

      <div className="summary-grid">
        <Card className="stat-card">
          <h3>Total Changes</h3>
          <div className="stat-value">{mockReport.summary.totalChanges}</div>
        </Card>
        <Card className="stat-card error-border">
          <h3>Breaking</h3>
          <div className="stat-value error-text">{mockReport.summary.breakingChanges}</div>
        </Card>
        <Card className="stat-card warning-border">
          <h3>Potential Breaking</h3>
          <div className="stat-value warning-text">{mockReport.summary.potentiallyBreakingChanges}</div>
        </Card>
        <Card className="stat-card info-border">
          <h3>Consumers Impacted</h3>
          <div className="stat-value info-text">{mockReport.summary.totalImpactedConsumers}</div>
        </Card>
      </div>

      <h2 style={{ marginTop: '3rem', marginBottom: '1rem' }}>Impact Blast Radius</h2>
      <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>
        Interactive graph showing how breaking changes propagate from the API endpoint through your internal Java services.
      </p>
      <ImpactGraphView />

      <h2 style={{ marginTop: '3rem', marginBottom: '1rem' }}>Detailed Changes</h2>

      
      <div className="changes-list">
        {mockReport.changes.map((change, idx) => (
          <Card key={idx} className="change-card">
            <div className="change-header">
              <div className="change-title">
                <span className={`method-badge ${change.endpointMethod.toLowerCase()}`}>{change.endpointMethod}</span>
                <span className="path-text">{change.endpointPath}</span>
              </div>
              <div className={`severity-badge severity-${change.severityLevel.toLowerCase()}`}>
                {change.severityLevel} ({change.impactScore})
              </div>
            </div>

            <div className="change-body">
              <p className="change-desc"><strong>{change.changeType}</strong>: {change.description}</p>
              <div className="diff-box">
                <div className="diff-old">- {change.oldValue}</div>
                <div className="diff-new">+ {change.newValue}</div>
              </div>
            </div>

            {change.warnings.length > 0 && (
              <div className="alert warning-alert">
                <strong>Warnings:</strong>
                <ul>{change.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
              </div>
            )}
            
            {change.recommendations.length > 0 && (
              <div className="alert success-alert">
                <strong>Recommendations:</strong>
                <ul>{change.recommendations.map((r, i) => <li key={i}>{r}</li>)}</ul>
              </div>
            )}

            {change.impactedConsumers.length > 0 && (
              <div className="consumers-section">
                <h4>Affected Consumers (AST Detected)</h4>
                {change.impactedConsumers.map((consumer, cIdx) => (
                  <div key={cIdx} className="consumer-item">
                    <div className="consumer-header">
                      <span>{consumer.className}.{consumer.methodName}</span>
                      <span className="confidence-badge">Confidence: {(consumer.confidence * 100).toFixed(0)}%</span>
                    </div>
                    <p className="evidence-text">Evidence: {consumer.evidence}</p>
                    <div className="dependency-path">
                      Path: {consumer.dependencyPath.join(' → ')}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
};
