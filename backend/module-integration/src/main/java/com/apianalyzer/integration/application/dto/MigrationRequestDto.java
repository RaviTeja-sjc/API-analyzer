package com.apianalyzer.integration.application.dto;

public class MigrationRequestDto {
    private java.util.UUID projectId;
    private String owner;
    private String repo;
    private String baseBranch;
    private String filePath;
    private String patchedContent;
    private String commitMessage;
    private String prTitle;
    private String prBody;

    // Getters and Setters
    public java.util.UUID getProjectId() { return projectId; }
    public void setProjectId(java.util.UUID projectId) { this.projectId = projectId; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }
    public String getBaseBranch() { return baseBranch; }
    public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getPatchedContent() { return patchedContent; }
    public void setPatchedContent(String patchedContent) { this.patchedContent = patchedContent; }
    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }
    public String getPrTitle() { return prTitle; }
    public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
    public String getPrBody() { return prBody; }
    public void setPrBody(String prBody) { this.prBody = prBody; }
}
