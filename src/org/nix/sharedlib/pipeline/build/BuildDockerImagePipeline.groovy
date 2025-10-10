package org.nix.sharedlib.pipeline.build

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

    BuildDockerImagePipeline(Script script) {
        super(script)
    }

    @Override
    protected void buildStage() {
        stage('Build Docker image') {
            script.dir(projectAbsoluteRepoPath) {
                currentVersion = script.readFile(versionFilePath).trim()
                log.info("Current version: ${currentVersion}")
                dockerUtils.lintDockerImage()
                dockerUtils.buildDockerImage(projectRepoName, dockerImageSubPath, currentVersion, testRelease)
            }
        }
    }

    @Override
    protected void parseArgs(Map args) {
        dockerImageSubPath = args.get('dockerImageSubPath', '')
        testRelease = args.get('testRelease', testRelease)
    }

}
