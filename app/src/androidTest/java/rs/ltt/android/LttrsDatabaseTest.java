package rs.ltt.android;

import androidx.paging.DataSource;
import androidx.room.Room;
import androidx.room.paging.LimitOffsetDataSource;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.EmailWithBodies;
import rs.ltt.android.entity.MailboxOverviewItem;
import rs.ltt.android.entity.MailboxWithRoleAndName;
import rs.ltt.android.entity.ThreadOverviewItem;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;
import rs.ltt.jmap.mua.Mua;
import rs.ltt.jmap.mua.util.StandardQueries;

@RunWith(AndroidJUnit4.class)
public class LttrsDatabaseTest {

    private MockWebServer mockWebServer;
    private LttrsDatabase lttrsDatabase;
    private Mua mua;

    @Before
    public void createDatabase() {}

    @Before
    public void setupMua() throws IOException {
        this.mockWebServer = new MockWebServer();
        final MockMailServer mockMailServer = new MockMailServer(128);
        mockMailServer.setAdvertiseWebSocket(false);
        mockWebServer.setDispatcher(mockMailServer);
        mockWebServer.start();

        this.lttrsDatabase =
                Room.inMemoryDatabaseBuilder(
                                ApplicationProvider.getApplicationContext(), LttrsDatabase.class)
                        .build();
        final DatabaseCache storage = new DatabaseCache(this.lttrsDatabase);

        this.mua =
                Mua.builder()
                        .username(mockMailServer.getUsername())
                        .password(JmapDispatcher.PASSWORD)
                        .accountId(mockMailServer.getAccountId())
                        .cache(storage)
                        .sessionResource(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH))
                        .build();
    }

    @Test
    public void unmodifiedThreadOverviewEquals() throws ExecutionException, InterruptedException {
        mua.refreshMailboxes().get();
        final MailboxWithRoleAndName inbox = lttrsDatabase.mailboxDao().getMailbox(Role.INBOX);
        mua.query(StandardQueries.mailbox(inbox)).get();
        final DataSource.Factory<Integer, ThreadOverviewItem> factory =
                lttrsDatabase
                        .queryDao()
                        .getThreadOverviewItems(StandardQueries.mailbox(inbox).asHash());
        final LimitOffsetDataSource<ThreadOverviewItem> dataSource =
                (LimitOffsetDataSource<ThreadOverviewItem>) factory.create();
        final List<ThreadOverviewItem> threadItems = dataSource.loadRange(0, 10);

        Assert.assertEquals(10, threadItems.size());

        final List<ThreadOverviewItem> threadItemsReload = dataSource.loadRange(0, 10);
        Assert.assertNotSame(threadItems, threadItemsReload);
        Assert.assertArrayEquals(
                threadItems.toArray(new ThreadOverviewItem[0]),
                threadItemsReload.toArray(new ThreadOverviewItem[0]));
    }

    @Test
    public void unmodifiedThreadEquals() throws ExecutionException, InterruptedException {
        mua.refreshMailboxes().get();
        final MailboxWithRoleAndName inbox = lttrsDatabase.mailboxDao().getMailbox(Role.INBOX);
        mua.query(StandardQueries.mailbox(inbox)).get();

        final DataSource.Factory<Integer, EmailWithBodies> factory =
                lttrsDatabase.threadAndEmailDao().getEmails("T2");

        final LimitOffsetDataSource<EmailWithBodies> dataSource =
                (LimitOffsetDataSource<EmailWithBodies>) factory.create();

        final List<EmailWithBodies> emails = dataSource.loadRange(0, 99);
        Assert.assertEquals(3, emails.size());

        final List<EmailWithBodies> emailsReload = dataSource.loadRange(0, 99);

        Assert.assertNotSame(emails, emailsReload);

        Assert.assertArrayEquals(
                emails.toArray(new EmailWithBodies[0]),
                emailsReload.toArray(new EmailWithBodies[0]));
    }

    @Test
    public void unmodifiedMailboxesEquals() throws ExecutionException, InterruptedException {
        mua.refreshMailboxes().get();
        final List<MailboxOverviewItem> mailboxes =
                LiveDataUtils.getOrAwaitValue(lttrsDatabase.mailboxDao().getMailboxes());

        Assert.assertNotNull(mailboxes);
        Assert.assertEquals(1, mailboxes.size());

        final List<MailboxOverviewItem> mailboxesReload =
                LiveDataUtils.getOrAwaitValue(lttrsDatabase.mailboxDao().getMailboxes());
        Assert.assertNotSame(mailboxes, mailboxesReload);
    }

    @After
    public void tearDownMua() throws IOException {
        this.mua.close();
        this.lttrsDatabase.close();
        this.mockWebServer.close();
    }
}
