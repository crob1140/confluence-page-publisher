package com.github.crob1140.confluence.gradle.tasks

import com.github.crob1140.confluence.ConfluenceClient
import com.github.crob1140.confluence.auth.AuthMethod
import com.github.crob1140.confluence.auth.BasicAuth
import com.github.crob1140.confluence.content.Content
import com.github.crob1140.confluence.content.ContentBodyType
import com.github.crob1140.confluence.content.ContentStatus
import com.github.crob1140.confluence.content.StandardContentType
import com.github.crob1140.confluence.content.expand.ExpandedContentProperties
import com.github.crob1140.confluence.gradle.ContentSpec
import com.github.crob1140.confluence.requests.CreateContentRequest
import com.github.crob1140.confluence.requests.GetContentRequest
import com.github.crob1140.confluence.requests.UpdateContentRequest
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget

/**
 * This task publishes a page to a Confluence server.
 */
class PublishConfluencePage extends DefaultTask implements ContentSpec {

    /**
     * The URL of the Confluence server.
     * This will be inherited from the 'confluence' extension if left empty.
     */
    @Input
    @Optional
    String serverUrl

    /**
     * The username of the Confluence user that will be used to publish the page.
     * This will be inherited from the 'confluence' extension if left empty.
     */
    @Input
    @Optional
    String username

    /**
     * The password of the Confluence user that will be used to publish the page.
     * This will be inherited from the 'confluence' extension if left empty.
     */
    @Input
    @Optional
    String password

    /**
     * The body of the page that will be published.
     */
    @Input
    String body

    /**
     * The title of the published page.
     */
    @Input
    String title

    /**
     * The key of the space to which the page will be published.
     */
    @Input
    String spaceKey

    /**
     * The labels to attach to the published page.
     */
    @Input
    @Optional
    List<String> labels = []

    /**
     * The title of the page that will be the parent of the publication.
     */
    @Input
    @Optional
    String parentTitle = null

    PublishConfluencePage() { setGroup("documentation") }

    /**
     * This method sends a request to the Confluence server to create the content
     * specified in the properties of this task.
     */
    @TaskAction
    void publish() {
        validate()

        WebTarget wikiTarget = ClientBuilder.newClient().target(this.serverUrl)
        AuthMethod auth = new BasicAuth(this.username, this.password)
        ConfluenceClient client = new ConfluenceClient(wikiTarget, auth)

        java.util.Optional<Content> existingPage = getExistingPage(client, this)
        if (existingPage.present) {
            updatePage(client, existingPage.get(), this)
        } else {
            createPage(client, this)
        }
    }

    /**
     * This method sets the URL of the Confluence server.
     * This will be inherited from the 'confluence' extension if left empty.
     *
     * @param serverUrl the URL of the Confluence Cloud server that the page will be published to
     */
    void serverUrl(String serverUrl) {
        this.serverUrl = serverUrl
    }

    /**
     * This method sets the username of the Confluence user that will be used to publish the page.
     * This will be inherited from the 'confluence' extension if left empty.
     *
     * @param username the username of the Confluence user that will be used to publish the page.
     */
    void username(String username) {
        this.username = username
    }

    /**
     * This method sets the password of the Confluence user that will be used to publish the page.
     * This will be inherited from the 'confluence' extension if left empty.
     */
    void password(String password) {
        this.password = password
    }

    /**
     * This method sets the body of the page that will be published.
     *
     * @param body The body of the page that will be published
     */
    ContentSpec body(String body) {
        this.body = body
        return this
    }

    /**
     * This method returns the body of the page that will be published.
     *
     * @return The body of the page that will be published.
     */
    String getBody() {
        return this.body
    }

    /**
     * The title of the published page.
     *
     * @param title the title of the published page
     * @return This instance
     */
    ContentSpec title(String title) {
        this.title = title
        return this
    }

    /**
     * This method returns the title of the published page.
     *
     * @return The title of the published page
     */
    String getTitle() {
        return this.title
    }

    /**
     * The key of the space to which the page will be published.
     *
     * @param spaceKey The key of the space to which the page will be published.
     */
    ContentSpec spaceKey(String spaceKey) {
        this.spaceKey = spaceKey
        return this
    }

    /**
     * This method returns the key of the space to which the page will be published.
     *
     * @return The key of the space to which the page will be published
     */
    String getSpaceKey() {
        return this.spaceKey
    }

