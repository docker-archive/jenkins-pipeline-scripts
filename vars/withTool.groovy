import hudson.tools.ToolInstallation

def call(tools, Closure body=null) {
  if (! (tools instanceof List)) {
    tools = [tools]
  }
  def toolEnv = []
  def pathEnv = []
  def toolNames = []

  for (int i = 0; i < tools.size(); i++) {
    def toolString = tools[i]

    def match = (toolString =~ /^([^@]+)(?:@(.+))?$/)
    if (!match) { continue; }

    def toolName = match[0][1]
    def toolVersion = match[0][2]

    toolNames << toolName

    for (int j = 0; j < ToolInstallation.all().size(); j++) {
      def toolDescriptor = ToolInstallation.all()[j]

      for (int k = 0; k < toolDescriptor.installations.size(); k++) {
        def toolInstallation = toolDescriptor.installations[k]
        // This is not the tool we're looking for
        if (toolInstallation.name != toolName) { continue; }
        // We found our tool and it doesn't have different versions
        if (!toolInstallation.toolVersion) {
          if (toolVersion) {
            echo "Tool installer: ${toolName} will be installed but versions are not configured so the version string '${toolVersion}' is being ignored."
            break
          } else {
            continue
          }
        }
        if (!toolVersion) {
          toolVersion = toolInstallation.toolVersion.versionsListSource.defaultValue
        }
        if (toolInstallation.hasAdditionalVariables()) {
          toolEnv.addAll(toolInstallation.additionalVariables.split("\n"))
        }
      }
    }
    // Still possible that we don't have a version
    if (toolVersion) {
      toolEnv << toolName.replaceAll(/\W/, '_').toUpperCase() + "_VERSION=${toolVersion}"
    }
  }
  echo "environment for tools: ${toolEnv}"
  withEnv(toolEnv) {
    for (i = 0; i < toolNames.size(); i++) {
      def toolName = toolNames.get(i)
      pathEnv << tool(toolName)
    }
    pathEnv << env.PATH
    echo "PATH for tools: ${pathEnv}"
    withEnv(["PATH=${pathEnv.join(":")}"]) {
      if (body) { body() }
    }
  }
}
