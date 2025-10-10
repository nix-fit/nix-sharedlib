package org.nix.sharedlib.git

import org.nix.sharedlib.pipeline.AbstractPipeline

import java.util.regex.Pattern

/**
 * Git utils
 */
class GitUtils extends AbstractPipeline {

    private final static String GITHUB_HTTPS_BASE_ADDRESS = 'https://github.com'
    private final static String GITHUB_SSH_BASE_ADDRESS = 'git@github.com'

    private final static String GITHUB_PROJECT_NAME = 'nix-fit'

    private final static String GITHUB_SSH_CREDENTIALS_ID = 'ssh_private'
    private final static String GITHUB_SSH_CREDENTIALS_VARIABLE = 'SSH_KEY'

    protected Pattern RELEASE_BRANCH_PATTERN = ~ /^(main|\d{1,2})$/

    public final static String GIT_DEVELOPMENT_BRANCH = 'develop'
    public final static String GIT_RELEASE_BRANCH = 'main'

    GitUtils(Script script) {
        super(script)
    }

    /**
     * clone GitHub repo via ssh
     */
    String cloneSshGitHubRepo(String repoType, String repoName, String fullRepoName = '', String branchName = GIT_RELEASE_BRANCH) {
        // fullRepoName (nix-kubernetes) or concat repoType (helm/docker) with repoName (keycloak)
        String targetRepo = fullRepoName ?: "${repoType}-${repoName}"
        log.info("Clonning repo: ${GITHUB_PROJECT_NAME}/${targetRepo}.git, branch: ${branchName}")
        // get absolute target repo path
        String absoluteTargetRepoPath = getAbsoluteDirPath(targetRepo)
        script.withCredentials([script.sshUserPrivateKey(
            credentialsId: GITHUB_SSH_CREDENTIALS_ID,
            keyFileVariable: GITHUB_SSH_CREDENTIALS_VARIABLE
        )]) {
            script.sh """
                export GIT_SSH_COMMAND="ssh -i ${script.env[GITHUB_SSH_CREDENTIALS_VARIABLE]} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes"

                git clone \\
                    --single-branch \\
                    --branch ${branchName} \\
                    --depth 1 \\
                    ${GITHUB_SSH_BASE_ADDRESS}:${GITHUB_PROJECT_NAME}/${targetRepo}.git ${absoluteTargetRepoPath}
            """
        }
        return absoluteTargetRepoPath
    }

    /**
     * get repo url from scm for Organization Folder (Multibranch Pipeline)
     */
    String getRepoUrlFromScm() {
        String repoUrl = script.scm.userRemoteConfigs[0].url
        log.info("Repo url: ${repoUrl}")
        return repoUrl
    }

    /**
     * is current branch for release or not
     */
    boolean isReleaseBranch() {
        return script.env.BRANCH_NAME ==~ RELEASE_BRANCH_PATTERN
    }
}
