String call() {
  if (env.GIT_COMMIT == null) {
    env.GIT_COMMIT = getOutput("git rev-parse HEAD").trim()
  }
  env.GIT_COMMIT
}
