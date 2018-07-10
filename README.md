# confluence-page-publisher
[![Build Status](https://travis-ci.org/crob1140/confluence-page-publisher.svg?branch=master)](https://travis-ci.org/crob1140/confluence-page-publisher)
[![Test Coverage](https://codecov.io/gh/crob1140/confluence-page-publisher/branch/master/graph/badge.svg)](https://codecov.io/gh/crob1140/confluence-page-publisher)

This plugin allows Confluence pages to be published through a Gradle task.

## Installation

Add the following to your build.gradle to include the plugin:
```groovy
plugins {
  id 'com.github.crob1140.confluence-page-publisher'
}
```
or alternatively:
```groovy
apply plugin: 'com.github.crob1140.confluence-page-publisher'
```
## Usage
The connection details for your Confluence server can be set through the 'confluence' extension that is added by this plugin:
```groovy
confluence {
  url = 'http://www.sample.com/wiki'
  username = 'sampleuser'
  password = 'samplepass'
}
```

You can then define one or more tasks of type PublishConfluencePage that will use these settings:
```groovy
task publishReleaseNotes(type: PublishConfluencePage) {
    spaceKey = 'SAMPLE'
    parentTitle = 'Sample Releases'
    title = "Sample v${project.version} Release Notes"
    body = '<ac:rich-text-body>SAMPLE</ac:rich-text-body>'
    labels = ['release-notes']
}
```

Whilst all tasks of type PublishConfluencePage will inherit their connection details from the 'confluence' extension by default, you can also set the connection details per task if you need to:
```groovy
task publishReleaseNotes(type: PublishConfluencePage) {
    serverUrl = 'http://www.sample.com/wiki'
    username = 'sampleuser'
    password = 'samplepass'
    spaceKey = 'SAMPLE'
    parentTitle = 'Sample Releases'
    title = "Sample v${project.version} Release Notes"
    body = '<ac:rich-text-body>SAMPLE</ac:rich-text-body>'
    labels = ['release-notes']
}
```

## Contribution

If you have any requests, feel free to raise them as issues. You are also free to fork the repository to make your own changes, and then raise a pull request so that your changes can be merged in.

## License
MIT