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

package rs.ltt.android.repository;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.NetworkType;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.android.entity.KeywordOverwriteEntity;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.QueryEntity;
import rs.ltt.android.entity.QueryItemOverwriteEntity;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.filter.EmailFilterCondition;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.Mua;

public abstract class AbstractMuaRepository {

    static final Executor IO_EXECUTOR = Executors.newSingleThreadExecutor();
    static final Constraints CONNECTED_CONSTRAINT = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMuaRepository.class);
    protected final Application application;
    protected final long accountId;
    protected final LttrsDatabase database;
    protected final ListenableFuture<Mua> mua;

    AbstractMuaRepository(final Application application, final long accountId) {
        this.application = application;
        this.accountId = accountId;
        LOGGER.debug("creating instance of {}", getClass().getSimpleName());
        this.database = LttrsDatabase.getInstance(application, accountId);
        this.mua = Futures.transform(getAccount(), account -> Mua.builder()
                .username(account.username)
                .password(account.password)
                .accountId(account.accountId)
                .sessionResource(account.sessionResource)
                .cache(new DatabaseCache(database))
                .sessionCache(new FileSessionCache(application.getCacheDir()))
                .queryPageSize(20L)
                .build(), MoreExecutors.directExecutor());
    }

    public ListenableFuture<AccountWithCredentials> getAccount() {
        return AppDatabase.getInstance(application).accountDao().getAccountFuture(accountId);
    }

    protected void insert(final Collection<KeywordOverwriteEntity> keywordOverwriteEntities) {
        database.overwriteDao().insertKeywordOverwrites(keywordOverwriteEntities);
    }

    protected void insertQueryItemOverwrite(final String threadId, final Role role) {
        insertQueryItemOverwrite(ImmutableSet.of(threadId), role);
    }

    protected void insertQueryItemOverwrite(final Collection<String> threadIds, final Role role) {
        MailboxOverviewItem mailbox = database.mailboxDao().getMailboxOverviewItem(role);
        if (mailbox != null) {
            insertQueryItemOverwrite(threadIds, mailbox);
        }
    }

    protected void insertQueryItemOverwrite(final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        insertQueryItemOverwrite(threadIds,
                EmailQuery.of(
                        EmailFilterCondition.builder()
                                .inMailbox(mailbox.getId())
                                .build(),
                        true),
                QueryItemOverwriteEntity.Type.MAILBOX
        );
    }

    protected void insertQueryItemOverwrite(final String threadId, final String keyword) {
        insertQueryItemOverwrite(ImmutableSet.of(threadId), keyword);
    }

    protected void insertQueryItemOverwrite(final Collection<String> threadIds, final String keyword) {
        insertQueryItemOverwrite(threadIds,
                EmailQuery.of(
                        EmailFilterCondition.builder()
                                .inMailboxOtherThan(
                                        database.mailboxDao().getMailboxes(Role.TRASH, Role.JUNK)
                                )
                                .hasKeyword(keyword)
                                .build(),
                        true),
                QueryItemOverwriteEntity.Type.KEYWORD
        );
    }

    protected void insertQueryItemOverwrite(final Collection<String> threadIds,
                                          final EmailQuery emailQuery,
                                          final QueryItemOverwriteEntity.Type type) {
        final String queryString = emailQuery.toQueryString();
        final QueryEntity queryEntity = database.queryDao().get(queryString);
        if (queryEntity != null) {
            database.overwriteDao().insertQueryOverwrites(
                    Collections2.transform(
                            threadIds,
                            threadId -> new QueryItemOverwriteEntity(queryEntity.id, threadId, type)
                    )
            );
        }
    }

    protected void deleteQueryItemOverwrite(final Collection<String> threadIds, final Role role) {
        MailboxOverviewItem mailbox = database.mailboxDao().getMailboxOverviewItem(role);
        if (mailbox != null) {
            deleteQueryItemOverwrite(threadIds, mailbox);
        }
    }

    protected void deleteQueryItemOverwrite(final Collection<String> threadIds, final IdentifiableMailboxWithRole mailbox) {
        deleteQueryItemOverwrite(threadIds,
                EmailQuery.of(
                        EmailFilterCondition.builder().
                                inMailbox(mailbox.getId())
                                .build(),
                        true),
                QueryItemOverwriteEntity.Type.MAILBOX
        );
    }

    protected void deleteQueryItemOverwrite(final Collection<String> threadIds, final String keyword) {
        deleteQueryItemOverwrite(threadIds,
                EmailQuery.of(
                        EmailFilterCondition.builder()
                                .hasKeyword(keyword)
                                .build(),
                        true),
                QueryItemOverwriteEntity.Type.KEYWORD
        );
    }

    private void deleteQueryItemOverwrite(final Collection<String> threadIds, final EmailQuery emailQuery, QueryItemOverwriteEntity.Type type) {
        final String queryString = emailQuery.toQueryString();
        QueryEntity queryEntity = database.queryDao().get(queryString);
        if (queryEntity != null) {
            database.overwriteDao().deleteQueryOverwrites(
                    Collections2.transform(
                            threadIds,
                            threadId -> new QueryItemOverwriteEntity(queryEntity.id, threadId, type)
                    )
            );
        }
    }

}
