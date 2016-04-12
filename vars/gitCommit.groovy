String call() {
  if (env.GIT_COMMIT == null) {
    def filename = "${env.BUILD_TAG}-git-revision.txt"
    try {
      sh "git rev-parse HEAD > '${filename}'"
      env.GIT_COMMIT = readFile(filename).trim()
    } finally {
      sh "rm -f '${filename}'"
    }
  }
  env.GIT_COMMIT
}
