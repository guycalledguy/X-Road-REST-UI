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

import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.util.CryptoUtils;
import ee.ria.xroad.signer.protocol.dto.CertRequestInfo;
import ee.ria.xroad.signer.protocol.dto.CertificateInfo;
import ee.ria.xroad.signer.protocol.dto.KeyInfo;
import ee.ria.xroad.signer.protocol.dto.TokenInfo;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.niis.xroad.restapi.converter.GlobalConfWrapper;
import org.niis.xroad.restapi.exceptions.NotFoundException;
import org.niis.xroad.restapi.openapi.model.Client;
import org.niis.xroad.restapi.openapi.model.ConnectionType;
import org.niis.xroad.restapi.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test ClientsApiController
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Transactional
@Slf4j
public class ClientsApiControllerIntegrationTest {

    // this is base64 encoded DER certificate from common-util/test/configuration-anchor.xml
    /**
     * Certificate:
     *     Data:
     *         Version: 3 (0x2)
     *         Serial Number: 1 (0x1)
     *     Signature Algorithm: sha512WithRSAEncryption
     *         Issuer: CN=N/A
     *         Validity
     *             Not Before: Jan  1 00:00:00 1970 GMT
     *             Not After : Jan  1 00:00:00 2038 GMT
     *         Subject: CN=N/A
     */
    private static byte[] certBytes =
            CryptoUtils.decodeBase64("MIICqTCCAZGgAwIBAgIBATANBgkqhkiG9w0BAQ0FADAOMQwwCgYDVQQDDANOL0EwHhcNN\n"
            + "zAwMTAxMDAwMDAwWhcNMzgwMTAxMDAwMDAwWjAOMQwwCgYDVQQDDANOL0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n"
            + "AoIBAQCdiI++CJsyo19Y0810Q80lOJmJ264CvGGqQuB9VYha4YFsHUhltAp3LIcEpxNPuh8k7Mn+pFoetIXtBh6p5cYGf3n\n"
            + "S0i07xSLaAAkQdGqzI6aiSNiGDhQGL5NdyM/cdthtdheQq3WquN7kNkmXo1c5RM2ZcK4SRy6Q44d+KdzC5O42mUgDdxyY2+\n"
            + "3xpSqcAJq1/2DuDPVzAIkWH/iU2+dgnaPACcNqCgnL8g0ALu2e9vHm/ZYhYpS3+e2xLXEOwRvxlprsGcE1aIjKeFupwoZ4n\n"
            + "nkqmHOA2AYS4wVVpcrmF0lDmemXAfi0gDqWCkyjqo9aWdo952uHVQpJarMBGothAgMBAAGjEjAQMA4GA1UdDwEB/wQEAwIG\n"
            + "QDANBgkqhkiG9w0BAQ0FAAOCAQEAMUt6UKCam3QyJnGeEMDJ0m8WbjSzD5NyUVbpR2EVrO+Kqbu8Kd/vjF8vdQN+TCNabqT\n"
            + "ynnrrmqkc4xBBIXHMJ+xS6SijHQ5+IJ6D/VSx+C3D6XrJbzCby4t+ESqGsqB6ShxiiKOSQ5A6MDaE4Doi00GMB5NymknQrn\n"
            + "wREOMPwTZy68CZEaEQyE4M9KezCeVJMCXmnJt1I9oudsw3xPDjq+aYzRORW74RvNFf+sztBjPGhkqFnkl+glbEK6otefyJP\n"
            + "n5vVwjz/+ywyqzx8YJM0vPkD/PghmJxunsJObbvif9FNZaxOaEzI9QDw0nWzbgvsCAqdcHqRjMEQwtU75fzfg==");

    @MockBean
    private GlobalConfWrapper globalConfWrapper;

    @MockBean
    private TokenRepository tokenRepository;

    @Before
    public void setup() throws Exception {
        when(globalConfWrapper.getMemberName(any())).thenReturn("test-member-name");

        List<TokenInfo> mockTokens = createMockTokenInfos(null);
        when(tokenRepository.getTokens()).thenReturn(mockTokens);
    }

    @Autowired
    private ClientsApiController clientsApiController;

    @Test
    @WithMockUser(authorities = "VIEW_CLIENTS")
    public void getClients() {
        ResponseEntity<List<org.niis.xroad.restapi.openapi.model.Client>> response =
                clientsApiController.getClients();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        org.niis.xroad.restapi.openapi.model.Client client = response.getBody().get(0);
        assertEquals("test-member-name", client.getMemberName());
        assertEquals("M1", client.getMemberCode());
    }

