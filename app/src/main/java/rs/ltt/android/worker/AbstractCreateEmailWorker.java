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

package rs.ltt.android.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rs.ltt.android.entity.IdentityWithNameAndEmail;
import rs.ltt.jmap.common.entity.Email;
import rs.ltt.jmap.common.entity.EmailAddress;
import rs.ltt.jmap.common.entity.EmailBodyPart;
import rs.ltt.jmap.common.entity.EmailBodyValue;
import rs.ltt.jmap.mua.Status;
import rs.ltt.jmap.mua.util.EmailAddressUtil;

public abstract class AbstractCreateEmailWorker extends AbstractMuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCreateEmailWorker.class);

    private static String IDENTITY_KEY = "identity";
    private static String IN_REPLY_TO_KEY = "in_reply_to";
    private static String TO_KEY = "to";
    private static String SUBJECT_KEY = "subject";
    private static String BODY_KEY = "body";

    private final String identity;
    private final List<String> inReplyTo;
    private final Collection<EmailAddress> to;
    private final String subject;
    private final String body;


    AbstractCreateEmailWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = workerParams.getInputData();
        this.identity = data.getString(IDENTITY_KEY);
        final String to = data.getString(TO_KEY);
        this.to = to == null ? Collections.emptyList() : EmailAddressUtil.parse(to);
        this.subject = data.getString(SUBJECT_KEY);
        this.body = data.getString(BODY_KEY);
        final String[] inReplyTo = data.getStringArray(IN_REPLY_TO_KEY);
        this.inReplyTo = inReplyTo == null ? Collections.emptyList() : Arrays.asList(inReplyTo);
    }

    public static Data data(final Long account,
                            final String identity,
                            final Collection<String> inReplyTo,
                            final Collection<EmailAddress> to,
                            final String subject,
                            final String body) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(IDENTITY_KEY, identity)
                .putStringArray(IN_REPLY_TO_KEY, inReplyTo.toArray(new String[0]))
                .putString(TO_KEY, EmailAddressUtil.toHeaderValue(to))
                .putString(SUBJECT_KEY, subject)
                .putString(BODY_KEY, body)
                .build();
    }


    protected void refresh() {
        try {
            final Status status = getMua().refresh().get();
        } catch (Exception e) {
            LOGGER.warn("Refresh after email creation failed", e);
        }
    }

    Email buildEmail(final IdentityWithNameAndEmail identity) {
        final EmailBodyValue emailBodyValue = EmailBodyValue.builder()
                .value(this.body)
                .build();
        final String partId = "0";
        final EmailBodyPart emailBodyPart = EmailBodyPart.builder()
                .partId(partId)
                .type("text/plain")
                .build();
        return Email.builder()
                .from(identity.getEmailAddress())
                .inReplyTo(this.inReplyTo)
                .to(this.to)
                .subject(this.subject)
                .bodyValue(partId, emailBodyValue)
                .textBody(emailBodyPart)
                .build();
    }

    IdentityWithNameAndEmail getIdentity() {
        return getDatabase().identityDao().get(this.identity);
    }
}
