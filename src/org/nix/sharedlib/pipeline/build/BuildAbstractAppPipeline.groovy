package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.AgentRunner
import org.nix.sharedlib.docker.DockerUtils
import org.nix.sharedlib.git.GitUtils
import org.nix.sharedlib.pipeline.AbstractPipeline

/**
 * Build abstract application pipeline
 */
abstract class BuildAbstractAppPipeline extends AbstractPipeline {

    protected int agentTimeout = 30

    protected String projectRepoUrl = ''
    protected String projectRepoName = ''
    protected String projectAbsoluteRepoPath = ''
    protected String projectReleaseRepoBranch = GitUtils.GIT_RELEASE_BRANCH

    protected AgentRunner agent
    protected DockerUtils dockerUtils
    protected GitUtils gitUtils

    protected BuildAbstractAppPipeline(Script script) {
        super(script)
        dockerUtils = new DockerUtils(script)
        gitUtils = new GitUtils(script)
    }

    /**
     * checkout project repository stage
     */
    protected void checkoutProjectRepoStage() {
        stage('Checkout project repo') {
            agent.clearWorkspace()
            projectRepoUrl = gitUtils.repoUrlFromScm()
            String sshProjectRepoUrl = gitUtils.httpsToSshGitHubUrl(projectRepoUrl)
            projectRepoName = gitUtils.getRepoNameFromScmUrl(sshProjectRepoUrl)
            String currentProjectRepoBranch = script.env.BRANCH_NAME
            projectAbsoluteRepoPath = gitUtils.cloneSshGitHubRepo(sshProjectRepoUrl, '', currentProjectRepoBranch)
        }
    }

    /**
     * parse args stage
     */
    protected abstract void parseArgs(Map args)

    /**
     * build stage
     */
    protected abstract void buildStage()

}
