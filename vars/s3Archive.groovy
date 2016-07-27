
def call(options=[:]) {
  def fullDestinationPath = options.fullDestinationPath
  def bucketRoot = options.bucket ?: 'docker-ci-artifacts'
  if (bucketRoot == 'docker-ci-artifacts') {
    options.region = 'us-west-2'
  }
  def s3Profile = options.s3Profile ?: 'ci@docker-qa.aws'
  def project = options.project
  if (project == null) {
    def path = scm.repositories[0].URIs[0].path
    def repoOwner = path.split('/')[0]
    def repoName = path.split('/')[-1].replaceAll(/\.git$/, '')
    project = "${repoOwner}/${repoName}"
  }
  def projectPath = options.path ?: ''
  if (options.files == null) {
    throw new Exception("No files specified for S3 archive")
  }
  def refName = options.ref ?: gitCommit()
  def refPathPart = (options.includeRef ?: true) ? "${refName}/" : ""

  step([
    $class: 'S3BucketPublisher',
    entries: [[
      bucket: fullDestinationPath ?: "${bucketRoot}/${project}/${refPathPart}${projectPath}/",
      excludedFile: options.excludes ?: '',
      flatten: options.flatten ?: false,
      gzipFiles: options.gzip ?: false,
      keepForever: options.keepForever ?: true,
      managedArtifacts: options.manageArtifacts ?: true,
      noUploadOnFailure: options.uploadOnFailure ?: false,
      selectedRegion: options.region,
      sourceFile: options.files,
      storageClass: options.storageClass ?: 'STANDARD',
      uploadFromSlave: options.uploadFromSlave ?: true,
      useServerSideEncryption: options.encrypt ?: false
    ]],
    profileName: s3Profile,
    userMetadata: option.userMetadata ?: [],
    dontWaitForConcurrentBuildCompletion: !(options.waitForConcurrentBuildCompletion ?: false),
  ])
}
