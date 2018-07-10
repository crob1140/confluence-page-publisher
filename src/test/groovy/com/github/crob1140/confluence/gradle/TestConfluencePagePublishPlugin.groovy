package com.github.crob1140.confluence.gradle

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.crob1140.confluence.requests.GetContentResponse
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TestConfluencePagePublishPlugin extends Specification {

    static final Integer WIRE_MOCK_PORT = 8888

    @ClassRule
    static final WireMockClassRule wireMockRule = new WireMockClassRule(WIRE_MOCK_PORT)

    @Rule
    final WireMockClassRule instanceRule = wireMockRule

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    void setup() {
        buildFile = testProjectDir.newFile("build.gradle")
    }

    def "PublishConfluencePage tasks should inherit missing properties from the extension"() {
        given: "A build configuration using the ConfluencePagePublisher plugin"
        buildFile << """
            import com.github.crob1140.confluence.gradle.tasks.PublishConfluencePage
            
            plugins {
                id 'com.github.crob1140.confluence-page-publisher'
            }
            
            confluence {
                ${extension.collect { "$it.key = '$it.value'" }.join('\n')}
            }
            
            task testPublish(type : PublishConfluencePage) {
                ${task.collect { "$it.key = '$it.value'" }.join('\n')}
                spaceKey = 'TEEEST'
                title = 'Test Page'
                body = '<html><body>Hello, World!</body></html>'
            }
        """

        and: "A WireMock server expecting a request to the configured endpoint"
        def expectedAuth = "${expected.username}:${expected.password}"

        // The stubs will only be hit if the expected relative path is applied,
        // so the task will fail if it did not apply as expected
        stubFor(get(urlMatching("/${expected.relativePath}/rest/api/content.*"))
                .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(new GetContentResponse([])))))
        stubFor(post(urlPathMatching("/${expected.relativePath}.*"))
                .withHeader('Authorization', equalTo("Basic ${Base64.encoder.encodeToString(expectedAuth.bytes)}"))
                .willReturn(aResponse().withStatus(200)))

        expect: "the PublishConfluencePage task in the configuration to succeed when invoked"
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments("testPublish")
                .withDebug(true)
                .build()

        result.task(":testPublish").outcome == SUCCESS

        where:
        extension                                                                     | task                                                                 | expected
        [url: getFullPath('extension'), username: 'extension', password: 'extension'] | [:]                                                                  | [relativePath: 'extension', username: 'extension', password: 'extension']
        [:]                                                                           | [serverUrl: getFullPath('task'), username: 'task', password: 'task'] | [relativePath: 'task', username: 'task', password: 'task']
        [url: getFullPath('extension'), username: 'extension', password: 'extension'] | [serverUrl: getFullPath('task'), username: 'task', password: 'task'] | [relativePath: 'task', username: 'task', password: 'task']
    }

    String getFullPath(relativePath) {
        return "http://localhost:$WIRE_MOCK_PORT/$relativePath"
    }

    static String toJson(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.writeValueAsString(obj)
    }
}