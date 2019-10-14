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

import org.niis.xroad.restapi.exceptions.DeviationAware;
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
import org.niis.xroad.restapi.exceptions.WarningDeviation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collection;

/**
 * Thrown if internal server error occurs.
 * Results in http 500 INTERNAL_SERVER_ERROR
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerErrorException extends OpenApiException {

    public InternalServerErrorException(DeviationAware deviations) {
        super(deviations.getErrorDeviation(), deviations.getWarningDeviations());
    }

    public InternalServerErrorException() {
    }

    public InternalServerErrorException(String msg) {
        super(msg);
    }

    public InternalServerErrorException(String msg, Throwable t) {
        super(msg, t);
    }

    public InternalServerErrorException(String msg, ErrorDeviation errorDeviation) {
        super(msg, errorDeviation);
    }

    public InternalServerErrorException(String msg, Throwable t, ErrorDeviation errorDeviation) {
        super(msg, t, errorDeviation);
    }

    public InternalServerErrorException(Throwable t, ErrorDeviation errorDeviation,
            Collection<WarningDeviation> warningDeviations) {
        super(t, errorDeviation, warningDeviations);
    }

    public InternalServerErrorException(ErrorDeviation errorDeviation, Collection<WarningDeviation> warningDeviations) {
        super(errorDeviation, warningDeviations);
    }

    public InternalServerErrorException(ErrorDeviation errorDeviation) {
        super(errorDeviation);
    }

    public InternalServerErrorException(Throwable t, ErrorDeviation errorDeviation) {
        super(t, errorDeviation);
    }

}
