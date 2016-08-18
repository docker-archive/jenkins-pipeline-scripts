

// Add the following code to any Jenkinsfile.
// It will return with success if there are no changes in the specified docs dir
// and Will return with success if run on a non Pull Request branch

// documentationChecker("docs")

// docsDir ~~ the subdirectory within your repo that the documentation resides
def call(String docsDir) {
  stage "docs PR checker"

  tokens = "${env.JOB_NAME}".tokenize('/')
  org = tokens[0]
  repo = tokens[1]
  branch = tokens[2]
  sha = gitCommit()
  imageName = "${repo}/${branch}:${env.BUILD_ID}"
  imageName = imageName.toLowerCase()
  containerName = "${repo}-${branch}-${env.BUILD_ID}"
  containerName = containerName.toLowerCase()

  changes = getOutput("git log origin/master..${sha} ${docsDir}").trim()
  if (changes.size() == 0) {
    echo "no changes found in ${docsDir}"
    return
  }

  try {
    echo changes
    if (env.CHANGE_ID) {
      slackSend channel: '#docs-automation', message: "Starting docs test of - <${env.CHANGE_URL}|${repo} PR#${env.CHANGE_ID}> : ${env.CHANGE_TITLE}- see <${env.BUILD_URL}/console|the Jenkins console for job ${env.BUILD_ID}>"
    } else {
      echo "Skipping slack start message; no CHANGE_ID"
    }
    sh "docker pull docs/base:oss"
    try {
      sh "docker build -t ${imageName} ${docsDir}"
      try {
        sh "docker run --name=${containerName} ${imageName}"

        // TODO: summarize the changes & errors (these are files used by GHPRB and the summary plugin
        sh "docker cp ${containerName}:/validate.junit.xml ."
        sh "docker cp ${containerName}:/docs/markdownlint.summary.txt ."

        //setGitHubPullRequestStatus message: "docs test complete"
      } finally {
        sh "docker rm -f ${containerName}"
      }
    } finally {
      sh "docker rmi -f ${imageName}"
    }
  } catch (err) {
    if (env.CHANGE_ID) {
      slackSend channel: '#docs-automation', message: "BUILD FAILURE: @${env.CHANGE_AUTHOR} - <${env.CHANGE_URL}|${repo} PR#${env.CHANGE_ID}> : ${env.CHANGE_TITLE}- see <${env.BUILD_URL}/console|the Jenkins console for job ${env.BUILD_ID}>"
    } else {
      echo "Skipping slack error message; no CHANGE_ID"
    }
  }
}
