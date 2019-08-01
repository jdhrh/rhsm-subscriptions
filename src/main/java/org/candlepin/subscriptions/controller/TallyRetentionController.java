/*
 * Copyright (c) 2009 - 2019 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.controller;

import org.candlepin.subscriptions.db.TallySnapshotRepository;
import org.candlepin.subscriptions.db.model.TallyGranularity;
import org.candlepin.subscriptions.files.AccountListSource;
import org.candlepin.subscriptions.retention.TallyRetentionPolicy;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;


/**
 * Cleans up stale tally snapshots for an account.
 */
@Component
public class TallyRetentionController {

    private final TallySnapshotRepository repository;
    private final TallyRetentionPolicy policy;

    private final AccountListSource accountListSource;

    public TallyRetentionController(TallySnapshotRepository repository, TallyRetentionPolicy policy,
        AccountListSource accountListSource) {
        this.repository = repository;
        this.policy = policy;
        this.accountListSource = accountListSource;
    }

    public void purgeSnapshots() throws IOException {
        List<String> accountList = accountListSource.list();
        accountList.forEach(this::cleanStaleSnapshotsForAccount);
    }

    @Transactional
    public void cleanStaleSnapshotsForAccount(String accountNumber) {
        for (TallyGranularity granularity : TallyGranularity.values()) {
            OffsetDateTime cutoffDate = policy.getCutoffDate(granularity);
            if (cutoffDate == null) {
                continue;
            }
            repository.deleteAllByAccountNumberAndGranularityAndSnapshotDateBefore(accountNumber,
                granularity, cutoffDate);
        }
    }
}