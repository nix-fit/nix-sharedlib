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
                script.sh """
                    dotnet restore
                    dotnet build
                    dotnet pack src \
                        --configuration Release \
                        -p:VersionSuffix=snapshot-${script.env.BUILD_NUMBER} \
                        --output nupkgs
                """
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        log.info('Placeholder for parseArgs')
    }

}
