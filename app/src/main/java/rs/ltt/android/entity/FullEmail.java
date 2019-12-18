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

package rs.ltt.android.entity;

import androidx.room.Relation;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rs.ltt.jmap.common.entity.Keyword;

public class FullEmail {

    public String id;
    public String preview;
    public String threadId;
    public Date receivedAt;

    @Relation(entity = EmailKeywordEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"keyword"})
    public Set<String> keywords;

    @Relation(entity = EmailEmailAddressEntity.class, parentColumn = "id", entityColumn = "emailId", projection = {"email", "name", "type"})
    public List<EmailAddress> emailAddresses;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyPartEntity> bodyPartEntities;

    @Relation(parentColumn = "id", entityColumn = "emailId")
    public List<EmailBodyValueEntity> bodyValueEntities;

    public boolean isDraft() {
        return keywords.contains(Keyword.DRAFT);
    }


    public String getPreview() {
        return preview;
    }

    public From getFrom() {
        if (isDraft()) {
            return new DraftFrom();
        }
        for (EmailAddress emailAddress : emailAddresses) {
            if (emailAddress.type == EmailAddressType.FROM) {
                return new NamedFrom(emailAddress);
            }
        }
        return null;
    }

    public String getText() {
        final ArrayList<EmailBodyPartEntity> textBody = new ArrayList<>();
        for (EmailBodyPartEntity entity : bodyPartEntities) {
            if (entity.bodyPartType == EmailBodyPartType.TEXT_BODY) {
                textBody.add(entity);
            }
        }
        Collections.sort(textBody, (o1, o2) -> o1.position.compareTo(o2.position));
        EmailBodyPartEntity first = Iterables.getFirst(textBody, null);
        Map<String, EmailBodyValueEntity> map = Maps.uniqueIndex(bodyValueEntities, value -> value.partId);
        EmailBodyValueEntity value = map.get(first.partId);
        return value.value;
    }

    public Collection<String> getTo() {
        LinkedHashMap<String, String> toMap = new LinkedHashMap<>();
        for (EmailAddress emailAddress : emailAddresses) {
            if (emailAddress.type == EmailAddressType.TO || emailAddress.type == EmailAddressType.CC) {
                toMap.put(emailAddress.getEmail(), emailAddress.getName());
            }
        }
        return toMap.values();
    }


    public interface From {

    }

    public static class DraftFrom implements From {

    }

    public static class NamedFrom implements From {
        private final EmailAddress emailAddress;

        private NamedFrom(EmailAddress emailAddress) {
            this.emailAddress = emailAddress;
        }

        public String getName() {
            return this.emailAddress.getName();
        }

        public String getEmail() {
            return this.emailAddress.getEmail();
        }
    }


}
