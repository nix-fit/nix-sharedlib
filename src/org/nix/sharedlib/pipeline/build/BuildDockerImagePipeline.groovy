package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory

/**
 * Build Docker image pipeline
 */
class BuildDockerImagePipeline extends BuildAbstractAppPipeline {

    private final static String DOCKER_REPO_PREFIX = 'docker-'

    protected String dockerImageSubPath = 'undefined'
    protected String versionFilePath = 'version'
    protected String currentVersion = ''
    protected boolean testRelease = false

    protected String cacheTag = 'cache'

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
            agent.nodeWrapper(agentTimeout, args) {
                checkoutProjectRepoStage()
                buildStage()
            }
        } catch (e) {
            log.error(e.message)
            throw e
        }
    }

    /**
     * build Docker image stage
     */
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
