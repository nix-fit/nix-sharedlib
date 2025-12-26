package org.nix.sharedlib.docker

import org.nix.sharedlib.git.GitUtils
import org.nix.sharedlib.pipeline.AbstractPipeline

import groovy.json.JsonOutput

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Docker utils
 */
class DockerUtils extends AbstractPipeline {

    private final static int VERSION_MAJOR_GROUP = 1
    private final static int VERSION_MINOR_GROUP = 2
    private final static int VERSION_PATCH_GROUP = 3
    private final static int VERSION_SUFFIX_GROUP = 4
    private final static Pattern VERSION_PATTERN =
            ~ /^(\d+)\.(\d+)(?:\.(\d+))?(?:-([^\s]+))?$/

    private final static String DOCKER_REGISTRY_ADDRESS = 'nix-docker.registry.twcstorage.ru'
    private final static String DOCKER_REGISTRY_CREDENTIALS_ID = 'docker_registry_auth'
    private final static String DOCKER_REGISTRY_CREDENTIALS_USER_VARIABLE = 'DOCKER_REGISTRY_AUTH_USER'
    private final static String DOCKER_REGISTRY_CREDENTIALS_PASSWORD_VARIABLE = 'DOCKER_REGISTRY_AUTH_PASSWORD'

    private final static String BUILDKIT_HOST = 'tcp://buildkitd.buildkit.svc.cluster.local:1234'

    protected String dockerfilePath = 'Dockerfile'

    protected String dockerBuildCacheSuffix = 'cache'

    protected GitUtils gitUtils

    DockerUtils(Script script) {
        super(script)
        gitUtils = new GitUtils(script)
    }

    /**
     * create Docker registry auth config
     */
    void withDockerRegistryAuth(String dockerRegistryAddress, String dockerRegistryCred, Closure body) {
        script.withCredentials([script.usernamePassword(
            credentialsId: dockerRegistryCred,
            usernameVariable: DOCKER_REGISTRY_CREDENTIALS_USER_VARIABLE,
            passwordVariable: DOCKER_REGISTRY_CREDENTIALS_PASSWORD_VARIABLE
        )]) {
            String authStringBase64 = "${script.env[DOCKER_REGISTRY_CREDENTIALS_USER_VARIABLE]}:${script.env[DOCKER_REGISTRY_CREDENTIALS_PASSWORD_VARIABLE]}".bytes.encodeBase64().toString()
            Map dockerRegistryAuthCfgRaw = [
                auths: [
                    (dockerRegistryAddress): [auth: authStringBase64]
                ]
            ]
            String dockerRegistryAuthCfg = JsonOutput.toJson(dockerRegistryAuthCfgRaw)
            String dockerRegistryAuthCfgPath = "${script.env.WORKSPACE}/.docker/config.json"
            script.writeFile file: dockerRegistryAuthCfgPath, text: dockerRegistryAuthCfg
            script.withEnv([
                "DOCKER_CONFIG=${script.env.WORKSPACE}/.docker",
            ]) {
                body.call()
            }
        }
    }

    /**
     * get latest tag from Docker registry
     */
    String getlatestDockerImageTagFromRegistry(String dockerImageName, String dockerImageSubPath, String version) {
        List dockerImageTags = []
        String latestImageTag = ''
        String skopeoResult = ''

        withDockerRegistryAuth(DOCKER_REGISTRY_ADDRESS, DOCKER_REGISTRY_CREDENTIALS_ID) {
            skopeoResult = script.sh(
                script: """
                    skopeo list-tags --authfile "\${DOCKER_CONFIG}/config.json" \
                        docker://${DOCKER_REGISTRY_ADDRESS}/${dockerImageSubPath}/${dockerImageName} | \
                    jq -r '.Tags[]' | \
                    grep -v cache | \
                    grep -v snapshot | \
                    grep ${version} | \
                    sort -n
                """,
                returnStdout: true
            ).trim()
        }

        dockerImageTags = skopeoResult.readLines().findAll { it }
        log.info("Overall image tags (matched current version): ${dockerImageTags}")

        if (dockerImageTags) {
            latestImageTag = dockerImageTags.sort().last()
            return latestImageTag
        } else {
            // empty result
            return latestImageTag
        }
    }

