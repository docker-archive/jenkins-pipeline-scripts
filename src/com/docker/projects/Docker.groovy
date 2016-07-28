package com.docker.projects;

import groovy.transform.Field

@Field
def versionString = null

@Field
def imageId = null

def makeTask(nodeType, taskNames, doStash, depends, extraEnv, Closure body=null) {
  return {
    wrappedNode(label: nodeType) {
      deleteDir()
      checkout(scm)
      echo "Pulling image ${imageId}"
      docker.image(imageId).pull()

      withEnv([
        "KEEPBUNDLE=true",
        "TESTFLAGS=-v",
        ] + (extraEnv ?: [])
      ) {
        for (int i = 0; i < depends.size(); i++) {
          unstash depends.get(i)
        }
        withChownWorkspace {
          sh("""
            export DOCKER_GRAPHDRIVER=\$( docker info | awk -F ': ' '\$1 == "Storage Driver" { print \$2; exit }' )
            docker run \\
              -i \\
              --rm \\
              --privileged \\
              -e KEEPBUNDLE \\
              -e TESTFLAGS \\
              -e DOCKER_BUILD_PKGS \\
              -v "\$(pwd)/bundles:/go/src/github.com/docker/docker/bundles" \\
              "${imageId}" \\
              hack/make.sh ${taskNames}
          """)
        }
        if (this.versionString == null) {
          sh("pushd bundles && ls | grep -v latest > ../version-string.txt && popd")
          this.versionString = readFile("version-string.txt").trim()
          sh("rm version-string.txt")
          echo "Got version string: ${this.versionString}"
        }
        if (body) { body() }
        echo("${taskNames} complete")
        if (doStash) {
          sh "[[ -L bundles/latest ]] && rm bundles/latest"
          def taskNameParts = taskNames.split(' ')
          for (i = 0; i < taskNameParts.size(); i++) {
            def taskName = taskNameParts.get(i)
            stash(name: taskName, includes: "bundles/${this.versionString}/${taskName}*/**")
            archive(includes: "bundles/${this.versionString}/${taskName}*/**")
          }
        }
      }
    }
  }
}

def go2xunit(task) {
  sh("cd bundles/${this.versionString}/${task} && docker run --rm bmangold/go2xunit < test-stdout.log > test.xml")
}

def junitArchive(task) {
  step([$class: 'JUnitResultArchiver', testResults: "bundles/${this.versionString}/${task}/test.xml", keepLongStdio: true])
}

def testTask(testName, label=null) {
  def needsXunit = testName != "test-docker-py"
  return this.makeTask(label ?: "docker", testName, false, ["binary"], []) {
    if (needsXunit) { this.go2xunit(testName) }
    this.junitArchive(testName)
  }
}

def integrationTask(label) {
  return this.testTask("test-integration-cli", label)
}

def packageTask(pkgTask, distro) {
  return this.makeTask("docker", "build-${pkgTask}", true, ["dynbinary"], ["DOCKER_BUILD_PKGS=${distro}"])
}

def buildTask(buildTaskName) {
  return this.makeTask("docker", buildTaskName, true, [], [])
}

def validateTask(validateTaskName) {
  return this.makeTask("docker", validateTaskName, false, [], [])
}

return this
