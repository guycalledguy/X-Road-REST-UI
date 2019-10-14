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

import org.niis.xroad.restapi.exceptions.DeviationAwareRuntimeException;
import org.niis.xroad.restapi.exceptions.FatalError;
import org.niis.xroad.restapi.exceptions.Warning;

import java.util.Collection;

/**
 * Root class of exceptions thrown from openapi controllers and converters,
 * that map into specific response http statuses. Mapping is done
 * with Spring's @ResponseStatus annotation
 */
public class OpenApiException extends DeviationAwareRuntimeException {
    public OpenApiException() {
        super();
    }

    public OpenApiException(String msg) {
        super(msg);
    }

    public OpenApiException(String msg, FatalError fatalError) {
        super(msg, fatalError);
    }

    public OpenApiException(String msg, Throwable t) {
        super(msg, t);
    }

    public OpenApiException(String msg, Throwable t, FatalError fatalError) {
        super(msg, t, fatalError);
    }

    public OpenApiException(String msg, Throwable t, FatalError fatalError, Collection<Warning> warnings) {
        super(msg, t, fatalError, warnings);
    }

    public OpenApiException(FatalError fatalError) {
        super(fatalError);
    }

    public OpenApiException(FatalError fatalError, Collection<Warning> warnings) {
        super(fatalError, warnings);
    }

    public OpenApiException(Throwable t) {
        super(t);
    }

    public OpenApiException(Throwable t, FatalError fatalError) {
        super(t, fatalError);
    }

    public OpenApiException(Throwable t, FatalError fatalError, Collection<Warning> warnings) {
        super(t, fatalError, warnings);
    }
}
