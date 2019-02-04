package com.nynja.mobile.communicator.utils.navigation.navigators

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.provider.Settings
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.FileProvider
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.nynja.mobile.communicator.BuildConfig
import com.nynja.mobile.communicator.R
import com.nynja.mobile.communicator.data.conference.LeaveVoiceMessageData
import com.nynja.mobile.communicator.data.models.nynjamodels.ContactModel
import com.nynja.mobile.communicator.data.models.nynjamodels.MessageModel
import com.nynja.mobile.communicator.data.transfer.ReplyTransferData
import com.nynja.mobile.communicator.ui.activities.*
import com.nynja.mobile.communicator.ui.activities.calls.CallActivity
import com.nynja.mobile.communicator.ui.activities.calls.IncomeCallActivity
import com.nynja.mobile.communicator.ui.activities.calls.LeaveVoiceMessageActivity
import com.nynja.mobile.communicator.ui.activities.support.WebViewActivity
import com.nynja.mobile.communicator.ui.activities.wallet.WalletPaymentActivity
import com.nynja.mobile.communicator.ui.base.BaseActivity
import com.nynja.mobile.communicator.utils.FileUtils
import com.nynja.mobile.communicator.utils.Utils
import com.nynja.mobile.communicator.utils.navigation.commands.ForwardActivityForResult
import com.nynja.mobile.communicator.utils.navigation.commands.TransitionForward
import ru.terrakok.cicerone.commands.BackTo
import ru.terrakok.cicerone.commands.Command
import ru.terrakok.cicerone.commands.Forward
import ru.terrakok.cicerone.commands.Replace
import timber.log.Timber
import java.io.File
import java.util.*

abstract class NynjaNavigator(activity: BaseActivity) : BaseNavigator(activity) {

    val waitingResultScreens: Stack<String> = Stack()


    companion object {

        const val WELCOME = "Welcome Activity"
        const val START = "Registration Activity"
        const val MAIN = "Main Activity"
        const val PICK_FILE = "Pick File Activity"
        const val VIDEO_PLAY = "Video Play Activity"


        const val PREVIEW_IMAGE = "Image Preview Activity"
        const val PREVIEW_AVATAR = "Avatar Preview Activity"

        const val MESSAGES_REPLIED = "Replied Messages Activity"
        const val MAIN_WITH_KEY = "Main Activity With Fragment Key"
        const val SUPPORT_FAQ = "FAQ Activity"
        const val SUPPORT_POLICY = "Policy Activity"
        const val TERMS_OF_SERVICE = "Terms of Service"
        const val SUPPORT_GET_HELP = "Get Help Activity"

        const val INVITE_FRIENDS_BY_SMS = "SMS Activity"
        const val INVITE_FRIENDS_BY_SHARE = "Share Activity"

        const val EMAIL_SUPPORT = "support@nynja.biz"

        const val OPEN_LINK = "Open Link"
        const val SHARE_LINK = "Share Link"
        const val SHARE_IMAGE = "Share Image"
        const val SHARE_VIDEO = "Share Video"
        const val SHARE_IMAGE_TYPE = "image/*"
        const val SHARE_VIDEO_TYPE = "video/*"

        const val GET_FILE = "Open File"
        const val VIEW_FILE = "View File activity"

        const val NOTIFICATION_CHANNEL_SETTINGS= "Notification Channel Settings"
        const val WALLET_PAYMENT = "Wallet Transfer Activity"
        const val URI_SCHEME = "package"
        const val APP_SETTINGS = "Navigate to application settings"

        const val ACTIVE_CALL = "Active Call Activity"
        const val INCOMMING_CALL = "Incomming Call Activity"
        const val MAIN_NEW_TASK = "Main New Task Activity"
        const val MAIN_NEW_CALL = "Main New Call Activity"
        const val MAIN_LEAVE_VOICE_MSG = "Main Leave Voice Msg Activity";
        const val LEAVE_VOICE_MESSAGE = "Leave Voice Msg Activity";

    }

