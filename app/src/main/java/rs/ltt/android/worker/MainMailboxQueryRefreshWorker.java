package rs.ltt.android.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountName;
import rs.ltt.android.ui.notification.EmailNotification;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.common.entity.query.EmailQuery;
import rs.ltt.jmap.mua.util.StandardQueries;

public class MainMailboxQueryRefreshWorker extends QueryRefreshWorker {

    public MainMailboxQueryRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    protected Result refresh(final EmailQuery emailQuery) throws ExecutionException, InterruptedException {
        throwOnEmpty(emailQuery);
        final LttrsDatabase database = getDatabase();
        final Set<String> preexistingEmailIds = ImmutableSet.copyOf(
                database.queryDao().getEmailIds(emailQuery.asHash())
        );
        getMua().query(emailQuery).get();
        final List<String> freshlyAddedEmailIds = freshlyAddedEmailIds(
                preexistingEmailIds,
                database.queryDao().getEmailIds(emailQuery.asHash())
        );
        final AccountName account = AppDatabase.getInstance(getApplicationContext()).accountDao()
                .getAccountName(this.account);

        EmailNotification.builder()
                .setAccount(account)
                .setContext(getApplicationContext())
                .setFreshlyAddedEmailIds(freshlyAddedEmailIds)
                .build()
                .refresh();
        return Result.success();
    }

    private static List<String> freshlyAddedEmailIds(final Set<String> preexistingEmailIds,
                                                     final List<String> postRefreshEmailIds) {
        final ImmutableList.Builder<String> freshlyAddedEmailIds = new ImmutableList.Builder<>();
        for (final String emailId : postRefreshEmailIds) {
            if (preexistingEmailIds.contains(emailId)) {
                return freshlyAddedEmailIds.build();
            }
            freshlyAddedEmailIds.add(emailId);
        }
        return freshlyAddedEmailIds.build();
    }

    public static Data data(final Long account, final boolean skipOverEmpty) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putBoolean(SKIP_OVER_EMPTY_KEY, skipOverEmpty)
                .build();
    }

    @Override
    protected EmailQuery getEmailQuery() {
        final IdentifiableMailboxWithRole inbox = getDatabase().mailboxDao().getMailbox(Role.INBOX);
        if (inbox == null) {
            return EmailQuery.unfiltered();
        } else {
            return StandardQueries.mailbox(inbox);
        }
    }

}
