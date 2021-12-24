/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.ltt.android.LttrsApplication;
import rs.ltt.android.R;
import rs.ltt.android.databinding.ActivityComposeBinding;
import rs.ltt.android.databinding.ItemAttachmentBinding;
import rs.ltt.android.ui.ChipDrawableSpan;
import rs.ltt.android.ui.ComposeAction;
import rs.ltt.android.ui.MaterialAlertDialogs;
import rs.ltt.android.ui.ViewIntent;
import rs.ltt.android.ui.model.ComposeViewModel;
import rs.ltt.android.ui.model.ComposeViewModel.EncryptionOptions;
import rs.ltt.android.ui.model.ComposeViewModel.UserEncryptionChoice;
import rs.ltt.android.util.Event;
import rs.ltt.android.util.MediaTypes;
import rs.ltt.autocrypt.client.Decision;
import rs.ltt.jmap.common.entity.Attachment;
import rs.ltt.jmap.mua.util.MailToUri;

// TODO handle save instance state
public class ComposeActivity extends AppCompatActivity {

    public static final String EDITING_TASK_ID_EXTRA = "work_request_id";
    public static final String DISCARDED_THREAD_EXTRA = "discarded_thread";
    private static final Logger LOGGER = LoggerFactory.getLogger(ComposeActivity.class);
    private static final String ACCOUNT_EXTRA = "account";
    private static final String COMPOSE_ACTION_EXTRA = "compose_action";
    private static final String EMAIL_ID_EXTRA = "email_id";
    private ActivityComposeBinding binding;
    private ComposeViewModel composeViewModel;
    private ActivityResultLauncher<String> getAttachmentLauncher;

    public static Bundle compose() {
        return launch(null, null, ComposeAction.NEW);
    }

    public static Bundle editDraft(Long account, final String emailId) {
        return launch(account, emailId, ComposeAction.EDIT_DRAFT);
    }

    public static Bundle replyAll(Long account, final String emailId) {
        return launch(account, emailId, ComposeAction.REPLY_ALL);
    }

    public static Bundle launch(
            final Long account, final String emailId, final ComposeAction action) {
        Preconditions.checkNotNull(action);
        final Bundle extras = new Bundle();
        if (account != null) {
            LOGGER.info("launching for account {} with {}", account, action);
            extras.putLong(ACCOUNT_EXTRA, account);
        } else {
            LOGGER.info("launching with {}", action);
        }
        extras.putString(COMPOSE_ACTION_EXTRA, action.toString());
        if (emailId != null) {
            extras.putString(EMAIL_ID_EXTRA, emailId);
        }
        return extras;
    }

    public static void launch(final AppCompatActivity activity, final Uri uri) {
        final Intent nextIntent = new Intent(activity, ComposeActivity.class);
        nextIntent.setAction(Intent.ACTION_VIEW);
        nextIntent.setData(uri);
    }

