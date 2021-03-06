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
package org.niis.xroad.restapi.service;

import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceDescriptionType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceType;
import ee.ria.xroad.common.identifier.ClientId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
import org.niis.xroad.restapi.repository.ClientRepository;
import org.niis.xroad.restapi.repository.ServiceDescriptionRepository;
import org.niis.xroad.restapi.util.FormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * service class for handling services
 */
@Slf4j
@Service
@Transactional
@PreAuthorize("denyAll")
public class ServiceService {

    private static final String HTTPS = "https";

    private final ClientRepository clientRepository;
    private final ServiceDescriptionRepository serviceDescriptionRepository;
    private final WsdlUrlValidator wsdlUrlValidator;

    @Autowired
    public ServiceService(ClientRepository clientRepository, ServiceDescriptionRepository serviceDescriptionRepository,
            WsdlUrlValidator wsdlUrlValidator) {
        this.clientRepository = clientRepository;
        this.serviceDescriptionRepository = serviceDescriptionRepository;
        this.wsdlUrlValidator = wsdlUrlValidator;
    }

    /**
     * get ServiceType by ClientId and service code that includes service version
     * see {@link FormatUtils#getServiceFullName(ServiceType)}
     * @param clientId
     * @param fullServiceCode
     * @return
     * @throws ClientNotFoundException if client with given id was not found
     * @throws ServiceNotFoundException if service with given fullServicecode was not found
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ServiceType getService(ClientId clientId, String fullServiceCode) throws ClientNotFoundException,
            ServiceNotFoundException {
        ClientType client = clientRepository.getClient(clientId);
        if (client == null) {
            throw new ClientNotFoundException("Client " + clientId.toShortString() + " not found");
        }
        return getServiceFromClient(client, fullServiceCode);
    }

    /**
     * Get {@link ServiceType} from a {@link ClientType} by comparing the service code (with version).
     * @param client
     * @param fullServiceCode
     * @return ServiceType
     * @throws ServiceNotFoundException if service with fullServiceCode was not found
     */
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ServiceType getServiceFromClient(ClientType client, String fullServiceCode) throws ServiceNotFoundException {
        Optional<ServiceType> foundService = client.getServiceDescription()
                .stream()
                .map(ServiceDescriptionType::getService)
                .flatMap(List::stream)
                .filter(serviceType -> FormatUtils.getServiceFullName(serviceType).equals(fullServiceCode))
                .findFirst();
        return foundService.orElseThrow(() -> new ServiceNotFoundException("Service "
                + fullServiceCode + " not found"));
    }

    /**
     * update a Service. clientId and fullServiceCode identify the updated service.
     * @param clientId clientId of the client associated with the service
     * @param fullServiceCode service code that includes service version
     * see {@link FormatUtils#getServiceFullName(ServiceType)}
     * @param url
     * @param urlAll
     * @param timeout
     * @param timeoutAll
     * @param sslAuth
     * @param sslAuthAll
     * @return ServiceType
     * @throws InvalidUrlException if given url was not valid
     * @throws ServiceNotFoundException if service with given fullServicecode was not found
     * @throws ClientNotFoundException if client with given id was not found
     */
    @PreAuthorize("hasAuthority('EDIT_SERVICE_PARAMS')")
    public ServiceType updateService(ClientId clientId, String fullServiceCode,
            String url, boolean urlAll, Integer timeout, boolean timeoutAll,
            boolean sslAuth, boolean sslAuthAll) throws InvalidUrlException, ServiceNotFoundException,
            ClientNotFoundException {
        if (!wsdlUrlValidator.isValidWsdlUrl(url)) {
            throw new InvalidUrlException("URL is not valid: " + url);
        }

        ServiceType serviceType = getService(clientId, fullServiceCode);

        if (serviceType == null) {
            throw new ServiceNotFoundException("Service " + fullServiceCode + " not found");
        }

        ServiceDescriptionType serviceDescriptionType = serviceType.getServiceDescription();

        serviceDescriptionType.getService().forEach(service -> {
            boolean serviceMatch = service == serviceType;
            if (urlAll || serviceMatch) {
                service.setUrl(url);
            }
            if (timeoutAll || serviceMatch) {
                service.setTimeout(timeout);
            }
            if (sslAuthAll || serviceMatch) {
                if (service.getUrl().startsWith(HTTPS)) {
                    service.setSslAuthentication(sslAuth);
                } else {
                    service.setSslAuthentication(null);
                }
            }
        });

        serviceDescriptionRepository.saveOrUpdate(serviceDescriptionType);

        return serviceType;
    }

    /**
     * If service was not found
     */
    public static class ServiceNotFoundException extends NotFoundException {
        public static final String ERROR_SERVICE_NOT_FOUND = "service_not_found";

        public ServiceNotFoundException(String s) {
            super(s, new ErrorDeviation(ERROR_SERVICE_NOT_FOUND));
        }
    }
}
