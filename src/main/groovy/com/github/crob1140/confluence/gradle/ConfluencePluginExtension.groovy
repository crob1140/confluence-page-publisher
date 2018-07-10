package com.github.crob1140.confluence.gradle

import org.gradle.api.Project

/**
 * This class a set of properties that will be used as the default
 * values for all {@link com.github.crob1140.confluence.gradle.tasks.PublishConfluencePage}
 * tasks in a project.
 *
 * This extension is applied through the {@link ConfluencePagePublishPlugin}
 */
class ConfluencePluginExtension {
    String url
    String username
    String password

    ConfluencePluginExtension(Project project) {}
}
