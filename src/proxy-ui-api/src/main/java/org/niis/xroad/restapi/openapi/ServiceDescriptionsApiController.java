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

import ee.ria.xroad.common.conf.serverconf.model.ServiceDescriptionType;
import ee.ria.xroad.common.identifier.ClientId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.converter.ServiceConverter;
import org.niis.xroad.restapi.converter.ServiceDescriptionConverter;
import org.niis.xroad.restapi.exceptions.BadRequestException;
import org.niis.xroad.restapi.exceptions.ConflictException;
import org.niis.xroad.restapi.exceptions.Error;
import org.niis.xroad.restapi.exceptions.InternalServerErrorException;
import org.niis.xroad.restapi.exceptions.ResourceNotFoundException;
import org.niis.xroad.restapi.openapi.model.IgnoreWarnings;
import org.niis.xroad.restapi.openapi.model.Service;
import org.niis.xroad.restapi.openapi.model.ServiceDescription;
import org.niis.xroad.restapi.openapi.model.ServiceDescriptionDisabledNotice;
import org.niis.xroad.restapi.openapi.model.ServiceDescriptionUpdate;
import org.niis.xroad.restapi.openapi.model.ServiceType;
import org.niis.xroad.restapi.service.InvalidUrlException;
import org.niis.xroad.restapi.service.ServiceAlreadyExistsException;
import org.niis.xroad.restapi.service.ServiceDescriptionNotFoundException;
import org.niis.xroad.restapi.service.ServiceDescriptionService;
import org.niis.xroad.restapi.service.UnhandledWarningsException;
import org.niis.xroad.restapi.service.WrongServiceDescriptionTypeException;
import org.niis.xroad.restapi.service.WsdlUrlAlreadyExistsException;
import org.niis.xroad.restapi.util.FormatUtils;
import org.niis.xroad.restapi.wsdl.WsdlNotFoundException;
import org.niis.xroad.restapi.wsdl.WsdlParseException;
import org.niis.xroad.restapi.wsdl.WsdlUrlMissingException;
import org.niis.xroad.restapi.wsdl.WsdlValidationFailedException;
import org.niis.xroad.restapi.wsdl.WsdlValidatorNotExecutableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * service descriptions api
 */
@Controller
@RequestMapping("/api")
@Slf4j
@PreAuthorize("denyAll")
public class ServiceDescriptionsApiController implements ServiceDescriptionsApi {

    private final ServiceDescriptionService serviceDescriptionService;
    private final ServiceDescriptionConverter serviceDescriptionConverter;
    private final ServiceConverter serviceConverter;

    /**
     * ServiceDescriptionsApiController constructor
     * @param serviceDescriptionService
     * @param serviceDescriptionConverter
     * @param serviceConverter
     */

    @Autowired
    public ServiceDescriptionsApiController(ServiceDescriptionService serviceDescriptionService,
            ServiceDescriptionConverter serviceDescriptionConverter,
            ServiceConverter serviceConverter) {
        this.serviceDescriptionService = serviceDescriptionService;
        this.serviceDescriptionConverter = serviceDescriptionConverter;
        this.serviceConverter = serviceConverter;
    }

