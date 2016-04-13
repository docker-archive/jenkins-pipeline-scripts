#!groovy

node('master') {
  checkout scm
  sh "git push jenkins@localhost:workflowLibs.git '+refs/remotes/origin/*:refs/heads/*'"
}
