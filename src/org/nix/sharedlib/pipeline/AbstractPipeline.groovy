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

    /**
     * parse Organization Folder repo url (Multibranch Pipeline)
     */ 
    List parseRepoUrl(String repoUrl) {
        String fullRepoName = repoUrl
            .tokenize('/')[-1]
            .replace('.git', '')

        List fullRepoNameAttrs = fullRepoName.split('-', 2)

        String repoType = fullRepoNameAttrs.size() > 1 ? fullRepoNameAttrs[0] : ''
        String repoName = fullRepoNameAttrs.size() > 1 ? fullRepoNameAttrs[1] : fullRepoNameAttrs[0]

        return [repoType, repoName]
    }

}