    @Override
    @PreAuthorize("hasAuthority('ENABLE_DISABLE_WSDL')")
    public ResponseEntity<Void> enableServiceDescription(String id) {
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        try {
            serviceDescriptionService.enableServices(Collections.singletonList(serviceDescriptionId));
        } catch (ServiceDescriptionNotFoundException e) {
            throw new ResourceNotFoundException();
        }
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('ENABLE_DISABLE_WSDL')")
    public ResponseEntity<Void> disableServiceDescription(String id,
            ServiceDescriptionDisabledNotice serviceDescriptionDisabledNotice) {
        String disabledNotice = null;
        if (serviceDescriptionDisabledNotice != null) {
            disabledNotice = serviceDescriptionDisabledNotice.getDisabledNotice();
        }
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        try {
            serviceDescriptionService.disableServices(Collections.singletonList(serviceDescriptionId),
                    disabledNotice);
        } catch (ServiceDescriptionNotFoundException e) {
            throw new ResourceNotFoundException();
        }
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('DELETE_WSDL')")
    public ResponseEntity<Void> deleteServiceDescription(String id) {
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        try {
            serviceDescriptionService.deleteServiceDescription(serviceDescriptionId);
        } catch (ServiceDescriptionNotFoundException e) {
            throw new ResourceNotFoundException();
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_WSDL')")
    public ResponseEntity<ServiceDescription> updateServiceDescription(String id,
            ServiceDescriptionUpdate serviceDescriptionUpdate) {
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        ServiceDescription serviceDescription;
        if (serviceDescriptionUpdate.getType() == ServiceType.WSDL) {
            ServiceDescriptionType updatedServiceDescription = null;
            try {
                updatedServiceDescription = serviceDescriptionService.updateWsdlUrl(
                        serviceDescriptionId, serviceDescriptionUpdate.getUrl(),
                        serviceDescriptionUpdate.getIgnoreWarnings());
            } catch (WsdlNotFoundException e) {
                throw new InternalServerErrorException();
            } catch (WsdlParseException e) {
                throw new BadRequestException(new Error(ServiceDescriptionsApiController.ERROR_INVALID_WSDL));
            } catch (WsdlValidationFailedException e) {
                throw new BadRequestException(new Error(ERROR_WSDL_VALIDATION_FAILED,
                        e.getOutput()));
            } catch (WsdlValidatorNotExecutableException e) {
                throw new BadRequestException(new Error(ERROR_WSDL_VALIDATOR_NOT_EXECUTABLE));
            } catch (WsdlUrlMissingException e) {
                throw new BadRequestException(new Error(ERROR_WSDL_URL_MISSING));
            } catch (ServiceDescriptionNotFoundException e) {
                throw new ResourceNotFoundException();
            } catch (WrongServiceDescriptionTypeException e) {
                throw new BadRequestException(new Error(ERROR_WRONG_TYPE));
            } catch (UnhandledWarningsException e) {
                throw new BadRequestException(e);
            } catch (ServiceAlreadyExistsException e) {
                throw new ConflictException(e);
            } catch (InvalidUrlException e) {
                throw new BadRequestException();
            } catch (WsdlUrlAlreadyExistsException e) {
                throw new ConflictException(new Error(ServiceDescriptionsApiController.ERROR_WSDL_EXISTS));
            }
            serviceDescription = serviceDescriptionConverter.convert(updatedServiceDescription);
        } else if (serviceDescriptionUpdate.getType() == ServiceType.REST) {
            return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        } else {
            throw new BadRequestException("ServiceType not recognized");
        }
        return new ResponseEntity<>(serviceDescription, HttpStatus.OK);
    }

    public static final String ERROR_INVALID_WSDL = "clients.invalid_wsdl";
    public static final String ERROR_WSDL_DOWNLOAD_FAILED = "clients.wsdl_download_failed";
    public static final String ERROR_WSDL_EXISTS = "clients.wsdl_exists";
    public static final String ERROR_MALFORMED_URL = "clients.malformed_wsdl_url";
    public static final String ERROR_WRONG_TYPE = "clients.servicedescription_wrong_type";
    public static final String ERROR_WSDL_VALIDATOR_NOT_EXECUTABLE = "clients.wsdl_validator_not_executable";
    public static final String ERROR_WSDL_VALIDATION_FAILED = "clients.wsdl_validation_failed";
    public static final String ERROR_WSDL_URL_MISSING = "clients.wsdl_url_missing";

    @Override
    @PreAuthorize("hasAuthority('REFRESH_WSDL')")
    public ResponseEntity<ServiceDescription> refreshServiceDescription(String id, IgnoreWarnings ignoreWarnings) {
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        ServiceDescription serviceDescription = null;
        try {
            serviceDescription = serviceDescriptionConverter.convert(
                    serviceDescriptionService.refreshServiceDescription(serviceDescriptionId,
                            ignoreWarnings.getIgnoreWarnings()));
        } catch (WsdlNotFoundException e) {
            throw new InternalServerErrorException();
        } catch (WsdlParseException e) {
            throw new BadRequestException(new Error(ServiceDescriptionsApiController.ERROR_INVALID_WSDL));
        } catch (WsdlValidationFailedException e) {
            throw new BadRequestException(new Error(ERROR_WSDL_VALIDATION_FAILED,
                    e.getOutput()));
        } catch (WsdlValidatorNotExecutableException e) {
            throw new BadRequestException(new Error(ERROR_WSDL_VALIDATOR_NOT_EXECUTABLE));
        } catch (WsdlUrlMissingException e) {
            throw new BadRequestException(new Error(ERROR_WSDL_URL_MISSING));
        } catch (ServiceDescriptionNotFoundException e) {
            throw new ResourceNotFoundException();
        } catch (WrongServiceDescriptionTypeException e) {
            throw new BadRequestException(new Error(ERROR_WRONG_TYPE));
        } catch (UnhandledWarningsException e) {
            throw new BadRequestException(e);
        } catch (ServiceAlreadyExistsException e) {
            throw new ConflictException(e);
        } catch (InvalidUrlException e) {
            throw new BadRequestException();
        } catch (WsdlUrlAlreadyExistsException e) {
            throw new ConflictException(new Error(ServiceDescriptionsApiController.ERROR_WSDL_EXISTS));
        }
        return new ResponseEntity<>(serviceDescription, HttpStatus.OK);
    }

    /**
     * Returns one service description, using primary key id.
     * {@inheritDoc}
     *
     * @param id primary key of service description
     */
    @Override
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ResponseEntity<ServiceDescription> getServiceDescription(String id) {
        ServiceDescriptionType serviceDescriptionType =
                getServiceDescriptionType(id);
        return new ResponseEntity<>(
                serviceDescriptionConverter.convert(serviceDescriptionType),
                HttpStatus.OK);
    }

    /**
     * Returns services of one service description.
     * Id = primary key of service description.
     * {@inheritDoc}
     *
     * @param id primary key of service description
     */
    @Override
    @PreAuthorize("hasAuthority('VIEW_CLIENT_SERVICES')")
    public ResponseEntity<List<Service>> getServiceDescriptionServices(String id) {
        ServiceDescriptionType serviceDescriptionType =
                getServiceDescriptionType(id);
        ClientId clientId = serviceDescriptionType.getClient().getIdentifier();
        List<Service> services = serviceDescriptionType.getService().stream()
                .map(serviceType -> serviceConverter.convert(serviceType, clientId))
                .collect(toList());
        return new ResponseEntity<>(services, HttpStatus.OK);
    }

    /**
     * return matching ServiceDescriptionType, or throw ResourceNotFoundException
     */
    private ServiceDescriptionType getServiceDescriptionType(String id) {
        Long serviceDescriptionId = FormatUtils.parseLongIdOrThrowNotFound(id);
        ServiceDescriptionType serviceDescriptionType =
                serviceDescriptionService.getServiceDescriptiontype(serviceDescriptionId);
        if (serviceDescriptionType == null) {
            throw new ResourceNotFoundException();
        }
        return serviceDescriptionType;
    }


}
