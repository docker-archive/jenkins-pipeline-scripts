import jenkins.model.Jenkins

def call(label) {
  def labelObj = Jenkins.instance.getLabel(label)
  return (labelObj.nodes.size() + labelObj.clouds.size()) > 0
}
