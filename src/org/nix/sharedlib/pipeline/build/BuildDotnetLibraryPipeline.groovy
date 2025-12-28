package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory
import org.nix.sharedlib.git.GitUtils

/**
 * Build .Net library pipeline
 */
class BuildDotnetLibraryPipeline extends BuildAbstractAppPipeline {

    protected boolean testRelease = false

    private final static String NUGET_GITHUB_PACKAGES_URL = 'https://nuget.pkg.github.com/nix-fit/index.json'
    private final static String NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_ID = 'github_token_classic'
    private final static String NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_IVARIABLE = 'NUGET_GITHUB_PACKAGES_TOKEN'

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
            script.dir(projectAbsoluteRepoPath) {
                String versionSuffixArg = (gitUtils.isReleaseBranch() || testRelease)
                    ? "" : "--version-suffix snapshot-${script.env.BUILD_NUMBER}"
                script.sh """
                    dotnet restore *.sln
                    dotnet build *.sln --configuration Release
                    dotnet pack *.sln \
                        --configuration Release \
                        --no-build \
                        ${versionSuffixArg} \
                        --output nupkgs
                """
            }
        }
    }

    protected void publishStage() {
        stage('Publish .Net library') {
            script.withCredentials([
                script.string(
                    credentialsId: NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_ID,
                    variable: NUGET_GITHUB_PACKAGES_TOKEN_CREDENTIALS_IVARIABLE
                )
            ]) {
                String skipDuplicate = (gitUtils.isReleaseBranch() || testRelease)
                    ? "--skip-duplicate" : ""
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
