package org.nix.sharedlib.helm

import org.nix.sharedlib.pipeline.AbstractPipeline
import org.nix.sharedlib.docker.DockerUtils

import groovy.json.JsonOutput

/**
 * Helm utils
 */
class HelmUtils extends AbstractPipeline {

    private final static Map HELM_CREDENTIALS_IDS = [
        'dev': [
            'kubeconfig': 'twc_dev_kubernetes_config',
            'sops': 'sops_dev_age_key'
        ],
        'prod': [
            'kubeconfig': 'twc_prod_kubernetes_config',
            'sops': 'sops_prod_age_key'
        ]
    ]
    private final static String HELM_KUBECONFIG_CREDENTIALS_VARIABLE = 'KUBECONFIG'
    private final static String HELM_SOPS_CREDENTIALS_VARIABLE = 'SOPS_AGE_KEY_FILE'

    protected DockerUtils dockerUtils

    HelmUtils(Script script) {
        super(script)
        dockerUtils = new DockerUtils(script)
    }

    /**
     * install Helm release
     */
    void installHelmRelease(String chartRepoName, String environment, String namespace, String environmentAbsoluteRepoPath, String deployTimeout) {
        script.withCredentials([
            script.file(
                credentialsId: HELM_CREDENTIALS_IDS[environment]['kubeconfig'],
                variable: HELM_KUBECONFIG_CREDENTIALS_VARIABLE
            ),
            script.file(
                credentialsId: HELM_CREDENTIALS_IDS[environment]['sops'],
                variable: HELM_SOPS_CREDENTIALS_VARIABLE
            )
        ]) {
            String helmValuesArgs = "--values ${environmentAbsoluteRepoPath}/environment/${environment}/main.yaml"
            String helmAppValuesFile = "${environmentAbsoluteRepoPath}/environment/${environment}/${chartRepoName}/main.yaml"
            if (script.fileExists(helmAppValuesFile)) {
                log.info("Detected ${chartRepoName}/main.yaml in Kubernetes environment repo. Adding it to helmValuesArgs")
                helmValuesArgs += " --values ${helmAppValuesFile}"
            }
            String helmSecretsValuesFile = "${environmentAbsoluteRepoPath}/environment/${environment}/${chartRepoName}/secrets.yaml"
            if (script.fileExists(helmSecretsValuesFile)) {
                log.info('Detected secrets.yaml in Kubernetes environment repo. Adding it to helmValuesArgs')
                helmValuesArgs += " --values ${helmSecretsValuesFile}"
            }
            log.info('Update dependencies')
            dockerUtils.withDockerRegistryAuth(DockerUtils.DOCKER_REGISTRY_ADDRESS, DockerUtils.DOCKER_REGISTRY_CREDENTIALS_ID) {
                script.sh 'helm dependency update'
            }
            log.info('Showing diff')
            script.sh """
                export ${HELM_SOPS_CREDENTIALS_VARIABLE}=${script.env[HELM_SOPS_CREDENTIALS_VARIABLE]}
                helm secrets diff upgrade --install ${chartRepoName} . \
                    --kubeconfig ${script.env[HELM_KUBECONFIG_CREDENTIALS_VARIABLE]} \
                    --namespace ${namespace} \
                    ${helmValuesArgs} \
                    --color \
                    --suppress-secrets \
                    --three-way-merge
            """
            log.info("Installing release: ${chartRepoName}")
            script.sh """
                export ${HELM_SOPS_CREDENTIALS_VARIABLE}=${script.env[HELM_SOPS_CREDENTIALS_VARIABLE]}
                helm secrets upgrade --install ${chartRepoName} . \
                    --kubeconfig ${script.env[HELM_KUBECONFIG_CREDENTIALS_VARIABLE]} \
                    --namespace ${namespace} \
                    ${helmValuesArgs} \
                    --atomic \
                    --timeout ${deployTimeout}
            """
        }
    }

}
