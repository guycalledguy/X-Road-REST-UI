/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.openapi;

import ee.ria.xroad.common.identifier.SecurityServerId;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.restapi.exceptions.BadRequestException;
import org.niis.xroad.restapi.exceptions.ResourceNotFoundException;
import org.niis.xroad.restapi.openapi.model.SecurityServer;
import org.niis.xroad.restapi.service.GlobalConfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * test securityservers api controller
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureTestDatabase
public class SecurityServersApiControllerTest {

    // our global configuration has only this security server
    public static final SecurityServerId EXISTING_SERVER_ID = SecurityServerId.create(
            "XRD2", "GOV", "M4", "server1");

    @MockBean
    private GlobalConfService globalConfFacade;

    @Autowired
    private SecurityServersApiController securityServersApiController;

    @Before
    public void setup() {
        // securityServerExists = true when parameter = EXISTING_SERVER_ID
        when(globalConfFacade.securityServerExists(any()))
                .thenAnswer(invocation -> invocation.getArguments()[0].equals(EXISTING_SERVER_ID));
    }

    @Test
    @WithMockUser(authorities = { "INIT_CONFIG" })
    public void getSecurityServerFindsOne() {
        ResponseEntity<SecurityServer> response = securityServersApiController.getSecurityServer(
                "XRD2:GOV:M4:server1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        SecurityServer securityServer = response.getBody();
        assertEquals("XRD2:GOV:M4:server1", securityServer.getId());
        assertEquals("XRD2", securityServer.getInstanceId());
        assertEquals("GOV", securityServer.getMemberClass());
        assertEquals("M4", securityServer.getMemberCode());
        assertEquals("server1", securityServer.getServerCode());
    }

    @Test(expected = ResourceNotFoundException.class)
    @WithMockUser(authorities = { "INIT_CONFIG" })
    public void getSecurityServerNoMatch() {
        securityServersApiController.getSecurityServer("XRD2:GOV:M4:server-does-not-exist");
    }

    @Test(expected = BadRequestException.class)
    @WithMockUser(authorities = { "INIT_CONFIG" })
    public void getSecurityServerBadRequest() {
        securityServersApiController.getSecurityServer("XRD2:GOV:M4:server:somethingExtra");
    }

}
