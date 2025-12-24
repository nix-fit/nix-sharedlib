package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory

/**
 * Build .Net library pipeline
 */
class BuildDotnetAppPipeline extends BuildAbstractAppPipeline {

    private final static String BACKEND_REPO_PREFIX = 'back-app-'
    private final static String BACKEND_DOCKER_IMAGE_SUB_PATH = 'apps/back'
    private final static String BACKEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME = 'docker-dotnet-template'

    private final static String NUGET_GITHUB_PACKAGES_URL = 'https://nuget.pkg.github.com/nix-fit-org/index.json'
    private final static String NUGET_GITHUB_PACKAGES_SECRET_CREDENTIALS_ID = 'github_token_classic_with_username'
    private final static String NUGET_GITHUB_PACKAGES_USER_CREDENTIALS_VARIABLE = 'GITHUB_USERNAME'
    private final static String NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE = 'GITHUB_TOKEN'

    protected String dotnetVersion = ''
    protected String appVersion = ''
    protected String dotnetDockerImageTemplateBranch = 'main'
    protected String platforms = 'linux/amd64'
    protected boolean testRelease = false

    BuildDotnetAppPipeline(Script script) {
        super(script)
    }

    /**
     * run
     */
    void run(Map args = [:]) {
        try {
            agent = BuildAgentFactory.getBuildDotnetAgent(script)
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
            script.withCredentials([
                script.usernamePassword(
                    credentialsId: NUGET_GITHUB_PACKAGES_SECRET_CREDENTIALS_ID,
                    usernameVariable: NUGET_GITHUB_PACKAGES_USER_CREDENTIALS_VARIABLE,
                    passwordVariable: NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE
            )]) {
                script.withEnv([
                    'DOTNET_NOLOGO=true',
                    'DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1',
                    'DOTNET_GENERATE_ASPNET_CERTIFICATE=false',
                    'DOTNET_CLI_TELEMETRY_OPTOUT=1'
                ]) {
                    script.dir(projectAbsoluteRepoPath) {
                        appVersion = script.sh(
                            script: 'dotnet msbuild *.csproj -getProperty:Version',
                            returnStdout: true
                        ).trim()
                        log.info("Current version: ${appVersion}")
                        gitUtils.getRawGitHubFile(
                            BACKEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME,
                            'Dockerfile',
                            dotnetDockerImageTemplateBranch
                        )
                        String dockerImageName = removeRepoPrefix(projectRepoName, BACKEND_REPO_PREFIX)
                        dockerUtils.buildDockerImage(
                            dockerImageName,
                            BACKEND_DOCKER_IMAGE_SUB_PATH,
                            appVersion,
                            testRelease,
                            [
                                platforms: platforms,
                                secrets: ["id=github_token,env=${NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE}"],
                                buildArgs: [
                                    "${NUGET_GITHUB_PACKAGES_USER_CREDENTIALS_VARIABLE}=" +
                                        "\${${NUGET_GITHUB_PACKAGES_USER_CREDENTIALS_VARIABLE}}",
                                    "NUGET_GITHUB_PACKAGES_URL=${NUGET_GITHUB_PACKAGES_URL}"
                                ]
                            ]
                        )
                    }
                }
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        dotnetVersion = args.get('dotnetVersion', dotnetVersion)
        dotnetDockerImageTemplateBranch = args.get('dotnetDockerImageTemplateBranch', dotnetDockerImageTemplateBranch)
        platforms = args.get('platforms', platforms)
        testRelease = args.get('testRelease', testRelease)
    }

}
