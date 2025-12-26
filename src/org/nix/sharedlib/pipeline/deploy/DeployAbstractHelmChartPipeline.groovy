package org.nix.sharedlib.pipeline.deploy

import org.nix.sharedlib.agent.AgentRunner
import org.nix.sharedlib.agent.DeployAgentFactory
import org.nix.sharedlib.git.GitUtils
import org.nix.sharedlib.helm.HelmUtils
import org.nix.sharedlib.pipeline.AbstractPipeline

/**
 * Deploy abstract Helm chart
 */
abstract class DeployAbstractHelmChartPipeline extends AbstractPipeline {

    protected int agentTimeout = 15
    protected String artifactAbsoluteRepoPath = ''
    protected String chartRepoName = ''
    protected String chartRepoBranch = GitUtils.GIT_RELEASE_BRANCH
    protected String deployTimeout = '10m'
    protected String environment = ''
    protected String environmentRepoBranch = GitUtils.GIT_RELEASE_BRANCH
    protected String environmentAbsoluteRepoPath = ''
    protected String namespace = ''

    protected final static String CDP_REPO_NAME = 'nix-kubernetes'

    protected AgentRunner agent
    protected GitUtils gitUtils
    protected HelmUtils helmUtils

    protected DeployAbstractHelmChartPipeline(Script script) {
        super(script)
        gitUtils = new GitUtils(script)
        helmUtils = new HelmUtils(script)
    }

    /**
     * run
     */
    void run(Map args = [:]) {
        try {
            agent = DeployAgentFactory.getAgent(script)
            parseArgs(args)
            agent.nodeWrapper('', agentTimeout) {
                checkoutEnvironmentRepoStage()
                downloadArtifactStage()
                deployStage()
            }
        } catch (e) {
            log.error(e.message)
            throw e
        }
    }

    /**
     * checkout environment repo stage
     */
    protected void checkoutEnvironmentRepoStage() {
        stage('Checkout Kubernetes environment repo') {
            agent.clearWorkspace()
            environmentAbsoluteRepoPath = gitUtils.cloneSshGitHubRepo('', CDP_REPO_NAME, environmentRepoBranch)
        }
    }

    /**
     * deploy stage
     */
    protected abstract void deployStage()

    /**
     * download artifact stage
     */
    protected abstract void downloadArtifactStage()

    /**
     * parse args
     */
    protected abstract void parseArgs(Map args)

}
