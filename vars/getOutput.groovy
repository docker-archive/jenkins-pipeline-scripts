String call(cmd) {
  echo 'getOutput is deprecated, just use the returnStdout option to sh'
  sh script: cmd, returnStdout: true
}
