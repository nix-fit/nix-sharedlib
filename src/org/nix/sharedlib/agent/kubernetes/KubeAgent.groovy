package org.nix.sharedlib.agent.kubernetes

import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate

import org.nix.sharedlib.agent.AgentRunner
import org.nix.sharedlib.pipeline.AbstractPipeline

/**
 * Kubernetes agent
 */
class KubeAgent extends AbstractPipeline implements AgentRunner {
    
    public final static String DEPLOY_CONTAINER_NAME = 'deploy'
    public final static String BUILD_CONTAINER_NAME = 'build'
    public final static String JNLP_CONTAINER_NAME = 'jnlp'

    public final static String DEPLOY_CONTAINER_CPU_REQUEST = '250m'
    public final static String DEPLOY_CONTAINER_CPU_LIMIT = '750m'
    public final static String DEPLOY_CONTAINER_MEMORY_REQUEST = '256Mi'
    public final static String DEPLOY_CONTAINER_MEMORY_LIMIT = '768Mi'

    public final static String BUILD_CONTAINER_CPU_REQUEST = '250m'
    public final static String BUILD_CONTAINER_CPU_LIMIT = '750m'
    public final static String BUILD_CONTAINER_MEMORY_REQUEST = '256Mi'
    public final static String BUILD_CONTAINER_MEMORY_LIMIT = '768Mi'

    public final static String JNLP_CONTAINER_CPU_REQUEST = '250m'
    public final static String JNLP_CONTAINER_CPU_LIMIT = '250'
    public final static String JNLP_CONTAINER_MEMORY_REQUEST = '256Mi'
    public final static String JNLP_CONTAINER_MEMORY_LIMIT = '256Mi'

    public final static String CONTAINER_CPU_REQUEST_ARG_NAME = 'cpuRequest'
    public final static String CONTAINER_CPU_LIMIT_ARG_NAME = 'cpuLimit'
    public final static String CONTAINER_MEMORY_REQUEST_ARG_NAME = 'memRequest'
    public final static String CONTAINER_MEMORY_LIMIT_ARG_NAME = 'memLimit'

    protected final static String DEPLOY_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/deploy/common-deploy:1.0.0000@sha256:45389a3093a2d26bea81a5320ec8a01e021c26f381fef610d822eb43dc9e5f26'
    protected final static String BUILD_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/build/common-build:1.0.0000@sha256:d16f74f1c0c7d968960285a069a6b3aa326d44a73ebe6c23f0be6ce891bd5939'
    protected final static String JNLP_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/jenkins/inbound-agent:3341.v0766d82b_dec0-1-jdk21@sha256:765a29591c3c85b062e124304bf0ca96e147c8539b8c3fca5f7a2bd4986cb21c'

    protected final static String CLOUD_NAME = 'kubernetes'

    protected final static String DEFAULT_CONTAINER_USER = '1000'
    protected final static String DEFAULT_CONTAINER_GROUP = '1000'

    protected final static String DEFAULT_CONTAINER_ENTRYPOINT = 'cat'

    KubeAgent(Script script) {
        super(script)
    }

    @Override
    void clearWorkspace() {
    }

    @Override
    void containerWrapper(String name, Closure body) {
        script.container(name) {
            body.call()
        }
    }

    /**
     * get deploy container spec
     */
    ContainerTemplate getDeployContainerSpec() {
        return script.containerTemplate(
            args: '',
            alwaysPullImage: false,
            command: DEFAULT_CONTAINER_ENTRYPOINT,
            envVars: [],
            image: DEPLOY_CONTAINER_IMAGE_NAME,
            name: DEPLOY_CONTAINER_NAME,
            resourceRequestCpu: DEPLOY_CONTAINER_CPU_REQUEST,
            resourceLimitCpu: DEPLOY_CONTAINER_CPU_LIMIT,
            resourceRequestMemory: DEPLOY_CONTAINER_MEMORY_REQUEST,
            resourceLimitMemory: DEPLOY_CONTAINER_MEMORY_LIMIT,
            runAsUser: DEFAULT_CONTAINER_USER,
            runAsGroup: DEFAULT_CONTAINER_GROUP,
            ttyEnabled: true,
        )
    }

    /**
     * get jnlp container spec
     */
    ContainerTemplate getJnlpContainerSpec() {
        return script.containerTemplate(
            args: '',
            alwaysPullImage: false,
            command: '',
            envVars: [],
            image: JNLP_CONTAINER_IMAGE_NAME,
            name: JNLP_CONTAINER_NAME,
            resourceRequestCpu: JNLP_CONTAINER_CPU_REQUEST,
            resourceLimitCpu: JNLP_CONTAINER_CPU_LIMIT,
            resourceRequestMemory: JNLP_CONTAINER_MEMORY_REQUEST,
            resourceLimitMemory: JNLP_CONTAINER_MEMORY_LIMIT,
            runAsUser: DEFAULT_CONTAINER_USER,
            runAsGroup: DEFAULT_CONTAINER_GROUP,
            ttyEnabled: true,
        )
    }

    /**
     * get build container spec
     */
    ContainerTemplate getBuildContainerSpec() {
        return script.containerTemplate(
            args: '',
            alwaysPullImage: false,
            command: DEFAULT_CONTAINER_ENTRYPOINT,
            envVars: [],
            image: BUILD_CONTAINER_IMAGE_NAME,
            name: BUILD_CONTAINER_NAME,
            resourceRequestCpu: BUILD_CONTAINER_CPU_REQUEST,
            resourceLimitCpu: BUILD_CONTAINER_CPU_LIMIT,
            resourceRequestMemory: BUILD_CONTAINER_MEMORY_REQUEST,
            resourceLimitMemory: BUILD_CONTAINER_MEMORY_LIMIT,
            runAsUser: DEFAULT_CONTAINER_USER,
            runAsGroup: DEFAULT_CONTAINER_GROUP,
            ttyEnabled: true,
        )
    }

    @Override
    void nodeWrapper(String agentLabels, int timeout, Closure body) {
    }

}
