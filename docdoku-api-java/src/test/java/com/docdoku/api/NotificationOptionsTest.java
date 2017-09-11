/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.docdoku.api;

import com.docdoku.api.client.ApiException;
import com.docdoku.api.client.ApiResponse;
import com.docdoku.api.models.NotificationOptionsDTO;
import com.docdoku.api.models.WorkspaceDTO;
import com.docdoku.api.services.WorkspacesApi;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NotificationOptionsTest {

    private WorkspacesApi workspacesApi = new WorkspacesApi(TestConfig.REGULAR_USER_CLIENT);
    private static WorkspaceDTO workspace;

    @BeforeClass
    public static void initWorkspace() throws ApiException {
        workspace = TestUtils.createWorkspace(NotificationOptionsTest.class.getName());
    }

    @AfterClass
    public static void deleteWorkspace() throws ApiException {
        TestUtils.deleteWorkspace(workspace);
    }

    @Test
    public void basicTests() throws ApiException {

        NotificationOptionsDTO notificationOptions = workspacesApi.getNotificationOptions(workspace.getId());
        Assert.assertNotNull(notificationOptions);
        Assert.assertTrue(notificationOptions.getSendEmails());
        Assert.assertEquals(workspace.getId(), notificationOptions.getWorkspaceId());

        notificationOptions.setSendEmails(false);
        ApiResponse<Void> response = workspacesApi.updateNotificationOptionsWithHttpInfo(workspace.getId(), notificationOptions);
        Assert.assertNotNull(response);
        Assert.assertEquals(204, response.getStatusCode());

        notificationOptions = workspacesApi.getNotificationOptions(workspace.getId());
        Assert.assertNotNull(notificationOptions);
        Assert.assertFalse(notificationOptions.getSendEmails());
        Assert.assertEquals(workspace.getId(), notificationOptions.getWorkspaceId());

    }
}
