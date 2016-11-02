import java.net.URLEncoder

// TODO: implement this in pure groovy instead of relying on a
// linux node and curl
//
// TODO: support other registry endpoints

@NonCPS
def urlEncode(Map map) {
  map.collect { k,v -> URLEncoder.encode(k, 'UTF-8') + '=' + URLEncoder.encode(v, 'UTF-8') }.join('&')
}

def call(String imageName, String credentialsId="dockerbuildbot-hub.docker.com") {
  if (imageName.contains("@")) {
    echo "dockerImageTagToDigest: ${imageName} appears to be a digest. Returning it unmodified."
    return imageName
  }

  if (!imageName.contains("/")) {
    // For some reason just automatically fixing this doesn't work. not sure why :(
    throw new Exception("Specify image name with 'library/${imageName}' instead of bare '${imageName}'")
  }

  def repo = imageName
  def tag = 'latest'

  if (imageName.contains(":")) {
    def imageNameParts = imageName.split(":", 2)
    repo = imageNameParts[0]
    tag = imageNameParts[1]
  }

  String token = null

  withTool("jq") {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: credentialsId,
      usernameVariable: '__JP_DOCKERHUB_USERNAME',
      passwordVariable: '__JP_DOCKERHUB_PASSWORD'
    ]]) {
      def params = [
        service: "registry.docker.io",
        scope: "repository:${repo}:pull",
        account: env.__JP_DOCKERHUB_USERNAME
      ]
      token = sh(
        returnStdout: true,
        script: """set +x; set -o pipefail; curl -sSl \\
        -u "\$__JP_DOCKERHUB_USERNAME:\$__JP_DOCKERHUB_PASSWORD" \\
        "https://auth.docker.io/token?${urlEncode(params)}" \\
        | jq -r .token"""
      ).trim()
    }
  }

  try {
    return repo + "@" + sh(
      returnStdout: true,
      script: """set +x; set -o pipefail; curl -sfi \\
      -H "Authorization: Bearer ${token}" \\
      -H "Accept: application/vnd.docker.distribution.manifest.v2+json" \\
      "https://registry-1.docker.io/v2/${repo}/manifests/${tag}" \\
      | awk -F ': ' '\$1 == "Docker-Content-Digest" {print \$2}'
    """).trim()
  } catch (Exception exc) {
    return false
  }
}
