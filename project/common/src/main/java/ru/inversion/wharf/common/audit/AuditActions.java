package ru.inversion.wharf.common.audit;

public final class AuditActions {

    public static final String CREATE_ORGANIZATION = "create-organization";
    public static final String RENAME_ORGANIZATION = "rename-organization";
    public static final String DELETE_ORGANIZATION = "delete-organization";
    public static final String CREATE_ORG_USER = "create-org-user";
    public static final String DELETE_ORG_USER = "delete-org-user";
    public static final String UPDATE_PRODUCT = "update-product";
    public static final String DELETE_PRODUCT = "delete-product";
    public static final String PUBLISH_RELEASE = "publish-release";
    public static final String CHANGE_RELEASE_CHANNEL = "change-release-channel";
    public static final String UPDATE_RELEASE = "update-release";
    public static final String DELETE_RELEASE = "delete-release";
    public static final String GRANT_ENTITLEMENT = "grant-entitlement";
    public static final String REVOKE_ENTITLEMENT = "revoke-entitlement";
    public static final String ISSUE_ENROLLMENT_TOKEN = "issue-enrollment-token";
    public static final String REVOKE_ENROLLMENT_TOKEN = "revoke-enrollment-token";

    private AuditActions() {
    }
}
