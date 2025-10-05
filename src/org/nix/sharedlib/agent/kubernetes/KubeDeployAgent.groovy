package org.nix.sharedlib.agent.kubernetes

/**
 * Kubernetes deploy agent
 */
class KubeDeployAgent extends KubeAgent {

    KubeDeployAgent(Script script) {
        super(script)
    }

    @Override
    void nodeWrapper(String agentLabels, int timeout, Closure body) {
        script.podTemplate(
            cloud: CLOUD_NAME,
            containers: [
                jnlpContainerSpec,
                deployContainerSpec,
            ]
        ) {
            script.node(script.env.POD_LABEL) {
                script.timeout(time: timeout) {
                    script.ansiColor('xterm') {
                        containerWrapper(DEPLOY_CONTAINER_NAME) {
                            body.call()
                        }
                    }
                }
            }
        }
    }

}
