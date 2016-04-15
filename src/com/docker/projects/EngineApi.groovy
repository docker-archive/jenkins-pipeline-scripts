package com.docker.projects;

import groovy.transform.Field

@Field
def gocycloMax = 0

@Field
def goPackage = "github.com/docker/engine-api"

def testJob(Map options) {
  def platform = options.get("platform", "linux")
  def label = options.get("label", "docker")
  def go_version = options.get("go_version", "1.5.3")

  return {
    wrappedNode(label: label) {
      deleteDir()
      if (platform == "windows") { tool 'hg' }
      checkout scm
      withEnv([
        "GOVERSION=${go_version}",
        "GOCYCLO_MAX=${this.gocycloMax}",
        "GOPACKAGE=${this.goPackage}"
      ]) {
        withCredentials([[$class: 'StringBinding', credentialsId: 'docker-jenkins.token.github.com', variable: 'GITHUB_TOKEN']]) {
          sh "hack/test-${platform}.sh"
        }
      }
      step([$class: 'JUnitResultArchiver', testResults: 'results/tests.xml', keepLongStdio: true])
      // step([$class: 'hudson.plugins.cobertura.CoberturaPublisher', coberturaReportFile: 'results/coverage.xml'])
      step([
        $class: 'WarningsPublisher',
        parserConfigurations: [[
          parserName: "Go Lint",
          pattern: "results/fmt.txt,results/lint.txt,results/cyclo.txt",
        ], [
          parserName: "Go Vet",
          pattern: "results/vet.txt"
        ]],
        unstableTotalAll: '0'
      ])
      archive 'results'
    }
  }
}
