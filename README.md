jenkins-pipeline-scripts
========================

*NOTE: This repository is being deprecated internally at Docker, Inc and hence
will receive few updates going forward.*

This repository contains helper functions and classes to be used with the Jenkins Pipeline Plugin.
This repository is used on https://jenkins.dockerproject.org and other Jenkins instances managed by Docker, Inc.

To use this library from your `Jenkinsfile`,
make sure you have installed the _GitHub Organization Folder_ in version 1.5 or later,
then start off with:

```groovy
@Library('github.com/docker/jenkins-pipeline-scripts') _
```

See [Extending with Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) for more
information on Jenkins pipeline extensions.
