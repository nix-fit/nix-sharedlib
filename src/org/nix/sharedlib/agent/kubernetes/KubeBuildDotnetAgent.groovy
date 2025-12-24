package org.nix.sharedlib.agent.kubernetes

/**
 * Kubernetes build Dotnet agent
 */
class KubeBuildDotnetAgent extends KubeAgent {

    KubeBuildDotnetAgent(Script script) {
        super(script)
    }

    @Override
    void nodeWrapper(int timeout, Map args = [:], Closure body) {
        script.podTemplate(
            cloud: CLOUD_NAME,
            containers: [
                jnlpContainerSpec,
                getBuildDotnetContainerSpec(args),
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
