import com.docker.utilities
def call(options=[:]) {
  def aws = new com.docker.utilities.AWSSync()
  aws.s3Download(options)
}
