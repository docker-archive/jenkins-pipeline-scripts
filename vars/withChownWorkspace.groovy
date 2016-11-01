def call(Closure body=null) {
  def retVal
  try {
    if (body) { retVal = body() }
  } finally {
    try {
      echo "chowning workspace"
      sh 'docker run --rm -v $(pwd):/workspace busybox chown -R "$(id -u):$(id -g)" /workspace'
    } catch (Exception e) {
      println e
    }
  }
  retVal
}
