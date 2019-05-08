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
package org.niis.xroad.restapi.converter;

import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.identifier.ClientId;

import org.apache.commons.lang.StringUtils;
import org.niis.xroad.restapi.exceptions.BadRequestException;
import org.niis.xroad.restapi.openapi.model.Client;
import org.niis.xroad.restapi.openapi.model.ClientStatus;
import org.niis.xroad.restapi.openapi.model.ConnectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Converter Client related data between openapi and (jaxb) service classes
 */
@Component
public class ClientConverter {

    private GlobalConfWrapper globalConfWrapper;

    public static final int INSTANCE_INDEX = 0;
    public static final int MEMBER_CLASS_INDEX = 1;
    public static final int MEMBER_CODE_INDEX = 2;
    public static final int SUBSYSTEM_CODE_INDEX = 3;
    public static final char ENCODED_CLIENT_ID_SEPARATOR = ':';

    public ClientConverter(@Autowired GlobalConfWrapper globalConfWrapper) {
        this.globalConfWrapper = globalConfWrapper;
    }

    /**
     * convert ClientType into openapi Client class
     * @param clientType
     * @return
     */
    public Client convert(ClientType clientType) {
        Client client = new Client();
        client.setId(convertId(clientType.getIdentifier()));
        client.setMemberClass(clientType.getIdentifier().getMemberClass());
        client.setMemberCode(clientType.getIdentifier().getMemberCode());
        client.setSubsystemCode(clientType.getIdentifier().getSubsystemCode());
        client.setMemberName(globalConfWrapper.getMemberName(clientType.getIdentifier()));
        Optional<ClientStatus> status = ClientStatusMapping.map(clientType.getClientStatus());
        client.setStatus(status.get());
        Optional<ConnectionType> connectionTypeEnum =
                ConnectionTypeMapping.map(clientType.getIsAuthentication());
        client.setConnectionType(connectionTypeEnum.get());
        return client;
    }

    /**
     * Convert ClientId into encoded member id
     * @param clientId
     * @return
     */
    public String convertId(ClientId clientId) {
        StringBuilder builder = new StringBuilder();
        builder.append(clientId.getXRoadInstance())
                .append(ENCODED_CLIENT_ID_SEPARATOR)
                .append(clientId.getMemberClass())
                .append(ENCODED_CLIENT_ID_SEPARATOR)
                .append(clientId.getMemberCode());
        if (StringUtils.isNotEmpty(clientId.getSubsystemCode())) {
            builder.append(ENCODED_CLIENT_ID_SEPARATOR)
                    .append(clientId.getSubsystemCode());
        }
        return builder.toString();
    }

    /**
     * Convert encoded member id into ClientId
     * @param encodedId
     * @throws BadRequestException if encoded id could not be decoded
     * @return
     */
    public ClientId convertId(String encodedId) throws BadRequestException {
        int separators = countOccurences(encodedId, ENCODED_CLIENT_ID_SEPARATOR);
        if (separators != MEMBER_CODE_INDEX && separators != SUBSYSTEM_CODE_INDEX) {
            throw new BadRequestException("Invalid client id " + encodedId);
        }
        List<String> parts = Arrays.asList(encodedId.split(String.valueOf(ENCODED_CLIENT_ID_SEPARATOR)));
        String instance = parts.get(INSTANCE_INDEX);
        String memberClass = parts.get(MEMBER_CLASS_INDEX);
        String memberCode = parts.get(MEMBER_CODE_INDEX);
        String subsystemCode = null;
        if (parts.size() != (MEMBER_CODE_INDEX + 1)
                && parts.size() != (SUBSYSTEM_CODE_INDEX + 1)) {
            throw new BadRequestException("Invalid client id " + encodedId);
        }
        if (parts.size() == (SUBSYSTEM_CODE_INDEX + 1)) {
            subsystemCode = parts.get(SUBSYSTEM_CODE_INDEX);
        }
        return ClientId.create(instance, memberClass, memberCode, subsystemCode);
    }

    private int countOccurences(String from, char searched) {
        String removed = from.replace(String.valueOf(searched), "");
        return from.length() - removed.length();
    }

}
