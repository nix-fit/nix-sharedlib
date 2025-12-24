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

    private final static String DEFAULT_PLATFORMS = 'linux/amd64,linux/arm64'
    private final static String CACHE_KEYWORD = 'cache'
    private final static String FORMAT_PATTERN = '%04d'

    protected String dockerfilePath = 'Dockerfile'

    protected String dockerBuildCacheSuffix = CACHE_KEYWORD

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
            String authString =
                "${script.env[DOCKER_REGISTRY_CREDENTIALS_USER_VARIABLE]}:" +
                "${script.env[DOCKER_REGISTRY_CREDENTIALS_PASSWORD_VARIABLE]}"
            String authStringBase64 = authString.bytes.encodeBase64()
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
        String versionRegex = buildVersionMatchRegex(version)

        withDockerRegistryAuth(DOCKER_REGISTRY_ADDRESS, DOCKER_REGISTRY_CREDENTIALS_ID) {
            skopeoResult = script.sh(
                script: """
                    skopeo list-tags --authfile "\${DOCKER_CONFIG}/config.json" \
                        docker://${DOCKER_REGISTRY_ADDRESS}/${dockerImageSubPath}/${dockerImageName} | \
                    jq -r '.Tags[]'
                """,
                returnStdout: true
            ).trim()
        }

        dockerImageTags = skopeoResult.readLines()
            .findAll { String tag -> tag }
            .findAll { String tag -> !tag.contains(CACHE_KEYWORD) && !tag.contains('snapshot') }
            .findAll { String tag -> tag ==~ versionRegex }
        log.info("Overall image tags (matched current version): ${dockerImageTags}")

        if (dockerImageTags) {
            latestImageTag = dockerImageTags.sort().last()
            return latestImageTag
        }
        return latestImageTag
    }

    /**
     * get new Docker image tag
     */
    String getNewDockerImageTag(String dockerImageName, String dockerImageSubPath, String version, String baseVersion) {
        String newImageTag = ''
        String latestImageTag = getlatestDockerImageTagFromRegistry(dockerImageName, dockerImageSubPath, version)
        if (latestImageTag) {
            log.info("Image tag ${latestImageTag} already exists, calling increment function")
            newImageTag = incrementVersion(latestImageTag)
            return newImageTag
        }
        return baseVersion
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
    void buildDockerImage(String dockerImageName, String dockerImageSubPath,
            String version, boolean testRelease, Map opts = [:]) {
        // validate and format version
        String baseVersion = formatVersion(version)
        log.info("Base version: ${baseVersion}")
        /* groovylint-disable-next-line UnnecessaryGetter */
        boolean isRelease = gitUtils.isReleaseBranch || testRelease
        String dockerImageTag = isRelease
            ? getNewDockerImageTag(dockerImageName, dockerImageSubPath, version, baseVersion)
            : baseVersion + '-snapshot'
        String dockerImageFullPath =
            "${DOCKER_REGISTRY_ADDRESS}/${dockerImageSubPath}/${dockerImageName}:${dockerImageTag}"
        String buildkitCacheArgs = isRelease ?
            '' :
            "--export-cache type=registry,ref=${dockerImageFullPath}" +
            "-${dockerBuildCacheSuffix},mode=max,image-manifest=true " +
            "--import-cache type=registry,ref=${dockerImageFullPath}" +
            "-${dockerBuildCacheSuffix}"
        String platforms = opts.get('platforms', DEFAULT_PLATFORMS)
        String secretsArgs = opts.secrets?.collect { String s -> "--secret ${s}" }?.join(' ') ?: ''
        /* groovylint-disable-next-line DuplicateStringLiteral */
        String buildArgs = opts.buildArgs?.collect { String s -> "--opt build-arg:${s}" }?.join(' ') ?: ''
        log.info("Image tag: ${dockerImageTag}")
        withDockerRegistryAuth(DOCKER_REGISTRY_ADDRESS, DOCKER_REGISTRY_CREDENTIALS_ID) {
            script.sh """
                buildctl build \
                    --frontend dockerfile.v0 \
                    --local context=. \
                    --local dockerfile=. \
                    --opt platform=${platforms} \
                    ${buildkitCacheArgs} \
                    ${secretsArgs} \
                    ${buildArgs} \
                    --output type=image,name=${dockerImageFullPath},push=true
            """
        }
    }

    /**
     * validate version
     */
    private static Matcher validateVersion(String version) {
        // accepts versions: X.Y.Z, X.Y.Z-suffix, X.Y or X.Y-suffix
        Matcher matcher = version =~ VERSION_PATTERN
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                'Invalid version format. Acceptable: X.Y.Z, X.Y.Z-suffix, X.Y or X.Y-suffix'
            )
        }
        return matcher
    }

    /**
     * build regex matching all tags for a version
     */
    private static String buildVersionMatchRegex(String version) {
        Matcher matcher = validateVersion(version)

        String major = matcher.group(VERSION_MAJOR_GROUP)
        String minor = matcher.group(VERSION_MINOR_GROUP)
        String patch = matcher.group(VERSION_PATCH_GROUP)
        String suffix = matcher.group(VERSION_SUFFIX_GROUP)

        String suffixRegex = suffix ? "-${Pattern.quote(suffix)}" : ''
        return patch
            ? "${major}\\.${minor}\\.${patch}\\d{3}${suffixRegex}"
            : "${major}\\.${minor}\\d{3}${suffixRegex}"
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
        }
        return "${major}.${minor}000" + (suffix ? "-${suffix}" : '')
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
            String patchPadded = String.format(FORMAT_PATTERN, patch)
            return "${major}.${minor}.${patchPadded}" + (suffix ? "-${suffix}" : '')
        }
        minor++
        String minorPadded = String.format(FORMAT_PATTERN, minor)
        return "${major}.${minorPadded}" + (suffix ? "-${suffix}" : '')
    }

}
