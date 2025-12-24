package org.nix.sharedlib.agent.kubernetes

/**
 * Kubernetes build agent
 */
class KubeBuildAgent extends KubeAgent {

    KubeBuildAgent(Script script) {
        super(script)
    }

    @Override
    void nodeWrapper(int timeout, Map args = [:], Closure body) {
        script.podTemplate(
            cloud: CLOUD_NAME,
            yaml: getBuildContainerSecuritySpec(true),
            containers: [
                buildContainerSpec,
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

}
