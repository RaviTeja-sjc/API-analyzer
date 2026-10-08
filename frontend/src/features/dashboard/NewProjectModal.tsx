import React, { useState } from 'react';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import './NewProjectModal.css';
import { apiClient } from '../../api/client';

interface NewProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const NewProjectModal: React.FC<NewProjectModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [repoUrl, setRepoUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      await apiClient.post('/projects', {
        name,
        description,
        repositoryUrl: repoUrl,
      });
      onSuccess();
      onClose();
      setName('');
      setDescription('');
      setRepoUrl('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create project');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Add New Project">
      <form onSubmit={handleSubmit} className="new-project-form">
        {error && <div className="form-error">{error}</div>}
        
        <div className="form-group">
          <label htmlFor="name">Project Name</label>
          <input 
            id="name" 
            type="text" 
            value={name} 
            onChange={e => setName(e.target.value)} 
            placeholder="e.g. Payment Gateway API"
            required 
          />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description</label>
          <textarea 
            id="description" 
            value={description} 
            onChange={e => setDescription(e.target.value)} 
            placeholder="A brief description of this project"
          />
        </div>

        <div className="form-group">
          <label htmlFor="repoUrl">GitHub Repository URL</label>
          <input 
            id="repoUrl" 
            type="url" 
            value={repoUrl} 
            onChange={e => setRepoUrl(e.target.value)} 
            placeholder="https://github.com/username/repo"
            required 
          />
        </div>

        <div className="form-actions">
          <Button type="button" variant="ghost" onClick={onClose} disabled={isLoading}>Cancel</Button>
          <Button type="submit" variant="primary" isLoading={isLoading}>Add Project</Button>
        </div>
      </form>
    </Modal>
  );
};
