package org.nix.sharedlib.git

import org.nix.sharedlib.pipeline.AbstractPipeline

import java.util.regex.Pattern

/**
 * Git utils
 */
class GitUtils extends AbstractPipeline {

    private final static String GITHUB_HTTPS_BASE_ADDRESS = 'https://github.com'
    private final static String GITHUB_SSH_BASE_ADDRESS = 'git@github.com:'

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
    String cloneSshGitHubRepo(String fullSshRepoUrl = '', String fullRepoName, String branchName = GIT_RELEASE_BRANCH) {
        // fullSshRepoUrl git@github.com:nix-fit/nix-kubernetes.git or fullRepoName (nix-kubernetes)
        String sshRepoUrl = fullSshRepoUrl ?: "${GITHUB_SSH_BASE_ADDRESS}${GITHUB_PROJECT_NAME}/${fullRepoName}.git"
        log.info("Clonning repo: ${sshRepoUrl}, branch: ${branchName}")
        String repoName = getRepoNameFromScmUrl(sshRepoUrl)
        // get absolute target repo path
        String absoluteTargetRepoPath = getAbsoluteDirPath(repoName)
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
                    ${sshRepoUrl} ${absoluteTargetRepoPath}
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
     * transform https url to ssh
     */  
    String httpsToSshGitHubUrl(String httpsUrl) {
        return httpsUrl
            .replaceFirst(GITHUB_HTTPS_BASE_ADDRESS, GITHUB_SSH_BASE_ADDRESS)
    }

    /**
     * get repo name from scm url
     */
    String getRepoNameFromScmUrl(String scmRepoUrl) {
        return scmRepoUrl
            .tokenize('/').last().replace('.git', '')
    }

    /**
     * is current branch for release or not
     */
    boolean isReleaseBranch() {
        return script.env.BRANCH_NAME ==~ RELEASE_BRANCH_PATTERN
    }
}
