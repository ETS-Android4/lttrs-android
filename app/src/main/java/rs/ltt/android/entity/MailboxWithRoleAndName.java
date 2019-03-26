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

import java.util.Collection;

import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;
import rs.ltt.jmap.common.entity.Role;

public class MailboxWithRoleAndName implements IdentifiableMailboxWithRole {

    public String id;
    public Role role;
    public String name;

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public String getId() {
        return id;
    }

    public static boolean isAnyOfRole(Collection<MailboxWithRoleAndName> mailboxes, Role role) {
        for(MailboxWithRoleAndName mailbox : mailboxes) {
            if (mailbox.role == role) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyNotOfRole(Collection<MailboxWithRoleAndName> mailboxes, Role role) {
        for(MailboxWithRoleAndName mailbox : mailboxes) {
            if (mailbox.role != role) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyOfLabel(Collection<MailboxWithRoleAndName> mailboxes, String label) {
        for(MailboxWithRoleAndName mailbox : mailboxes) {
            if (mailbox.role == null && mailbox.name != null && mailbox.name.equals(label)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNoneOfRole(Collection<MailboxWithRoleAndName> mailboxes, Role role) {
        for(MailboxWithRoleAndName mailbox : mailboxes) {
            if (mailbox.role == role) {
                return false;
            }
        }
        return true;
    }


    public static MailboxWithRoleAndName findByLabel(Collection<MailboxWithRoleAndName> mailboxes, String label) {
        for(MailboxWithRoleAndName mailbox : mailboxes) {
            if (mailbox.role == null && mailbox.name != null && mailbox.name.equals(label)) {
                return mailbox;
            }
        }
        return null;
    }
}
