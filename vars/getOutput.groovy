String call(cmd) {
  sh(script: cmd, returnStdout: true)
}
