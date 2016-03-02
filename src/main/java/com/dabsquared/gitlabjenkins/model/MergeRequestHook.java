package com.dabsquared.gitlabjenkins.model;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class MergeRequestHook extends WebHook {

    private User user;
    private Project project;
    private ObjectAttributes objectAttributes;
    private Repository repository;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(ObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestHook that = (MergeRequestHook) o;
        return new EqualsBuilder()
                .append(user, that.user)
                .append(project, that.project)
                .append(objectAttributes, that.objectAttributes)
                .append(repository, that.repository)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(project)
                .append(objectAttributes)
                .append(repository)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("project", project)
                .append("objectAttributes", objectAttributes)
                .append("repository", repository)
                .toString();
    }
}
