package org.nix.sharedlib.pipeline.deploy

import org.nix.sharedlib.git.GitUtils

/**
 * Deploy Helm chart
 */
class DeployHelmChartPipeline extends DeployAbstractHelmChartPipeline {

    private final static String HELM_CHART_REPO_PREFIX = 'helm-'

    DeployHelmChartPipeline(Script script) {
        super(script)
    }

    @Override
    protected void deployStage() {
        stage('Deploy chart') {
            script.dir(artifactAbsoluteRepoPath) {
                // remove repo prefix
                String releaseName = removeRepoPrefix(chartRepoName, HELM_CHART_REPO_PREFIX)
                helmUtils.installHelmRelease(
                    releaseName,
                    environment,
                    namespace,
                    environmentAbsoluteRepoPath,
                    deployTimeout
                )
            }
        }
    }

    @Override
    protected void downloadArtifactStage() {
        stage('Download artifact') {
            artifactAbsoluteRepoPath = gitUtils.cloneSshGitHubRepo('', chartRepoName, chartRepoBranch)
        }
    }

    @Override
    protected void parseArgs(Map args) {
        // chart repo name
        chartRepoName = args.get('chartRepoName')
        // chart repo branch
        chartRepoBranch = args.get('chartRepoBranch', GitUtils.GIT_RELEASE_BRANCH)
        // kubernetes environment (dev/prod)
        environment = args.get('environment')
        // kubernetes namespace (frontend/backend/keycloak)
        namespace = args.get('namespace')
        // kubernetes environment repo branch
        environmentRepoBranch = args.get('environmentRepoBranch', GitUtils.GIT_RELEASE_BRANCH)
        // deploy timeout
        deployTimeout = args.get('deployTimeout', '10m')
    }

}
