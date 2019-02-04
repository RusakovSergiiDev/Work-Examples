package com.nynja.mobile.communicator.ui.chat.views.base;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nynja.mobile.communicator.R;
import com.nynja.mobile.communicator.data.models.nynjamodels.MessageModel;
import com.nynja.mobile.communicator.interfaces.OnChatItemLongClickListener;
import com.nynja.mobile.communicator.ui.chat.listeners.OnNynjaChatEventListener;
import com.nynja.mobile.communicator.ui.chat.views.ChatBubbleViewHelper;
import com.nynja.mobile.communicator.ui.chat.views.downloadupload.ChatDownloadUploadButton;
import com.nynja.mobile.communicator.ui.chat.interfaces.IChatBubbleView;
import com.nynja.mobile.communicator.ui.chat.interfaces.IChatHolderView;
import com.nynja.mobile.communicator.ui.views.DeliveryStatusImageView;

public abstract class ChatBaseHolderView<T extends IChatBubbleView> extends FrameLayout implements
        IChatHolderView {

    protected final int STAR_VIEW_SIZE = (int) getContext().getResources().getDimension(R.dimen.chat_star_icon_size);
    protected final int DOWNLOAD_UPLOAD_BUTTON_SIZE = (int) getContext().getResources().getDimension(R.dimen.chat_download_upload_button_size);

    protected boolean mIsMyMessage;
    protected boolean mIsGroupMessage;

    protected T mChatBubbleView;
    //Star
    protected AppCompatImageView mStarView;
    protected FrameLayout.LayoutParams mStarViewLp;
    //Download/Upload
    protected ChatDownloadUploadButton mChatDownloadUploadButton;
    protected FrameLayout.LayoutParams mChatDownloadUploadButtonLp;

    private int mCurrentBubbleWidth = 0;
    private int mCurrentBubbleHeight = 0;

    private int mCurrentHeaderHeight = 0;
    private int mCurrentReplyOrForwardHeight = 0;
    private int mCurrentMainViewHeight = 0;
    private int mCurrentDownloadUploadBtnMarginTop = 0;

    protected OnChatItemLongClickListener mOnChatItemLongClickListener;
    protected OnNynjaChatEventListener mOnNynjaChatEventListener;

    public ChatBaseHolderView(@NonNull Context context, boolean isMyMessage, boolean isGroupMessage) {
        super(context);
        this.mIsMyMessage = isMyMessage;
        this.mIsGroupMessage = isGroupMessage;
        setupUI();

        mChatBubbleView = getBubbleViewInstance();
        addView((View) mChatBubbleView);
    }

    private void setupUI() {
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(lp);
    }

    protected abstract T getBubbleViewInstance();

    @Override
    public void setMessageState(ChatBubbleViewHelper.ChatBubbleState state, MessageModel originMessage, MessageModel replyMessage) {
        if (mChatBubbleView != null)
            mChatBubbleView.setMessageState(state, originMessage, replyMessage);
    }

    @Override
    public void setMessageStar(boolean isStar) {
        if (isStar) {
            if (mStarView == null) {
                mStarView = new AppCompatImageView(getContext());
                mStarView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.v_stared));
                mStarViewLp = new FrameLayout.LayoutParams(STAR_VIEW_SIZE, STAR_VIEW_SIZE);
                addView(mStarView, mStarViewLp);
            }
            refreshStarPosition();
        } else {
            if (mStarView == null) {
                mStarViewLp = null;
                return;
            }
            removeView(mStarView);
            mStarView = null;
            mStarViewLp = null;
        }
    }

    @Override
    public void setMessageTime(String time) {
        if (mChatBubbleView != null) mChatBubbleView.setMessageTime(time);
    }

    @Override public void setMessageDeliveryStatus(DeliveryStatusImageView.State deliveryStatus) {
        if (mChatBubbleView != null) mChatBubbleView.setMessageDeliveryStatus(deliveryStatus);
    }

    @Override
    public void setMessageIncomingUserInfo(String avatar, String userName) {
        if (mChatBubbleView != null) mChatBubbleView.setMessageIncomingUserInfo(avatar, userName);
    }

    @Override public void setListener(OnNynjaChatEventListener onNynjaChatEventListener) {
        this.mOnNynjaChatEventListener = onNynjaChatEventListener;
        if (mChatBubbleView != null) mChatBubbleView.setListener(mOnNynjaChatEventListener);
    }

    @Override public void onChatBubbleSizeChanged(int width, int height) {
        this.mCurrentBubbleWidth = width;
        this.mCurrentBubbleHeight = height;
        refreshStarPosition();
        refreshDownloadUploadBtnPosition();
    }

    @Override public void onChatMainViewSizeChanged(int width, int height) {
        this.mCurrentMainViewHeight = height;
        refreshDownloadUploadBtnPosition();
    }

    //Download\Upload
    public void setupDownloadUploadBtnUI() {
        if (mChatDownloadUploadButton == null) {
            mChatDownloadUploadButton = new ChatDownloadUploadButton(getContext());
            mChatDownloadUploadButtonLp = new FrameLayout.LayoutParams(DOWNLOAD_UPLOAD_BUTTON_SIZE, DOWNLOAD_UPLOAD_BUTTON_SIZE);
            addView(mChatDownloadUploadButton, mChatDownloadUploadButtonLp);
        }
        mChatDownloadUploadButton.setOnClickListener(view -> {
            if (mOnNynjaChatEventListener != null)
                mOnNynjaChatEventListener.onDownloadUploadBtnClick();
        });
        refreshDownloadUploadBtnPosition();
    }

    public void setTransferState(ChatDownloadUploadButton.ChatDownloadUploadButtonState state) {
        if (mChatDownloadUploadButton != null) mChatDownloadUploadButton.setState(state);
    }

    private void refreshDownloadUploadBtnPosition() {
        if (mChatDownloadUploadButtonLp == null) return;

        this.mCurrentHeaderHeight = mChatBubbleView.getHeaderHeight();
        this.mCurrentReplyOrForwardHeight = mChatBubbleView.getReplyOrForwardHeight();
        int[] chatBubbleMargins = mChatBubbleView.getChatBubbleMargins();
        mCurrentDownloadUploadBtnMarginTop = chatBubbleMargins[1] + mCurrentHeaderHeight + mCurrentReplyOrForwardHeight + mCurrentMainViewHeight / 2 - DOWNLOAD_UPLOAD_BUTTON_SIZE / 2;

        int bubbleWidth = ((View) mChatBubbleView).getWidth();

        if (mIsMyMessage) {
            int marginRight = chatBubbleMargins[2] + bubbleWidth + (int) getContext().getResources().getDimension(R.dimen.margin_normal);
            mChatDownloadUploadButtonLp.gravity = Gravity.END;
            mChatDownloadUploadButtonLp.setMargins(0, mCurrentDownloadUploadBtnMarginTop, marginRight, 0);
            mChatDownloadUploadButton.setLayoutParams(mChatDownloadUploadButtonLp);
        } else {
            int marginLeft = chatBubbleMargins[0] + bubbleWidth + (int) getContext().getResources().getDimension(R.dimen.margin_normal);
            mChatDownloadUploadButtonLp.gravity = Gravity.START;
            mChatDownloadUploadButtonLp.setMargins(marginLeft, mCurrentDownloadUploadBtnMarginTop, 0, 0);
            mChatDownloadUploadButton.setLayoutParams(mChatDownloadUploadButtonLp);
        }
    }

    private void refreshStarPosition() {
        if (mStarViewLp == null) return;
        if (mIsMyMessage) {
            mStarViewLp.gravity = Gravity.END;
            int marginRight = mChatBubbleView.getChatBubbleMargins()[2] - STAR_VIEW_SIZE / 2;
            mStarViewLp.setMargins(0, 0, marginRight, 0);
        } else {
            mStarViewLp.gravity = Gravity.START;
            int marginLeft = mChatBubbleView.getChatBubbleMargins()[0] + mCurrentBubbleWidth - STAR_VIEW_SIZE / 2;
            mStarViewLp.setMargins(marginLeft, 0, 0, 0);
        }
    }
}
