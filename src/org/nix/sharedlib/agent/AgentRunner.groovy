package org.nix.sharedlib.agent

/**
 * Agent interface
 */
interface AgentRunner {

    /**
     * clear workspace before/after job execution
     */ 
    void clearWorkspace()

    /**
     * wrapper for container
     */ 
    void containerWrapper(String name, Closure body)

    /**
     * wrapper for node
     */ 
    void nodeWrapper(String agentLabels, int timeout, Closure body)

}
