String call(cmd) {
  def filename = "${env.BUILD_TAG}-getOutput.txt"
  try {
    sh "${cmd} > '${filename}'"
    result = readFile(filename)
  } finally {
    sh "rm -f '${filename}'"
  }
  return result
}
