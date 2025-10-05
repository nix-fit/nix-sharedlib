package org.nix.sharedlib.helm

import org.nix.sharedlib.pipeline.AbstractPipeline

/**
 * Helm utils
 */
class HelmUtils extends AbstractPipeline {

    private final static Map HELM_KUBECONFIG_CREDENTIALS_IDS = [
        'dev': 'twc-dev-kubernetes-config',
        'prod': 'twc-prod-kubernetes-config',
    ]
    private final static String HELM_KUBECONFIG_CREDENTIALS_ID = 'twc-dev-kubernetes-config'
    private final static String HELM_KUBECONFIG_CREDENTIALS_VARIABLE = 'KUBECONFIG'

    HelmUtils(Script script) {
        super(script)
    }

    /**
     * install Helm release
     */
    void installHelmRelease(String chartRepoName, String environment, String namespace, String environmentAbsoluteRepoPath, String deployTimeout) {
        script.withCredentials([script.file(
            credentialsId: HELM_KUBECONFIG_CREDENTIALS_IDS[environment],
            variable: HELM_KUBECONFIG_CREDENTIALS_VARIABLE
        )]) {
            log.info('Showing diff')
            script.sh """
                helm diff upgrade --install ${chartRepoName} . \
                    --kubeconfig ${script.env[HELM_KUBECONFIG_CREDENTIALS_VARIABLE]} \
                    --namespace ${namespace} \
                    --values ${environmentAbsoluteRepoPath}/environment/${environment}/main.yaml \
                    --values ${environmentAbsoluteRepoPath}/environment/${environment}/${chartRepoName}/main.yaml \
                    --color \
                    --suppress-secrets \
                    --three-way-merge
            """
            log.info("Installing release: ${chartRepoName}")
            script.sh """
                helm upgrade --install ${chartRepoName} . \
                    --kubeconfig ${script.env[HELM_KUBECONFIG_CREDENTIALS_VARIABLE]} \
                    --namespace ${namespace} \
                    --values ${environmentAbsoluteRepoPath}/environment/${environment}/main.yaml \
                    --values ${environmentAbsoluteRepoPath}/environment/${environment}/${chartRepoName}/main.yaml \
                    --atomic \
                    --timeout ${deployTimeout}
            """
        }
    }

}
