package org.nix.sharedlib.agent.kubernetes

import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate

/**
 * Kubernetes build .Net agent
 */
class KubeBuildDotnetAgent extends KubeAgent {

    protected final static String BUILD_DOTNET_AGENT_IMAGE_VERSION_9 = '9'

    KubeBuildDotnetAgent(Script script) {
        super(script)
    }

    @Override
    void nodeWrapper(int timeout, Map args = [:], Closure body) {
        boolean useBuildkit = args.get('useBuildkit', false)
        script.podTemplate(
            cloud: CLOUD_NAME,
            yaml: getBuildContainerSecuritySpec(useBuildkit),
            containers: [
                getBuildDotnetContainerSpec(args, useBuildkit),
                jnlpContainerSpec,
            ]
        ) {
            script.node(script.env.POD_LABEL) {
                script.timeout(time: timeout) {
                    script.ansiColor('xterm') {
                        containerWrapper(BUILD_CONTAINER_NAME) {
                            body.call()
                        }
                    }
                }
            }
        }
    }

    /**
     * get build .Net container spec
     */
    ContainerTemplate getBuildDotnetContainerSpec(Map args = [:], boolean useBuildkit = false) {
        String dotnetVersion = args.get('dotnetVersion', BUILD_DOTNET_AGENT_IMAGE_VERSION_9)
        String image = getBuildDotnetAgentImage(dotnetVersion)
        return script.containerTemplate(
            args: getBuildkitArgs(useBuildkit),
            alwaysPullImage: false,
            command: getBuildkitEntrypoint(useBuildkit),
            envVars: [],
            image: image,
            name: BUILD_CONTAINER_NAME,
            resourceRequestCpu: args.get(CONTAINER_CPU_REQUEST_ARG_NAME, BUILD_CONTAINER_CPU_REQUEST),
            resourceLimitCpu: args.get(CONTAINER_CPU_LIMIT_ARG_NAME, BUILD_CONTAINER_CPU_LIMIT),
            resourceRequestMemory: args.get(CONTAINER_MEMORY_REQUEST_ARG_NAME, BUILD_CONTAINER_MEMORY_REQUEST),
            resourceLimitMemory: args.get(CONTAINER_MEMORY_LIMIT_ARG_NAME, BUILD_CONTAINER_MEMORY_LIMIT),
            runAsUser: DEFAULT_CONTAINER_USER,
            runAsGroup: DEFAULT_CONTAINER_GROUP,
            ttyEnabled: true,
        )
    }

    /**
     * get .Net agent image
     */
    String getBuildDotnetAgentImage(String dotnetVersion) {
        switch (dotnetVersion) {
            case '9':
                return 'nix-docker.registry.twcstorage.ru/ci/build/dotnet-build:9.0002' +
                    '@sha256:adb707390c56a55ccdc7916fbd8b357b70e065883dda0db34dc81ec9c4b0b732'
            default:
                throw new IllegalArgumentException("Unsupported .Net: ${dotnetVersion}")
        }
    }

}