    private static MailToUri getUri(@NonNull final Intent intent) {
        final Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        try {
            return MailToUri.get(data.toString());
        } catch (final IllegalArgumentException e) {
            LOGGER.warn(
                    "activity was called with invalid URI {}. {}", data.toString(), e.getMessage());
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LttrsApplication.get(this).noAccountsConfigured()) {
            redirectToSetupActivity();
            finishAffinity();
            return;
        }

        this.getAttachmentLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(), this::addAttachment);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_compose);

        setupActionBar();

        final ViewModelProvider viewModelProvider =
                new ViewModelProvider(
                        this,
                        new ComposeViewModel.Factory(
                                getApplication(), getViewModelParameter(savedInstanceState)));
        composeViewModel = viewModelProvider.get(ComposeViewModel.class);

        composeViewModel.getErrorMessage().observe(this, this::onErrorMessage);
        composeViewModel.getDownloadErrorEvent().observe(this, this::onErrorMessage);
        composeViewModel.getAttachments().observe(this, this::onAttachmentsUpdated);
        composeViewModel.getViewIntentEvent().observe(this, this::onViewIntentEvent);

        binding.setComposeViewModel(composeViewModel);
        binding.setLifecycleOwner(this);

        binding.to.addTextChangedListener(new ChipTextWatcher(binding.to));
        binding.to.setOnFocusChangeListener(this::focusOnRecipientFieldChanged);

        binding.cc.addTextChangedListener(new ChipTextWatcher(binding.cc));
        binding.cc.setOnFocusChangeListener(this::focusOnRecipientFieldChanged);

        binding.moreAddresses.setOnClickListener((v -> composeViewModel.showExtendedAddresses()));

        binding.subject.setOnFocusChangeListener(this::focusOnBodyOrSubject);
        binding.body.setOnFocusChangeListener(this::focusOnBodyOrSubject);

        binding.toLabel.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.to));
        binding.ccLabel.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.cc));
        binding.placeholder.setOnClickListener(v -> requestFocusAndOpenKeyboard(binding.body));

        composeViewModel.getEncryptionOptions().observe(this, this::onEncryptionOptionChanged);

        // TODO once we handle instance state ourselves we need to call ChipDrawableSpan.reset() on
        // `to`
    }

    private void onEncryptionOptionChanged(ComposeViewModel.EncryptionOptions encryptionOptions) {
        LOGGER.info("onEncryptionOptionChanged({})", encryptionOptions);
        this.invalidateOptionsMenu();
    }

    private void onErrorMessage(final Event<String> event) {
        MaterialAlertDialogs.error(this, event);
    }

    private void onViewIntentEvent(final Event<ViewIntent> viewIntentEvent) {
        if (viewIntentEvent.isConsumable()) {
            viewIntentEvent.consume().launch(this);
        }
    }

    private void onAttachmentsUpdated(final List<? extends Attachment> attachments) {
        if (attachments.isEmpty()) {
            binding.attachments.setVisibility(View.GONE);
        } else {
            binding.attachments.setVisibility(View.VISIBLE);
            binding.attachments.removeAllViews();
            for (final Attachment attachment : attachments) {
                binding.attachments.addView(renderAttachment(attachment));
            }
        }
    }

    private View renderAttachment(final Attachment attachment) {
        final ItemAttachmentBinding attachmentBinding =
                ItemAttachmentBinding.inflate(getLayoutInflater(), binding.attachments, false);
        attachmentBinding.setAttachment(attachment);
        attachmentBinding.action.setImageResource(R.drawable.ic_baseline_close_24);
        attachmentBinding.action.setOnClickListener((v) -> deleteAttachment(attachment));
        attachmentBinding.getRoot().setOnClickListener((v -> composeViewModel.open(attachment)));
        // TODO wire up 'open/view'
        return attachmentBinding.getRoot();
    }

    private void deleteAttachment(final Attachment attachment) {
        composeViewModel.deleteAttachment(attachment);
    }

    private void addAttachment(final Uri uri) {
        if (uri == null) {
            LOGGER.warn("addAttachment called with null uri");
            return;
        }
        composeViewModel.addAttachment(uri);
    }

    private void redirectToSetupActivity() {
        final Intent currentIntent = getIntent();
        final Uri data = currentIntent == null ? null : currentIntent.getData();
        final MailToUri uri = data == null ? null : MailToUri.parse(data.toString());
        final Intent nextIntent = new Intent(this, SetupActivity.class);
        if (uri != null) {
            nextIntent.putExtra(SetupActivity.EXTRA_NEXT_ACTION, data.toString());
        }
        startActivity(nextIntent);
    }

    private ComposeViewModel.Parameter getViewModelParameter(final Bundle savedInstanceState) {
        final boolean freshStart = savedInstanceState == null || savedInstanceState.isEmpty();
        final Intent i = getIntent();
        final MailToUri uri = i != null ? getUri(i) : null;
        if (uri != null) {
            return new ComposeViewModel.Parameter(uri, freshStart);
        }
        final Long account;
        if (i != null && i.hasExtra(ACCOUNT_EXTRA)) {
            account = i.getLongExtra(ACCOUNT_EXTRA, 0L);
        } else {
            account = null;
        }
        final ComposeAction action =
                ComposeAction.of(i == null ? null : i.getStringExtra(COMPOSE_ACTION_EXTRA));
        final String emailId = i == null ? null : i.getStringExtra(EMAIL_ID_EXTRA);
        return new ComposeViewModel.Parameter(account, freshStart, action, emailId);
    }

    private void focusOnRecipientFieldChanged(final View view, final boolean hasFocus) {
        if (view instanceof TextView) {
            final TextView textView = (TextView) view;
            LOGGER.info("hasFocus({})", hasFocus);
            ChipDrawableSpan.apply(this, textView.getEditableText(), hasFocus);
            return;
        }
        throw new IllegalArgumentException("View parameter is not of type TextView");
    }

    private void focusOnBodyOrSubject(final View view, final boolean hasFocus) {
        if (hasFocus) {
            composeViewModel.suggestHideExtendedAddresses();
        }
    }

    private void requestFocusAndOpenKeyboard(AppCompatEditText editText) {
        editText.requestFocus();
        final InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.activity_compose, menu);
        prepareEncryptionMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void prepareEncryptionMenu(@NonNull final Menu menu) {
        final MenuItem encryptionOptionsMenuItem = menu.findItem(R.id.action_encryption_options);
        final MenuItem cleartextMenuItem = menu.findItem(R.id.encryption_option_clear_text);
        final MenuItem encryptedMenuItem = menu.findItem(R.id.encryption_option_encrypted);
        final EncryptionOptions encryptionOptions =
                EncryptionOptions.of(composeViewModel.getEncryptionOptions());
        final UserEncryptionChoice choice = encryptionOptions.userEncryptionChoice;
        final Decision decision = encryptionOptions.decision;
        if (encryptionOptions.decision == Decision.DISABLE) {
            encryptionOptionsMenuItem.setVisible(false);
        } else {
            encryptionOptionsMenuItem.setVisible(true);
            if (encryptionOptions.decision == Decision.DISCOURAGE) {
                encryptionOptionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            } else {
                encryptionOptionsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
        final boolean encrypted;
        if (choice == UserEncryptionChoice.NONE) {
            encrypted = decision == Decision.ENCRYPT;
        } else {
            encrypted = choice == UserEncryptionChoice.ENCRYPTED;
        }
        if (encrypted) {
            encryptionOptionsMenuItem.setIcon(R.drawable.ic_lock_white_24dp);
            encryptedMenuItem.setChecked(true);
        } else {
            encryptionOptionsMenuItem.setIcon(R.drawable.ic_lock_open_white_24dp);
            cleartextMenuItem.setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveDraft();
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.action_send) {
            send();
            return true;
        } else if (itemId == R.id.action_discard) {
            discardDraft();
            return true;
        } else if (itemId == R.id.action_attach_file) {
            this.getAttachmentLauncher.launch(MediaTypes.toString(MediaType.ANY_TYPE));
            return true;
        } else if (itemId == R.id.encryption_option_clear_text) {
            composeViewModel.setUserEncryptionChoice(UserEncryptionChoice.CLEARTEXT);
            return true;
        } else if (itemId == R.id.encryption_option_encrypted) {
            composeViewModel.setUserEncryptionChoice(UserEncryptionChoice.ENCRYPTED);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        saveDraft();
        super.onBackPressed();
    }

    private void send() {
        try {
            final UUID workInfoId = composeViewModel.send();
            setResultIntent(workInfoId);
            finish();
        } catch (final IllegalStateException e) {
            LOGGER.debug("Aborting send", e);
        }
    }

    private void saveDraft() {
        final UUID uuid = composeViewModel == null ? null : composeViewModel.saveDraft();
        if (uuid != null) {
            LOGGER.info("Storing draft saving worker task uuid");
            setResultIntent(uuid);
        }
    }

    private void setResultIntent(final UUID workInfoId) {
        final Intent intent = new Intent();
        intent.putExtra(EDITING_TASK_ID_EXTRA, workInfoId);
        setResult(RESULT_OK, intent);
    }

    private void discardDraft() {
        final boolean isOnlyEmailInThread = composeViewModel.discard();
        Intent intent = new Intent();
        intent.putExtra(DISCARDED_THREAD_EXTRA, isOnlyEmailInThread);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onDestroy() {
        if (isFinishing() && composeViewModel != null) {
            composeViewModel.saveDraft();
        }
        super.onDestroy();
    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        final ActionBar actionbar = requireActionBar();
        final boolean displayUpButton = !isTaskRoot();
        actionbar.setHomeButtonEnabled(displayUpButton);
        actionbar.setDisplayHomeAsUpEnabled(displayUpButton);
    }

    private @NonNull ActionBar requireActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new IllegalStateException("No ActionBar found");
        }
        return actionBar;
    }

    private static class ChipTextWatcher implements TextWatcher {

        private final EditText editText;

        private ChipTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable editable) {
            ChipDrawableSpan.apply(editText.getContext(), editable, editText.hasFocus());
        }
    }
}
