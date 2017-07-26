def call(Closure body=null) {
  def retVal
  try {
    if (body) { retVal = body() }
  } finally {
    try {
      echo "chowning workspace"
      def arch = sh(script: "uname -m", returnStdout: true).trim()
      def image = "busybox"
      if (arch.startsWith("arm")) {
        image = "armhf/busybox"
      } else if (arch == "aarch64" ) {
        image = "arm64v8/busybox"
      } else if (arch == "ppc64le" || arch == "s390x") {
        image = "${arch}/busybox"
      }
      sh "docker run --rm -v \$(pwd):/workspace ${image} chown -R \"\$(id -u):\$(id -g)\" /workspace"
    } catch (Exception e) {
      println e
    }
  }
  retVal
}
