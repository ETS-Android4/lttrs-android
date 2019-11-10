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
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import rs.ltt.android.entity.EmailWithMailboxes;

public class MarkImportantWorker extends AbstractMailboxModificationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkImportantWorker.class);


    public MarkImportantWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected ListenableFuture<Boolean> modify(List<EmailWithMailboxes> emails) {
        LOGGER.info("Modifying {} emails in thread {}", emails.size(), threadId);
        return mua.copyToImportant(emails);
    }

    public static String uniqueName(String threadId) {
        return "important-toggle-" + threadId;
    }
}
