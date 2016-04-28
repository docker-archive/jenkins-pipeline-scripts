def call(args=null, Closure body=null) {
  args = args ?: [:]

  def githubCredentials = args.get("github_credentials", "docker-jenkins.token.github.com")
  def packageName = args.get("package", null)
  def label = args.get("label", "docker")
  def goVersion = args.get("go_version", null)
  def gocycloMax = args.get('gocyclo_max', null)
  def testerTag = args.get("golang_tag", goVersion) ?: "gimme"
  def envVars = args.get("env_vars", [])
  def gocovArgs = args.get("gocov_args", "")
  def maxWarnings = args.get("max_warnings", 0)

  if (!packageName) {
    throw new Exception("Please specify a 'package': `golangTester(package: 'github.com/docker/my-proj')`")
  }

  if (!goVersion) {
    echo "INFO: using latest version of golang in 'gimme' image. To change this, specify a go_version: `golangTester(go_version: '1.6.1')`"
  }

  return {
    wrappedNode(label: label) {
      deleteDir()
      checkout scm
      def image = docker.image("dockerautomation/golang-tester:${testerTag}")
      def testsPassed
      image.pull()

      withCredentials([[
        variable: "GITHUB_TOKEN",
        credentialsId: githubCredentials,
        $class: "StringBinding",
      ]]) {
        sh 'echo "machine github.com login $GITHUB_TOKEN" > net-rc'
      }

      withChownWorkspace {
        withEnv([
          "GOVERSION=${goVersion ?: ''}",
          "GOCYCLO_MAX=${gocycloMax ?: ''}",
          "GOPACKAGE=${packageName}",
          "GOCOV_ARGS=${gocovArgs}"
        ] + envVars) {
          def cmd = """#!/bin/bash -x
            docker run \\
            --rm \\
            -i \\
            -v "\$(pwd)/net-rc:/root/.netrc" \\
            -v "\$(pwd):/go/src/\$GOPACKAGE" \\
            -v "\$(pwd)/results:/output" \\
            -e "GOVERSION=\$GOVERSION" \\
            -e "GOCYCLO_MAX" \\
            -e "GOPACKAGE" \\
            -e "GOCOV_ARGS" \\
          """
          for (int i = 0; i < envVars.size(); i++) {
            cmd += "-e \"${envVars.get(i)}\" \\\n"
          }
          cmd += image.id
          try {
            sh(cmd)
            testsPassed = true
          } catch (Exception exc) {
            testsPassed = false
          }
        }
      }

      if (readFile('results/tests.xml').size() != 0) {
        step([$class: 'JUnitResultArchiver', testResults: 'results/tests.xml', keepLongStdio: true])
      }
      /* Cobertura publisher not yet supported in Pipeline */
      // if (readFile('results/coverage.xml').size() != 0) {
      //   step([$class: 'CoberturaPublisher', coberturaReportFile: 'results/coverage.xml'])
      // }
      step([
        $class: 'WarningsPublisher',
        parserConfigurations: [[
          parserName: "Go Lint",
          pattern: "results/fmt.txt,results/lint.txt,results/cyclo.txt",
        ], [
          parserName: "Go Vet",
          pattern: "results/vet.txt"
        ]],
        unstableTotalAll: "${maxWarnings}"
      ])
      archive 'results'
      if (body) { body() }
      if (!testsPassed && currentBuild.result && currentBuild.result == 'SUCCESS') {
        currentBuild.result = 'UNSTABLE'
      }
    }
  }
}
