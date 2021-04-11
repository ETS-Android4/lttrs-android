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

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.OneTimeWorkRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.worker.MailboxQueryRefreshWorker;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class MailboxQueryViewModel extends AbstractQueryViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxQueryViewModel.class);


    private final LiveData<MailboxOverviewItem> mailbox;
    private final String mailboxId;

    private final LiveData<EmailQuery> emailQueryLiveData;

    MailboxQueryViewModel(final Application application, final long accountId, final String mailboxId) {
        super(application, accountId);
        this.mailboxId = mailboxId;
        this.mailbox = this.queryRepository.getMailboxOverviewItem(mailboxId);
        this.emailQueryLiveData = Transformations.map(mailbox, mailbox -> {
            if (mailbox == null) {
                return EmailQuery.unfiltered(true);
            } else {
                return StandardQueries.mailbox(mailbox);
            }
        });
        init();
    }

    public LiveData<MailboxOverviewItem> getMailbox() {
        return mailbox;
    }

    @Override
    protected OneTimeWorkRequest getRefreshWorkRequest() {
        LOGGER.info("building OneTimeWorkRequest with mailboxId={}", mailboxId);
        return new OneTimeWorkRequest.Builder(MailboxQueryRefreshWorker.class)
                .setInputData(MailboxQueryRefreshWorker.data(queryRepository.getAccountId(), mailboxId))
                .build();
    }

    @Override
    protected LiveData<EmailQuery> getQuery() {
        return emailQueryLiveData;
    }


    public static class Factory implements ViewModelProvider.Factory {

    private final Application application;
    private final long accountId;
    private final String id;

    public Factory(Application application, final long accountId, String id) {
        this.application = application;
        this.accountId = accountId;
        this.id = id;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return modelClass.cast(new MailboxQueryViewModel(application, accountId, id));
    }
}
}
