package org.nix.sharedlib.agent.kubernetes

import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate

import org.yaml.snakeyaml.Yaml

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
    public final static String BUILD_CONTAINER_CPU_LIMIT = '1'
    public final static String BUILD_CONTAINER_MEMORY_REQUEST = '256Mi'
    public final static String BUILD_CONTAINER_MEMORY_LIMIT = '1Gi'

    public final static String JNLP_CONTAINER_CPU_REQUEST = '250m'
    public final static String JNLP_CONTAINER_CPU_LIMIT = '250'
    public final static String JNLP_CONTAINER_MEMORY_REQUEST = '256Mi'
    public final static String JNLP_CONTAINER_MEMORY_LIMIT = '256Mi'

    public final static String CONTAINER_CPU_REQUEST_ARG_NAME = 'cpuRequest'
    public final static String CONTAINER_CPU_LIMIT_ARG_NAME = 'cpuLimit'
    public final static String CONTAINER_MEMORY_REQUEST_ARG_NAME = 'memRequest'
    public final static String CONTAINER_MEMORY_LIMIT_ARG_NAME = 'memLimit'

    protected final static String DEPLOY_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/deploy/common-deploy:1.3.0000' +
            '@sha256:a5fd6674d4a133c8f1281e13e3194d023f3ed71f6e69ff4c1aecbe18c072e74b'
    protected final static String BUILD_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/build/common-build:1.3.0000' +
            '@sha256:d3e75572ed03fb58cc99332682598171b207d4752af1ce568a6d12b0adb8b24d'
    protected final static String JNLP_CONTAINER_IMAGE_NAME =
            'nix-docker.registry.twcstorage.ru/ci/jenkins/inbound-agent:' +
            '3341.v0766d82b_dec0-1-jdk21' +
            '@sha256:765a29591c3c85b062e124304bf0ca96e147c8539b8c3fca5f7a2bd4986cb21c'

    protected final static String TMP_VOLUME_NAME = 'tmp'
    protected final static String XDG_VOLUME_NAME = 'xdg'
    protected final static String SHARE_VOLUME_NAME = 'share'

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
            args: getBuildkitArgs(true),
            alwaysPullImage: false,
            command: getBuildkitEntrypoint(true),
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

    /**
     * get buildkit entrypoint
     */
    String getBuildkitEntrypoint(boolean useBuildkit) {
        return useBuildkit ?
            'rootlesskit ' +
            '--state-dir=/home/jenkins/agent/.rootlesskit ' +
            '--subid-source=static ' +
            'buildkitd' :
            DEFAULT_CONTAINER_ENTRYPOINT
    }

    /**
     * get buildkit args
     */
    String getBuildkitArgs(boolean useBuildkit) {
        return useBuildkit ?
            '--oci-worker=true ' +
            '--oci-worker-no-process-sandbox ' +
            '--oci-worker-binary=crun ' +
            '--containerd-worker=false ' +
            '--root=/home/jenkins/agent/buildkit ' +
            '--addr=unix:///home/jenkins/agent/buildkit/buildkitd.sock ' +
            '--otel-socket-path=/home/jenkins/agent/buildkit/otel-grpc.sock' :
            ''
    }

    /**
     * get build container security spec
     */
    /* groovylint-disable DuplicateMapLiteral, DuplicateListLiteral, DuplicateStringLiteral */
    String getBuildContainerSecuritySpec(boolean useBuildkit) {
        Map defaultSecurityContext = [
            allowPrivilegeEscalation: false,
            runAsNonRoot: true,
            readOnlyRootFilesystem: true,
            capabilities: [
                drop: ['ALL']
            ],
            seccompProfile: [type: 'RuntimeDefault'],
        ]
        Map buildkitSecurityContext = [
            allowPrivilegeEscalation: true,
            runAsNonRoot: true,
            readOnlyRootFilesystem: true,
            seccompProfile: [type: 'Unconfined'],
            appArmorProfile: [type: 'Unconfined'],
            capabilities: [
                drop: ['ALL'],
                add: ['SETUID', 'SETGID']
            ]
        ]
        Map jnlpSecurityContext = [
            allowPrivilegeEscalation: false,
            runAsNonRoot: true,
            readOnlyRootFilesystem: true,
            capabilities: [
                drop: ['ALL']
            ],
            seccompProfile: [type: 'RuntimeDefault'],
        ]
        Map podSpec = [
            spec: [
                containers: [
                    [
                        name: BUILD_CONTAINER_NAME,
                        securityContext: useBuildkit ? buildkitSecurityContext : defaultSecurityContext,
                        volumeMounts: [
                            [name: TMP_VOLUME_NAME, mountPath: '/tmp'],
                            [name: XDG_VOLUME_NAME, mountPath: '/home/jenkins/agent/.local/xdg'],
                            [name: SHARE_VOLUME_NAME, mountPath: '/home/jenkins/agent/.local/share']
                        ]
                    ],
                    [
                        name: JNLP_CONTAINER_NAME,
                        securityContext: jnlpSecurityContext
                    ]
                ],
                volumes: [
                    [name: TMP_VOLUME_NAME, emptyDir: [medium: '']],
                    [name: XDG_VOLUME_NAME, emptyDir: [medium: '']],
                    [name: SHARE_VOLUME_NAME, emptyDir: [medium: '']]
                ]
            ]
        ]
        return new Yaml().dump(podSpec)
    }
    /* groovylint-enable DuplicateMapLiteral, DuplicateListLiteral, DuplicateStringLiteral */

    @Override
    void nodeWrapper(int timeout, Map args = [:], Closure body) {
    }

}
