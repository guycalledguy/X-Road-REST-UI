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

import ee.ria.xroad.common.conf.serverconf.model.AccessRightType;
import ee.ria.xroad.common.conf.serverconf.model.ClientType;
import ee.ria.xroad.common.conf.serverconf.model.LocalGroupType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceDescriptionType;
import ee.ria.xroad.common.conf.serverconf.model.ServiceType;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.LocalGroupId;
import ee.ria.xroad.common.identifier.XRoadId;
import ee.ria.xroad.common.identifier.XRoadObjectType;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.dto.AccessRightHolderDto;
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
import org.niis.xroad.restapi.repository.ClientRepository;
import org.niis.xroad.restapi.repository.LocalGroupRepository;
import org.niis.xroad.restapi.repository.ServiceDescriptionRepository;
import org.niis.xroad.restapi.util.FormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LocalGroupRepository localGroupRepository;
    private final ServiceDescriptionRepository serviceDescriptionRepository;

    @Autowired
    public ServiceService(ClientRepository clientRepository, LocalGroupRepository localGroupRepository,
            ServiceDescriptionRepository serviceDescriptionRepository) {
        this.clientRepository = clientRepository;
        this.localGroupRepository = localGroupRepository;
        this.serviceDescriptionRepository = serviceDescriptionRepository;
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
     * @param client
     * @param fullServiceCode
     * @return {@link ServiceType}
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
        if (!FormatUtils.isValidUrl(url)) {
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

    private AccessRightHolderDto accessRightTypeToDto(AccessRightType accessRightType,
            Map<String, LocalGroupType> localGroupMap) {
        AccessRightHolderDto accessRightHolderDto = new AccessRightHolderDto();
        XRoadId subjectId = accessRightType.getSubjectId();
        accessRightHolderDto.setRightsGiven(
                FormatUtils.fromDateToOffsetDateTime(accessRightType.getRightsGiven()));
        accessRightHolderDto.setSubjectId(subjectId);
        if (subjectId.getObjectType() == XRoadObjectType.LOCALGROUP) {
            LocalGroupId localGroupId = (LocalGroupId) subjectId;
            LocalGroupType localGroupType = localGroupMap.get(localGroupId.getGroupCode());
            accessRightHolderDto.setLocalGroupId(localGroupType.getId().toString());
            accessRightHolderDto.setLocalGroupCode(localGroupType.getGroupCode());
            accessRightHolderDto.setLocalGroupDescription(localGroupType.getDescription());
        }
        return accessRightHolderDto;
    }

    /**
     * Get access right holders by Service
     * @param clientId
     * @param fullServiceCode
     * @return
     * @throws ClientNotFoundException if client with given id was not found
     * @throws ServiceNotFoundException if service with given fullServicecode was not found
     */
    @PreAuthorize("hasAuthority('VIEW_SERVICE_ACL')")
    public List<AccessRightHolderDto> getAccessRightHoldersByService(ClientId clientId, String fullServiceCode)
            throws ClientNotFoundException, ServiceNotFoundException {
        ClientType clientType = clientRepository.getClient(clientId);
        if (clientType == null) {
            throw new ClientNotFoundException("Client " + clientId.toShortString() + " not found");
        }

        ServiceType serviceType = getServiceFromClient(clientType, fullServiceCode);

        List<AccessRightHolderDto> accessRightHolderDtos = new ArrayList<>();

        Map<String, LocalGroupType> localGroupMap = new HashMap<>();

        clientType.getLocalGroup().forEach(localGroupType -> localGroupMap.put(localGroupType.getGroupCode(),
                localGroupType));

        clientType.getAcl().forEach(accessRightType -> {
            if (accessRightType.getEndpoint().getServiceCode().equals(serviceType.getServiceCode())) {
                AccessRightHolderDto accessRightHolderDto = accessRightTypeToDto(accessRightType, localGroupMap);
                accessRightHolderDtos.add(accessRightHolderDto);
            }
        });

        return accessRightHolderDtos;
    }

    /**
     * Remove AccessRights from a Service
     * @param clientId
     * @param fullServiceCode
     * @param subjectIds
     * @throws ClientNotFoundException if client with given id was not found
     * @throws ServiceNotFoundException if service with given fullServicecode was not found
     * @throws AccessRightNotFoundException if attempted to delete access right that did not exist for the service
     */
    @PreAuthorize("hasAuthority('EDIT_SERVICE_ACL')")
    public void deleteServiceAccessRights(ClientId clientId, String fullServiceCode, Set<XRoadId> subjectIds)
            throws ClientNotFoundException, AccessRightNotFoundException, ServiceNotFoundException {
        ClientType clientType = clientRepository.getClient(clientId);
        if (clientType == null) {
            throw new ClientNotFoundException("Client " + clientId.toShortString() + " not found");
        }

        ServiceType serviceType = getServiceFromClient(clientType, fullServiceCode);

        List<AccessRightType> accessRightsToBeRemoved = clientType.getAcl()
                .stream()
                .filter(accessRightType -> accessRightType.getEndpoint().getServiceCode()
                        .equals(serviceType.getServiceCode()) && subjectIds.contains(accessRightType.getSubjectId()))
                .collect(Collectors.toList());

        List<XRoadId> subjectsToBeRemoved = accessRightsToBeRemoved
                .stream()
                .map(AccessRightType::getSubjectId)
                .collect(Collectors.toList());

        if (!subjectsToBeRemoved.containsAll(subjectIds)) {
            throw new AccessRightNotFoundException();
        }

        clientType.getAcl().removeAll(accessRightsToBeRemoved);

        clientRepository.saveOrUpdate(clientType);
    }

    /**
     * Remove AccessRights from a Service
     * @param clientId
     * @param fullServiceCode
     * @param subjectIds
     * @param localGroupIds
     * @throws LocalGroupNotFoundException if tried to remove local group access right
     * for a local group that does not exist
     * @throws AccessRightNotFoundException if tried to remove access rights that did not exist for the service
     * @throws ClientNotFoundException if client with given id was not found
     * @throws ServiceNotFoundException if service with given fullServicecode was not found
     */
    @PreAuthorize("hasAuthority('EDIT_SERVICE_ACL')")
    public void deleteServiceAccessRights(ClientId clientId, String fullServiceCode, Set<XRoadId> subjectIds,
            Set<Long> localGroupIds) throws LocalGroupNotFoundException,
            ClientNotFoundException, AccessRightNotFoundException, ServiceNotFoundException {
        Set<XRoadId> localGroups = new HashSet<>();
        for (Long groupId: localGroupIds) {
            LocalGroupType localGroup = localGroupRepository.getLocalGroup(groupId); // no need to batch
            if (localGroup == null) {
                throw new LocalGroupNotFoundException("LocalGroup with id " + groupId + " not found");
            }
            localGroups.add(LocalGroupId.create(localGroup.getGroupCode()));
        }
        subjectIds.addAll(localGroups);
        deleteServiceAccessRights(clientId, fullServiceCode, subjectIds);
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

    /**
     * If access right was not found
     */
    public static class AccessRightNotFoundException extends NotFoundException {
        public static final String ERROR_ACCESSRIGHT_NOT_FOUND = "accessright_not_found";
        public AccessRightNotFoundException() {
            super(new ErrorDeviation(ERROR_ACCESSRIGHT_NOT_FOUND));
        }
    }

}