    /**
     * get new Docker image tag
     */
    String getNewDockerImageTag(String dockerImageName, String dockerImageSubPath, String baseVersion) {
        String newImageTag = ''
        String latestImageTag = getlatestDockerImageTagFromRegistry(dockerImageName, dockerImageSubPath, baseVersion)
        if (latestImageTag) {
            log.info("Image tag ${latestImageTag} already exists, calling increment function")
            newImageTag = incrementVersion(latestImageTag)
            return newImageTag
        } else {
            return baseVersion
        }
    }

    /**
     * validate version
     */
    private static Matcher validateVersion(String version) {
        // accepts versions: X.Y.Z, X.Y.Z-suffix, X.Y or X.Y-suffix
        Matcher matcher = version =~ VERSION_PATTERN
        if (!matcher.matches()) {
            throw new IllegalArgumentException('Invalid version format. Acceptable: X.Y.Z, X.Y.Z-suffix, X.Y or X.Y-suffix')
        }
        return matcher
    }

    /**
     * format version
     */
    private static String formatVersion(String version) {
        Matcher matcher = validateVersion(version)

        String major = matcher.group(VERSION_MAJOR_GROUP)
        String minor = matcher.group(VERSION_MINOR_GROUP)
        String patch = matcher.group(VERSION_PATCH_GROUP)
        String suffix = matcher.group(VERSION_SUFFIX_GROUP)

        if (patch) {
            return "${major}.${minor}.${patch}000" + (suffix ? "-${suffix}" : '')
        } else {
            return "${major}.${minor}000" + (suffix ? "-${suffix}" : '')
        }
    }

    /**
     * increment version by 1
     */
    private static String incrementVersion(String version) {
        Matcher matcher = validateVersion(version)

        int major = matcher.group(VERSION_MAJOR_GROUP) as int
        int minor = matcher.group(VERSION_MINOR_GROUP) as int
        String patchStr = matcher.group(VERSION_PATCH_GROUP)
        String suffix = matcher.group(VERSION_SUFFIX_GROUP)

        if (patchStr) {
            int patch = patchStr as int
            patch++
            String patchPadded = String.format("%04d", patch)
            return "${major}.${minor}.${patchPadded}" + (suffix ? "-${suffix}" : '')
        } else {
            minor++
            String minorPadded = String.format("%04d", minor)
            return "${major}.${minorPadded}" + (suffix ? "-${suffix}" : '')
        }
    }

    /**
     * lint Docker image
     */
    void lintDockerImage() {
        script.sh """
            hadolint --ignore=DL3033 --ignore=DL3041 ${dockerfilePath}
        """
    }

    /**
     * build Docker image
     */
    void buildDockerImage(String dockerImageName, String dockerImageSubPath, String version, boolean testRelease) {
        // validate and format version X.Y.Z000 or X.Y.Z000-suffix
        String baseVersion = formatVersion(version)
        log.info("Base version: ${baseVersion}")
        boolean isReleaseBranch = gitUtils.isReleaseBranch() || testRelease
        String dockerImageTag = isReleaseBranch
            ? getNewDockerImageTag(dockerImageName, dockerImageSubPath, baseVersion)
            : baseVersion + "-snapshot"
        String dockerImageFullPath = "${DOCKER_REGISTRY_ADDRESS}/${dockerImageSubPath}/${dockerImageName}:${dockerImageTag}"
        String buildkitCacheArgs = isReleaseBranch ?
            '' :
            "--export-cache type=registry,ref=${dockerImageFullPath}-${dockerBuildCacheSuffix},mode=max,image-manifest=true " +
            "--import-cache type=registry,ref=${dockerImageFullPath}-${dockerBuildCacheSuffix}"
        log.info("Image tag: ${dockerImageTag}")
        withDockerRegistryAuth(DOCKER_REGISTRY_ADDRESS, DOCKER_REGISTRY_CREDENTIALS_ID) {
            script.withEnv([
                "BUILDKIT_HOST=${BUILDKIT_HOST}"
            ]) {
                script.sh """
                    buildctl build \
                        --frontend dockerfile.v0 \
                        --local context=. \
                        --local dockerfile=. \
                        ${buildkitCacheArgs} \
                        --output type=image,name=${dockerImageFullPath},push=true
                """
            }
        }
    }

}
