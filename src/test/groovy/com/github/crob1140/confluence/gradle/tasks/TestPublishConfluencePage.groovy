package com.github.crob1140.confluence.gradle.tasks

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.crob1140.confluence.content.Content
import com.github.crob1140.confluence.content.StandardContentType
import com.github.crob1140.confluence.content.Version
import com.github.crob1140.confluence.requests.GetContentResponse
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*

class TestPublishConfluencePage extends Specification {

    static final Integer WIRE_MOCK_PORT = 8888

    static final String WIRE_MOCK_URL = "http://localhost:$WIRE_MOCK_PORT"

    @ClassRule
    static WireMockClassRule wireMockRule = new WireMockClassRule(WIRE_MOCK_PORT)

    @Rule
    WireMockClassRule instanceRule = wireMockRule

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    Project project

    void setup() {
        project = ProjectBuilder.builder().build()
    }

    void initWireMockStubs(List<Content> existingContent) {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)))
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200)))
        stubFor(get(urlPathMatching("/rest/api/content")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(new GetContentResponse([])))
                .withStatus(200)))
        existingContent.each {
            stubFor(get(urlPathMatching("/rest/api/content"))
                    .withQueryParam("spaceKey", equalTo(it.space.key))
                    .withQueryParam("title", equalTo(it.title))
                    .willReturn(aResponse().withHeader("Content-Type", "application/json")
                    .withBody(toJson(new GetContentResponse(existingContent)))
                    .withStatus(200)))
        }
    }

    def "The task should throw an IllegalStateException if any of the mandatory properties are missing"() {
        given: "A Confluence server with no existing content"
        initWireMockStubs([])

        when: "a PublishConfluencePage with a missing mandatory property value is invoked"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl = serverUrlVal
            username = usernameVal
            password = passwordVal
            spaceKey = spaceKeyVal
            title = titleVal
            body = bodyVal
        }

        task.publish()

        then: "an IllegalStateException should be thrown"
        thrown(IllegalStateException)

        where:
        serverUrlVal  | spaceKeyVal | titleVal    | usernameVal | passwordVal | bodyVal
        WIRE_MOCK_URL | 'spaceKey'  | 'pageTitle' | 'username'  | 'password'  | null
        WIRE_MOCK_URL | 'spaceKey'  | 'pageTitle' | 'username'  | null        | 'template'
        WIRE_MOCK_URL | 'spaceKey'  | 'pageTitle' | null        | 'password'  | 'template'
        WIRE_MOCK_URL | 'spaceKey'  | null        | 'username'  | 'password'  | 'template'
        WIRE_MOCK_URL | null        | 'pageTitle' | 'username'  | 'password'  | 'template'
        null          | 'spaceKey'  | 'pageTitle' | 'username'  | 'password'  | 'template'
    }

    def "The task should not throw an IllegalStateException when all of the mandatory properties are set"() {
        given: "A Confluence server with no existing content"
        initWireMockStubs([])

        and: "a PublishConfluencePage task with all of the mandatory parameters set"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl WIRE_MOCK_URL
            spaceKey 'spaceKey'
            title 'title'
            username 'username'
            password 'password'
            body 'body'
        }

        when: "the task is invoked"
        task.publish()

        then: "no IllegalStateException should be thrown"
        notThrown(IllegalStateException)
    }

    def "The task should only attempt to create the content if there is no existing matching content, and update the matching content otherwise"() {
        given: "A Confluence server with the given existing content"
        initWireMockStubs(existingContent)

        and: "a valid PublishConfluencePage task"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl WIRE_MOCK_URL
            spaceKey 'spaceKey'
            title 'title'
            username 'username'
            password 'password'
            body 'body'
        }

        and: "a WireMock endpoint that records what method it was called with"
        stubFor(post(urlMatching("/rest/api/content"))
                .inScenario("Record action")
                .willSetStateTo("Created")
                .willReturn(aResponse().withStatus(200)))
        stubFor(put(urlMatching("/rest/api/content/.*"))
                .inScenario("Record action")
                .willSetStateTo("Updated")
                .willReturn(aResponse().withStatus(200)))

        expect: "the recorded method to match the expected state when the task is called"
        task.publish()
        String recordedState = getAllScenarios().first().state
        recordedState == expectedState

        cleanup:
        resetAllScenarios()

        where:
        existingContent                                                                                                                                        | expectedState
        []                                                                                                                                                     | "Created"
        [new Content.Builder().setId("123").setType(StandardContentType.PAGE).setSpaceKey("spaceKey").setTitle("notTitle").setVersion(new Version(1)).build()] | "Created"
        [new Content.Builder().setId("123").setType(StandardContentType.PAGE).setSpaceKey("notSpaceKey").setTitle("title").setVersion(new Version(1)).build()] | "Created"
        [new Content.Builder().setId("123").setType(StandardContentType.PAGE).setSpaceKey("spaceKey").setTitle("title").setVersion(new Version(1)).build()]    | "Updated"
    }

    def "Any labels that are set through the methods should be appended to the existing label collection"() {
        given: "A Confluence server with the given existing content"
        initWireMockStubs(existingContent)

        and: "a task with some initial labels"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl WIRE_MOCK_URL
            spaceKey 'spaceKey'
            title 'title'
            username 'username'
            password 'password'
            body 'body'
            labels = ['first-label']
        }

        and: "additional labels are provided through the 'labels' function"
        task.labels('second-label')
        task.labels(['third-label'])

        expect: "the tasks label to contain both the initial labels and the added labels"
        task.getLabels() == ['first-label', 'second-label', 'third-label']

        and: "the task to succeed"
        task.publish()

        where:
        existingContent                                                                                                   | _
        []                                                                                                                | _
        [new Content.Builder().setId("123").setSpaceKey("spaceKey").setTitle("title").setVersion(new Version(1)).build()] | _
    }

    def "The task should throw an IllegalArgumentException if a parent is specified but does not exist"() {
        given: "A Confluence server with no existing content"
        initWireMockStubs(existingContent)

        and: "a valid PublishConfluencePage task with a specified parent title"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl WIRE_MOCK_URL
            spaceKey 'spaceKey'
            parentTitle 'parentTitle'
            title 'title'
            username 'username'
            password 'password'
            body 'body'
        }

        when: "the task is executed"
        task.publish()

        then: "an IllegalArgumentException should be thrown"
        thrown(IllegalArgumentException)

        where:
        existingContent                                                                                                   | _
        []                                                                                                                | _
        [new Content.Builder().setId("123").setSpaceKey("spaceKey").setTitle("title").setVersion(new Version(1)).build()] | _
    }

    def "The task should not throw an IllegalStateException if a parent is specified and a page with the given title exists"() {
        given: "A Confluence server with existing parent content"
        def parentContent = new Content.Builder().setId("123").setType(StandardContentType.PAGE).setSpaceKey("spaceKey").setTitle("parentTitle").setVersion(new Version(1)).build()
        existingContent.add(parentContent)
        initWireMockStubs(existingContent)

        and: "a valid PublishConfluencePage task with a specified parent title"
        def task = project.tasks.create("testTask", PublishConfluencePage) {
            serverUrl WIRE_MOCK_URL
            spaceKey 'spaceKey'
            parentTitle 'parentTitle'
            title 'title'
            username 'username'
            password 'password'
            body 'body'
        }

        expect: "the task to succeed"
        task.publish()

        where:
        existingContent                                                                                                   | _
        []                                                                                                                | _
        [new Content.Builder().setId("123").setSpaceKey("spaceKey").setTitle("title").setVersion(new Version(1)).build()] | _
    }

    static String toJson(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper()
        return mapper.writeValueAsString(obj)
    }

}
