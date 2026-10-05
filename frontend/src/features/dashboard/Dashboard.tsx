import React from 'react';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';

import { useNavigate } from 'react-router-dom';

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 className="gradient-text" style={{ fontSize: '2.5rem', fontWeight: 700 }}>API Analyzer</h1>
          <p style={{ color: 'var(--text-muted)' }}>Semantic diffing & impact analysis engine.</p>
        </div>
        <Button size="lg">New Project</Button>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
        <Card title="Payment Gateway API" subtitle="Last analyzed: 2 hours ago">
          <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
            <div style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--status-error)', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.875rem' }}>
              3 Breaking Changes
            </div>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-success)', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.875rem' }}>
              4 Safe Changes
            </div>
          </div>
          <div style={{ marginTop: '1.5rem', display: 'flex', gap: '1rem' }}>
            <Button variant="secondary" style={{ flex: 1 }} onClick={() => navigate('/analysis')}>View Report</Button>
            <Button variant="primary" style={{ flex: 1 }} onClick={() => navigate('/migration')}>Review Patches</Button>
          </div>
        </Card>

        <Card title="User Service API" subtitle="Last analyzed: 1 day ago">
          <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
            <div style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-success)', padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.875rem' }}>
              0 Breaking Changes
            </div>
          </div>
          <div style={{ marginTop: '1.5rem' }}>
            <Button variant="secondary" style={{ width: '100%' }}>View Report</Button>
          </div>
        </Card>
      </div>
    </div>
  );
};