    @Test
    @WithMockUser(authorities = "VIEW_CLIENT_DETAILS")
    public void getClient() {
        ResponseEntity<org.niis.xroad.restapi.openapi.model.Client> response =
                clientsApiController.getClient("FI:GOV:M1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Client client = response.getBody();
        assertEquals(org.niis.xroad.restapi.openapi.model.ConnectionType.HTTP, client.getConnectionType());
        assertEquals(org.niis.xroad.restapi.openapi.model.ClientStatus.REGISTERED, client.getStatus());
        assertEquals("test-member-name", client.getMemberName());
        assertEquals("GOV", client.getMemberClass());
        assertEquals("M1", client.getMemberCode());
        assertEquals("FI:GOV:M1", client.getId());
        assertEquals(null, client.getSubsystemCode());

        response = clientsApiController.getClient("FI:GOV:M1:SS1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        client = response.getBody();
        assertEquals(org.niis.xroad.restapi.openapi.model.ConnectionType.HTTPS_NO_AUTH, client.getConnectionType());
        assertEquals(org.niis.xroad.restapi.openapi.model.ClientStatus.REGISTERED, client.getStatus());
        assertEquals("test-member-name", client.getMemberName());
        assertEquals("GOV", client.getMemberClass());
        assertEquals("M1", client.getMemberCode());
        assertEquals("FI:GOV:M1:SS1", client.getId());
        assertEquals("SS1", client.getSubsystemCode());

        try {
            response = clientsApiController.getClient("FI:GOV:M1:SS2");
            fail("should throw NotFoundException to 404");
        } catch (NotFoundException expected) {
        }
    }

    @Test
    @WithMockUser(authorities = { "EDIT_CLIENT_INTERNAL_CONNECTION_TYPE",
            "VIEW_CLIENT_DETAILS" })
    public void updateClient() throws Exception {
        ResponseEntity<org.niis.xroad.restapi.openapi.model.Client> response =
                clientsApiController.getClient("FI:GOV:M1:SS1");
        assertEquals(ConnectionType.HTTPS_NO_AUTH, response.getBody().getConnectionType());

        response = clientsApiController.updateClient("FI:GOV:M1:SS1", ConnectionType.HTTP);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ConnectionType.HTTP, response.getBody().getConnectionType());

        response = clientsApiController.getClient("FI:GOV:M1:SS1");
        assertEquals(ConnectionType.HTTP, response.getBody().getConnectionType());
    }

    @Test
    @WithMockUser(authorities = "VIEW_CLIENT_DETAILS")
    public void getClientCertificates() throws Exception {
        ResponseEntity<List<org.niis.xroad.restapi.openapi.model.Certificate>> certificates =
                clientsApiController.getClientCertificates("FI:GOV:M1");
        assertEquals(HttpStatus.OK, certificates.getStatusCode());
        assertEquals(0, certificates.getBody().size());

        CertificateInfo mockCertificate = new CertificateInfo(
                ClientId.create("FI", "GOV", "M1"),
                true, true, CertificateInfo.STATUS_REGISTERED,
                    "id", certBytes, null);
        when(tokenRepository.getTokens()).thenReturn(createMockTokenInfos(mockCertificate));
        certificates = clientsApiController.getClientCertificates("FI:GOV:M1");
        assertEquals(HttpStatus.OK, certificates.getStatusCode());
        assertEquals(1, certificates.getBody().size());

        org.niis.xroad.restapi.openapi.model.Certificate onlyCertificate = certificates.getBody().get(0);
        assertEquals("N/A", onlyCertificate.getIssuerCommonName());
        assertEquals(OffsetDateTime.parse("1970-01-01T00:00:00Z"), onlyCertificate.getNotBefore());
        assertEquals(OffsetDateTime.parse("2038-01-01T00:00:00Z"), onlyCertificate.getNotAfter());
        assertEquals("1", onlyCertificate.getSerial());
        assertEquals(new Integer(3), onlyCertificate.getVersion());
        assertEquals("SHA512withRSA", onlyCertificate.getSignatureAlgorithm());
        assertEquals("RSA", onlyCertificate.getPublicKeyAlgorithm());
        assertEquals("A2293825AA82A5429EC32803847E2152A303969C", onlyCertificate.getHash());
        assertEquals(org.niis.xroad.restapi.openapi.model.State.IN_USE, onlyCertificate.getState());
        assertTrue(onlyCertificate.getSignature().startsWith("314b7a50a09a9b74322671"));
        assertTrue(onlyCertificate.getRsaPublicKeyModulus().startsWith("9d888fbe089b32a35f58"));
        assertEquals(new Integer(65537), onlyCertificate.getRsaPublicKeyExponent());
        assertEquals(new ArrayList<>(Arrays.asList(org.niis.xroad.restapi.openapi.model.KeyUsage.NON_REPUDIATION)),
                new ArrayList<>(onlyCertificate.getKeyUsages()));

        try {
            certificates = clientsApiController.getClientCertificates("FI:GOV:M2");
            fail("should throw NotFoundException for 404");
        } catch (NotFoundException expected) {
        }
    }

    @Test
    @WithMockUser(roles = "WRONG_ROLE")
    public void forbidden() {
        try {
            ResponseEntity<List<org.niis.xroad.restapi.openapi.model.Client>> response =
                    clientsApiController.getClients();
            fail("should throw AccessDeniedException");
        } catch (AccessDeniedException expected) { }
    }


    /**
     * @param certificateInfo one certificate to put inside this tokenInfo
     *                        structure
     * @return
     */
    private List<TokenInfo> createMockTokenInfos(CertificateInfo certificateInfo) {
        List<TokenInfo> mockTokens = new ArrayList<>();
        List<CertificateInfo> certificates = new ArrayList<>();
        if (certificateInfo != null) {
            certificates.add(certificateInfo);
        }
        KeyInfo keyInfo = new KeyInfo(false, null,
                "friendlyName", "id", "label", "publicKey",
                certificates, new ArrayList<CertRequestInfo>(),
                "signMecchanismName");
        TokenInfo tokenInfo = new TokenInfo("type",
                "friendlyName", "id",
                false, false, false,
                "serialNumber", "label", -1,
                null, Arrays.asList(keyInfo), null);
        mockTokens.add(tokenInfo);
        return mockTokens;
    }


}
