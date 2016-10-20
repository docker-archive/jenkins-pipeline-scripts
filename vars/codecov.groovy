import org.kohsuke.github.GitHub

def call(credsName=null) {
  if (!credsName) {
    def path = scm.repositories[0].URIs[0].path
    def repoOwner = path.split('/')[0]
    def repoName = path.split('/')[-1].replaceAll(/\.git$/, '')
    credsName = repoName
    if (repoOwner && repoOwner != 'docker') {
      credsName = "${repoOwner}-${credsName}"
    }
  }

  def branchName = env.BRANCH_NAME
  if (env.CHANGE_ID) {
    def repoUrl = sh script: "git config --get remote.origin.url", returnStdout: true
    // Need to get name from url, supports these variants:
    //  git@github.com:docker/docker.git -> docker/docker
    //  git://github.com/docker/docker.git -> docker/docker
    //  https://github.com/docker/docker.git -> docker/docker
    //  ssh://git@github.com/docker/docker.git -> docker/docker
    // 1. split on colon, take the last part.
    // 2. split that on slash, take the last 2 parts and rejoin them with /.
    // 3. remove .git at the end
    // 4. ta-da
    def repoName = repoUrl.split(":")[-1].split("/")[-2..-1].join("/").replaceAll(/\.git$/, '')
    def githubToken
    withCredentials([[
      variable: "GITHUB_TOKEN",
      credentialsId: "docker-ci-scanner.token-only.github.com",
      $class: "StringBinding",
    ]]) {
      githubToken = env.GITHUB_TOKEN
    }
    def gh = GitHub.connectUsingOAuth(githubToken)
    def pr = gh.getRepository(repoName).getPullRequest(env.CHANGE_ID.toInteger())
    branchName = "${pr.head.repo.owner.login}/${pr.head.ref}"
  }

  // Set some env variables so codecov detection script works correctly
  withEnv(["ghprbPullId=${env.CHANGE_ID}", "GIT_BRANCH=${branchName}"]) {
    withCredentials([[$class: 'StringBinding', credentialsId: "${credsName}.codecov-token", variable: 'CODECOV_TOKEN']]) {
      sh 'bash <(curl -s https://codecov.io/bash) || echo "codecov exited with \$?"'
    }
  }
}
