def call(credsName=null) {
  if (!credsName) {
    def path = scm.repositories[0].URIs[0].path
    def repoOwner = path.split('/')[0]
    def repoName = path.split('/')[-1].replaceAll(/\.git$/, '')
    credsName = repoName
    if (repoOwner != 'docker') {
      credsName = "${repoOwner}-${credsName}"
    }
  }
  withCredentials([[$class: 'StringBinding', credentialsId: "${credsName}.codecov-token", variable: 'CODECOV_TOKEN']]) {
    sh "bash <(curl -s https://codecov.io/bash) -t \$CODECOV_TOKEN"
  }
}
