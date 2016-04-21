import hudson.tools.ToolInstallation

def call(tools, Closure body=null) {
  if (! (tools instanceof List)) {
    tools = [tools]
  }
  def toolEnv = []
  def pathEnv = [env.PATH]
  def toolNames = []

  for (int i = 0; i < tools.size(); i++) {
    def toolString = tools.get(i)

    def match = (toolString =~ /^([^@]+)(?:@(.+))?$/)
    if (!match) { continue; }

    def toolName = match[0][1]
    def toolVersion = match[0][2]

    toolNames.add(toolName)

    // No specified version, look it up
    if (!toolVersion) {
      for (int j = 0; j < ToolInstallation.all().size(); j++) {
        def toolDescriptor = ToolInstallation.all()[j]

        for (int k = 0; k < toolDescriptor.installations.size(); k++) {
          def toolInstallation = toolDescriptor.installations[k]
          if (toolInstallation.name != toolName) { continue; }
          toolVersion = toolInstallation.toolVersion.versionsListSource.defaultValue
        }
      }
    }
    // Still possible that we don't have a version
    if (toolVersion) {
      toolEnv.add(toolName.replaceAll(/\W/, '_').toUpperCase() + "_VERSION=${toolVersion}")
    }
  }
  withEnv(toolEnv) {
    for (i = 0; i < toolNames.size(); i++) {
      def toolName = toolNames.get(i)
      pathEnv << tool(toolName)
    }
    withEnv(["PATH=${pathEnv.join(":")}"]) {
      if (body) { body() }
    }
  }
}
