package org.nix.sharedlib.agent

import org.nix.sharedlib.agent.kubernetes.KubeBuildAgent

/**
 * Build agent factory
 */
class BuildAgentFactory {

    /**
     * get build agent
     */ 
    static AgentRunner getAgent(Script script) {
        return new KubeBuildAgent(script)
    }

}
