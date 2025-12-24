package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory

/**
 * Build .Net library pipeline
 */
class BuildDotnetLibraryPipeline extends BuildAbstractAppPipeline {

    private final static String NUGET_GITHUB_PACKAGES_URL = 'https://nuget.pkg.github.com/nix-fit-org/index.json'
    private final static String NUGET_GITHUB_PACKAGES_SECRET_CREDENTIALS_ID = 'github_token_classic'
    private final static String NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE = 'NUGET_GITHUB_PACKAGES_TOKEN'

    protected boolean testRelease = false

    BuildDotnetLibraryPipeline(Script script) {
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
                publishStage()
            }
        } catch (e) {
            log.error(e.message)
            throw e
        }
    }

    @Override
    protected void buildStage() {
        stage('Build .Net library') {
            script.withEnv([
                'DOTNET_NOLOGO=true',
                'DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1',
                'DOTNET_GENERATE_ASPNET_CERTIFICATE=false',
                'DOTNET_CLI_TELEMETRY_OPTOUT=1',
            ]) {
                script.dir(projectAbsoluteRepoPath) {
                    /* groovylint-disable-next-line UnnecessaryGetter */
                    String versionSuffixArg = (gitUtils.isReleaseBranch() || testRelease)
                        ? '' : "--version-suffix snapshot-${script.env.BUILD_NUMBER}"
                    script.sh """
                        dotnet restore *.sln
                        dotnet build *.sln --configuration Release --no-restore
                        dotnet pack *.sln \
                            --configuration Release \
                            --no-build \
                            ${versionSuffixArg} \
                            --output nupkgs
                    """
                }
            }
        }
    }

    /**
     * publish stage
     */
    protected void publishStage() {
        stage('Publish .Net library') {
            script.withCredentials([
                script.string(
                    credentialsId: NUGET_GITHUB_PACKAGES_SECRET_CREDENTIALS_ID,
                    variable: NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_VARIABLE
            )]) {
                /* groovylint-disable-next-line UnnecessaryGetter */
                String skipDuplicate = (gitUtils.isReleaseBranch() || testRelease)
                    ? '--skip-duplicate' : ''
                script.dir(projectAbsoluteRepoPath) {
                    script.sh """
                        dotnet nuget push nupkgs/*.nupkg \
                            --source ${NUGET_GITHUB_PACKAGES_URL} \
                            --api-key \${NUGET_GITHUB_PACKAGES_TOKEN} \
                            ${skipDuplicate}
                    """
                }
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        testRelease = args.get('testRelease', testRelease)
    }

}
