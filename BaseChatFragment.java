package com.nynja.mobile.communicator.ui.fragments.chats;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.nynja.mobile.communicator.NynjaApp;
import com.nynja.mobile.communicator.data.NynjaKeyboardManager;
import com.nynja.mobile.communicator.data.sticker.StickerManager;
import com.nynja.mobile.communicator.interfaces.OnSoftKeyboardListener;
import com.nynja.mobile.communicator.mvp.presenters.ChatPresenter;
import com.nynja.mobile.communicator.mvp.view.BaseChatMvpView;
import com.nynja.mobile.communicator.ui.base.BaseActivity;
import com.nynja.mobile.communicator.ui.base.BaseFragment;
import com.nynja.mobile.communicator.ui.secondkeyboard.EmojiAndStickerKeyboardView;
import com.nynja.mobile.communicator.ui.secondkeyboard.OnEmojiAndStickerKeyboardViewListener;
import com.nynja.mobile.communicator.ui.secondkeyboard.SwitchKeyboardView;
import com.nynja.mobile.communicator.ui.views.ChatInputEditText;
import com.nynja.mobile.communicator.ui.views.SearchStickerResultView;
import com.nynja.mobile.communicator.utils.StringUtils;
import com.nynja.mobile.communicator.utils.container.EmojiManager;
import com.nynja.mobile.communicator.utils.container.data.Emoji;
import com.nynja.mobile.communicator.utils.container.data.model.Sticker;
import com.nynja.mobile.communicator.utils.container.data.packs.NynjaStickerPack;
import com.nynja.mobile.communicator.utils.container.listeners.OnSearchStickerEventListener;
import com.nynja.mobile.communicator.utils.container.sticker.view.StickerPreviewView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public abstract class BaseChatFragment extends BaseFragment implements BaseChatMvpView,
        OnEmojiAndStickerKeyboardViewListener {

    private final int KEYBOARD_TRANSITION_TIME = 300; //TODO time is different on different devices. Maybe try to find way how measure it

    @Nullable private ChatInputEditText mChatInputEditText;
    @Nullable private SwitchKeyboardView mKeyboardSwitchView;
    @Nullable private ViewGroup mSecondKeyboardContainer; //can be null for channel-subscriber in future
    @Nullable private EmojiAndStickerKeyboardView mEmojiAndStickerKeyboardView;
    @Nullable private SearchStickerResultView mSearchStickerResultView;
    @Nullable private StickerPreviewView mStickerPreviewView;

    private CompositeDisposable mInputsDisposable = new CompositeDisposable();

    private String mBeforeSearchTextSnapshot = "";
    private NynjaKeyboardManager mNynjaKeyboardManager;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNynjaKeyboardManager = NynjaApp.getComponent().nynjaKeyboardManager();
        mNynjaKeyboardManager.setAdjustResizeInputMode();

        //TODO replace to back-press navigation code
        if (getActivity() instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) getActivity();
            baseActivity.setOnBackPressedListener(this::onBackPressed);
            baseActivity.setOnSoftKeyboardListener(new OnSoftKeyboardListener() {
                @Override public void onKeyboardClose() {
                    if (mSearchStickerResultView != null && mSearchStickerResultView.getVisibility() == View.VISIBLE) {
                        if (getChatPresenter() != null)
                            getChatPresenter().setSearchStickerEnabled(false);
                        closeSearchStickerResultView();
                    }
                }

                @Override public void onKeyboardOpen(int keyBoardHeight) {

                }
            });
        }
    }

    public boolean onBackPressed() {
        if (mSearchStickerResultView != null && mSearchStickerResultView.getVisibility() == View.VISIBLE) {
            if (getChatPresenter() != null) getChatPresenter().setSearchStickerEnabled(false);
            closeSearchStickerResultView();
            return true;
        } else if (mEmojiAndStickerKeyboardView != null) {
            dismissEmojiAndStickerKeyboard();
            if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(false);
            requestHomeContainer();
            new Handler().postDelayed(() -> mNynjaKeyboardManager.setAdjustResizeInputMode(), KEYBOARD_TRANSITION_TIME);
            return true;
        }
        if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(false);
        return false;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reinitViews();
    }

    protected void reinitViews() {
        mInputsDisposable.clear();
        this.mChatInputEditText = registerInputTextView();
        this.mKeyboardSwitchView = registerKeyboardSwitchView();
        this.mSecondKeyboardContainer = registerSecondKeyboardContainer();
        this.mSearchStickerResultView = registerSearchStickerResultView();
        this.mStickerPreviewView = registerStickerPreviewView();
        initClickTypeListeners();
        initTextWatcherListeners();
        initSearchStickerResultViewListener();
    }


    @Override public void onKeyboardClose() {
        super.onKeyboardClose();
        if (getChatPresenter() == null) return;
        if (mSearchStickerResultView != null && mSearchStickerResultView.getVisibility() == View.VISIBLE) {
            getChatPresenter().setSearchStickerEnabled(false);
            closeSearchStickerResultView();
        }
    }

    /**
     * !!! ADD DISPOSABLE(S) LOGIC INCLUDE !!!
     * Set/Add onClick/onTouch/onLongClick/onFocusChange Listeners
     */
    private void initClickTypeListeners() {
        if (mChatInputEditText != null) {
            mInputsDisposable.add(RxView.clicks(mChatInputEditText).subscribe(o -> showKeyboard()));
            addDisposable(RxView.focusChanges(mChatInputEditText).skipInitialValue().subscribe(is -> {
                if (mKeyboardSwitchView == null) return;
                if (mKeyboardSwitchView.isKeyboardType() && is) showKeyboard();
            }, Timber::e));
        }

        if (mKeyboardSwitchView != null)
            mInputsDisposable.add(RxView.clicks(mKeyboardSwitchView).subscribe(o -> {
                if (mKeyboardSwitchView.isKeyboardType()) {
                    showKeyboard();
                } else {
                    showEmojiAndStickerKeyboard();
                }
            }, Timber::e));
    }

    private void initSearchStickerResultViewListener() {
        if (mSearchStickerResultView == null) return;
        mSearchStickerResultView.setOnSearchStickerEventListener(new OnSearchStickerEventListener() {

            @Override public void onStickerSelected(Sticker sticker) {
                sendSticker(sticker);
                closeSearchStickerResultView();
            }

            @Override public void onShowSticker(Sticker sticker) {
                if (mStickerPreviewView == null) return;
                mStickerPreviewView.showSticker(sticker, false);
            }
        });
    }

    /**
     * !!! ADD DISPOSABLE(S) LOGIC INCLUDE !!!
     * Set/Add Text-Change listener(s) of our main input-text view
     * Use for:
     * //TODO replace from child class -> mentions
     * search sticker
     * //TODO replace from child class -> typing-logic
     */
    private void initTextWatcherListeners() {
        if (mChatInputEditText != null) {
            mInputsDisposable.add(RxTextView.afterTextChangeEvents(mChatInputEditText)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(charSequence -> {
                        if (mSearchStickerResultView != null)
                            mSearchStickerResultView.callActionUp();
                        if (getChatPresenter() != null && getChatPresenter().isSearchStickerEnabled()) {
                            Emoji emoji = EmojiManager.getInstance().findEmoji(mChatInputEditText.getText());
                            if (emoji != null) {
                                searchByEmoji(emoji);
                                return;
                            }
                            searchByKeyword(mChatInputEditText.getText().toString());
                        } else {
                            //When in EditText introduced only one word and this word is emoji
                            String sequence = mChatInputEditText.getText().toString();
                            int spaceIndex = sequence.indexOf(" ");
                            Emoji emoji = EmojiManager.getInstance().findEmoji(mChatInputEditText.getText());
                            if (spaceIndex == -1 && !sequence.isEmpty() && emoji != null) {
                                searchByEmoji(emoji);
                            } else {
                                closeSearchStickerResultView();
                            }
                        }
                    }, Timber::e));
        }
    }

    /**
     * In Nynja Chat-Screen (p2p, Group, Self, Channel) we have only one input-text label
     * Keyboard work with this label in context on show/close
     */
    @Nullable
    abstract ChatInputEditText registerInputTextView();

    /**
     * In Nynja Chat-Screen (p2p, Group, Self, Channel) this view-button used to open/change-between Native(Device) Keyboard AND Emoji/Sticker Keyboard
     */
    @Nullable
    abstract SwitchKeyboardView registerKeyboardSwitchView();

    /**
     * In Nynja Chat-Screen (p2p, Group, Self, Channel) this view contains Emoji/Sticker Keyboard
     */
    @Nullable
    abstract ViewGroup registerSecondKeyboardContainer();

    /**
     * In Nynja Chat-Screen (p2p, Group, Self, Channel) this view user to show Sticker(s) by some user action(s)
     */
    @Nullable
    abstract SearchStickerResultView registerSearchStickerResultView();

    /**
     * This container will be used to show Sticker under User finger
     */
    @Nullable
    abstract StickerPreviewView registerStickerPreviewView();

    @Nullable
    abstract ChatPresenter getChatPresenter();

    /**
     * Show native(device) keyboard
     */
    protected void showKeyboard() {
        if (!mNynjaKeyboardManager.isAdjustResizeInputMode()) {
            if (mEmojiAndStickerKeyboardView == null) {
                mNynjaKeyboardManager.setAdjustResizeInputMode();
            }
        }
        if (!mNynjaKeyboardManager.isAdjustResizeInputMode()) {
            if (mKeyboardSwitchView != null) mKeyboardSwitchView.setClickable(false);
            new Handler().postDelayed(() -> {
                if (mKeyboardSwitchView != null) mKeyboardSwitchView.setClickable(true);
                dismissEmojiAndStickerKeyboard();
                requestHomeContainer();
                mNynjaKeyboardManager.setAdjustResizeInputMode();
            }, KEYBOARD_TRANSITION_TIME);
        }
        mNynjaKeyboardManager.showKeyboard(mChatInputEditText);
        if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(false);
    }

    /**
     * Show Emoji/Sticker keyboard
     */
    protected void showEmojiAndStickerKeyboard() {
        if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(true);
        if (mSearchStickerResultView != null && mSearchStickerResultView.getVisibility() == View.VISIBLE) {
            closeSearchStickerResultView();
        }
        if (mSecondKeyboardContainer != null && mEmojiAndStickerKeyboardView == null) {
            mEmojiAndStickerKeyboardView = mNynjaKeyboardManager.createSecondKeyboardView(getContext(), this);
            mEmojiAndStickerKeyboardView.setOnEmojiAndStickerKeyboardViewListener(this);
            mSecondKeyboardContainer.addView(mEmojiAndStickerKeyboardView);
            mNynjaKeyboardManager.setAdjustNothingInputMode();
        }
        requestHomeContainer();
        dismissKeyboard();
    }

    /**
     * Show native(device) keyboard
     */
    protected void dismissKeyboard() {
        mNynjaKeyboardManager.closeKeyboard(mChatInputEditText);
    }

    /**
     * Close native(device) keyboard
     */
    protected void dismissEmojiAndStickerKeyboard() {
        if (mSecondKeyboardContainer == null || mEmojiAndStickerKeyboardView == null) return;
        mSecondKeyboardContainer.removeView(mEmojiAndStickerKeyboardView);
        mSecondKeyboardContainer.invalidate();
        mEmojiAndStickerKeyboardView = null;
    }

    /**
     * Close native(device) keyboard and Emoji/Sticker keyboard
     */
    protected void dismissBothKeyboards() {
        closeSearchStickerResultView();
        requestHomeContainer();
        dismissEmojiAndStickerKeyboard();
        mNynjaKeyboardManager.closeKeyboard(mChatInputEditText);
        mNynjaKeyboardManager.setAdjustResizeInputMode();
    }

    protected void sendSticker(Sticker sticker) {
        //TODO override by extends classes
    }

    @Override public void refreshUserRecentStickers(List<Sticker> stickers) {
        if (mEmojiAndStickerKeyboardView != null)
            mEmojiAndStickerKeyboardView.refreshUserRecentStickers(stickers);
    }

    private void searchByEmoji(Emoji emoji) {
        if (mSearchStickerResultView == null || getChatPresenter() == null) return;
        if (emoji == null) return;
        List<Sticker> result = getChatPresenter().findStickers(emoji);
        if (!result.isEmpty()) {
            mSearchStickerResultView.setVisibility(View.VISIBLE);
            mSearchStickerResultView.setFilteredContent(result);
            mSearchStickerResultView.showNotFoundTv(false);
        } else {
            mSearchStickerResultView.setVisibility(View.GONE);
            mSearchStickerResultView.setFilteredContent(new ArrayList<>());
            mSearchStickerResultView.showNotFoundTv(true);
        }
    }

    private void searchByKeyword(String currentText) {
        if (mSearchStickerResultView == null || getChatPresenter() == null) return;
        if (currentText.length() < mBeforeSearchTextSnapshot.length()) {
            closeSearchStickerResultView();
            return;
        }
        if (!currentText.substring(0, mBeforeSearchTextSnapshot.length()).equals(mBeforeSearchTextSnapshot)) {
            closeSearchStickerResultView();
            return;
        }
        String searchText = currentText.substring(mBeforeSearchTextSnapshot.length());
        List<Sticker> result = getChatPresenter().findStickers(searchText);
        if (result != null) mSearchStickerResultView.setFilteredContent(result);
        if (result != null && result.size() == 0 && StringUtils.isNotEmpty(searchText)) {
            mSearchStickerResultView.showNotFoundTv(true);
        } else {
            mSearchStickerResultView.showNotFoundTv(false);
        }
    }

    private void showSearchStickerResultView() {
        if (mSearchStickerResultView == null || getChatPresenter() == null) return;
        mSearchStickerResultView.setVisibility(View.VISIBLE);
        mSearchStickerResultView.setSearchContent(StickerManager.extrudeStickersFromStickerPacks(getChatPresenter().getUserStickerPacks()));
        if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(false);
        showKeyboard();
    }

    private void closeSearchStickerResultView() {
        if (mSearchStickerResultView == null || getChatPresenter() == null) return;
        mSearchStickerResultView.setVisibility(View.GONE);
        getChatPresenter().setSearchStickerEnabled(false);
    }

    @Override public void onOpenWheelView() {
        super.onOpenWheelView();
        dismissBothKeyboards();
    }

    @Override public List<NynjaStickerPack> getAvailableStickersPacks() {
        if (getChatPresenter() == null) return new ArrayList<>();
        return getChatPresenter().getUserStickerPacks();
    }

    @Override public List<Sticker> getRecentStickers() {
        if (getChatPresenter() == null) return new ArrayList<>();
        return getChatPresenter().getUserRecentStickers();
    }

    @Override public void onStickerClick(@NonNull Sticker sticker) {
        sendSticker(sticker);
    }

    @Override
    public void onStickerPreview(@NonNull Sticker sticker, boolean isShowStickerInfo) {
        if (mStickerPreviewView == null) return;
        mStickerPreviewView.showSticker(sticker, isShowStickerInfo);
    }

    @Override public void onStartStickerSearch() {
        if (getChatPresenter() == null) return;
        showKeyboard();
        if (mKeyboardSwitchView != null) mKeyboardSwitchView.setType(false);
        getChatPresenter().setSearchStickerEnabled(true);
        showSearchStickerResultView();
    }

    @Override public void onBackspaceClick() {
        if (mChatInputEditText != null) mChatInputEditText.backspace();
    }

    @Override
    public void onEmojiClick(@NonNull Emoji emoji) {
        if (mChatInputEditText != null) mChatInputEditText.input(emoji);
    }

    @Override public void onDestroy() {
        dismissEmojiAndStickerKeyboard();
        super.onDestroy();
    }
}
