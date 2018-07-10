package com.github.crob1140.confluence.gradle

/**
 * This interface specifies the options available when creating or updating content.
 */
interface ContentSpec {
    /**
     * The key of the space to which the page will be published.
     *
     * @param spaceKey The key of the space to which the page will be published.
     */
    ContentSpec spaceKey(String spaceKey)

    /**
     * This method returns the key of the space to which the page will be published.
     *
     * @return The key of the space to which the page will be published
     */
    String getSpaceKey()

    /**
     * The title of the published page.
     *
     * @param title the title of the published page
     */
    ContentSpec title(String title)

    /**
     * This method returns the title of the published page.
     *
     * @return The title of the published page
     */
    String getTitle()

    /**
     * This method sets the title of the page that will be the parent of the publication.
     *
     * @param parentTitle the title of the page that will be the parent of the publication.
     */
    ContentSpec parentTitle(String parentTitle)

    /**
     * This method returns the title of the page that will be the parent of the publication.
     *
     * @return The title of the page that will be the parent of the publication
     */
    String getParentTitle()

    /**
     * This method sets the body of the page that will be published.
     *
     * @param body The body of the page that will be published
     */
    ContentSpec body(String body)

    /**
     * This method returns the body of the page that will be published.
     *
     * @return The body of the page that will be published
     */
    String getBody()

    /**
     * This method sets the labels for the published page.
     *
     * @param labels the labels for the published page.
     */
    ContentSpec labels(Iterable<String> labels)

    /**
     * This method sets the labels for the published page.
     *
     * @param labels the labels for the published page.
     */
    ContentSpec labels(String... labels)

    /**
     * This method returns the labels for the published page.
     *
     * @return The labels for the published page
     */
    List<String> getLabels()
}
