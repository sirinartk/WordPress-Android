package org.wordpress.android.ui.posts

import android.app.Activity
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.WPWebViewUsageCategory
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePreviewLogicHelper @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val activityLauncherWrapper: ActivityLauncherWrapper,
    private val postUtilsWrapper: PostUtilsWrapper
) {
    enum class RemotePreviewType {
        NOT_A_REMOTE_PREVIEW,
        REMOTE_PREVIEW,
        REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
    }

    enum class PreviewLogicOperationResult {
        PREVIEW_NOT_AVAILABLE,
        NETWORK_NOT_AVAILABLE,
        MEDIA_UPLOAD_IN_PROGRESS,
        CANNOT_SAVE_EMPTY_DRAFT,
        GENERATING_PREVIEW,
        OPENING_PREVIEW,
        CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
    }

    interface RemotePreviewHelperFunctions {
        fun notifyUploadInProgress(post: PostModel): Boolean
        fun updatePostIfNeeded(): PostModel? = null
        fun notifyEmptyDraft() {}
        fun startUploading(isRemoteAutoSave: Boolean, post: PostModel)
        fun notifyEmptyPost() {}
    }

    fun runPostPreviewLogic(
        activity: Activity,
        site: SiteModel,
        post: PostModel,
        helperFunctions: RemotePreviewHelperFunctions
    ): PreviewLogicOperationResult {
        if (!site.isUsingWpComRestApi) {
            activityLauncherWrapper.showActionableEmptyView(
                    activity,
                    WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                    post.title
            )
            return PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE
        } else if (!networkUtilsWrapper.isNetworkAvailable()) {
            activityLauncherWrapper.showActionableEmptyView(
                    activity,
                    WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK,
                    post.title
            )
            return PreviewLogicOperationResult.NETWORK_NOT_AVAILABLE
        } else {
            if (helperFunctions.notifyUploadInProgress(post)) {
                return PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS
            }

            val updatedPost = helperFunctions.updatePostIfNeeded() ?: post

            if (shouldUpload(updatedPost)) {
                if (!postUtilsWrapper.isPublishable(updatedPost)) {
                    helperFunctions.notifyEmptyDraft()
                    return PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT
                }
                helperFunctions.startUploading(false, updatedPost)
            } else if (shouldRemoteAutoSave(updatedPost)) {
                if (!postUtilsWrapper.isPublishable(updatedPost)) {
                    helperFunctions.notifyEmptyPost()
                    return PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST
                }
                // TODO: remove the following Jetpack exception when the API bug for Jetpack sites is fixed
                // and previewing auto-saves are working. More informations about this on the following ticket:
                // https://github.com/Automattic/wp-calypso/issues/20265
                if (site.isJetpackConnected) {
                    activityLauncherWrapper.showActionableEmptyView(
                            activity,
                            WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE,
                            post.title
                    )
                    return PreviewLogicOperationResult.PREVIEW_NOT_AVAILABLE
                }
                helperFunctions.startUploading(true, updatedPost)
            } else {
                activityLauncherWrapper.previewPostOrPageForResult(
                        activity,
                        site,
                        updatedPost,
                        RemotePreviewType.REMOTE_PREVIEW
                )
                return PreviewLogicOperationResult.OPENING_PREVIEW
            }
        }
        return PreviewLogicOperationResult.GENERATING_PREVIEW
    }

    private fun shouldUpload(post: PostModel): Boolean {
        val status = PostStatus.fromPost(post)
        return post.isLocalDraft ||
                (status == PostStatus.DRAFT && post.isLocallyChanged)
    }

    private fun shouldRemoteAutoSave(post: PostModel): Boolean {
        val status = PostStatus.fromPost(post)
        return (status == PostStatus.PUBLISHED || status == PostStatus.SCHEDULED) && post.isLocallyChanged
    }
}