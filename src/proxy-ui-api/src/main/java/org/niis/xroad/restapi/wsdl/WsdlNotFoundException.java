/**
 *  The MIT License
 *  Copyright (c) 2018 Estonian Information System Authority (RIA),
 *  Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 *  Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package org.niis.xroad.restapi.wsdl;

import org.niis.xroad.restapi.exceptions.FatalError;
import org.niis.xroad.restapi.openapi.ServiceDescriptionsApiController;
import org.niis.xroad.restapi.service.ServiceException;

/**
 * Thrown if WSDL file is not found
 */
public class WsdlNotFoundException extends ServiceException {

    // To do: remove from ServiceDescriptionsApiController?
    public static final String ERROR_WSDL_DOWNLOAD_FAILED =
            ServiceDescriptionsApiController.ERROR_WSDL_DOWNLOAD_FAILED;

    public WsdlNotFoundException(Throwable cause) {
        super(cause, new FatalError(ERROR_WSDL_DOWNLOAD_FAILED));
    }
}
