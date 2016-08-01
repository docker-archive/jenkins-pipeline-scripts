package com.docker.utilities

def s3UpdateOptions(options) {
  options.bucket = options.bucket ?: 'docker-ci-artifacts'
  if (options.bucket == 'docker-ci-artifacts') {
    options.region = 'us-west-2'
  }
  options.credentials = options.credentials ?: 'ci@docker-qa.aws'
  if (options.project == null) {
    def path = scm.repositories[0].URIs[0].path
    def repoOwner = path.split('/')[0]
    def repoName = path.split('/')[-1].replaceAll(/\.git$/, '')
    options.project = "${repoOwner}/${repoName}"
  }
  options.path = options.path ?: ''
  options.ref = options.ref ?: gitCommit()
  if (options.includeRef == null) {
    options.includeRef = true
  }
  options.refPathPart = options.includeRef ? "${options.ref}/" : ""
  if (options.fullRemotePath == null) {
    options.fullRemotePath = "${options.bucket}/${options.project}/${options.refPathPart}${options.path}/"
  }
}

def s3Sync(options, src, dst) {
  withEnv(["AWS_DEFAULT_REGION=${options.region}"]) {
    withCredentials([[$class: "AmazonWebServicesCredentialsBinding", credentialsId: options.credentials]]) {
      sh """
      docker run \\
        --rm \\
        -e AWS_SECRET_ACCESS_KEY \\
        -e AWS_ACCESS_KEY_ID \\
        -e AWS_DEFAULT_REGION \\
        -v "\$(pwd):/files" \\
        --workdir="/files" \\
        anigeo/awscli \\
          s3 sync ${options.s3SyncArgs ?: ''} "${src}" "${dst}"
      """
    }
  }
}
def s3Download(options=[:]) {
  s3UpdateOptions(options)

  withChownWorkspace {
    try {
      s3Sync(options, options.fullRemotePath, options.destinationPath ?: ".")
    } catch (Exception exc) {
      if (options.required) {
        throw exc
      } else {
        echo "Ignoring error in s3Download. set `required: true` to propagate error."
      }
    }
  }
}

def s3Upload(options=[:]) {
  s3UpdateOptions(options)
  s3Sync(options, options.sourcePath ?: ".", options.fullRemotePath)
}

return this
