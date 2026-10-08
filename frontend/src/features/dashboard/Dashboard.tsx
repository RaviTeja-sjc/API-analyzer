import React, { useState, useEffect } from 'react';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { useNavigate } from 'react-router-dom';
import './Dashboard.css';
import { NewProjectModal } from './NewProjectModal';
import { apiClient } from '../../api/client';

interface Project {
  id: string;
  name: string;
  description: string;
  repositoryUrl: string;
  createdAt: string;
  // Additional fields from the backend
}

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchProjects = async () => {
    try {
      setIsLoading(true);
      const response = await apiClient.get('/projects');
      // Spring Data REST Page response typically wraps data in content
      setProjects(response.data.content || response.data || []);
    } catch (error) {
      console.error("Failed to fetch projects:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div>
          <h1 className="dashboard-title">API Analyzer</h1>
          <p className="dashboard-subtitle">Semantic diffing & impact analysis engine.</p>
        </div>
        <Button size="lg" onClick={() => setIsModalOpen(true)}>New Project</Button>
      </header>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>Loading projects...</div>
      ) : projects.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '4rem', color: 'var(--text-muted)' }}>
          No projects found. Create one to get started!
        </div>
      ) : (
        <div className="dashboard-grid">
          {projects.map(project => (
            <div className="dashboard-card-wrapper" key={project.id}>
              <Card title={project.name} subtitle={project.description || "No description"}>
                <div style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Repo: <a href={project.repositoryUrl} target="_blank" rel="noreferrer" style={{color: '#818cf8'}}>{project.repositoryUrl}</a>
                </div>
                {/* Status Badges could be fetched per project analysis */}
                <div className="status-badges">
                   <div className="badge badge-success">Analyzed</div>
                </div>
                <div className="card-actions-wrapper">
                  <Button variant="secondary" onClick={() => navigate(`/analysis/${project.id}`)}>View Report</Button>
                  <Button variant="primary" onClick={() => navigate(`/migration/${project.id}`)}>Review Patches</Button>
                </div>
              </Card>
            </div>
          ))}
        </div>
      )}

      <NewProjectModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSuccess={fetchProjects} 
      />
    </div>
  );
};
