import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Dashboard } from '../features/dashboard/Dashboard';
import { AnalysisReportView } from '../features/analysis/AnalysisReportView';
import { MigrationReviewView } from '../features/analysis/MigrationReviewView';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/analysis" element={<AnalysisReportView />} />
      <Route path="/migration" element={<MigrationReviewView />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