    override fun applyCommand(command: Command?) {
        if (command is ForwardActivityForResult) {
            val activityIntent = createActivityIntent(command.screenKey, command.transitionData)

            // Start activity for result
            if (activityIntent != null) {
                try {
//                    if (!command.fromActivity) {
//                        waitingResultScreens.push(mNynjaViewManager.currentScreenKey)
//                    }
                    Utils.hideKeyboard(activity)
                    activity.startActivityForResult(activityIntent, command.requestCode)
                } catch (ex: ActivityNotFoundException) {
                    Timber.e(ex)
                }
                return
            }

        } else if (command is TransitionForward) {
            val activityIntent = createActivityIntent(command.screenKey, command.transitionData)

            // Start activity
            if (activityIntent != null) {
                try {
                    Utils.hideKeyboard(activity)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                            command.view, command.transitionName)

                    if (command.transitionData is Bundle) {
                        var bundle: Bundle = command.transitionData as Bundle
                        bundle.putAll(options.toBundle())
                        activity.startActivity(activityIntent, bundle)
                    } else {
                        activity.startActivity(activityIntent, options.toBundle())
                    }
                } catch (ex: ActivityNotFoundException) {
                    Timber.e(ex)
                }
                return
            }

        } else if (command is Forward) {
            val activityIntent = createActivityIntent(command.screenKey, command.transitionData)

            // Start activity
            if (activityIntent != null) {
                try {
                    Utils.hideKeyboard(activity)
                    if (command.transitionData is Bundle) {
                        activity.startActivity(activityIntent, command.transitionData as Bundle)
                    } else {
                        activity.startActivity(activityIntent)
                    }
                } catch (ex: ActivityNotFoundException) {
                    Timber.e(ex)
                }
                return
            }

        } else if (command is Replace) {
            val activityIntent = createActivityIntent(command.screenKey, command.transitionData)

            // Replace activity
            if (activityIntent != null) {
                Utils.hideKeyboard(activity)
                activity.finish()
                if (command.transitionData is Bundle) {
                    activity.startActivity(activityIntent, command.transitionData as Bundle)
                } else {
                    activity.startActivity(activityIntent)
                }
                return
            }
//        } else if (command is SendResult) run {
//            mNynjaViewManager.deliverResult(getLastWaitingResultScreenKey(),
//                    command.requestCode!!,
//                    command.resultCode,
//                    command.transitionData as Intent?)
        }

