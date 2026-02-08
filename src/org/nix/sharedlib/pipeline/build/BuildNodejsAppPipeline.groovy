package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory

/**
 * Build Node.js app pipeline
 */
class BuildNodejsAppPipeline extends BuildAbstractAppPipeline {

    private final static String FRONTEND_REPO_PREFIX = 'front-app-'
    private final static String FRONTEND_DOCKER_IMAGE_SUB_PATH = 'apps/front'
    private final static String FRONTEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME = 'docker-nodejs-template'

    protected String nodejsVersion = ''
    protected String appVersion = ''
    protected String nodejsDockerImageTemplateBranch = ''
    protected String platforms = 'linux/amd64'
    protected boolean testRelease = false

    BuildNodejsAppPipeline(Script script) {
        super(script)
    }

    /**
     * run
     */
    void run(Map args = [:]) {
        try {
            agent = BuildAgentFactory.getBuildNodejsAgent(script)
            parseArgs(args)
            args.useBuildkit = true
            agent.nodeWrapper(agentTimeout, args) {
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
            script.withEnv([
                // 'DOTNET_NOLOGO=true',
                // 'DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1',
                // 'DOTNET_GENERATE_ASPNET_CERTIFICATE=false',
                // 'DOTNET_CLI_TELEMETRY_OPTOUT=1'
            ]) {
                script.dir(projectAbsoluteRepoPath) {
                    appVersion = script.sh(
                        script: 'npm pkg get version',
                        returnStdout: true
                    ).trim().replaceAll('"', '')
                    log.info("Current version: ${appVersion}")
                    gitUtils.getRawGitHubFile(
                        FRONTEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME,
                        'Dockerfile',
                        nodejsDockerImageTemplateBranch
                    )
                    String dockerImageName = removeRepoPrefix(projectRepoName, FRONTEND_REPO_PREFIX)
                    dockerUtils.buildDockerImage(
                        dockerImageName,
                        FRONTEND_DOCKER_IMAGE_SUB_PATH,
                        appVersion,
                        testRelease,
                        [
                            platforms: platforms,
                            secrets: [],
                            buildArgs: []
                        ]
                    )
                }
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        nodejsVersion = args.get('nodejsVersion', nodejsVersion)
        nodejsDockerImageTemplateBranch = args.get('nodejsDockerImageTemplateBranch', nodejsVersion)
        platforms = args.get('platforms', platforms)
        testRelease = args.get('testRelease', testRelease)
    }

}
