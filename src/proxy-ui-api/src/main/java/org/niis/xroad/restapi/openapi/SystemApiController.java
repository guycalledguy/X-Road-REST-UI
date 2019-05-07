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

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.converter.CertificateConverter;
import org.niis.xroad.restapi.openapi.model.Certificate;
import org.niis.xroad.restapi.service.InternalTlsCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * system api controller
 */
@Controller
@RequestMapping("/api")
@Slf4j
@PreAuthorize("denyAll")
public class SystemApiController implements org.niis.xroad.restapi.openapi.SystemApi {

    private final NativeWebRequest request;

    @Autowired
    private InternalTlsCertificateService internalTlsCertificateService;

    @Autowired
    private CertificateConverter certificateConverter;

    @org.springframework.beans.factory.annotation.Autowired
    public SystemApiController(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    @Override
    @PreAuthorize("hasAuthority('EXPORT_PROXY_INTERNAL_CERT')")
    public ResponseEntity<Resource> downloadSystemCertificate() {
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("certs.tar.gz")
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);

        byte[] certificateTar = internalTlsCertificateService.exportInternalTlsCertificate();
        Resource resource = new ByteArrayResource(certificateTar);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_PROXY_INTERNAL_CERT')")
    public ResponseEntity<Certificate> getSystemCertificate() {
        X509Certificate x509Certificate = internalTlsCertificateService.getInternalTlsCertificate();
        Certificate certificate = certificateConverter.convert(x509Certificate);
        return new ResponseEntity<>(certificate, HttpStatus.OK);
    }
}
