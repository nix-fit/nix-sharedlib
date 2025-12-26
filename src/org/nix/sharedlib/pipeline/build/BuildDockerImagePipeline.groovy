package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory
import org.nix.sharedlib.git.GitUtils

/**
 * Build Docker image pipeline
 */
class BuildDockerImagePipeline extends BuildAbstractAppPipeline {

    protected String dockerImageSubPath = 'undefined'
    protected String contextDir = '.'
    protected String versionFilePath = 'version'
    protected String currentVersion = ''
    protected boolean testRelease = false

    protected String cacheTag = 'cache'

    private final static String DOCKER_REPO_PREFIX = 'docker-'

    BuildDockerImagePipeline(Script script) {
        super(script)
    }

    /**
     * run
     */
    void run(Map args = [:]) {
        try {
            agent = BuildAgentFactory.getBuildAgent(script)
            parseArgs(args)
            agent.nodeWrapper('', agentTimeout) {
                checkoutProjectRepoStage()
                buildStage()
            }
        } catch (e) {
            log.error(e.message)
            throw e
        }
    }

    @Override
    protected void buildStage() {
        stage('Build Docker image') {
            script.dir(projectAbsoluteRepoPath) {
                String dockerImageName = removeRepoPrefix(projectRepoName, DOCKER_REPO_PREFIX)
                currentVersion = script.readFile(versionFilePath).trim()
                log.info("Current version: ${currentVersion}")
                dockerUtils.lintDockerImage()
                dockerUtils.buildDockerImage(dockerImageName, dockerImageSubPath, currentVersion, testRelease)
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        dockerImageSubPath = args.get('dockerImageSubPath', dockerImageSubPath)
        testRelease = args.get('testRelease', testRelease)
    }

}
