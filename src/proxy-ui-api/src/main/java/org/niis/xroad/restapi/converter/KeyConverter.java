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

import ee.ria.xroad.signer.protocol.dto.KeyInfo;

import com.google.common.collect.Streams;
import org.niis.xroad.restapi.openapi.model.Key;
import org.niis.xroad.restapi.openapi.model.KeyUsageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Convert Key related data between openapi and service domain classes
 */
@Component
public class KeyConverter {

    private final TokenCertificateConverter tokenCertificateConverter;
    private final TokenCertificateSigningRequestConverter tokenCsrConverter;

    @Autowired
    public KeyConverter(TokenCertificateConverter tokenCertificateConverter,
            TokenCertificateSigningRequestConverter tokenCsrConverter) {
        this.tokenCertificateConverter = tokenCertificateConverter;
        this.tokenCsrConverter = tokenCsrConverter;
    }

    /**
     * Convert {@link KeyInfo} to openapi {@link Key} object
     * @param keyInfo
     */
    public Key convert(KeyInfo keyInfo) {
        Key key = new Key();
        key.setId(keyInfo.getId());
        key.setName(keyInfo.getFriendlyName());
        key.setLabel(keyInfo.getLabel());
        if (keyInfo.getUsage() != null) {
            if (keyInfo.isForSigning()) {
                key.setUsage(KeyUsageType.SIGNING);
            } else {
                key.setUsage(KeyUsageType.AUTHENTICATION);
            }
        }

        key.setAvailable(keyInfo.isAvailable());
        key.setSavedToConfiguration(isSavedToConfiguration(keyInfo));

        key.setCertificates(tokenCertificateConverter.convert(keyInfo.getCerts()));
        key.setCertificateSigningRequests(tokenCsrConverter.convert(keyInfo.getCertRequests()));

        return key;
    }

    /**
     * Logic to determine if a key is saved to configuration,
     * copied from token_renderer.rb#key_saved_to_configuration
     * @param keyInfo
     */
    public boolean isSavedToConfiguration(KeyInfo keyInfo) {
        if (!keyInfo.getCertRequests().isEmpty()) {
            return true;
        }
        return keyInfo.getCerts().stream()
                       .anyMatch(certificateInfo -> certificateInfo.isSavedToConfiguration());
    }

    /**
     * Convert a group of {@link KeyInfo keyInfos} to a list of {@link Key keyInfos}
     * @param keyInfos
     * @return List of {@link KeyInfo keyInfos}
     */
    public List<Key> convert(Iterable<KeyInfo> keyInfos) {
        return Streams.stream(keyInfos)
                .map(this::convert)
                .collect(Collectors.toList());
    }
}
