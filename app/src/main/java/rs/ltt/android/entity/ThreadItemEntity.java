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

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import rs.ltt.jmap.common.entity.Thread;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "thread_item",
        primaryKeys = {"threadId","emailId"},
        foreignKeys = @ForeignKey(entity = ThreadEntity.class,
                parentColumns = {"threadId"},
                childColumns = {"threadId"},
                onDelete = CASCADE
        )
)
public class ThreadItemEntity {
    @NonNull
    public String threadId;
    @NonNull
    public String emailId;

    public Integer position;

    public ThreadItemEntity(@NonNull String threadId, @NonNull String emailId, @NonNull Integer position) {
        this.threadId = threadId;
        this.emailId = emailId;
        this.position = position;
    }


    public int getPosition() {
        return this.position;
    }

    public static List<ThreadItemEntity> of(final Thread thread) {
        final List<ThreadItemEntity> entities = new ArrayList<>();
        List<String> emailIds = thread.getEmailIds();
        for(int i = 0; i < emailIds.size(); ++i) {
            entities.add(new ThreadItemEntity(thread.getId(), emailIds.get(i), i));
        }
        return entities;
    }
}
