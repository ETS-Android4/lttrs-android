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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.common.util.concurrent.ListenableFuture;

import rs.ltt.android.entity.MailboxOverwriteEntity;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.MailboxUtil;

public class SearchQueryViewModel extends AbstractQueryViewModel {

    private final String searchTerm;
    private final LiveData<EmailQuery> searchQueryLiveData;
    private final ListenableFuture<MailboxWithRoleAndName> inbox;

    SearchQueryViewModel(final Application application, final String searchTerm) {
        super(application);
        this.searchTerm = searchTerm;
        this.inbox = queryRepository.getInbox();
        this.searchQueryLiveData = Transformations.map(queryRepository.getTrashAndJunk(), trashAndJunk -> EmailQuery.of(
                EmailFilterCondition.builder().text(searchTerm).inMailboxOtherThan(trashAndJunk).build(),
                true
        ));
        init();
    }

    public LiveData<String> getSearchTerm() {
        return new MutableLiveData<>(searchTerm);
    }

    @Override
    protected LiveData<EmailQuery> getQuery() {
        return searchQueryLiveData;
    }

    public boolean isInInbox(ThreadOverviewItem item) {
        if (MailboxOverwriteEntity.hasOverwrite(item.mailboxOverwriteEntities, Role.ARCHIVE)) {
            return false;
        }
        if (MailboxOverwriteEntity.hasOverwrite(item.mailboxOverwriteEntities, Role.INBOX)) {
            return true;
        }
        MailboxWithRoleAndName inbox = getInbox();
        if (inbox == null) {
            return false;
        }
        return MailboxUtil.anyIn(item.emails, inbox.id);
    }

    private MailboxWithRoleAndName getInbox() {
        try {
            return this.inbox.get();
        } catch (Exception e) {
            return null;
        }
    }
}
