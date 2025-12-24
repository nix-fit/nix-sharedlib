package org.nix.sharedlib.agent

import org.nix.sharedlib.agent.kubernetes.KubeBuildAgent
import org.nix.sharedlib.agent.kubernetes.KubeBuildDotnetAgent

/**
 * Build agent factory
 */
class BuildAgentFactory {

    /**
     * get build agent
     */
    static AgentRunner getBuildAgent(Script script) {
        return new KubeBuildAgent(script)
    }

    /**
     * get build Dotnet agent
     */
    static AgentRunner getBuildDotnetAgent(Script script) {
        return new KubeBuildDotnetAgent(script)
    }

}
