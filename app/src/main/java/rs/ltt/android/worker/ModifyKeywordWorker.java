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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rs.ltt.android.entity.EmailWithKeywords;

public class ModifyKeywordWorker extends MuaWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyKeywordWorker.class);

    private static final String THREAD_ID_KEY = "threadId";
    private static final String KEYWORD_KEY = "keyword";
    private static final String TARGET_STATE_KEY = "target";

    private final String threadId;
    private final String keyword;
    private final boolean target;

    public ModifyKeywordWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = getInputData();
        this.threadId = data.getString(THREAD_ID_KEY);
        this.keyword = data.getString(KEYWORD_KEY);
        this.target = data.getBoolean(TARGET_STATE_KEY, false);
    }

    public static String uniqueName(String threadId, String keyword) {
        return "toggle-keyword-" + keyword + "-" + threadId;
    }

    public static Data data(final String threadId, final String keyword, final boolean targetState) {
        return new Data.Builder()
                .putString(THREAD_ID_KEY, threadId)
                .putString(KEYWORD_KEY, keyword)
                .putBoolean(TARGET_STATE_KEY, targetState)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        List<EmailWithKeywords> emails = threadId == null ? Collections.emptyList() : database.threadAndEmailDao().getEmailsWithKeywords(threadId);
        if (target) {
            LOGGER.info("Setting keyword {} for {} emails in thread {}", keyword, emails.size(), threadId);
        } else {
            LOGGER.info("Removing keyword {} for {} emails in thread {}", keyword, emails.size(), threadId);
        }
        try {
            final boolean madeChanges;
            if (target) {
                madeChanges = mua.setKeyword(emails, keyword).get();
            } else {
                madeChanges = mua.removeKeyword(emails, keyword).get();
            }
            if (!madeChanges) {
                final int deletedOverwrites = database.overwriteDao().deleteKeywordOverwritesByThread(threadId);
                if (deletedOverwrites > 0) {
                    LOGGER.info("Deleted {} overwrites after not making any changes to thread {}", deletedOverwrites, threadId);
                }
            }
            return Result.success();
        } catch (ExecutionException e) {
            return toResult(e);
        } catch (InterruptedException e) {
            return Result.failure();
        }
    }
}
