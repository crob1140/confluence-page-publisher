package com.github.crob1140.confluence.gradle

import com.github.crob1140.confluence.gradle.tasks.PublishConfluencePage
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This plugin adds the 'confluence' extension to a project, which can be used to
 * define default values for all {@link PublishConfluencePage} tasks.
 */
class ConfluencePagePublishPlugin implements Plugin<Project> {

    /**
     * This method applies this plugin to the given project.
     * @param project The project to apply the plugin to
     */
    @Override
    void apply(Project project) {
        def extension = project.extensions.create("confluence", ConfluencePluginExtension, project)
        project.afterEvaluate {
            def publishTasks = project.tasks.findAll { task -> task instanceof PublishConfluencePage }
            publishTasks.forEach { task ->
                if (task.serverUrl == null) {
                    task.serverUrl = extension.url
                }

                if (task.username == null) {
                    task.username = extension.username
                }

                if (task.password == null) {
                    task.password = extension.password
                }
            }
        }
    }
}