    /**
     * This method adds the given labels to the published page.
     *
     * @param labels the labels to add to the published page.
     */
    ContentSpec labels(Iterable<String> labels) {
        this.labels.addAll(labels)
        return this
    }

    /**
     * This method adds the given labels to the published page.
     *
     * @param labels the labels to add to the published page.
     */
    ContentSpec labels(String... labels) {
        this.labels.addAll(labels)
        return this
    }

    /**
     * This method returns the labels for the published page.
     *
     * @return The labels for the published page
     */
    List<String> getLabels() {
        return this.labels
    }

    /**
     * This method sets the title of the page that will be the parent of the publication.
     *
     * @param parentTitle the title of the page that will be the parent of the publication.
     */
    ContentSpec parentTitle(String parentTitle) {
        this.parentTitle = parentTitle
        return this
    }

    /**
     * This method returns the title of the page that will be the parent of the publication.
     *
     * @return The title of the page that will be the parent of the publication
     */
    String getParentTitle() {
        return this.parentTitle
    }

    /**
     * This method asserts that the task is valid.
     * @throws IllegalStateException if any of the mandatory properties are not defined
     */
    void validate() {
        if (serverUrl == null) {
            throw new IllegalStateException("Missing mandatory parameter 'serverUrl'")
        }
        if (username == null) {
            throw new IllegalStateException("Missing mandatory parameter 'username'")
        }
        if (password == null) {
            throw new IllegalStateException("Missing mandatory parameter 'password'")
        }
        if (title == null) {
            throw new IllegalStateException("Missing mandatory parameter 'title'")
        }
        if (body == null) {
            throw new IllegalStateException("Missing mandatory parameter 'body'")
        }
        if (spaceKey == null) {
            throw new IllegalStateException("Missing mandatory parameter 'spaceKey'")
        }
    }

    private
    static java.util.Optional<Content> getExistingPage(ConfluenceClient client, ContentSpec contentSpec) {
        List<Content> existingPages = client.getContent(new GetContentRequest.Builder()
                .setSpaceKey(contentSpec.getSpaceKey())
                .setTitle(contentSpec.getTitle())
                .setExpandedProperties(new ExpandedContentProperties.Builder().addVersion().build())
                .setLimit(1)
                .build())

        return (existingPages.empty ? java.util.Optional.empty() : java.util.Optional.of(existingPages.first()))
    }

    private static void createPage(ConfluenceClient client, ContentSpec contentSpec) {
        CreateContentRequest.Builder requestBuilder = new CreateContentRequest.Builder()
                .setType(StandardContentType.PAGE)
                .setSpaceKey(contentSpec.spaceKey)
                .setTitle(contentSpec.title)
                .setBody(ContentBodyType.STORAGE, contentSpec.body)

        if (contentSpec.parentTitle != null) {
            String parentId = getContentId(contentSpec.spaceKey, contentSpec.parentTitle, client)
            requestBuilder.setAncestor(parentId)
        }

        if (contentSpec.labels != null) {
            for (String label : contentSpec.labels) {
                requestBuilder.addLabel(label)
            }
        }

        client.createContent(requestBuilder.build())
    }

    private
    static void updatePage(ConfluenceClient client, Content existingPage, ContentSpec contentSpec) {
        UpdateContentRequest.Builder requestBuilder = new UpdateContentRequest.Builder()
                .setId(existingPage.id)
                .setType(StandardContentType.PAGE)
                .setStatus(ContentStatus.CURRENT)
                .setTitle(contentSpec.title)
                .setBody(ContentBodyType.STORAGE, contentSpec.body)
                .setVersion(existingPage.version.number + 1)

        if (contentSpec.parentTitle != null) {
            String parentId = getContentId(contentSpec.spaceKey, contentSpec.parentTitle, client)
            requestBuilder.setAncestor(parentId)
        }

        if (contentSpec.labels != null) {
            for (String label : contentSpec.labels) {
                requestBuilder.addLabel(label)
            }
        }

        client.updateContent(requestBuilder.build())
    }

    private
    static String getContentId(String spaceKey, String parentTitle, ConfluenceClient client) {
        List<Content> parentPages = client.getContent(new GetContentRequest.Builder()
                .setType(StandardContentType.PAGE)
                .setSpaceKey(spaceKey)
                .setTitle(parentTitle)
                .setLimit(1)
                .build())
        if (parentPages.empty) {
            throw new IllegalArgumentException("Parent page '$parentTitle' in space '$spaceKey' does not exist")
        }

        return parentPages.first().id
    }
}