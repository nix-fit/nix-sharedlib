package org.nix.sharedlib.agent

import org.nix.sharedlib.agent.kubernetes.KubeDeployAgent

/**
 * Deploy agent factory
 */
class DeployAgentFactory {

    /**
     * get deploy agent
     */
    static AgentRunner getAgent(Script script) {
        return new KubeDeployAgent(script)
    }

}
