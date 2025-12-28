package org.nix.sharedlib.pipeline.build

import org.nix.sharedlib.agent.BuildAgentFactory
import org.nix.sharedlib.git.GitUtils

/**
 * Build .Net library pipeline
 */
class BuildDotnetLibraryPipeline extends BuildAbstractAppPipeline {

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
                    credentialsId: 'github_token_classic',
                    variable: 'NUGET_GITHUB_PACKAGES_TOKEN'
                )
            ]) {
                String skipDuplicate = (gitUtils.isReleaseBranch() || testRelease)
                    ? "--skip-duplicate" : ""
                script.dir(projectAbsoluteRepoPath) {
                    script.sh """
                        dotnet nuget push nupkgs/*.nupkg \
                            --source https://nuget.pkg.github.com/nix-fit/index.json \
                            --api-key \${NUGET_GITHUB_PACKAGES_TOKEN} \
                            ${skipDuplicate}
                    """
                }
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        log.info('Placeholder for parseArgs')
    }

}
