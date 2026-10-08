// @ts-nocheck
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { AnalysisReportView } from '../AnalysisReportView';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { apiClient } from '../../../api/client';
import { vi, describe, test, expect, beforeEach } from 'vitest';

vi.mock('../../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  }
}));

// Mock ResizeObserver for ReactFlow/Dagre
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserver;

const mockReportData = {
  status: "COMPLETED",
  scoring: {
    securityScore: 82,
    apiHealthScore: null
  },
  projectAnalysis: {
    summary: { total: 1, critical: 1, high: 0, medium: 0, low: 0, overallRisk: "CRITICAL" },
    issues: [
      {
        id: "issue-1",
        title: "Hardcoded Secret",
        category: "SECURITY",
        severity: "CRITICAL",
        problemDescription: "Found a hardcoded password in DatabaseConfig.",
        impact: "Credential exposure.",
        evidence: "password = 'supersecret'",
        filePath: "src/config/DatabaseConfig.java",
        confidence: "HIGH"
      }
    ]
  },
  apiAnalysis: {
    baseVersion: "HEAD~1",
    headVersion: "HEAD",
    summary: { totalChanges: 1, breakingChanges: 1, potentiallyBreakingChanges: 0, totalImpactedConsumers: 0 },
    changes: [
      {
        endpointMethod: "GET",
        endpointPath: "/api/v1/users",
        changeType: "ENDPOINT_REMOVED",
        severityLevel: "CRITICAL",
        impactScore: 100,
        description: "Endpoint was removed completely.",
        oldValue: "/api/v1/users",
        newValue: null,
        warnings: ["Breaking change detected"]
      }
    ],
    dependencyGraph: {
      nodes: [{ id: "n1", data: { label: "Node 1" }, className: "node-change" }],
      edges: []
    }
  },
  recommendations: [
    {
      projectIssueId: "issue-1",
      title: "Use Environment Variables",
      remediation: "Move the credential to managed secret configuration.",
      category: "SECURITY"
    },
    {
      category: "API",
      filePath: "/api/v1/users",
      ruleId: "API-ENDPOINT_REMOVED",
      title: "Deprecate gracefully",
      remediation: "Use @Deprecated and support both for 1 version."
    }
  ]
};

describe('AnalysisReportView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter initialEntries={['/analysis/123']}>
        <Routes>
          <Route path="/analysis/:id" element={<AnalysisReportView />} />
        </Routes>
      </MemoryRouter>
    );
  };

  test('1. Real score rendering & 2. API N/A rendering', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockReportData });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('82 / 100')).toBeTruthy();
      expect(screen.getAllByText('N/A').length).toBeGreaterThan(0); // For API Health
    });
  });

  test('3. Real issue rendering & 6. Evidence rendering', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockReportData });
    renderComponent();
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('ENTIRE PROJECT ANALYZER'));
    });

    expect(screen.getByText('Hardcoded Secret')).toBeTruthy();
    expect(screen.getByText("password = 'supersecret'")).toBeTruthy();
    expect(screen.getByText('src/config/DatabaseConfig.java')).toBeTruthy();
  });

  test('4. Severity/category filtering', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockReportData });
    renderComponent();
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('ENTIRE PROJECT ANALYZER'));
    });
    
    expect(screen.getByText('Hardcoded Secret')).toBeTruthy();

    const severitySelect = screen.getByDisplayValue('All Severities');
    fireEvent.change(severitySelect, { target: { value: 'LOW' } });

    expect(screen.queryByText('Hardcoded Secret')).toBeNull();
    expect(screen.getByText('No issues found matching the current filters.')).toBeTruthy();
  });

  test('5. Recommendation rendering', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockReportData });
    renderComponent();
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('ENTIRE PROJECT ANALYZER'));
    });

    expect(screen.getByText('Recommendation: Use Environment Variables')).toBeTruthy();
    expect(screen.getByText('Move the credential to managed secret configuration.')).toBeTruthy();
  });

  test('7. API metadata rendering', async () => {
    (apiClient.get as any).mockResolvedValue({ data: mockReportData });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('ENDPOINT_REMOVED')).toBeTruthy();
      expect(screen.getByText('/api/v1/users')).toBeTruthy();
    });
  });

  test('9. Empty findings', async () => {
    const emptyData = { ...mockReportData, projectAnalysis: { summary: {}, issues: [] }, apiAnalysis: { ...mockReportData.apiAnalysis, changes: [] } };
    (apiClient.get as any).mockResolvedValue({ data: emptyData });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('No API changes or commits found for this repository.')).toBeTruthy();
    });
  });

  test('10. Backend/network error state', async () => {
    (apiClient.get as any).mockRejectedValue(new Error('Network error'));
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Report not found.')).toBeTruthy();
    });
  });

  test('11. Loading/progress state', async () => {
    (apiClient.get as any).mockResolvedValue({ data: { status: 'RUNNING', progress: 50, logs: 'Analyzing...' } });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Analysis is running...')).toBeTruthy();
      expect(screen.getByText('Progress: 50%')).toBeTruthy();
    });
  });
});
