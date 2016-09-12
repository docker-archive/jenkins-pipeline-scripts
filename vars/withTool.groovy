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
    match = null

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
            echo "Tool installer: '${toolName}' will be installed but versions are not configured so the version string '${toolVersion}' is being ignored."
            toolVersion = null
          }
          break
        }

        if (!toolVersion) {
          // Versions are enabled but toolVersion is not set. use default
          toolVersion = toolInstallation.toolVersion.versionsListSource.defaultValue
        } else {
          // Let's check if the request version exists
          def versionSource = toolInstallation.toolVersion.versionsListSource
          def availableVersions = versionSource.value.split(versionSource.multiSelectDelimiter)
          if (!availableVersions.contains(toolVersion)) {
            throw new Exception("Tool installer: '${toolName}' has no configuration for version '${toolVersion}'")
          }
        }

        if (toolInstallation.hasAdditionalVariables()) {
          def extraVars = toolInstallation.additionalVariables.split("\n")
          for (int l = 0; l < extraVars.size(); l++) {
            def extraVar = extraVars[l].trim()
            if (extraVar.size() == 0) { continue; }
            if (!extraVar.contains('=')) {
              echo "Ignoring invalid extra variable for '${toolName}': '${extraVar}'"
              continue
            }
            toolEnv.add(extraVar)
          }
        }
      }
    }
    // Still possible that we don't have a version
    if (toolVersion) {
      toolEnv << toolName.replaceAll(/\W/, '_').toUpperCase() + "_VERSION=${toolVersion}"
    }
  }
  withEnv(toolEnv) {
    for (i = 0; i < toolNames.size(); i++) {
      def toolName = toolNames[i]
      pathEnv << tool(toolName)
    }
    pathEnv << env.PATH
    withEnv(["PATH=${pathEnv.join(":")}"]) {
      if (body) { body() }
    }
  }
}
