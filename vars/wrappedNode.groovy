def call(Map vars, Closure body=null) {
  vars = vars ?: [:] 
  node(vars.get("label", null)) {
    withDockerRegistry(vars.get("registryUrl", "https://index.docker.io/v1/"), vars.get("registryCreds", "dockerbuildbot-index.docker.io")) {
      wrap([$class: 'TimestamperBuildWrapper']) {
        wrap([$class: 'AnsiColorBuildWrapper']) {
          if (body) { body() }
        }
      }
    }
  }  
}
