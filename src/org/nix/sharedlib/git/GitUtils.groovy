package org.nix.sharedlib.git

import org.nix.sharedlib.pipeline.AbstractPipeline

import java.util.regex.Pattern

/**
 * Git utils
 */
class GitUtils extends AbstractPipeline {

    private final static String GITHUB_HTTPS_BASE_ADDRESS = 'https://github.com'
    private final static String GITHUB_SSH_BASE_ADDRESS = 'git@github.com:'
    private final static String GITHUB_HTTPS_RAW_USER_CONTENT_ADDRESS =
        'https://raw.githubusercontent.com'

    private final static String GITHUB_PROJECT_NAME = 'nix-fit-org'

    private final static String GITHUB_SSH_CREDENTIALS_ID = 'ssh_private'
    private final static String GITHUB_SSH_CREDENTIALS_VARIABLE = 'SSH_KEY'

    public final static String GIT_DEVELOPMENT_BRANCH = 'develop'
    public final static String GIT_RELEASE_BRANCH = 'main'

    protected Pattern releaseBranchPattern = ~ /^(main|\d{1,2})$/

    GitUtils(Script script) {
        super(script)
    }

    /**
     * clone GitHub repo via ssh
     */
    String cloneSshGitHubRepo(String fullSshRepoUrl = '', String fullRepoName, String branchName = GIT_RELEASE_BRANCH) {
        // fullSshRepoUrl git@github.com:nix-fit-org/nix-kubernetes.git or fullRepoName (nix-kubernetes)
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
                export GIT_SSH_COMMAND="ssh \
                    -i ${script.env[GITHUB_SSH_CREDENTIALS_VARIABLE]} \
                    -o UserKnownHostsFile=/dev/null \
                    -o StrictHostKeyChecking=no \
                    -o IdentitiesOnly=yes"
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
     * download raw GitHub file (public repos)
     */
    void getRawGitHubFile(String repoName, String filePath, String branchName = GIT_RELEASE_BRANCH) {
        String rawGitHubFileUrl =
            "${GITHUB_HTTPS_RAW_USER_CONTENT_ADDRESS}/${GITHUB_PROJECT_NAME}/${repoName}/${branchName}/${filePath}"
        log.info("Downloading file: ${rawGitHubFileUrl}"
        )
        script.sh "curl --fail-with-body -kLo ${filePath} ${rawGitHubFileUrl} && cat ${filePath}"
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
        return script.env.BRANCH_NAME ==~ releaseBranchPattern
    }

}
