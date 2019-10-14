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
package org.niis.xroad.restapi.util;

import ee.ria.xroad.common.conf.serverconf.model.ServiceType;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.GlobalGroupId;
import ee.ria.xroad.common.identifier.LocalGroupId;
import ee.ria.xroad.common.identifier.XRoadId;

import org.apache.commons.lang.StringUtils;
import org.niis.xroad.restapi.converter.Converters;
import org.niis.xroad.restapi.openapi.ResourceNotFoundException;
import org.niis.xroad.restapi.wsdl.WsdlParser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Format utils
 */
public final class FormatUtils {
    private FormatUtils() {
        // noop
    }

    /**
     * Converts Date to OffsetDateTime with ZoneOffset.UTC
     * @param date
     * @return OffsetDateTime with offset ZoneOffset.UTC
     * @see ZoneOffset#UTC
     */
    public static OffsetDateTime fromDateToOffsetDateTime(Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
    }

    /**
     * @param url
     * @return true or false depending on the validity of the provided url
     */
    public static boolean isValidUrl(String url) {
        try {
            URL wsdlUrl = new URL(url);
            URI uri = wsdlUrl.toURI();
            uri.parseServerAuthority();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }

    /**
     * Get the full service name (e.g. myService.v1) from ServiceType object
     * @param serviceType
     * @return full service name as String
     */
    public static String getServiceFullName(ServiceType serviceType) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceType.getServiceCode());
        if (!StringUtils.isEmpty(serviceType.getServiceVersion())) {
            sb.append(".").append(serviceType.getServiceVersion());
        }
        return sb.toString();
    }

    /**
     * Get the full service name (e.g. myService.v1) from WsdlParser.ServiceInfo object
     * @param serviceInfo
     * @return full service name as String
     */
    public static String getServiceFullName(WsdlParser.ServiceInfo serviceInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(serviceInfo.name);
        if (!StringUtils.isEmpty(serviceInfo.version)) {
            sb.append(".").append(serviceInfo.version);
        }
        return sb.toString();
    }

    /**
     * in case of NumberFormatException we throw ResourceNotFoundException. Client should not
     * know about id parameter details, such as "it should be numeric" -
     * the resource with given id just cant be found, and that's all there is to it
     * @param id as String
     * @return id as Long
     */
    public static Long parseLongIdOrThrowNotFound(String id) throws ResourceNotFoundException {
        Long groupId = null;
        try {
            groupId = Long.valueOf(id);
        } catch (NumberFormatException nfe) {
            throw new ResourceNotFoundException("bad id", nfe);
        }
        return groupId;
    }

    /**
     * Count occurrences of searched char
     * @param from
     * @param searched
     * @return occurences, or zero if String was null
     */
    public static int countOccurences(String from, char searched) {
        if (from == null) {
            return 0;
        }
        String removed = from.replace(String.valueOf(searched), "");
        return from.length() - removed.length();
    }

    /**
     * Converts {@link XRoadId} to an encoded String format
     * @param xRoadId
     * @return
     */
    public static String xRoadIdToEncodedId(XRoadId xRoadId) {
        StringBuilder encodedId = new StringBuilder();
        switch (xRoadId.getObjectType()) {
            case MEMBER:
                ClientId memberId = (ClientId) xRoadId;
                encodedId.append(memberId.getXRoadInstance())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(memberId.getMemberClass())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(memberId.getMemberCode());
                break;
            case SUBSYSTEM:
                ClientId subSystemId = (ClientId) xRoadId;
                encodedId.append(subSystemId.getXRoadInstance())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(subSystemId.getMemberClass())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(subSystemId.getMemberCode())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(subSystemId.getSubsystemCode());
                break;
            case GLOBALGROUP:
                GlobalGroupId globalGroupId = (GlobalGroupId) xRoadId;
                encodedId.append(globalGroupId.getXRoadInstance())
                        .append(Converters.ENCODED_ID_SEPARATOR)
                        .append(globalGroupId.getGroupCode());
                break;
            case LOCALGROUP:
                LocalGroupId localGroupId = (LocalGroupId) xRoadId;
                encodedId.append(localGroupId.getGroupCode());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + xRoadId.getObjectType()); // never ever
        }
        return encodedId.toString();
    }
}
