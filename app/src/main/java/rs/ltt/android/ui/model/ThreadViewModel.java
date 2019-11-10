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

package rs.ltt.android.ui.model;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.PagedList;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import rs.ltt.android.entity.ExpandedPosition;
import rs.ltt.android.entity.FullEmail;
import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.ThreadHeader;
import rs.ltt.android.repository.ThreadRepository;
import rs.ltt.android.util.CombinedListsLiveData;
import rs.ltt.jmap.common.entity.Role;

public class ThreadViewModel extends AndroidViewModel {

    public final AtomicBoolean jumpedToFirstUnread = new AtomicBoolean(false);
    public final ListenableFuture<List<ExpandedPosition>> expandedPositions;
    public final HashSet<String> expandedItems = new HashSet<>();
    private final String threadId;
    private final String label;
    private final ThreadRepository threadRepository;
    private LiveData<PagedList<FullEmail>> emails;
    private LiveData<ThreadHeader> header;
    private LiveData<List<MailboxWithRoleAndName>> mailboxes;
    private LiveData<MenuConfiguration> menuConfiguration;


    ThreadViewModel(@NonNull Application application, String threadId, String label) {
        super(application);
        this.threadId = threadId;
        this.label = label;
        this.threadRepository = new ThreadRepository(application);
        this.header = this.threadRepository.getThreadHeader(threadId);
        this.emails = this.threadRepository.getEmails(threadId);
        this.mailboxes = this.threadRepository.getMailboxes(threadId);
        this.expandedPositions = this.threadRepository.getExpandedPositions(threadId);
        this.expandedPositions.addListener(() -> this.threadRepository.markRead(threadId), MoreExecutors.directExecutor());

        LiveData<List<MailboxOverwriteEntity>> overwriteEntityLiveData = this.threadRepository.getMailboxOverwrites(threadId);

        CombinedListsLiveData<MailboxOverwriteEntity, MailboxWithRoleAndName> combined = new CombinedListsLiveData<>(overwriteEntityLiveData, mailboxes);

        this.menuConfiguration = Transformations.map(combined, pair -> {
            List<MailboxOverwriteEntity> overwrites = pair.first;
            List<MailboxWithRoleAndName> list = pair.second;

            Log.d("lttrs", "num mailbox overwrites = " + overwrites.size());

            boolean wasPutInArchiveOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.ARCHIVE);
            boolean wasPutInTrashOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.TRASH);
            boolean wasPutInInboxOverwrite = MailboxOverwriteEntity.hasOverwrite(overwrites, Role.INBOX);
            final MailboxOverwriteEntity importantOverwrite = MailboxOverwriteEntity.find(overwrites, Role.IMPORTANT);

            final boolean removeLabel = MailboxWithRoleAndName.isAnyOfLabel(list, this.label);
            final boolean archive = !removeLabel && (MailboxWithRoleAndName.isAnyOfRole(list, Role.INBOX) || wasPutInInboxOverwrite) && !wasPutInArchiveOverwrite && !wasPutInTrashOverwrite;
            final boolean moveToInbox = (MailboxWithRoleAndName.isAnyOfRole(list, Role.ARCHIVE) || MailboxWithRoleAndName.isAnyOfRole(list, Role.TRASH) || wasPutInArchiveOverwrite || wasPutInTrashOverwrite) && !wasPutInInboxOverwrite;
            final boolean moveToTrash = (MailboxWithRoleAndName.isAnyNotOfRole(list, Role.TRASH) || wasPutInInboxOverwrite) && !wasPutInTrashOverwrite;

            final boolean markedAsImportant = importantOverwrite != null ? importantOverwrite.value : MailboxWithRoleAndName.isAnyOfRole(list, Role.IMPORTANT);

            return new MenuConfiguration(archive,
                    removeLabel,
                    moveToInbox,
                    moveToTrash,
                    !markedAsImportant,
                    markedAsImportant
            );
        });
    }

    public LiveData<PagedList<FullEmail>> getEmails() {
        return emails;
    }

    public LiveData<ThreadHeader> getHeader() {
        return this.header;
    }

    public LiveData<MenuConfiguration> getMenuConfiguration() {
        return menuConfiguration;
    }

    public void toggleFlagged(String threadId, boolean target) {
        threadRepository.toggleFlagged(threadId, target);
    }

    public void markUnread() {
        threadRepository.markUnRead(threadId);
    }

    public String getLabel() {
        return this.label;
    }

    public void archive() {
        this.threadRepository.archive(this.threadId);
    }

    public void removeLabel() {
        final List<MailboxWithRoleAndName> mailboxes = this.mailboxes.getValue();
        final MailboxWithRoleAndName mailbox = mailboxes == null ? null : MailboxWithRoleAndName.findByLabel(mailboxes, this.label);
        if (mailbox == null) {
            throw new IllegalStateException("No mailbox found with the label " + this.label);
        }
        this.threadRepository.removeFromMailbox(this.threadId, mailbox);
    }

    public void moveToTrash() {
        this.threadRepository.moveToTrash(this.threadId);
    }

    public void moveToInbox() {
        this.threadRepository.moveToInbox(this.threadId);
    }

    public void markImportant() {
        this.threadRepository.markImportant(this.threadId);
    }

    public void markNotImportant() {
        this.threadRepository.markNotImportant(this.threadId);
    }

    public static class MenuConfiguration {
        public final boolean archive;
        public final boolean removeLabel;
        public final boolean moveToInbox;
        public final boolean moveToTrash;
        public final boolean markImportant;
        public final boolean markNotImportant;

        public MenuConfiguration(boolean archive, boolean removeLabel, boolean moveToInbox, boolean moveToTrash, boolean markImportant, boolean markNotImportant) {
            this.archive = archive;
            this.removeLabel = removeLabel;
            this.moveToInbox = moveToInbox;
            this.moveToTrash = moveToTrash;
            this.markImportant = markImportant;
            this.markNotImportant = markNotImportant;
        }

        public static MenuConfiguration none() {
            return new MenuConfiguration(false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }
}