        // Use default fragments navigation
        super.applyCommand(command)
    }

    private fun getLastWaitingResultScreenKey(): String? {
        if (!waitingResultScreens.empty()) {
            return waitingResultScreens.pop()
        } else {
            return ""
        }
    }


    /**
     * Creates Intent to start Activity for `screenKey`.
     *
     *
     * **Warning:** This method does not work with [BackTo] command.
     *
     *
     * @param screenKey screen key
     * @param data      initialization data, can be null
     * @return intent to start Activity for the passed screen key
     */
    private fun createActivityIntent(screenKey: String, data: Any?): Intent? {
        return when (screenKey) {
            WELCOME -> WelcomeActivity.getLaunchIntent(activity)
            START -> RegistrationActivity.getLaunchIntent(activity)
            MAIN -> MainActivity.getLaunchIntent(activity)
            PICK_FILE -> getPickFileIntent()
            VIDEO_PLAY -> VideoPlayActivity.getLaunchIntent(activity, data as MessageModel)
            PREVIEW_IMAGE -> ImageViewerActivity.getLaunchIntent(activity, data as MessageModel?)
            PREVIEW_AVATAR -> ImageViewerActivity.getLaunchIntent(activity, data as String?)
            MESSAGES_REPLIED -> ReplyActivity.getLaunchIntent(activity, data as ReplyTransferData)
            MAIN_WITH_KEY -> MainActivity.getLaunchIntent(activity, data as String)
            SUPPORT_FAQ -> WebViewActivity.getLaunchIntent(activity, data as String)
            SUPPORT_POLICY -> WebViewActivity.getLaunchIntent(activity, data as String)
            TERMS_OF_SERVICE -> WebViewActivity.getLaunchIntent(activity, data as String)
            SUPPORT_GET_HELP -> getContactSupportIntent()
            INVITE_FRIENDS_BY_SMS -> getSMSIntent(data as String)
            INVITE_FRIENDS_BY_SHARE -> getInviteShareIntent()
            OPEN_LINK -> getLinkIntent(data as String)
            APP_SETTINGS -> openAppPermissionsSettings()
            SHARE_LINK -> shareLinkIntent(data as String)
            SHARE_IMAGE -> shareMessageContent(data as String, SHARE_IMAGE)
            SHARE_VIDEO -> shareMessageContent(data as String, SHARE_VIDEO)
            VIEW_FILE -> getViewFileIntent(data as String)
            WALLET_PAYMENT -> WalletPaymentActivity.getLaunchIntent(activity, data as ContactModel)
            GET_FILE -> getFileIntent()
            NOTIFICATION_CHANNEL_SETTINGS -> openNotificationChannelSettings(data as String)
            ACTIVE_CALL -> CallActivity.getLaunchIntent(activity)
            INCOMMING_CALL -> IncomeCallActivity.getLaunchIntent(activity)
            MAIN_NEW_TASK -> MainActivity.getLaunchNewTaskIntent(activity)
            MAIN_NEW_CALL -> MainActivity.getLaunchNewCallIntent(activity, data as Boolean)
            MAIN_LEAVE_VOICE_MSG -> MainActivity.getLaunchToLeaveVoiceMessageIntent(activity, data as LeaveVoiceMessageData)
            LEAVE_VOICE_MESSAGE -> LeaveVoiceMessageActivity.getLaunchIntent(activity, data as LeaveVoiceMessageData)
            else -> null
        }
    }

    override fun showSystemMessage(message: String?) {
        // Toast by default
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun exit() {
        // Finish by default
        activity.finish()
    }

    private fun getPickFileIntent(): Intent {
        val intent: Intent
        //TODO should be changed
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
        } else {
            intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else {
                Intent(Intent.ACTION_GET_CONTENT)
            }
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    private fun getViewFileIntent(localFilePath: String): Intent {
        val intent = Intent()
        try {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(localFilePath)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
            val uri = FileProvider.getUriForFile(activity, FileUtils.AUTHORITY_FILE_PROVIDER, File(localFilePath))
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
        return intent
    }

    private fun getContactSupportIntent(): Intent {
        val supportIntent = Intent(Intent.ACTION_SENDTO)
        supportIntent.type = "text/plain"
        supportIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_SUPPORT))
        supportIntent.data = Uri.parse("mailto:" + EMAIL_SUPPORT)
        supportIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return Intent.createChooser(supportIntent, activity.getString(R.string.get_help))
    }

    private fun getSMSIntent(recipients: String): Intent {
        val uri = Uri.parse("smsto:" + recipients)
        val smsIntent = Intent(Intent.ACTION_SENDTO, uri)
        smsIntent.putExtra("sms_body", activity.getString(R.string.contacts_invite_friends_share_text))
        return smsIntent
    }

    private fun getInviteShareIntent(): Intent {
        val inviteShareIntent = Intent(Intent.ACTION_SEND)
        inviteShareIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.contacts_invite_friends_share_text))
        inviteShareIntent.type = "text/plain"
        return inviteShareIntent
    }

    private fun getLinkIntent(url: String): Intent {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.packageName)
        return intent
    }

    private fun shareLinkIntent(url: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.type = "text/plain"
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(shareIntent, activity.getString(R.string.share_link))
    }

    private fun shareMessageContent(filePath: String, contentType: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        var shareTitle = ""
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        when (contentType) {
            SHARE_IMAGE -> {
                shareIntent.type = SHARE_IMAGE_TYPE
                shareTitle = activity.getString(R.string.share_photo)
            }
            SHARE_VIDEO -> {
                shareIntent.type = SHARE_VIDEO_TYPE
                shareTitle = activity.getString(R.string.share_video)
            }
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath))
        return Intent.createChooser(shareIntent, shareTitle)
    }

    private fun openNotificationChannelSettings(channel: String): Intent {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel)
        return intent
    }

    private fun openAppPermissionsSettings(): Intent {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts(URI_SCHEME, activity.packageName, null)
        settingsIntent.data = uri
        return settingsIntent
    }

    private fun getFileIntent(): Intent {
        val intent: Intent
        //TODO should be changed
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
            intent.putExtra("CONTENT_TYPE", "*/*")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            } else {
                intent = Intent(Intent.ACTION_GET_CONTENT)
            }
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        }
        return intent
    }
}
