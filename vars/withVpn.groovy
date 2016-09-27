def call(vpnHost, Closure body=null) {
  def vpnImage = docker.image("dckr/vpn-client")
  def vpnContainerName = "${env.BUILD_TAG}-vpn-client"
  withEnv(["DOCKER_CONFIG=${sh(script: 'pwd', returnStdout:true)}/.docker"]) {
    withDockerRegistry(credentialsId: "dockerbuildbot-index.docker.io") {
      vpnImage.pull()
    }
  }
  try {
    // Start VPN client for stage
    withCredentials([[
      usernameVariable: "VPN_USERNAME",
      passwordVariable: "VPN_PASSWORD",
      credentialsId: "jenkins.${vpnHost}",
      $class: "UsernamePasswordMultiBinding"
    ]]) {
      sh """
        docker run \\
          -d \\
          --name "${vpnContainerName}" \\
          --restart=always \\
          --cap-add NET_ADMIN \\
          --device /dev/net/tun \\
          -e REMOTE_VPN=${vpnHost} \\
          -e VPN_USERNAME \\
          -e VPN_PASSWORD \\
          --net=host \\
          ${vpnImage.imageName()}
      """
    }

    if (body) { body() }

  } finally {
    sh "docker rm -f ${vpnContainerName} ||:"
  }
}
