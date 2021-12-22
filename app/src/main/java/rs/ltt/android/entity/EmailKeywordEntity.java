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

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import rs.ltt.jmap.common.entity.Email;

@Entity(
        tableName = "email_keyword",
        primaryKeys = {"emailId", "keyword"},
        foreignKeys =
                @ForeignKey(
                        entity = EmailEntity.class,
                        parentColumns = {"id"},
                        childColumns = {"emailId"},
                        onDelete = ForeignKey.CASCADE))
public class EmailKeywordEntity {

    @NonNull public String emailId;
    @NonNull public String keyword;

    public EmailKeywordEntity(@NonNull String emailId, @NonNull String keyword) {
        this.emailId = emailId;
        this.keyword = keyword;
    }

    public static List<EmailKeywordEntity> of(Email email) {
        final Map<String, Boolean> keywords = email.getKeywords();
        if (keywords == null) {
            return Collections.emptyList();
        }
        final ImmutableList.Builder<EmailKeywordEntity> builder = new ImmutableList.Builder<>();
        for (String keyword : keywords.keySet()) {
            builder.add(new EmailKeywordEntity(email.getId(), keyword));
        }
        return builder.build();
    }
}
