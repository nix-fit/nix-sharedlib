package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory
import org.nix.sharedlib.git.GitUtils

/**
 * Build .Net library pipeline
 */
class BuildDotnetAppPipeline extends BuildAbstractAppPipeline {

    protected String dotnetVersion = ''
    protected String appVersion = ''
    protected String dotnetDockerImageTemplateBranch = 'main'
    protected boolean testRelease = false

    private final static String BACKEND_REPO_PREFIX = 'back-app-'
    private final static String BACKEND_DOCKER_IMAGE_SUB_PATH = 'apps/back'
    private final static String BACKEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME = 'docker-dotnet-template'

    private final static String NUGET_GITHUB_PACKAGES_URL = 'https://nuget.pkg.github.com/nix-fit/index.json'
    private final static String NUGET_GITHUB_PACKAGES_SECRET_CREDENTIALS_ID = 'github_token_classic_with_username'
    private final static String NUGET_GITHUB_PACKAGES_USER_CREDENTIALS_VARIABLE = 'GITHUB_USERNAME'
    private final static String NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE = 'GITHUB_TOKEN'

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
            agent.nodeWrapper(agentTimeout, args) {
                checkoutProjectRepoStage()
                buildStage()
                buildDockerImageStage()
            }
        } catch (e) {
            log.error(e.message)
            throw e
        }
    }

    @Override
    protected void buildStage() {
        stage('Build .Net app') {
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
                        script.sh """
                            dotnet restore *.csproj
                            dotnet build *.csproj \
                                --configuration Release \
                                --no-restore \
                                -p:AssemblyName=app \
                                -p:UseAppHost=true
                            dotnet publish *.csproj \
                                --configuration Release \
                                --no-build \
                                -p:AssemblyName=app \
                                -p:UseAppHost=true \
                                -p:PublishDir=dist
                        """
                    }
                }
            }
        }
    }

    @Override
    protected void buildDockerImageStage() {
        stage('Build Docker image') {
            script.dir(projectAbsoluteRepoPath) {
                gitUtils.getRawGitHubFile(BACKEND_DOCKER_IMAGE_TEMPLATE_REPO_NAME, 'Dockerfile', dotnetDockerImageTemplateBranch)
                String dockerImageName = removeRepoPrefix(projectRepoName, BACKEND_REPO_PREFIX)
                dockerUtils.buildDockerImage(
                    dockerImageName, BACKEND_DOCKER_IMAGE_SUB_PATH, appVersion, testRelease
                )
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        dotnetVersion = args.get('dotnetVersion', dotnetVersion)
        dotnetDockerImageTemplateBranch = args.get('dotnetDockerImageTemplateBranch', dotnetDockerImageTemplateBranch)
        testRelease = args.get('testRelease', testRelease)
    }

}
