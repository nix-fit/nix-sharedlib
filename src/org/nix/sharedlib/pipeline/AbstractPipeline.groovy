package org.nix.sharedlib.pipeline

import org.nix.sharedlib.logging.LogUtils

/**
 * Abstract pipeline
 */
class AbstractPipeline {

    protected final static String SOME_ENV = ''

    protected Script script
    protected String errorMessage = ''
    protected String failedStage = ''

    protected LogUtils log

    AbstractPipeline(Script script) {
        this.script = script
        this.log = new LogUtils(script, LogUtils.LEVEL_INFO)
    }

    /**
     * fail current build
     */ 
    void failCurrentBuild(String errorMessage) {
        script.currentBuild.result = 'FAILURE'
        this.errorMessage = errorMessage
    }

    /**
     * stage
     */ 
    void stage(String name, Closure body) {
        script.stage(name) {
            failedStage = name
            body.call()
        }
    }

    /**
     * get absolute dir path
     */ 
    String getAbsoluteDirPath(String dirName) {
        return "${script.env.WORKSPACE}/${dirName}"
    }

}
